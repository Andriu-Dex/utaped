package ec.edu.uta.utaped.identity;

import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Accounts {
    public record Account(UUID id, String email, String displayName, String systemRole, boolean active, boolean mustChangePassword) {}
    private final JdbcTemplate jdbc;
    public Accounts(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public Account find(String email) {
        return jdbc.query("SELECT id,email,display_name,system_role,active,must_change_password FROM app_user WHERE email=?",
            (rs, n) -> new Account(rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("display_name"), rs.getString("system_role"), rs.getBoolean("active"), rs.getBoolean("must_change_password")), normalize(email))
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicie sesión nuevamente."));
    }
    public Account current(String email) {
        Account account = find(email);
        if (!account.active()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicie sesión nuevamente.");
        return account;
    }
}
