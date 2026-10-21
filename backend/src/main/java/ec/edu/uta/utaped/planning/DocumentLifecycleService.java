package ec.edu.uta.utaped.planning;

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
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentLifecycleService {
    public record Change(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=1000) String reason) {}
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final Accounts accounts;private final AuditService audit;private final ZoneId zone;private final boolean enforce;
    public DocumentLifecycleService(JdbcTemplate jdbc,WorkPlanService plans,Accounts accounts,AuditService audit,@Value("${app.planning.time-zone:America/Guayaquil}") String zone,@Value("${app.planning.enforce-preparation-window:true}") boolean enforce) { this.jdbc=jdbc;this.plans=plans;this.accounts=accounts;this.audit=audit;this.zone=ZoneId.of(zone);this.enforce=enforce; }
    private void check(WorkPlanModels.Plan plan,Change data) {
        if(!plan.canManage()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable principal puede gestionar el ciclo del documento.");
        if(plan.archivedAt()!=null||plan.documentState().equals("IN_REVIEW")) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento está archivado o tiene una revisión en curso.");
        if(plan.rowVersion()!=data.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió. Recargue antes de continuar.");
    }
    @Transactional public WorkPlanModels.Plan archive(UUID doc,String email,Change data) {
        var plan=plans.lock(doc,email);check(plan,data);
        if(plan.documentType().equals("T1")&&jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE base_plan_id=? AND archived_at IS NULL",Integer.class,doc)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Archive primero el Informe asociado para conservar el circuito como historial.");
        archiveRow(doc,email,data.reason());if(plan.documentState().equals("DRAFT")&&!accounts.current(email).systemRole().equals("ADMIN"))return plan.discarded();return plans.get(doc,email);
    }
    private void archiveRow(UUID doc,String email,String reason) {
        UUID actor=accounts.current(email).id();jdbc.update("UPDATE work_plan SET discarded=(document_state='DRAFT'),archived_at=now(),archived_by=?,archive_reason=?,row_version=row_version+1,updated_at=now() WHERE id=?",actor,reason.trim(),doc);audit.record(actor,"DOCUMENT_ARCHIVED",doc,actor);
    }
    @Transactional public WorkPlanModels.Plan nextVersion(UUID doc,String email,Change data) {
        var plan=plans.lock(doc,email);check(plan,data);
        if(plan.scopeId()==null||!plan.documentState().equals("APPROVED")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Una nueva versión formal requiere un documento aprobado con ámbito institucional.");
        if(plan.documentType().equals("T3")) throw new ResponseStatusException(HttpStatus.CONFLICT,"Un Informe de ejecución aprobado no se versiona: elabore el siguiente Informe de ejecución del Plan.");
        var dates=jdbc.queryForMap("SELECT p.starts_on,p.ends_on,p.preparation_starts_on,GREATEST(p.preparation_ends_on,s.preparation_extended_until) AS preparation_ends_on FROM academic_period p LEFT JOIN organizational_scope s ON s.id=? WHERE p.id=?",plan.scopeId(),plan.periodId());
        var start=((java.sql.Date)dates.get(plan.documentType().equals("T1")?"preparation_starts_on":"starts_on")).toLocalDate();var end=((java.sql.Date)dates.get(plan.documentType().equals("T1")?"preparation_ends_on":"ends_on")).toLocalDate();var today=LocalDate.now(zone);
        if(enforce&&(today.isBefore(start)||today.isAfter(end))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La ventana de elaboración está cerrada; una reapertura requiere autorización institucional.");
        if(plan.documentType().equals("T1")&&jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE base_plan_id=? AND document_state<>'APPROVED' AND archived_at IS NULL",Integer.class,doc)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Finalice o archive el Informe en elaboración antes de actualizar su Plan fuente.");
        UUID actor=accounts.current(email).id(),id=UUID.randomUUID();int number=Integer.parseInt(plan.formalVersion().split("\\.")[0])+1;
        archiveRow(doc,email,"Actualización formal: "+data.reason());
        jdbc.update("""
            INSERT INTO work_plan(id,teacher_id,created_by,scope_id,group_id,period_id,request_key,title,creation_title,preparation_date,institutional_unit,career,document_type,base_plan_id,justification,objective,formal_version,previous_version_id,version_reason,attachments_enabled,privacy_notice_enabled)
            SELECT ?,?,?,scope_id,group_id,period_id,?,title,title,?,institutional_unit,career,document_type,base_plan_id,justification,objective,?,?,?,attachments_enabled,privacy_notice_enabled FROM work_plan WHERE id=?
            """,id,actor,actor,UUID.randomUUID(),today,number+".0",doc,data.reason().trim(),doc);
        jdbc.update("INSERT INTO document_structure(work_plan_id,revision,sections,contents) SELECT ?,revision,sections,contents FROM document_structure WHERE work_plan_id=?",id,doc);
        jdbc.update("INSERT INTO document_author(work_plan_id,user_id,sort_order) SELECT ?,user_id,sort_order FROM document_author WHERE work_plan_id=?",id,doc);
        jdbc.update("INSERT INTO work_plan_matrix(work_plan_id,source,activities) SELECT ?,source,activities FROM work_plan_matrix WHERE work_plan_id=?",id,doc);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) SELECT ?,user_id,? FROM document_access WHERE work_plan_id=?",id,actor,doc);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?) ON CONFLICT DO NOTHING",id,actor,actor);
        jdbc.update("""
            INSERT INTO document_attachment(id,work_plan_id,file_id,title,description,sort_order,request_key)
            SELECT gen_random_uuid(),?,file_id,title,description,sort_order,gen_random_uuid() FROM document_attachment WHERE work_plan_id=? AND removed_at IS NULL
            """,id,doc);
        if(plan.documentType().equals("T2")) jdbc.update("""
            INSERT INTO work_report(id,base_plan_id,base_artifact_id,base_formal_version,background,conclusions,improvements,results,delegations,previous_version_id)
            SELECT ?,base_plan_id,base_artifact_id,base_formal_version,background,conclusions,improvements,results,delegations,id FROM work_report WHERE id=?
            """,id,doc);
        audit.record(actor,"DOCUMENT_FORMAL_VERSION_CREATED",id);return plans.get(id,email);
    }
    public List<Map<String,Object>> history(UUID doc,String email) {
        plans.get(doc,email);
        var rows=jdbc.queryForList("""
            WITH RECURSIVE ancestors AS (SELECT id,previous_version_id FROM work_plan WHERE id=? UNION ALL SELECT w.id,w.previous_version_id FROM work_plan w JOIN ancestors a ON w.id=a.previous_version_id),
            family AS (SELECT id FROM ancestors WHERE previous_version_id IS NULL UNION ALL SELECT w.id FROM work_plan w JOIN family f ON w.previous_version_id=f.id)
            SELECT w.id,w.title,w.formal_version,w.document_state,w.review_round,w.archived_at,w.archive_reason,w.version_reason,w.created_at FROM work_plan w JOIN family f ON f.id=w.id ORDER BY w.created_at,w.id
            """,doc);
        // Retained metadata never grants access to a sibling version.
        return rows.stream().filter(r->{ try { plans.get((UUID)r.get("id"),email);return true; } catch(ResponseStatusException denied) { if(denied.getStatusCode()!=HttpStatus.NOT_FOUND) throw denied;return false; } }).toList();
    }
}
