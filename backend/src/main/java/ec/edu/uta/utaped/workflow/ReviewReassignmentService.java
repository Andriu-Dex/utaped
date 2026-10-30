package ec.edu.uta.utaped.workflow;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.planning.WorkPlanService;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ReviewReassignmentService {
    public record Replace(@NotNull @PositiveOrZero Long rowVersion,@NotNull UUID actorId,
                          @NotBlank @Size(max=1000) String reason,@NotNull UUID requestKey) {}
    public record Candidate(UUID id,String name) {}
    public record Options(long rowVersion,UUID actorId,String actorName,boolean canReassign,
                          List<Candidate> candidates,List<Map<String,Object>> history) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final WorkPlanService plans;private final AuditService audit;
    public ReviewReassignmentService(JdbcTemplate jdbc,Accounts accounts,WorkPlanService plans,AuditService audit) {
        this.jdbc=jdbc;this.accounts=accounts;this.plans=plans;this.audit=audit;
    }
    private Map<String,Object> authorized(UUID task,String email,boolean lock) {
        var rows=jdbc.queryForList("SELECT t.*,r.work_plan_id,r.status AS review_status,w.scope_id FROM review_task t JOIN document_review r ON r.id=t.review_id JOIN work_plan w ON w.id=r.work_plan_id WHERE t.id=?",task);
        if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tarea no disponible.");
        var row=rows.getFirst();var user=accounts.current(email);
        plans.get((UUID)row.get("work_plan_id"),email);
        if(row.get("scope_id")==null||jdbc.queryForObject("SELECT count(*) FROM scope_member m JOIN organizational_scope s ON s.id=m.scope_id WHERE m.scope_id=? AND m.user_id=? AND m.active AND s.active AND m.membership_role='COORDINATOR'",Integer.class,row.get("scope_id"),user.id())==0)
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el coordinador del ámbito puede sustituir al participante.");
        if(lock) {
            jdbc.queryForList("SELECT id FROM document_review WHERE id=? FOR UPDATE",row.get("review_id"));
            plans.lock((UUID)row.get("work_plan_id"),email);
            return authorized(task,email,false);
        }
        return row;
    }
    private boolean available(Map<String,Object> row) {
        return jdbc.queryForObject("SELECT u.active AND NOT EXISTS(SELECT 1 FROM scope_member m WHERE m.scope_id=? AND m.user_id=u.id AND NOT m.active) FROM app_user u WHERE u.id=?",Boolean.class,row.get("scope_id"),row.get("actor_id"));
    }
    private boolean open(Map<String,Object> row) {
        return Set.of("ACTIVE","PENDING").contains(row.get("status"))&&"IN_REVIEW".equals(row.get("review_status"))&&row.get("acted_at")==null
            &&jdbc.queryForObject("SELECT count(*) FROM work_plan w JOIN academic_period p ON p.id=w.period_id WHERE w.id=? AND p.status='ACTIVE' AND NOT w.discarded AND w.archived_at IS NULL",Integer.class,row.get("work_plan_id"))>0;
    }
    private List<Candidate> candidates(Map<String,Object> row) {
        return jdbc.query("""
            SELECT u.id,u.display_name FROM scope_member m JOIN app_user u ON u.id=m.user_id
            WHERE m.scope_id=? AND m.active AND u.active AND u.id<>?
              AND NOT EXISTS(SELECT 1 FROM review_task t WHERE t.review_id=? AND t.actor_id=u.id)
              AND NOT EXISTS(SELECT 1 FROM document_author a WHERE a.work_plan_id=? AND a.user_id=u.id)
              AND u.id<>(SELECT teacher_id FROM work_plan WHERE id=?)
              AND u.id<>(SELECT principal_user_id FROM organizational_scope WHERE id=?)
            ORDER BY u.display_name,u.id
            """,(rs,n)->new Candidate(rs.getObject("id",UUID.class),rs.getString("display_name")),row.get("scope_id"),row.get("actor_id"),row.get("review_id"),row.get("work_plan_id"),row.get("work_plan_id"),row.get("scope_id"));
    }
    @Transactional(readOnly=true) public Options options(UUID task,String email) {
        var row=authorized(task,email,false);
        return new Options(((Number)row.get("row_version")).longValue(),(UUID)row.get("actor_id"),(String)row.get("actor_name"),open(row)&&!available(row),candidates(row),
            jdbc.queryForList("SELECT id,previous_actor_id,previous_actor_name,actor_id,actor_name,authorized_by,authorized_by_name,reason,created_at FROM review_task_reassignment WHERE task_id=? ORDER BY created_at,id",task));
    }
    @Transactional public Options replace(UUID task,String email,Replace input) {
        var row=authorized(task,email,true);UUID coordinator=accounts.current(email).id();
        var prior=jdbc.queryForList("SELECT * FROM review_task_reassignment WHERE task_id=? AND request_key=?",task,input.requestKey());
        if(!prior.isEmpty()) {
            var saved=prior.getFirst();
            if(!coordinator.equals(saved.get("authorized_by"))||!input.actorId().equals(saved.get("actor_id"))||!input.reason().trim().equals(saved.get("reason"))||input.rowVersion()!=((Number)saved.get("previous_row_version")).longValue())
                throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya se utilizó para otra sustitución.");
            return options(task,email);
        }
        if(!open(row)||available(row))throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se sustituye a un participante no disponible en una tarea sin decisión.");
        if(input.rowVersion()!=((Number)row.get("row_version")).longValue())throw new ResponseStatusException(HttpStatus.CONFLICT,"La tarea cambió. Recargue antes de sustituir.");
        var target=candidates(row).stream().filter(c->c.id().equals(input.actorId())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un integrante autorizado sin otra intervención en esta ronda."));
        jdbc.update("INSERT INTO review_task_reassignment(id,task_id,previous_actor_id,previous_actor_name,actor_id,actor_name,authorized_by,authorized_by_name,reason,previous_row_version,request_key) VALUES (?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),task,row.get("actor_id"),row.get("actor_name"),target.id(),target.name(),coordinator,accounts.current(email).displayName(),input.reason().trim(),input.rowVersion(),input.requestKey());
        jdbc.update("UPDATE review_task SET slot_actor_id=COALESCE(slot_actor_id,actor_id),actor_id=?,actor_name=?,row_version=row_version+1 WHERE id=?",target.id(),target.name(),task);
        audit.record(coordinator,"REVIEW_TASK_REASSIGNED",(UUID)row.get("work_plan_id"));
        if("ACTIVE".equals(row.get("status")))audit.record(coordinator,"REVIEW_TASK_ASSIGNED",(UUID)row.get("work_plan_id"),target.id());
        return options(task,email);
    }
}
