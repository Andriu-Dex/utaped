package ec.edu.uta.utaped.identity;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AuthThrottle {
    private final JdbcTemplate jdbc;
    public AuthThrottle(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public static String digest(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public void check(String key, int limit) {
        int attempts = jdbc.queryForObject("""
            INSERT INTO auth_throttle(key_hash,attempts,window_started_at) VALUES (?,1,now())
            ON CONFLICT(key_hash) DO UPDATE SET
              attempts=CASE WHEN auth_throttle.window_started_at < now()-interval '15 minutes' THEN 1 ELSE auth_throttle.attempts+1 END,
              window_started_at=CASE WHEN auth_throttle.window_started_at < now()-interval '15 minutes' THEN now() ELSE auth_throttle.window_started_at END
            RETURNING attempts
            """, Integer.class, digest(key));
        if (attempts > limit) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Demasiados intentos. Espere 15 minutos.");
    }
}
