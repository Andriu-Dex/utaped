package ec.edu.uta.utaped.workflow;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.documents.*;
import ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.planning.WorkPlanService;
import ec.edu.uta.utaped.signing.*;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ReviewService {
    public record Decision(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Pattern(regexp="COMPLETE|RETURN") String action,
        @NotNull @Size(max=4000) String message,@NotNull UUID requestKey) {}
    public record Observation(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=4000) String message,
        @PositiveOrZero Integer pageIndex,@DecimalMin("0") @DecimalMax("1") Double x,@DecimalMin("0") @DecimalMax("1") Double y,
        @DecimalMin(value="0",inclusive=false) @DecimalMax("1") Double width,@DecimalMin(value="0",inclusive=false) @DecimalMax("1") Double height) {}
    public record Task(UUID id,UUID reviewId,UUID documentId,String title,String groupName,String periodName,String actorName,String stageLabel,String action,
        boolean requiresSignature,String status,long rowVersion,int roundNumber,int position) {}
    public record State(Map<String,Object> review,List<Map<String,Object>> tasks,List<Map<String,Object>> observations,int pageCount,String pdfHash,boolean canAct) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final WorkPlanService plans;private final ArtifactService artifacts;
    private final StoredFiles files;private final AuditService audit;private final CertificateBindings bindings;private final boolean allowHttp;
    private final JsonMapper json=JsonMapper.builder().build();private final Semaphore signing=new Semaphore(1);
    public ReviewService(JdbcTemplate jdbc,Accounts accounts,WorkPlanService plans,ArtifactService artifacts,StoredFiles files,AuditService audit,CertificateBindings bindings,@Value("${app.signing.allow-http:false}") boolean allowHttp) {
        this.jdbc=jdbc;this.accounts=accounts;this.plans=plans;this.artifacts=artifacts;this.files=files;this.audit=audit;this.bindings=bindings;this.allowHttp=allowHttp;
    }
    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT,message); }
    private Snapshot frozen(UUID doc,UUID artifact) {
        return jdbc.query("SELECT snapshot::text FROM document_artifact WHERE work_plan_id=? AND id=?",(rs,n)->json.readValue(rs.getString(1),Snapshot.class),doc,artifact).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Artefacto no disponible."));
    }
    public List<Blocker> submissionBlockers(UUID doc,UUID artifact,String email) {
        var plan=plans.get(doc,email);if(plan.scopeId()==null) return List.of();
        var workflow=frozen(doc,artifact).workflow();var blockers=new ArrayList<Blocker>();var actors=new HashSet<UUID>();actors.add(plan.teacherId());
        if(workflow==null||workflow.stages().isEmpty()) return List.of(new Blocker("Flujo","Configure y publique un flujo con participantes antes de firmar el Plan."));
        var slots=artifacts.list(doc,email).stream().filter(a->a.id().equals(artifact)).findFirst().orElseThrow().signatureSlots();
        int count=0;
        for(var stage:workflow.stages()) {
            if(stage.participants().isEmpty()) blockers.add(new Blocker("Flujo",stage.label()+": necesita al menos una persona responsable de registrar la decisión."));
            for(var person:stage.participants()) {
                count++;
                if(!actors.add(person.id())) blockers.add(new Blocker("Flujo","Una misma persona no puede elaborar y revisar, ni cubrir dos etapas incompatibles."));
                if(!person.active()||jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id=? AND active",Integer.class,person.id())==0) blockers.add(new Blocker("Flujo",stage.label()+": participante inactivo."));
                if(stage.requiresSignature()&&slots.stream().noneMatch(s->person.id().equals(s.actorId())&&s.action().equals(DocumentWorkflowSnapshot.signatureAction(stage.action()))&&s.bounds()!=null)) blockers.add(new Blocker("Ubicación",stage.label()+": genere un artefacto con ubicación real de firma."));
            }
        }
        if(count>100) blockers.add(new Blocker("Flujo","El flujo supera el límite técnico de 100 participantes."));
        return List.copyOf(blockers);
    }
    // Called atomically by the author signature service after persisting the immutable signed PDF.
    @Transactional public void start(UUID doc,UUID artifact,UUID signedId,String email) {
        var plan=plans.get(doc,email);if(plan.scopeId()==null) return;
        if(!submissionBlockers(doc,artifact,email).isEmpty()) throw conflict("El flujo requiere correcciones antes del envío.");
        var signed=jdbc.queryForMap("SELECT file_id FROM signed_document_artifact WHERE id=? AND work_plan_id=? AND artifact_id=?",signedId,doc,artifact);
        UUID review=UUID.randomUUID();jdbc.update("INSERT INTO document_review(id,work_plan_id,artifact_id,author_signature_id,current_file_id,round_number,status) VALUES (?,?,?,?,?,?,'IN_REVIEW')",review,doc,artifact,signedId,signed.get("file_id"),plan.reviewRound());
        int position=0;UUID firstActor=null;
        for(var stage:frozen(doc,artifact).workflow().stages()) for(var person:stage.participants()) {
            jdbc.update("INSERT INTO review_task(id,review_id,actor_id,actor_name,stage_label,stage_action,position,requires_signature,signature_action,status) VALUES (?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),review,person.id(),person.name(),stage.label(),stage.action(),position,stage.requiresSignature(),DocumentWorkflowSnapshot.signatureAction(stage.action()),position==0?"ACTIVE":"PENDING");
            if(position++==0) firstActor=person.id();
        }
        jdbc.update("UPDATE work_plan SET document_state='IN_REVIEW',row_version=row_version+1,updated_at=now() WHERE id=?",doc);
        audit.record(accounts.current(email).id(),"REVIEW_TASK_ASSIGNED",doc,firstActor);
    }
    public List<Task> inbox(String email) {
        return jdbc.query("""
            SELECT t.*,r.work_plan_id,r.round_number,w.title,g.name AS group_name,p.name AS period_name
            FROM review_task t JOIN document_review r ON r.id=t.review_id JOIN work_plan w ON w.id=r.work_plan_id
            JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id
            WHERE t.actor_id=? AND t.status IN ('ACTIVE','COMPLETED','RETURNED') AND g.active ORDER BY (t.status='ACTIVE') DESC,r.created_at DESC,t.position LIMIT 200
            """,(rs,n)->new Task(rs.getObject("id",UUID.class),rs.getObject("review_id",UUID.class),rs.getObject("work_plan_id",UUID.class),rs.getString("title"),rs.getString("group_name"),rs.getString("period_name"),rs.getString("actor_name"),rs.getString("stage_label"),rs.getString("stage_action"),rs.getBoolean("requires_signature"),rs.getString("status"),rs.getLong("row_version"),rs.getInt("round_number"),rs.getInt("position")),accounts.current(email).id());
    }
    private Map<String,Object> authorized(UUID review,String email) {
        var record=jdbc.queryForList("SELECT * FROM document_review WHERE id=?",review).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Revisión no disponible."));
        plans.get((UUID)record.get("work_plan_id"),email);return record;
    }
    @Transactional(readOnly=true) public State state(UUID review,String email) {
        var record=authorized(review,email);var file=files.metadata((UUID)record.get("current_file_id"));
        var tasks=jdbc.queryForList("SELECT id,actor_id,actor_name,stage_label,stage_action,position,requires_signature,status,row_version,acted_at FROM review_task WHERE review_id=? ORDER BY position",review);
        var observations=jdbc.queryForList("SELECT o.*,u.display_name AS actor_name FROM review_observation o JOIN app_user u ON u.id=o.actor_id WHERE o.review_id=? ORDER BY o.created_at,o.id",review);
        boolean canAct=jdbc.queryForObject("SELECT count(*) FROM work_plan w JOIN academic_period p ON p.id=w.period_id WHERE w.id=? AND p.status='ACTIVE'",Integer.class,record.get("work_plan_id"))>0&&tasks.stream().anyMatch(t->accounts.current(email).id().equals(t.get("actor_id"))&&t.get("status").equals("ACTIVE"));
        return new State(record,tasks,observations,file.pageCount(),file.sha256(),canAct);
    }
    public List<Map<String,Object>> history(UUID doc,String email) { plans.get(doc,email);return jdbc.queryForList("SELECT id,round_number,status,created_at,closed_at FROM document_review WHERE work_plan_id=? ORDER BY round_number DESC",doc); }
    public byte[] content(UUID review,String email) {
        var record=authorized(review,email);
        if(!record.get("status").equals("APPROVED")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La descarga final está disponible después de la aprobación.");
        return files.read((UUID)record.get("current_file_id"));
    }
    public byte[] page(UUID review,int index,String email) { return artifacts.renderPage(files.read((UUID)authorized(review,email).get("current_file_id")),index); }
    private Map<String,Object> lockTask(UUID task,String email,long rowVersion) {
        var initial=jdbc.queryForList("SELECT review_id FROM review_task WHERE id=? AND actor_id=?",task,accounts.current(email).id()).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Tarea no disponible."));
        jdbc.queryForList("SELECT id FROM document_review WHERE id=? FOR UPDATE",initial.get("review_id"));
        var record=jdbc.queryForMap("SELECT t.*,r.work_plan_id,r.artifact_id,r.current_file_id,r.status AS review_status FROM review_task t JOIN document_review r ON r.id=t.review_id WHERE t.id=?",task);
        authorized((UUID)record.get("review_id"),email);
        plans.lock((UUID)record.get("work_plan_id"),email);
        if(!"ACTIVE".equals(record.get("status"))||!"IN_REVIEW".equals(record.get("review_status"))) throw conflict("Esta tarea ya no está activa.");
        if(((Number)record.get("row_version")).longValue()!=rowVersion) throw conflict("La tarea cambió. Recargue antes de actuar.");return record;
    }
    @Transactional public void observe(UUID task,String email,Observation data) {
        var record=lockTask(task,email,data.rowVersion());var meta=files.metadata((UUID)record.get("current_file_id"));
        boolean geometry=data.x()!=null||data.y()!=null||data.width()!=null||data.height()!=null;
        if(data.pageIndex()!=null&&data.pageIndex()>=meta.pageCount()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Página no disponible.");
        if(geometry&&(data.pageIndex()==null||data.x()==null||data.y()==null||data.width()==null||data.height()==null||!Double.isFinite(data.x()+data.y()+data.width()+data.height())||data.x()<0||data.y()<0||data.width()<=0||data.height()<=0||data.x()+data.width()>1.000001||data.y()+data.height()>1.000001)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El resaltado debe quedar dentro de la página.");
        jdbc.update("INSERT INTO review_observation(id,review_id,task_id,actor_id,message,page_index,x,y,width,height) VALUES (?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),record.get("review_id"),task,accounts.current(email).id(),data.message().trim(),data.pageIndex(),data.x(),data.y(),data.width(),data.height());
        jdbc.update("UPDATE review_task SET row_version=row_version+1 WHERE id=?",task);audit.record(accounts.current(email).id(),"REVIEW_OBSERVATION_ADDED",(UUID)record.get("work_plan_id"));
    }
    @Transactional public void decide(UUID task,String email,Decision data) {
        if(idempotent(task,email,data.requestKey(),data.action())) return;
        var record=lockTask(task,email,data.rowVersion());
        if(data.action().equals("RETURN")) {
            if(data.message().isBlank()&&jdbc.queryForObject("SELECT count(*) FROM review_observation WHERE task_id=?",Integer.class,task)==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Registre al menos una observación para devolver el documento.");
            if(!data.message().isBlank()) jdbc.update("INSERT INTO review_observation(id,review_id,task_id,actor_id,message) VALUES (?,?,?,?,?)",UUID.randomUUID(),record.get("review_id"),task,accounts.current(email).id(),data.message().trim());
            jdbc.update("UPDATE review_task SET status='RETURNED',row_version=row_version+1,acted_at=now(),request_key=? WHERE id=?",data.requestKey(),task);
            jdbc.update("UPDATE review_task SET status='CANCELLED',row_version=row_version+1 WHERE review_id=? AND status='PENDING'",record.get("review_id"));
            jdbc.update("UPDATE document_review SET status='RETURNED',closed_at=now() WHERE id=?",record.get("review_id"));
            jdbc.update("UPDATE work_plan SET document_state='DRAFT',review_round=review_round+1,row_version=row_version+1,updated_at=now() WHERE id=?",record.get("work_plan_id"));
            var owner=jdbc.queryForObject("SELECT COALESCE(s.principal_user_id,w.teacher_id) FROM work_plan w LEFT JOIN organizational_scope s ON s.id=w.scope_id WHERE w.id=?",UUID.class,record.get("work_plan_id"));
            audit.record(accounts.current(email).id(),"DOCUMENT_RETURNED",(UUID)record.get("work_plan_id"),owner);return;
        }
        if((boolean)record.get("requires_signature")) throw conflict("Esta etapa requiere firma electrónica. Use la acción de firma.");
        advance(record,task,email,data.requestKey(),null,null);
    }
    private boolean idempotent(UUID task,String email,UUID key,String action) {
        var records=jdbc.queryForList("SELECT id,review_id,status FROM review_task WHERE actor_id=? AND request_key=?",accounts.current(email).id(),key);
        if(records.isEmpty()) return false;
        var previous=records.getFirst();if(!task.equals(previous.get("id"))||!previous.get("status").equals(action.equals("RETURN")?"RETURNED":"COMPLETED")) throw conflict("La solicitud corresponde a otra decisión.");
        authorized((UUID)previous.get("review_id"),email);return true;
    }
    private void advance(Map<String,Object> record,UUID task,String email,UUID key,UUID output,UUID binding) {
        jdbc.update("UPDATE review_task SET status='COMPLETED',row_version=row_version+1,acted_at=now(),request_key=?,output_file_id=?,binding_id=? WHERE id=?",key,output,binding,task);
        if(output!=null) jdbc.update("UPDATE document_review SET current_file_id=? WHERE id=?",output,record.get("review_id"));
        var next=jdbc.queryForList("SELECT id,actor_id FROM review_task WHERE review_id=? AND status='PENDING' ORDER BY position LIMIT 1",record.get("review_id"));
        if(next.isEmpty()) {
            jdbc.update("UPDATE document_review SET status='APPROVED',closed_at=now() WHERE id=?",record.get("review_id"));
            jdbc.update("UPDATE work_plan SET document_state='APPROVED',row_version=row_version+1,updated_at=now() WHERE id=?",record.get("work_plan_id"));
            var owner=jdbc.queryForObject("SELECT COALESCE(s.principal_user_id,w.teacher_id) FROM work_plan w LEFT JOIN organizational_scope s ON s.id=w.scope_id WHERE w.id=?",UUID.class,record.get("work_plan_id"));
            audit.record(accounts.current(email).id(),"DOCUMENT_APPROVED",(UUID)record.get("work_plan_id"),owner);
        } else { jdbc.update("UPDATE review_task SET status='ACTIVE',row_version=row_version+1 WHERE id=?",next.getFirst().get("id"));audit.record(accounts.current(email).id(),"REVIEW_TASK_ASSIGNED",(UUID)record.get("work_plan_id"),(UUID)next.getFirst().get("actor_id")); }
        audit.record(accounts.current(email).id(),"REVIEW_TASK_COMPLETED",(UUID)record.get("work_plan_id"));
    }
    @Transactional public void sign(UUID task,String email,long rowVersion,String inputHash,UUID key,byte[] body,boolean secure) {
        try(var material=new SigningMaterial(body)) {
            if(!secure&&!allowHttp) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La firma requiere HTTPS.");
            if(idempotent(task,email,key,"COMPLETE")) return;
            var record=lockTask(task,email,rowVersion);
            if(!(boolean)record.get("requires_signature")) throw conflict("Esta etapa no requiere firma.");
            var meta=files.metadata((UUID)record.get("current_file_id"));if(!meta.sha256().equals(inputHash)) throw conflict("El PDF cambió. Recargue antes de firmar.");
            var binding=bindings.current(accounts.current(email).id());if(binding==null) throw conflict("Administración debe vincular su certificado público.");
            var source=jdbc.queryForObject("SELECT signature_slots::text FROM document_artifact WHERE id=?",String.class,record.get("artifact_id"));
            var slot=Arrays.stream(json.readValue(source,SignatureSlot[].class)).filter(s->accounts.current(email).id().equals(s.actorId())&&record.get("signature_action").equals(s.action())).findFirst().orElseThrow(()->conflict("Artefacto sin ubicación válida de firma."));
            if(!signing.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Hay otra firma en curso. Intente nuevamente.");
            byte[] output;
            try { output=new Pkcs12PdfSigner().signPinnedAdditional(files.read(meta.id()),material.container,material.password,binding.fingerprint(),Instant.now(),slot,accounts.current(email).displayName()); }
            catch(Pkcs12PdfSigner.SigningException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); } finally { signing.release(); }
            var file=files.put(output,"documento-firmado.pdf",meta.pageCount());advance(record,task,email,key,file.id(),binding.id());
        }
    }
}
