package ec.edu.uta.utaped.identity;

import java.util.UUID;
import ec.edu.uta.utaped.audit.AuditService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BootstrapAdmin implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final String email;
    private final String password;
    public BootstrapAdmin(JdbcTemplate jdbc,PasswordEncoder encoder,AuditService audit,@Value("${app.bootstrap.email}") String email,@Value("${app.bootstrap.password}") String password) {
        this.jdbc=jdbc;this.encoder=encoder;this.audit=audit;this.email=email;this.password=password;
    }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(email.isBlank() && password.isBlank()) return;
        if(jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class)>0) return;
        if(email.isBlank() || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$") || email.length()>254) throw new IllegalArgumentException("Configure bootstrap administrator email");
        PasswordService.validate(password); UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role) VALUES (?,?,?,?,?)",id,Accounts.normalize(email),"Administrador inicial",encoder.encode(password),"ADMIN");
        audit.record(id,"ADMIN_BOOTSTRAPPED",id);
    }
}
