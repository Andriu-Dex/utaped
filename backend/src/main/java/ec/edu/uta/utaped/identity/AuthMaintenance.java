package ec.edu.uta.utaped.identity;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class AuthMaintenance {
    private final JdbcTemplate jdbc;
    public AuthMaintenance(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    @Scheduled(fixedDelay=3600000, initialDelay=3600000)
    public void removeExpiredAuthenticationData() {
        jdbc.update("DELETE FROM password_reset WHERE expires_at<now()");
        jdbc.update("DELETE FROM auth_throttle WHERE window_started_at<now()-interval '1 day'");
    }
}
