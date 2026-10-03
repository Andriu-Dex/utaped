package ec.edu.uta.utaped.audit;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ec.edu.uta.utaped.notifications.NotificationWriter;

@Service
@Transactional
public class AuditService {
    private final JdbcTemplate jdbc;
    private final NotificationWriter notifications;
    public AuditService(JdbcTemplate jdbc,NotificationWriter notifications) { this.jdbc = jdbc;this.notifications=notifications; }
    public void record(UUID actor, String action, UUID target) {
        record(actor,action,target,null);
    }
    public void record(UUID actor,String action,UUID target,UUID subjectUser) {
        UUID event=UUID.randomUUID();
        jdbc.update("INSERT INTO audit_event(id, actor_id, action, target_id, subject_user_id, actor_display_name) VALUES (?, ?, ?, ?, ?, (SELECT display_name FROM app_user WHERE id=?))", event, actor, action, target,subjectUser,actor);
        notifications.fromEvent(event,actor,action,target,subjectUser);
    }
}
