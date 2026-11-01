package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class ActivityProposals {
    public record Create(@NotNull UUID requestKey,@NotBlank @Size(max=500) String title) {}
    public record Accept(@NotNull @PositiveOrZero Long rowVersion,@NotNull UUID periodId,UUID careerId,@NotBlank @Size(max=500) String title) {}
    public record Reject(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=1000) String reason) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final WorkPlanService plans;private final ActivityService activities;private final AuditService audit;
    public ActivityProposals(JdbcTemplate jdbc,Accounts accounts,WorkPlanService plans,ActivityService activities,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.plans=plans;this.activities=activities;this.audit=audit; }
    private UUID admin(String email) { var actor=accounts.current(email);if(!actor.systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido.");return actor.id(); }
    public List<Map<String,Object>> own(UUID doc,String email) { plans.get(doc,email);return jdbc.queryForList("SELECT id,title,status,created_at,resolution_note FROM activity_proposal WHERE source_document_id=? ORDER BY created_at,id",doc); }
    public List<Map<String,Object>> pending(UUID group,String email) { admin(email);return jdbc.queryForList("SELECT id,title,status,row_version,source_title,source_formal_version,source_period_ends_on,career_id,created_at FROM activity_proposal WHERE group_id=? AND status='PENDING' ORDER BY created_at,id",group); }
    @Transactional public UUID create(UUID doc,String email,Create input) {
        var plan=plans.lock(doc,email);var actor=accounts.current(email);
        if(!plan.documentType().equals("T2") || !plan.documentState().equals("APPROVED") || plan.archivedAt()!=null || !plan.teacherId().equals(actor.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable principal puede proponer actividades desde un Informe final aprobado y vigente.");
        var previous=jdbc.queryForList("SELECT id,title,source_document_id FROM activity_proposal WHERE created_by=? AND request_key=?",actor.id(),input.requestKey());
        if(!previous.isEmpty()) { var row=previous.getFirst();if(!doc.equals(row.get("source_document_id")) || !input.title().strip().equals(row.get("title"))) throw conflict();return (UUID)row.get("id"); }
        var source=jdbc.queryForMap("SELECT r.artifact_id,f.sha256 FROM document_review r JOIN stored_file f ON f.id=r.current_file_id WHERE r.work_plan_id=? AND r.status='APPROVED' ORDER BY r.round_number DESC LIMIT 1",doc);
        var period=jdbc.queryForObject("SELECT ends_on FROM academic_period WHERE id=?",java.time.LocalDate.class,plan.periodId());
        UUID career=plan.scopeId()==null?null:jdbc.queryForObject("SELECT career_id FROM organizational_scope WHERE id=?",UUID.class,plan.scopeId());
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO activity_proposal(id,source_document_id,source_artifact_id,source_pdf_hash,group_id,career_id,source_title,source_formal_version,source_period_ends_on,title,created_by,request_key) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",id,doc,source.get("artifact_id"),source.get("sha256"),plan.groupId(),career,plan.title(),plan.formalVersion(),period,input.title().strip(),actor.id(),input.requestKey());
        audit.record(actor.id(),"FUTURE_ACTIVITY_PROPOSED",doc);return id;
    }
    private Map<String,Object> lock(UUID id,long version) { var rows=jdbc.queryForList("SELECT * FROM activity_proposal WHERE id=? FOR UPDATE",id);if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Propuesta no disponible.");var row=rows.getFirst();if(!row.get("status").equals("PENDING") || ((Number)row.get("row_version")).longValue()!=version) throw conflict();return row; }
    @Transactional public ActivityModels.Definition accept(UUID id,String email,Accept input) {
        UUID actor=admin(email);var row=lock(id,input.rowVersion());
        var periods=jdbc.queryForList("SELECT starts_on FROM academic_period WHERE id=? AND status='ACTIVE' AND starts_on>?",input.periodId(),row.get("source_period_ends_on"));
        if(periods.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un período activo posterior al del Informe de origen.");
        if(row.get("career_id")!=null && input.careerId()==null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione una carrera activa para la propuesta.");
        var context=new ActivityModels.Context("report-proposal:"+id,"Informe final "+row.get("source_formal_version")+": "+row.get("source_title"),null,null,null,null,null,null,null,input.periodId(),input.careerId());
        var definition=activities.saveDefinition(null,new ActivityModels.DefinitionInput((UUID)row.get("group_id"),input.title().strip(),"IMPROVEMENT_ACTION",false,true,context),email);
        jdbc.update("UPDATE activity_proposal SET status='ACCEPTED',row_version=row_version+1,catalog_id=?,resolved_by=?,resolved_at=now() WHERE id=?",definition.id(),actor,id);audit.record(actor,"FUTURE_ACTIVITY_ACCEPTED",id);return definition;
    }
    @Transactional public void reject(UUID id,String email,Reject input) { UUID actor=admin(email);lock(id,input.rowVersion());jdbc.update("UPDATE activity_proposal SET status='REJECTED',row_version=row_version+1,resolved_by=?,resolved_at=now(),resolution_note=? WHERE id=?",actor,input.reason().strip(),id);audit.record(actor,"FUTURE_ACTIVITY_REJECTED",id); }
    private ResponseStatusException conflict() { return new ResponseStatusException(HttpStatus.CONFLICT,"La propuesta cambió o ya fue resuelta. Recargue la lista."); }
}
