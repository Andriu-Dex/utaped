package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.documents.*;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EvidenceService {
    public record Evidence(UUID id,UUID activityId,int mediumIndex,int version,String originalName,long sizeBytes,int pageCount,String sha256,String uploadedBy,OffsetDateTime uploadedAt,String evidenceKind,String externalUrl,String referenceTitle) {}
    public record Medium(int index,String label,List<Evidence> versions,String evidenceKind) {}
    public record Reference(@NotNull @PositiveOrZero Integer expectedVersion,@NotBlank @Size(max=200) String title,@NotBlank @Size(max=2048) String url) {}
    public record Responsible(UUID id,String name,LocalDate deadline,long extensionVersion) {}
    public record Execution(UUID id,String title,LocalDate startsOn,LocalDate endsOn,LocalDate deadline,long extensionVersion,boolean canUpload,boolean complete,List<Medium> media,List<Responsible> responsible) {}
    public record State(UUID documentId,String documentState,boolean coordinator,int requiredMedia,int coveredMedia,int completedActivities,List<Execution> activities,long maxFileBytes) {}
    public record Extension(@NotNull @PositiveOrZero Long rowVersion,@NotNull UUID userId,@NotNull LocalDate endsOn,@NotBlank @Size(max=1000) String reason) {}
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final ActivityService activities;private final StoredFiles files;
    private final PdfValidation validation;private final Accounts accounts;private final AuditService audit;private final ZoneId zone;private final long maxBytes;private final int extensionDays;
    public EvidenceService(JdbcTemplate jdbc,WorkPlanService plans,ActivityService activities,StoredFiles files,PdfValidation validation,Accounts accounts,AuditService audit,
        @Value("${app.planning.time-zone:America/Guayaquil}") String zone,@Value("${app.documents.max-file-bytes:10485760}") long maxBytes,@Value("${app.planning.max-extension-days:3}") int extensionDays) {
        this.jdbc=jdbc;this.plans=plans;this.activities=activities;this.files=files;this.validation=validation;this.accounts=accounts;this.audit=audit;this.zone=ZoneId.of(zone);this.maxBytes=maxBytes;this.extensionDays=extensionDays;
    }
    private List<Evidence> evidence(UUID doc) {
        return jdbc.query("SELECT e.*,f.original_name,f.size_bytes,f.page_count,f.sha256,u.display_name FROM execution_evidence e LEFT JOIN stored_file f ON f.id=e.file_id JOIN app_user u ON u.id=e.uploaded_by WHERE e.work_plan_id=? ORDER BY e.version_number DESC,e.id",(rs,n)->new Evidence(rs.getObject("id",UUID.class),rs.getObject("activity_id",UUID.class),rs.getInt("medium_index"),rs.getInt("version_number"),rs.getString("original_name"),rs.getLong("size_bytes"),rs.getInt("page_count"),rs.getString("sha256"),rs.getString("display_name"),rs.getObject("uploaded_at",OffsetDateTime.class),rs.getString("evidence_kind"),rs.getString("external_url"),rs.getString("reference_title")),doc);
    }
    public State get(UUID doc,String email) {
        var plan=plans.get(doc,email);var actor=accounts.current(email);var matrix=activities.get(doc,email);var evidence=evidence(doc);
        boolean coordinator=plan.documentType().equals("T1")&&plan.archivedAt()==null&&plan.scopeId()!=null&&jdbc.queryForObject("SELECT count(*) FROM scope_member WHERE scope_id=? AND user_id=? AND active AND membership_role='COORDINATOR'",Integer.class,plan.scopeId(),actor.id())>0;
        var rows=new ArrayList<Execution>();int required=0,covered=0,completed=0;
        for(var activity:matrix.activities()) {
            var extension=jdbc.queryForList("SELECT ends_on,row_version FROM deadline_extension WHERE work_plan_id=? AND activity_id=? AND user_id=?",doc,activity.id(),actor.id());
            LocalDate deadline=extension.isEmpty()?activity.endsOn():((java.sql.Date)extension.getFirst().get("ends_on")).toLocalDate();
            var media=new ArrayList<Medium>();
            for(int i=0;i<activity.means().size();i++) {
                int index=i;var versions=evidence.stream().filter(e->e.activityId().equals(activity.id())&&e.mediumIndex()==index).toList();
                media.add(new Medium(i,activity.means().get(i).label(),versions,activity.means().get(i).effectiveEvidenceKind()));required++;if(!versions.isEmpty()) covered++;
            }
            boolean complete=!media.isEmpty()&&media.stream().allMatch(m->!m.versions().isEmpty());if(complete) completed++;
            boolean open=deadline!=null&&Instant.now().isBefore(deadline.plusDays(1).atStartOfDay(zone).toInstant());
            var responsible=new ArrayList<Responsible>();
            for(var user:activity.responsibleIds()) {
                var extra=jdbc.queryForList("SELECT ends_on,row_version FROM deadline_extension WHERE work_plan_id=? AND activity_id=? AND user_id=?",doc,activity.id(),user);
                String name=jdbc.queryForObject("SELECT display_name FROM app_user WHERE id=?",String.class,user);
                responsible.add(new Responsible(user,name,extra.isEmpty()?activity.endsOn():((java.sql.Date)extra.getFirst().get("ends_on")).toLocalDate(),extra.isEmpty()?0:((Number)extra.getFirst().get("row_version")).longValue()));
            }
            rows.add(new Execution(activity.id(),activity.title(),activity.startsOn(),activity.endsOn(),deadline,extension.isEmpty()?0:((Number)extension.getFirst().get("row_version")).longValue(),plan.documentType().equals("T1")&&plan.documentState().equals("APPROVED")&&plan.archivedAt()==null&&activity.responsibleIds().contains(actor.id())&&open,complete,List.copyOf(media),List.copyOf(responsible)));
        }
        return new State(doc,plan.documentState(),coordinator,required,covered,completed,List.copyOf(rows),maxBytes);
    }
    @Transactional public Evidence upload(UUID doc,UUID activity,int medium,int expectedVersion,String email,MultipartFile input) {
        plans.lock(doc,email);
        var state=get(doc,email);var row=state.activities().stream().filter(a->a.id().equals(activity)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Actividad no disponible."));
        if(!row.canUpload()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo los responsables pueden cargar evidencias de un Plan aprobado dentro del plazo.");
        if(medium<0||medium>=row.media().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Medio de verificación no disponible.");
        if(!row.media().get(medium).evidenceKind().equals("PDF")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Este medio requiere una referencia externa, no un PDF.");
        var versions=row.media().get(medium).versions();int version=versions.isEmpty()?0:versions.getFirst().version();
        if(version!=expectedVersion) throw new ResponseStatusException(HttpStatus.CONFLICT,"La evidencia cambió. Recargue antes de reemplazarla.");
        if(input.isEmpty()||input.getSize()>maxBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"El PDF supera el límite de tamaño permitido.");
        byte[] bytes;try { bytes=input.getBytes(); } catch(java.io.IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo leer el archivo."); }
        int pages=validation.pages(bytes);String name=Objects.toString(input.getOriginalFilename(),"evidencia.pdf").replaceAll("[\\p{Cntrl}/\\\\]","_");if(name.length()>200) name=name.substring(name.length()-200);
        var stored=files.put(bytes,name,pages);UUID id=UUID.randomUUID();jdbc.update("INSERT INTO execution_evidence(id,work_plan_id,activity_id,medium_index,version_number,file_id,uploaded_by) VALUES (?,?,?,?,?,?,?)",id,doc,activity,medium,version+1,stored.id(),accounts.current(email).id());
        audit.record(accounts.current(email).id(),"EXECUTION_EVIDENCE_UPLOADED",doc);return evidence(doc).stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();
    }
    @Transactional public Evidence reference(UUID doc,UUID activity,int medium,String email,Reference input) {
        plans.lock(doc,email);var row=get(doc,email).activities().stream().filter(a->a.id().equals(activity)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Actividad no disponible."));
        if(!row.canUpload()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo los responsables pueden registrar evidencias dentro del plazo de un Plan aprobado.");
        if(medium<0||medium>=row.media().size()||!row.media().get(medium).evidenceKind().equals("URL")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Este medio requiere PDF; no se admite un enlace como sustituto.");
        var versions=row.media().get(medium).versions();int version=versions.isEmpty()?0:versions.getFirst().version();
        if(version!=input.expectedVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"La referencia cambió. Recargue antes de reemplazarla.");
        String url=input.url().trim();
        try {
            var uri=new java.net.URI(url);
            if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||url.chars().anyMatch(c->Character.isWhitespace(c)||Character.isISOControl(c))) throw new IllegalArgumentException();
        } catch(java.net.URISyntaxException|IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use una URL HTTPS absoluta, sin credenciales ni espacios."); }
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO execution_evidence(id,work_plan_id,activity_id,medium_index,version_number,uploaded_by,evidence_kind,external_url,reference_title) VALUES (?,?,?,?,?,?,'URL',?,?)",id,doc,activity,medium,version+1,accounts.current(email).id(),url,input.title().trim());
        audit.record(accounts.current(email).id(),"EXECUTION_REFERENCE_REGISTERED",doc);
        return evidence(doc).stream().filter(e->e.id().equals(id)).findFirst().orElseThrow();
    }
    @Transactional public void extend(UUID doc,UUID activity,String email,Extension input) {
        plans.lock(doc,email);var plan=plans.get(doc,email);var state=get(doc,email);
        if(!state.coordinator()||!plan.documentState().equals("APPROVED")||plan.archivedAt()!=null) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el coordinador del ámbito puede conceder una extensión en ejecución.");
        var row=state.activities().stream().filter(a->a.id().equals(activity)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Actividad no disponible."));
        var responsible=row.responsible().stream().filter(r->r.id().equals(input.userId())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un responsable de esta actividad."));
        if(jdbc.queryForObject("SELECT count(*) FROM scope_member m JOIN app_user u ON u.id=m.user_id AND u.active WHERE m.scope_id=? AND m.user_id=? AND m.active",Integer.class,plan.scopeId(),input.userId())==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El responsable ya no pertenece al ámbito activo.");
        if(input.rowVersion()!=responsible.extensionVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"La extensión cambió. Recargue antes de guardar.");
        if(!input.endsOn().isAfter(row.endsOn())||input.endsOn().isAfter(row.endsOn().plusDays(extensionDays))||input.endsOn().isAfter(activities.get(doc,email).periodEndsOn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La extensión debe estar dentro del período y del límite técnico configurado de "+extensionDays+" días calendario desde el vencimiento original.");
        if(!input.endsOn().isAfter(responsible.deadline())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El nuevo plazo debe ampliar el plazo vigente del responsable.");
        var actor=accounts.current(email).id();
        jdbc.update("INSERT INTO deadline_extension(work_plan_id,activity_id,user_id,ends_on,reason,granted_by,row_version) VALUES (?,?,?,?,?,?,1) ON CONFLICT(work_plan_id,activity_id,user_id) DO UPDATE SET ends_on=excluded.ends_on,reason=excluded.reason,granted_by=excluded.granted_by,row_version=deadline_extension.row_version+1,granted_at=now()",doc,activity,input.userId(),input.endsOn(),input.reason().trim(),actor);
        jdbc.update("INSERT INTO deadline_extension_history(id,work_plan_id,activity_id,user_id,old_deadline,new_deadline,reason,authorized_by) VALUES (?,?,?,?,?,?,?,?)",UUID.randomUUID(),doc,activity,input.userId(),responsible.deadline(),input.endsOn(),input.reason().trim(),actor);
        audit.record(actor,"ACTIVITY_EXTENSION_GRANTED",doc,input.userId());
    }
    public byte[] content(UUID doc,UUID evidence,String email) {
        plans.get(doc,email);var file=jdbc.queryForList("SELECT file_id FROM execution_evidence WHERE work_plan_id=? AND id=? AND evidence_kind='PDF'",UUID.class,doc,evidence).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"PDF no disponible para esta evidencia."));return files.read(file);
    }
    public List<Map<String,Object>> extensionHistory(UUID doc,String email) {
        plans.get(doc,email);
        return jdbc.queryForList("SELECT h.*,u.display_name AS responsible_name,a.display_name AS authorized_by_name FROM deadline_extension_history h JOIN app_user u ON u.id=h.user_id JOIN app_user a ON a.id=h.authorized_by WHERE h.work_plan_id=? ORDER BY h.created_at DESC,h.id",doc);
    }
    public List<AttachmentModels.Attachment> annexes(UUID doc,String email) {
        var state=get(doc,email);var list=new ArrayList<AttachmentModels.Attachment>();
        for(var row:state.activities()) for(var medium:row.media()) if(!medium.versions().isEmpty()&&medium.evidenceKind().equals("PDF")) {
            var e=medium.versions().getFirst();UUID file=jdbc.queryForObject("SELECT file_id FROM execution_evidence WHERE id=?",UUID.class,e.id());
            list.add(new AttachmentModels.Attachment(e.id(),file,AttachmentService.label(list.size()),medium.label(),row.title()+" · Evidencia v"+e.version()+".0",e.originalName(),e.sizeBytes(),e.pageCount(),e.sha256()));
        }
        return List.copyOf(list);
    }
}
