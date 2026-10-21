package ec.edu.uta.utaped.planning;

import static ec.edu.uta.utaped.planning.WorkPlanModels.*;
import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import java.sql.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkPlanService {
    private final JdbcTemplate jdbc; private final Accounts accounts; private final AuditService audit;
    private final boolean singlePlan,enforceWindow,legacyCreation; private final ZoneId zone;
    public static final String ACCESS="""
        g.active AND ((w.scope_id IS NULL AND w.teacher_id=actor.id AND EXISTS
          (SELECT 1 FROM membership m WHERE m.user_id=actor.id AND m.group_id=w.group_id))
        OR (s.active AND EXISTS (SELECT 1 FROM scope_member m WHERE m.scope_id=s.id AND m.user_id=actor.id AND m.active)
          AND EXISTS (SELECT 1 FROM document_access da WHERE da.work_plan_id=w.id AND da.user_id=actor.id))
        OR EXISTS (SELECT 1 FROM document_review r JOIN review_task t ON t.review_id=r.id
          WHERE r.work_plan_id=w.id AND t.actor_id=actor.id AND t.status IN ('ACTIVE','COMPLETED','RETURNED'))
        OR EXISTS (SELECT 1 FROM document_review r JOIN review_task t ON t.review_id=r.id JOIN work_plan report ON report.id=r.work_plan_id
          WHERE report.base_plan_id=w.id AND t.actor_id=actor.id AND t.status IN ('ACTIVE','COMPLETED','RETURNED')))
        """;
    private static final String SELECT="""
        SELECT w.*,COALESCE(s.principal_user_id,w.teacher_id) AS responsible_id,u.display_name AS teacher_name,
          g.name AS group_name,p.name AS period_name,p.status AS period_status,p.preparation_starts_on,p.preparation_ends_on,p.starts_on,p.ends_on,
          GREATEST(p.preparation_ends_on,s.preparation_extended_until) AS preparation_effective_ends_on,
          (actor.id=COALESCE(s.principal_user_id,w.teacher_id)) AS is_editor,
          EXISTS(SELECT 1 FROM signed_document_artifact sig JOIN document_artifact a ON a.id=sig.artifact_id WHERE sig.work_plan_id=w.id AND a.source_row_version=w.row_version) AS authors_locked
        FROM work_plan w JOIN app_user actor ON actor.id=?
        JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id
        LEFT JOIN organizational_scope s ON s.id=w.scope_id
        JOIN app_user u ON u.id=COALESCE(s.principal_user_id,w.teacher_id)
        WHERE
        """+ACCESS;
    private final DocumentTemplateService templateService;
    public WorkPlanService(JdbcTemplate jdbc,Accounts accounts,AuditService audit,DocumentTemplateService templateService,
        @Value("${app.planning.single-plan-per-scope:false}") boolean singlePlan,
        @Value("${app.planning.enforce-preparation-window:true}") boolean enforceWindow,
        @Value("${app.planning.time-zone:America/Guayaquil}") String zone,
        @Value("${app.planning.allow-legacy-creation:false}") boolean legacyCreation) {
        this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.templateService=templateService;this.singlePlan=singlePlan;this.enforceWindow=enforceWindow;this.zone=ZoneId.of(zone);this.legacyCreation=legacyCreation;
    }
    private boolean within(LocalDate start,LocalDate end) { var today=LocalDate.now(zone);return !today.isBefore(start)&&!today.isAfter(end); }
    private boolean editable(ResultSet rs) throws SQLException {
        return rs.getString("period_status").equals("ACTIVE")&&rs.getBoolean("is_editor")&&!rs.getBoolean("authors_locked")&&rs.getObject("archived_at")==null&&rs.getString("document_state").equals("DRAFT")
            &&(!enforceWindow||within(rs.getObject(rs.getString("document_type").equals("T2")?"starts_on":"preparation_starts_on",LocalDate.class),rs.getObject(rs.getString("document_type").equals("T2")?"ends_on":"preparation_effective_ends_on",LocalDate.class)));
    }
    private Plan map(ResultSet rs,int index) throws SQLException {
        return new Plan(rs.getObject("id",UUID.class),rs.getObject("responsible_id",UUID.class),rs.getString("teacher_name"),rs.getObject("group_id",UUID.class),rs.getString("group_name"),
            rs.getObject("period_id",UUID.class),rs.getString("period_name"),rs.getString("title"),rs.getString("institutional_unit"),rs.getString("career"),rs.getString("justification"),rs.getString("objective"),
            rs.getString("document_state"),rs.getString("formal_version"),rs.getLong("row_version"),rs.getObject("preparation_date",LocalDate.class),
            rs.getObject("created_at",OffsetDateTime.class),rs.getObject("updated_at",OffsetDateTime.class),editable(rs),rs.getObject("scope_id",UUID.class),rs.getObject("created_by",UUID.class),rs.getInt("review_round"),rs.getObject("archived_at",OffsetDateTime.class),rs.getString("document_type"),rs.getObject("base_plan_id",UUID.class),rs.getBoolean("is_editor")&&!rs.getBoolean("authors_locked")&&rs.getString("period_status").equals("ACTIVE"));
    }
    public Options options(String email) {
        var actor=accounts.current(email);
        var scopes=jdbc.query("""
            SELECT s.id,s.group_id,s.period_id,g.name AS group_name,p.name AS period_name,c.name AS career_name,
              (s.principal_user_id=? AND p.status='ACTIVE') AS is_principal,p.preparation_starts_on,GREATEST(p.preparation_ends_on,s.preparation_extended_until) AS preparation_ends_on,
              NOT EXISTS (SELECT 1 FROM work_plan w WHERE w.scope_id=s.id AND (w.archived_at IS NULL OR w.document_state='APPROVED') AND w.document_type='T1') AS available
            FROM organizational_scope s JOIN scope_member m ON m.scope_id=s.id AND m.user_id=? AND m.active
            JOIN institutional_group g ON g.id=s.group_id AND g.active JOIN academic_period p ON p.id=s.period_id
            LEFT JOIN academic_career c ON c.id=s.career_id WHERE s.active AND (c.id IS NULL OR c.active)
            ORDER BY p.starts_on DESC,g.name,c.name
            """,(rs,n)->new ScopeOption(rs.getObject("id",UUID.class),rs.getObject("group_id",UUID.class),rs.getObject("period_id",UUID.class),rs.getString("group_name"),rs.getString("period_name"),rs.getString("career_name"),
                rs.getBoolean("is_principal")&&rs.getBoolean("available")&&(!enforceWindow||within(rs.getObject("preparation_starts_on",LocalDate.class),rs.getObject("preparation_ends_on",LocalDate.class))),DocumentTitles.plan(rs.getString("group_name"))),actor.id(),actor.id());
        var groups=jdbc.query("SELECT g.id,g.name FROM membership m JOIN institutional_group g ON g.id=m.group_id WHERE m.user_id=? AND g.active ORDER BY g.name",(rs,n)->new GroupOption(rs.getObject("id",UUID.class),rs.getString("name")),actor.id());
        var periods=jdbc.query("SELECT id,name,status,preparation_starts_on,preparation_ends_on FROM academic_period ORDER BY starts_on DESC",(rs,n)-> {
            var start=rs.getObject("preparation_starts_on",LocalDate.class);var end=rs.getObject("preparation_ends_on",LocalDate.class);
            return new PeriodOption(rs.getObject("id",UUID.class),rs.getString("name"),start,end,rs.getString("status").equals("ACTIVE")&&(!enforceWindow||within(start,end)),rs.getString("status"));
        });
        return new Options(groups,periods,true,enforceWindow,scopes,templateService.getJustificationOrDefault("T1"));
    }
    public Plan get(UUID targetDocId,String email) {
        return jdbc.query(SELECT+" AND w.id=?",this::map,accounts.current(email).id(),targetDocId).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento no disponible."));
    }
    public Plan lock(UUID targetDocId,String email) {
        var initial=get(targetDocId,email);
        new ec.edu.uta.utaped.institution.PeriodGuard(jdbc).writable(initial.periodId());
        if(initial.scopeId()!=null) {
            jdbc.queryForList("SELECT id FROM organizational_scope WHERE id=? FOR SHARE",initial.scopeId());
            jdbc.queryForList("SELECT user_id FROM scope_member WHERE scope_id=? AND user_id=? FOR SHARE",initial.scopeId(),accounts.current(email).id());
        }
        jdbc.queryForList("SELECT id FROM work_plan WHERE id=? FOR UPDATE",targetDocId);
        return get(targetDocId,email);
    }
    public Page list(String email,int page,int size,UUID groupId,UUID periodId,String query) {
        var actor=accounts.current(email);
        String filters=" AND (?::uuid IS NULL OR w.group_id=?::uuid) AND (?::uuid IS NULL OR w.period_id=?::uuid) AND position(lower(?) in lower(w.title))>0";
        Object[] args={actor.id(),groupId,groupId,periodId,periodId,query};
        long total=jdbc.queryForObject("SELECT count(*) FROM ("+SELECT+filters+") plans",Long.class,args);
        var items=jdbc.query(SELECT+filters+" ORDER BY w.updated_at DESC,w.id LIMIT ? OFFSET ?",(rs,n)->new Summary(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("group_name"),rs.getString("period_name"),rs.getString("document_state"),rs.getString("formal_version"),rs.getObject("updated_at",OffsetDateTime.class),editable(rs),rs.getString("document_type"),rs.getObject("archived_at",OffsetDateTime.class)),actor.id(),groupId,groupId,periodId,periodId,query,size,(long)page*size);
        return new Page(items,total,page,size);
    }
    @Transactional public Plan create(String email,Create data) {
        String requestedTitle=Objects.toString(data.title(),"").trim();
        new ec.edu.uta.utaped.institution.PeriodGuard(jdbc).writable(data.periodId());
        var actor=accounts.current(email);jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",actor.id());
        var existing=jdbc.queryForList("SELECT id,group_id,period_id,scope_id,creation_title FROM work_plan WHERE created_by=? AND request_key=?",actor.id(),data.requestKey());
        if(!existing.isEmpty()) {
            var prior=existing.getFirst();
            if(!prior.get("group_id").equals(data.groupId())||!prior.get("period_id").equals(data.periodId())||!Objects.equals(prior.get("scope_id"),data.scopeId())||!prior.get("creation_title").equals(requestedTitle)) throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya fue utilizada para otro documento.");
            return get((UUID)prior.get("id"),email);
        }
        String career="";
        if(data.scopeId()!=null) {
            var scopes=jdbc.queryForList("""
                SELECT s.*,c.name AS career_name FROM organizational_scope s
                JOIN institutional_group g ON g.id=s.group_id AND g.active LEFT JOIN academic_career c ON c.id=s.career_id
                WHERE s.id=? AND s.active AND (c.id IS NULL OR c.active) FOR UPDATE OF s
                """,data.scopeId());
            if(scopes.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Ámbito no disponible.");
            var scope=scopes.getFirst();
            if(!data.groupId().equals(scope.get("group_id"))||!data.periodId().equals(scope.get("period_id"))||!actor.id().equals(scope.get("principal_user_id"))||jdbc.queryForObject("SELECT count(*) FROM scope_member WHERE scope_id=? AND user_id=? AND active",Integer.class,data.scopeId(),actor.id())==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable principal puede crear el Plan de su ámbito.");
            if(jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE scope_id=? AND (archived_at IS NULL OR document_state='APPROVED') AND document_type='T1'",Integer.class,data.scopeId())>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un Plan activo en este grupo, carrera y período.");
            career=Objects.toString(scope.get("career_name"),"");
        } else {
            if(!legacyCreation) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un ámbito institucional con responsable principal asignado.");
            if(jdbc.queryForObject("SELECT count(*) FROM membership m JOIN institutional_group g ON g.id=m.group_id WHERE m.user_id=? AND m.group_id=? AND g.active",Integer.class,actor.id(),data.groupId())==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo puede crear un Plan en sus grupos activos.");
            if(singlePlan&&jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE teacher_id=? AND group_id=? AND period_id=? AND archived_at IS NULL",Integer.class,actor.id(),data.groupId(),data.periodId())>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un Plan para este ámbito anterior.");
        }
        requireWindow(data.periodId(),data.scopeId());UUID id=UUID.randomUUID();
        if(data.scopeId()==null && requestedTitle.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indique un título para el documento anterior.");
        String title=requestedTitle.isBlank()?DocumentTitles.plan(jdbc.queryForObject("SELECT name FROM institutional_group WHERE id=?",String.class,data.groupId())):requestedTitle;
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,scope_id,group_id,period_id,request_key,title,creation_title,preparation_date,institutional_unit,career,justification) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)",id,actor.id(),actor.id(),data.scopeId(),data.groupId(),data.periodId(),data.requestKey(),title,requestedTitle,LocalDate.now(zone),"FISEI — Universidad Técnica de Ambato",career,templateService.getJustificationOrDefault("T1"));
        if(data.scopeId()!=null) jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) SELECT ?,user_id,? FROM scope_member WHERE scope_id=? AND active",id,actor.id(),data.scopeId());
        audit.record(actor.id(),"WORK_PLAN_CREATED",id);return get(id,email);
    }
    public boolean signatureWindowOpen(UUID doc,String email) {
        var plan=get(doc,email);
        var dates=jdbc.queryForMap("SELECT p.status,p.starts_on,p.ends_on,p.preparation_starts_on,GREATEST(p.preparation_ends_on,s.preparation_extended_until) AS preparation_ends_on FROM academic_period p LEFT JOIN organizational_scope s ON s.id=? WHERE p.id=?",plan.scopeId(),plan.periodId());
        String from=plan.documentType().equals("T2")?"starts_on":"preparation_starts_on",to=plan.documentType().equals("T2")?"ends_on":"preparation_ends_on";
        return "ACTIVE".equals(dates.get("status")) && plan.archivedAt()==null && plan.documentState().equals("DRAFT") && (!enforceWindow||within(((java.sql.Date)dates.get(from)).toLocalDate(),((java.sql.Date)dates.get(to)).toLocalDate()));
    }
    private void requireWindow(UUID periodId,UUID scopeId) {
        var dates=jdbc.query("SELECT p.preparation_starts_on,GREATEST(p.preparation_ends_on,s.preparation_extended_until) FROM academic_period p LEFT JOIN organizational_scope s ON s.id=? WHERE p.id=?",(rs,n)->new LocalDate[]{rs.getObject(1,LocalDate.class),rs.getObject(2,LocalDate.class)},scopeId,periodId);
        if(dates.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un período válido.");
        if(enforceWindow&&!within(dates.getFirst()[0],dates.getFirst()[1])) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La ventana de elaboración del período está cerrada.");
    }
    @Transactional public Plan update(UUID targetDocId,String email,Update data) {
        var plan=lock(targetDocId,email);
        if(!plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El documento está en modo de solo lectura.");
        if(plan.documentType().equals("T2")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Edite el Informe desde su formulario T2.");
        if(plan.scopeId()!=null&&(!data.career().trim().equals(plan.career())||!data.institutionalUnit().trim().equals(plan.institutionalUnit()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La carrera y unidad institucional derivan del ámbito; no se editan como texto libre.");
        int changed=jdbc.update("UPDATE work_plan SET title=?,institutional_unit=?,career=?,justification=?,objective=?,row_version=row_version+1,updated_at=now() WHERE id=? AND row_version=? AND document_state='DRAFT'",data.title().trim(),data.institutionalUnit().trim(),data.career().trim(),data.justification(),data.objective(),targetDocId,data.rowVersion());
        if(changed==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió en otra sesión. Recargue la versión actual antes de guardar.");
        audit.record(accounts.current(email).id(),"WORK_PLAN_DRAFT_SAVED",targetDocId);return get(targetDocId,email);
    }
}
