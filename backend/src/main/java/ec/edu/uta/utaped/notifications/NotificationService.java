package ec.edu.uta.utaped.notifications;

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
public class NotificationService {
    public record Notification(UUID id,String kind,String targetType,UUID targetId,String title,String message,OffsetDateTime createdAt,OffsetDateTime readAt,boolean targetAvailable) {}
    public record Page(List<Notification> items,long total,long unread,int page,int size) {}
    public record Target(String type,UUID id) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final WorkPlanService plans;
    public NotificationService(JdbcTemplate jdbc,Accounts accounts,WorkPlanService plans) { this.jdbc=jdbc;this.accounts=accounts;this.plans=plans; }
    private static final String SELECT="""
        SELECT n.*,CASE WHEN n.target_type='WORK_PLAN' THEN EXISTS (
          SELECT 1 FROM work_plan w JOIN institutional_group g ON g.id=w.group_id WHERE w.id=n.target_id AND w.teacher_id=n.recipient_id AND g.active
          AND EXISTS (SELECT 1 FROM membership m WHERE m.group_id=w.group_id AND m.user_id=n.recipient_id))
        ELSE EXISTS (SELECT 1 FROM institutional_group g WHERE g.id=n.target_id AND g.active AND (? OR EXISTS (SELECT 1 FROM membership m WHERE m.group_id=g.id AND m.user_id=n.recipient_id))) END AS target_available
        FROM personal_notification n WHERE n.recipient_id=?
        """;
    private Notification map(java.sql.ResultSet rs,int n) throws java.sql.SQLException { return new Notification(rs.getObject("id",UUID.class),rs.getString("kind"),rs.getString("target_type"),rs.getObject("target_id",UUID.class),rs.getString("title"),rs.getString("message"),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("read_at",OffsetDateTime.class),rs.getBoolean("target_available")); }
    public long unread(String email) { return jdbc.queryForObject("SELECT count(*) FROM personal_notification WHERE recipient_id=? AND read_at IS NULL",Long.class,accounts.current(email).id()); }
    public Page list(String email,Boolean read,int page,int size) {
        var actor=accounts.current(email);String filter=" AND (?::boolean IS NULL OR (read_at IS NOT NULL)=?)";
        long total=jdbc.queryForObject("SELECT count(*) FROM personal_notification WHERE recipient_id=?"+filter,Long.class,actor.id(),read,read);
        var items=jdbc.query(SELECT+filter+" ORDER BY n.created_at DESC,n.id DESC LIMIT ? OFFSET ?",this::map,actor.systemRole().equals("ADMIN"),actor.id(),read,read,size,(long)page*size);
        return new Page(items,total,unread(email),page,size);
    }
    private Notification owned(String email,UUID id) {
        var actor=accounts.current(email);return jdbc.query(SELECT+" AND n.id=?",this::map,actor.systemRole().equals("ADMIN"),actor.id(),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Notificación no disponible."));
    }
    @Transactional public void read(String email,UUID id) { owned(email,id);jdbc.update("UPDATE personal_notification SET read_at=COALESCE(read_at,now()) WHERE id=? AND recipient_id=?",id,accounts.current(email).id()); }
    @Transactional public long readAll(String email) { return jdbc.update("UPDATE personal_notification SET read_at=now() WHERE recipient_id=? AND read_at IS NULL",accounts.current(email).id()); }
    @Transactional public Target open(String email,UUID id) {
        var notification=owned(email,id);
        if(!notification.targetAvailable()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"El objeto ya no está disponible con sus permisos actuales.");
        if(notification.targetType().equals("WORK_PLAN")) plans.get(notification.targetId(),email);
        read(email,id);return new Target(notification.targetType(),notification.targetId());
    }
}
