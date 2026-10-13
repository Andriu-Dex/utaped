package ec.edu.uta.utaped.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Clears temporary personal data; batch metadata and audit remain available. */
@Component
public class UserImportMaintenance {
    private final JdbcTemplate jdbc;
    public UserImportMaintenance(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Scheduled(fixedDelayString="${app.identity.import-cleanup-delay-ms:600000}")
    public void purge() { jdbc.update("UPDATE user_import_batch SET rows='[]'::jsonb WHERE expires_at<now() AND rows<>'[]'::jsonb"); }
}
