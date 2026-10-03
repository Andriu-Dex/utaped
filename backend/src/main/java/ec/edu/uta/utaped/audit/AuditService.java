package ec.edu.uta.utaped.audit;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class AuditService {
    private final JdbcTemplate jdbc;
    public AuditService(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public void record(UUID actor, String action, UUID target) {
        record(actor,action,target,null);
    }
    public void record(UUID actor,String action,UUID target,UUID subjectUser) {
        jdbc.update("INSERT INTO audit_event(id, actor_id, action, target_id, subject_user_id, actor_display_name) VALUES (?, ?, ?, ?, ?, (SELECT display_name FROM app_user WHERE id=?))", UUID.randomUUID(), actor, action, target,subjectUser,actor);
    }
}
