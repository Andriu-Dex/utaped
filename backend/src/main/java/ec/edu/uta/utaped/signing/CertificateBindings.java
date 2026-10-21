package ec.edu.uta.utaped.signing;

import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CertificateBindings {
    public record Binding(UUID id,String fingerprint,String verificationNote,boolean active,OffsetDateTime createdAt) {}
    public record Register(@NotBlank @Pattern(regexp="[a-fA-F0-9]{64}") String fingerprint,
        @AssertTrue boolean identityVerified,@NotBlank @Size(max=500) String verificationNote,UUID expectedBindingId) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final AuditService audit;
    public CertificateBindings(JdbcTemplate jdbc,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.audit=audit; }
    private void admin(String email) { if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo administración puede vincular certificados públicos."); }
    public List<Binding> list(UUID user,String email) { admin(email);return query(user,false); }
    private List<Binding> query(UUID user,boolean active) {
        return jdbc.query("SELECT * FROM signing_certificate_binding WHERE user_id=? AND (?=false OR active) ORDER BY created_at DESC,id",
            (rs,n)->new Binding(rs.getObject("id",UUID.class),rs.getString("fingerprint"),rs.getString("verification_note"),rs.getBoolean("active"),rs.getObject("created_at",OffsetDateTime.class)),user,active);
    }
    public Binding current(UUID user) { return query(user,true).stream().findFirst().orElse(null); }
    @Transactional public Binding register(UUID user,String email,Register input) {
        admin(email);
        if(!input.identityVerified()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Compruebe la identidad del titular antes de vincular la huella.");
        String fingerprint=input.fingerprint().toLowerCase(Locale.ROOT);
        jdbc.queryForList("SELECT pg_advisory_xact_lock(hashtextextended(?,0))",fingerprint);
        var users=jdbc.queryForList("SELECT active FROM app_user WHERE id=? FOR UPDATE",user);
        if(users.isEmpty() || !Boolean.TRUE.equals(users.getFirst().get("active"))) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Seleccione un usuario activo.");
        var existing=current(user);
        if(!Objects.equals(existing==null?null:existing.id(),input.expectedBindingId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"La vinculación cambió. Recargue antes de continuar.");
        if(existing!=null && existing.fingerprint().equals(fingerprint)) return existing;
        if(jdbc.queryForObject("SELECT count(*) FROM signing_certificate_binding WHERE fingerprint=? AND active",Integer.class,fingerprint)>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"La huella ya está vinculada a otra cuenta.");
        jdbc.update("UPDATE signing_certificate_binding SET active=false WHERE user_id=? AND active",user);
        var id=UUID.randomUUID();jdbc.update("INSERT INTO signing_certificate_binding(id,user_id,fingerprint,verification_note,verified_by) VALUES (?,?,?,?,?)",id,user,fingerprint,input.verificationNote().trim(),accounts.current(email).id());
        audit.record(accounts.current(email).id(),"SIGNING_CERTIFICATE_BOUND",user);return current(user);
    }
    @Transactional public void revoke(UUID user,UUID bindingId,String email) {
        admin(email);jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",user);
        if(jdbc.update("UPDATE signing_certificate_binding SET active=false WHERE id=? AND user_id=? AND active",bindingId,user)>0)
            audit.record(accounts.current(email).id(),"SIGNING_CERTIFICATE_UNBOUND",user);
    }
}
