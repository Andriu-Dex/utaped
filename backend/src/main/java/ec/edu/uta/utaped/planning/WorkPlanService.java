package ec.edu.uta.utaped.planning;

import static ec.edu.uta.utaped.planning.WorkPlanModels.*;
import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WorkPlanService {
    private final JdbcTemplate jdbc;
    private final Accounts accounts;
    private final AuditService audit;
    private final boolean singlePlan;
    private final boolean enforceWindow;
    private final ZoneId zone;
    private static final String SELECT="""
        SELECT w.*,u.display_name AS teacher_name,g.name AS group_name,p.name AS period_name,
          p.preparation_starts_on,p.preparation_ends_on
        FROM work_plan w JOIN app_user u ON u.id=w.teacher_id
        JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id
        WHERE w.teacher_id=? AND g.active AND EXISTS (
          SELECT 1 FROM membership m WHERE m.user_id=w.teacher_id AND m.group_id=w.group_id)
        """;
    public WorkPlanService(JdbcTemplate jdbc,Accounts accounts,AuditService audit,
        @Value("${app.planning.single-plan-per-scope:false}") boolean singlePlan,
        @Value("${app.planning.enforce-preparation-window:true}") boolean enforceWindow,
        @Value("${app.planning.time-zone:America/Guayaquil}") String zone) {
        this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.singlePlan=singlePlan;this.enforceWindow=enforceWindow;this.zone=ZoneId.of(zone);
    }
    private boolean within(LocalDate start,LocalDate end) { var today=LocalDate.now(zone);return !today.isBefore(start) && !today.isAfter(end); }
    private Plan map(ResultSet rs,int index) throws SQLException {
        boolean editable=rs.getString("document_state").equals("DRAFT") && (!enforceWindow || within(rs.getObject("preparation_starts_on",LocalDate.class),rs.getObject("preparation_ends_on",LocalDate.class)));
        return new Plan(rs.getObject("id",UUID.class),rs.getObject("teacher_id",UUID.class),rs.getString("teacher_name"),rs.getObject("group_id",UUID.class),rs.getString("group_name"),
            rs.getObject("period_id",UUID.class),rs.getString("period_name"),rs.getString("title"),rs.getString("institutional_unit"),rs.getString("career"),rs.getString("justification"),rs.getString("objective"),
            rs.getString("document_state"),rs.getString("formal_version"),rs.getLong("row_version"),rs.getObject("preparation_date",LocalDate.class),
            rs.getObject("created_at",OffsetDateTime.class),rs.getObject("updated_at",OffsetDateTime.class),editable);
    }
    public Options options(String email) {
        var actor=accounts.current(email);
        var groups=jdbc.query("SELECT g.id,g.name FROM membership m JOIN institutional_group g ON g.id=m.group_id WHERE m.user_id=? AND g.active ORDER BY g.name",(rs,n)->new GroupOption(rs.getObject("id",UUID.class),rs.getString("name")),actor.id());
        var periods=jdbc.query("SELECT id,name,preparation_starts_on,preparation_ends_on FROM academic_period ORDER BY starts_on DESC",(rs,n)-> {
            var start=rs.getObject("preparation_starts_on",LocalDate.class);var end=rs.getObject("preparation_ends_on",LocalDate.class);
            return new PeriodOption(rs.getObject("id",UUID.class),rs.getString("name"),start,end,!enforceWindow || within(start,end));
        });
        return new Options(groups,periods,singlePlan,enforceWindow);
    }
    public Plan get(UUID targetDocId,String email) {
        return jdbc.query(SELECT+" AND w.id=?",this::map,accounts.current(email).id(),targetDocId).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento no disponible."));
    }
    public Page list(String email,int page,int size,UUID groupId,UUID periodId,String query) {
        var actor=accounts.current(email);
        String filters=" AND (?::uuid IS NULL OR w.group_id=?::uuid) AND (?::uuid IS NULL OR w.period_id=?::uuid) AND position(lower(?) in lower(w.title))>0";
        Object[] args={actor.id(),groupId,groupId,periodId,periodId,query};
        long total=jdbc.queryForObject("SELECT count(*) FROM ("+SELECT+filters+") plans",Long.class,args);
        Object[] paged={actor.id(),groupId,groupId,periodId,periodId,query,size,(long)page*size};
        String summarySelect=SELECT.replace("w.*", "w.id,w.title,w.document_state,w.formal_version,w.updated_at");
        var items=jdbc.query(summarySelect+filters+" ORDER BY w.updated_at DESC,w.id LIMIT ? OFFSET ?",(rs,n)->new Summary(
            rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("group_name"),rs.getString("period_name"),
            rs.getString("document_state"),rs.getString("formal_version"),rs.getObject("updated_at",OffsetDateTime.class),
            rs.getString("document_state").equals("DRAFT") && (!enforceWindow || within(rs.getObject("preparation_starts_on",LocalDate.class),rs.getObject("preparation_ends_on",LocalDate.class)))),paged);
        return new Page(items,total,page,size);
    }
    @Transactional public Plan create(String email,Create data) {
        var actor=accounts.current(email);
        // Serialize creation per identity; an idempotent retry never creates another document.
        jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",actor.id());
        var existing=jdbc.queryForList("SELECT id,group_id,period_id,creation_title FROM work_plan WHERE teacher_id=? AND request_key=?",actor.id(),data.requestKey());
        if(!existing.isEmpty()) {
            var prior=existing.getFirst();
            if(!prior.get("group_id").equals(data.groupId()) || !prior.get("period_id").equals(data.periodId()) || !prior.get("creation_title").equals(data.title().trim()))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya fue utilizada para otro documento.");
            return get((UUID)prior.get("id"),email);
        }
        var memberships=jdbc.queryForList("SELECT m.user_id FROM membership m JOIN institutional_group g ON g.id=m.group_id WHERE m.user_id=? AND m.group_id=? AND g.active FOR KEY SHARE OF m,g",actor.id(),data.groupId());
        if(memberships.isEmpty()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo puede crear un Plan en sus grupos activos.");
        requireWindow(data.periodId());
        if(singlePlan && jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE teacher_id=? AND group_id=? AND period_id=?",Integer.class,actor.id(),data.groupId(),data.periodId())>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un Plan para este docente, grupo y período.");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,group_id,period_id,request_key,title,creation_title,preparation_date) VALUES (?,?,?,?,?,?,?,?)",id,actor.id(),data.groupId(),data.periodId(),data.requestKey(),data.title().trim(),data.title().trim(),LocalDate.now(zone));
        audit.record(actor.id(),"WORK_PLAN_CREATED",id);
        return get(id,email);
    }
    private void requireWindow(UUID periodId) {
        var dates=jdbc.query("SELECT preparation_starts_on,preparation_ends_on FROM academic_period WHERE id=?",(rs,n)->new LocalDate[]{rs.getObject(1,LocalDate.class),rs.getObject(2,LocalDate.class)},periodId);
        if(dates.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un período válido.");
        if(enforceWindow && !within(dates.getFirst()[0],dates.getFirst()[1])) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La ventana de elaboración del período está cerrada.");
    }
    @Transactional public Plan update(UUID targetDocId,String email,Update data) {
        Plan plan=get(targetDocId,email);
        if(!plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El documento está en modo de solo lectura.");
        var memberships=jdbc.queryForList("SELECT user_id FROM membership WHERE user_id=? AND group_id=? FOR KEY SHARE",plan.teacherId(),plan.groupId());
        if(memberships.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento no disponible.");
        int changed=jdbc.update("""
            UPDATE work_plan SET title=?,institutional_unit=?,career=?,justification=?,objective=?,row_version=row_version+1,updated_at=now()
            WHERE id=? AND teacher_id=? AND row_version=? AND document_state='DRAFT'
            """,data.title().trim(),data.institutionalUnit().trim(),data.career().trim(),data.justification(),data.objective(),targetDocId,plan.teacherId(),data.rowVersion());
        if(changed==0) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió en otra sesión. Recargue la versión actual antes de guardar.");
        audit.record(plan.teacherId(),"WORK_PLAN_DRAFT_SAVED",targetDocId);
        return get(targetDocId,email);
    }
}
