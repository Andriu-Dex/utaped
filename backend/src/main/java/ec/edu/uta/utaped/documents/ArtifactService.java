package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.planning.ActivityService;
import ec.edu.uta.utaped.planning.WorkPlanService;
import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.concurrent.Semaphore;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ArtifactService {
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final ActivityService activities;
    private final AttachmentService attachments;private final StoredFiles files;private final T1Template template;
    private final T1PdfEngine engine;private final AuditService audit;private final Accounts accounts;
    private final DocumentWorkflowSnapshot workflows;
    private final JsonMapper json=JsonMapper.builder().build();
    private final Semaphore generation=new Semaphore(1),rendering=new Semaphore(2);
    public ArtifactService(JdbcTemplate jdbc,WorkPlanService plans,ActivityService activities,AttachmentService attachments,
        StoredFiles files,T1Template template,T1PdfEngine engine,AuditService audit,Accounts accounts,DocumentWorkflowSnapshot workflows) {
        this.jdbc=jdbc;this.plans=plans;this.activities=activities;this.attachments=attachments;this.files=files;
        this.template=template;this.engine=engine;this.audit=audit;this.accounts=accounts;
        this.workflows=workflows;
    }
    public Readiness readiness(UUID id,String email) {
        var plan=plans.get(id,email);var matrix=activities.get(id,email);var annexes=attachments.get(id,email);
        var blockers=new ArrayList<Blocker>();
        if(plan.institutionalUnit().isBlank()) blockers.add(new Blocker("Información general","Indique la unidad institucional."));
        if(plan.justification().isBlank()) blockers.add(new Blocker("Contenido","Complete la justificación."));
        if(plan.objective().isBlank()) blockers.add(new Blocker("Contenido","Complete el objetivo."));
        if(jdbc.queryForObject("SELECT count(*) FROM work_plan_matrix WHERE work_plan_id=?",Integer.class,id)==0 || matrix.activities().isEmpty())
            blockers.add(new Blocker("Actividades y matriz","Guarde al menos una actividad completa."));
        var members=new HashSet<UUID>();matrix.members().forEach(m->members.add(m.id()));
        for(var a:matrix.activities()) {
            boolean invalid=a.startsOn()==null || a.endsOn()==null || a.responsibleIds().isEmpty() || a.resources().isEmpty() || a.means().isEmpty();
            invalid|=!members.containsAll(a.responsibleIds());
            invalid|=a.collective() && (matrix.collectiveLabel().isBlank() || !new HashSet<>(a.responsibleIds()).equals(members));
            if(a.startsOn()!=null && a.endsOn()!=null) {
                invalid|=a.startsOn().isAfter(a.endsOn()) || a.startsOn().isBefore(matrix.periodStartsOn()) || a.endsOn().isAfter(matrix.periodEndsOn());
                invalid|=matrix.restrictHolidayEndpoints() && matrix.holidays().stream().anyMatch(h->h.date().equals(a.startsOn()) || h.date().equals(a.endsOn()));
            }
            invalid|=java.util.stream.Stream.concat(a.resources().stream(),a.means().stream()).anyMatch(c->c.label()==null || c.label().isBlank());
            if(invalid) blockers.add(new Blocker("Actividades y matriz","Revise fechas, responsables y selecciones de: "+a.title()));
        }
        // Mandatory catalog changes also apply when a draft is temporarily read-only.
        for(var d:matrix.definitions()) if(d.active() && d.mandatory() && matrix.activities().stream().noneMatch(a->d.id().equals(a.catalogId())))
            blockers.add(new Blocker("Actividades y matriz","Falta la actividad obligatoria: "+d.title()));
        if(annexes.enabled() && annexes.items().isEmpty()) blockers.add(new Blocker("Anexos","Agregue un PDF o seleccione No en anexos."));
        return new Readiness(plan.rowVersion(),blockers.isEmpty(),List.copyOf(blockers));
    }
    private Snapshot snapshot(UUID id,String email) {
        var plan=plans.get(id,email);var matrix=activities.get(id,email);var annexes=attachments.get(id,email);
        var names=new HashMap<UUID,String>();matrix.members().forEach(m->names.put(m.id(),m.name()));
        var rows=matrix.activities().stream().map(a->new Activity(a.title(),a.category(),a.startsOn()==null?null:a.startsOn().toString(),a.endsOn()==null?null:a.endsOn().toString(),
            a.collective()?matrix.collectiveLabel():String.join("\n",a.responsibleIds().stream().map(m->names.getOrDefault(m,"Responsable no disponible")).toList()),
            a.responsibleIds(),a.resources().stream().map(c->c.label()).toList(),a.means().stream().map(c->c.label()).toList())).toList();
        return new Snapshot(plan,matrix.source(),matrix.elaboratedBy(),annexes.privacyNoticeEnabled(),rows,annexes.items(),workflows.capture(plan.groupId()));
    }
    private String hash(Snapshot snapshot) { return StoredFiles.hash(json.writeValueAsString(snapshot).getBytes(StandardCharsets.UTF_8)); }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public List<Artifact> list(UUID id,String email) {
        String current=hash(snapshot(id,email));
        return jdbc.query("SELECT * FROM document_artifact WHERE work_plan_id=? ORDER BY created_at DESC,id",(rs,n)->{
            var pages=List.of(json.readValue(rs.getString("pages"),Page[].class));
            return new Artifact(rs.getObject("id",UUID.class),rs.getLong("source_row_version"),rs.getString("source_hash"),rs.getString("template_version"),pages.size(),pages,
                List.of(json.readValue(rs.getString("signature_slots"),SignatureSlot[].class)),rs.getObject("created_at",OffsetDateTime.class),
                current.equals(rs.getString("source_hash")) && template.version().equals(rs.getString("template_version")));
        },id);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ) public Artifact generate(UUID id,String email,Generate input) {
        var state=attachments.lock(id,email);
        if(!plans.get(id,email).documentState().equals("DRAFT")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo los borradores permiten generar una nueva previsualización.");
        if(state.rowVersion()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió. Recargue antes de generar la previsualización.");
        var ready=readiness(id,email);
        if(!ready.ready()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete los requisitos de preparación antes de generar el PDF.");
        var snapshot=snapshot(id,email);String sha=hash(snapshot);
        var existing=list(id,email).stream().filter(a->a.sourceHash().equals(sha) && a.templateVersion().equals(template.version())).findFirst();
        if(existing.isPresent()) return existing.get();
        if(!generation.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Hay otra generación en curso. Intente nuevamente en unos momentos.");
        try {
            if(snapshot.attachments().stream().mapToInt(AttachmentModels.Attachment::pageCount).sum()>450) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Los anexos superan el límite técnico de 450 páginas por previsualización.");
            Generated generated;
            try { generated=engine.generate(snapshot); }
            catch(IllegalStateException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo generar el PDF. Revise el contenido o intente nuevamente."); }
            var file=files.put(generated.pdf(),"plan-trabajo-t1.pdf",generated.pages().size());UUID artifactId=UUID.randomUUID();
            jdbc.update("INSERT INTO document_artifact(id,work_plan_id,actor_id,source_row_version,source_hash,template_version,file_id,snapshot,pages,signature_slots) VALUES (?,?,?,?,?,?,?,?::jsonb,?::jsonb,?::jsonb)",
                artifactId,id,accounts.current(email).id(),state.rowVersion(),sha,template.version(),file.id(),json.writeValueAsString(snapshot),json.writeValueAsString(generated.pages()),json.writeValueAsString(generated.signatureSlots()));
            audit.record(accounts.current(email).id(),"T1_PREVIEW_GENERATED",id);
            return list(id,email).stream().filter(a->a.id().equals(artifactId)).findFirst().orElseThrow();
        } finally { generation.release(); }
    }
    private UUID fileId(UUID id,UUID artifactId,String email) {
        plans.get(id,email);
        return jdbc.query("SELECT file_id FROM document_artifact WHERE id=? AND work_plan_id=?",(rs,n)->rs.getObject(1,UUID.class),artifactId,id).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Previsualización no disponible."));
    }
    public byte[] content(UUID id,UUID artifactId,String email) { return files.read(fileId(id,artifactId,email)); }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public SignaturePreparation signaturePreparation(UUID id,UUID artifactId,String email) {
        var plan=plans.get(id,email);
        var artifact=list(id,email).stream().filter(a->a.id().equals(artifactId)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Previsualización no disponible."));
        var stored=jdbc.queryForMap("SELECT snapshot::text FROM document_artifact WHERE id=? AND work_plan_id=?",artifactId,id);
        var frozen=json.readValue((String)stored.get("snapshot"),Snapshot.class);
        var blockers=new ArrayList<Blocker>();
        if(!artifact.current()) blockers.add(new Blocker("Artefacto","El contenido, la plantilla o la configuración del flujo cambió. Genere una nueva previsualización."));
        if(!plan.editable()) blockers.add(new Blocker("Período","El Plan no está habilitado para elaboración en este momento."));
        blockers.addAll(readiness(id,email).blockers());
        if(frozen.workflow()==null) blockers.add(new Blocker("Flujo","Esta previsualización no contiene una revisión de flujo configurada."));
        else for(var stage:frozen.workflow().stages()) {
            if((!stage.recipientKind().equals("COLLEGIATE") || stage.requiresSignature()) && stage.participants().isEmpty())
                blockers.add(new Blocker("Flujo",stage.label()+": no hay participantes resueltos."));
            if(stage.participants().stream().anyMatch(p->!p.active())) blockers.add(new Blocker("Flujo",stage.label()+": contiene participantes inactivos."));
        }
        blockers.add(new Blocker("Validación institucional","Pendiente confirmar etapas, participantes y condición de avance por grupo. La configuración no habilita aprobación."));
        blockers.add(new Blocker("Política de firma","Pendiente definir vinculación certificado–usuario, confianza, revocación y perfil de firma antes de habilitar la carga de .p12/.pfx."));
        // Reading checks the stored hash. No secret upload or state transition is exposed.
        String pdfHash=StoredFiles.hash(content(id,artifactId,email));
        return new SignaturePreparation(id,artifactId,plan.rowVersion(),pdfHash,artifact.current(),frozen.workflow(),"PKCS12",false,List.copyOf(blockers));
    }
    public byte[] page(UUID id,UUID artifactId,int pageIndex,String email) {
        byte[] bytes=content(id,artifactId,email);
        if(!rendering.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Previsualización ocupada. Intente nuevamente.");
        try(var pdf=Loader.loadPDF(bytes)) {
            if(pageIndex<0 || pageIndex>=pdf.getNumberOfPages()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Página no disponible.");
            // Bound raster dimensions independently of uploaded PDF media boxes.
            var box=pdf.getPage(pageIndex).getCropBox();float scale=Math.min(1.5f,1800f/Math.max(box.getWidth(),box.getHeight()));
            var out=new ByteArrayOutputStream();ImageIO.write(new PDFRenderer(pdf).renderImage(pageIndex,scale),"png",out);return out.toByteArray();
        } catch(IOException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo visualizar la página."); }
        finally { rendering.release(); }
    }
}
