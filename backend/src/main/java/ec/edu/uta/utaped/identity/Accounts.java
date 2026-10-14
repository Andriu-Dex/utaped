package ec.edu.uta.utaped.identity;

import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class Accounts {
    public record Account(UUID id, String email, String displayName, String systemRole, boolean active, boolean mustChangePassword, String username, String firstNames, String lastNames) {}
    private final JdbcTemplate jdbc;
    public Accounts(JdbcTemplate jdbc) { this.jdbc = jdbc; }
    public static String normalize(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public Account find(String email) {
        return jdbc.query("SELECT id,email,display_name,system_role,active,must_change_password,username,first_names,last_names FROM app_user WHERE email=?",
            (rs, n) -> new Account(rs.getObject("id", UUID.class), rs.getString("email"), rs.getString("display_name"), rs.getString("system_role"), rs.getBoolean("active"), rs.getBoolean("must_change_password"),rs.getString("username"),rs.getString("first_names"),rs.getString("last_names")), normalize(email))
            .stream().findFirst().orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicie sesión nuevamente."));
    }
    public Account current(String email) {
        Account account = find(email);
        if (!account.active()) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Inicie sesión nuevamente.");
        return account;
    }
    public static String defaultUsername(String email) {
        String value=normalize(email).split("@",2)[0].replaceAll("[^a-z0-9._-]","-");
        if(value.isEmpty() || !Character.isLetterOrDigit(value.charAt(0))) value="user-"+value;
        return value.substring(0,Math.min(100,value.length()));
    }
    public String loginKey(String input) {
        String normalized=normalize(input);
        return jdbc.queryForList("SELECT email FROM app_user WHERE email=? OR username=?",String.class,normalized,normalized).stream().findFirst().orElse(normalized);
    }
}
