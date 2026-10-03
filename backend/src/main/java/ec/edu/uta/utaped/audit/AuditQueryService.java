package ec.edu.uta.utaped.audit;

import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.planning.WorkPlanService;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuditQueryService {
    public record Event(UUID id,UUID actorId,String actorName,boolean historicalActorName,String action,UUID targetId,UUID subjectUserId,OffsetDateTime occurredAt) {}
    public record Page(List<Event> items,long total,int page,int size) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final WorkPlanService plans;
    public AuditQueryService(JdbcTemplate jdbc,Accounts accounts,WorkPlanService plans) { this.jdbc=jdbc;this.accounts=accounts;this.plans=plans; }
    private void admin(String email) { if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido."); }
    public List<String> actions(String email) { admin(email);return jdbc.queryForList("SELECT DISTINCT action FROM audit_event ORDER BY action",String.class); }
    public Page administration(String email,String action,UUID actorId,UUID targetId,OffsetDateTime from,OffsetDateTime to,int page,int size) {
        admin(email);return query(action,actorId,targetId,from,to,page,size,false);
    }
    @Transactional(readOnly=true)
    public Page history(String email,UUID targetDocId,int page,int size) {
        plans.get(targetDocId,email);return query("",null,targetDocId,null,null,page,size,true);
    }
    private Page query(String action,UUID actorId,UUID targetId,OffsetDateTime from,OffsetDateTime to,int page,int size,boolean documentOnly) {
        if(from!=null && to!=null && from.isAfter(to)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El inicio debe preceder al fin del intervalo.");
        String where=" WHERE (?='' OR e.action=?) AND (?::uuid IS NULL OR e.actor_id=?::uuid) AND (?::uuid IS NULL OR e.target_id=?::uuid) AND (?::timestamptz IS NULL OR e.occurred_at>=?::timestamptz) AND (?::timestamptz IS NULL OR e.occurred_at<=?::timestamptz)";
        if(documentOnly) where+=" AND e.action IN ('WORK_PLAN_CREATED','WORK_PLAN_DRAFT_SAVED','WORK_PLAN_MATRIX_SAVED','DOCUMENT_ATTACHMENT_SETTINGS_SAVED','DOCUMENT_ATTACHMENT_ADDED','DOCUMENT_ATTACHMENT_UPDATED','DOCUMENT_ATTACHMENT_REMOVED','DOCUMENT_ATTACHMENTS_REORDERED','T1_PREVIEW_GENERATED')";
        var args=new ArrayList<Object>(Arrays.asList(action,action,actorId,actorId,targetId,targetId,from,from,to,to));
        long total=jdbc.queryForObject("SELECT count(*) FROM audit_event e"+where,Long.class,args.toArray());args.add(size);args.add((long)page*size);
        var items=jdbc.query("SELECT e.*,COALESCE(e.actor_display_name,u.display_name) AS actor_name FROM audit_event e LEFT JOIN app_user u ON u.id=e.actor_id"+where+" ORDER BY e.occurred_at DESC,e.id DESC LIMIT ? OFFSET ?",(rs,n)->new Event(rs.getObject("id",UUID.class),rs.getObject("actor_id",UUID.class),rs.getString("actor_name"),rs.getString("actor_display_name")!=null,rs.getString("action"),rs.getObject("target_id",UUID.class),rs.getObject("subject_user_id",UUID.class),rs.getObject("occurred_at",OffsetDateTime.class)),args.toArray());
        return new Page(items,total,page,size);
    }
}
