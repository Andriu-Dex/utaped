package ec.edu.uta.utaped.identity;

import ec.edu.uta.utaped.audit.AuditService;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Async;

@Service
public class PasswordService {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder encoder;
    private final JdbcIndexedSessionRepository sessions;
    private final JavaMailSender mail;
    private final AuditService audit;
    private final String publicUrl;
    private final String mailFrom;
    public PasswordService(JdbcTemplate jdbc, PasswordEncoder encoder, JdbcIndexedSessionRepository sessions, JavaMailSender mail, AuditService audit,
        @Value("${app.public-url}") String publicUrl, @Value("${app.mail-from}") String mailFrom) {
        this.jdbc=jdbc; this.encoder=encoder; this.sessions=sessions; this.mail=mail; this.audit=audit; this.publicUrl=publicUrl; this.mailFrom=mailFrom;
    }
    public static void validate(String password) {
        if (password == null || password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use al menos 12 caracteres y un máximo de 72 bytes UTF-8.");
    }
    private void revoke(String email) { sessions.findByPrincipalName(email).keySet().forEach(sessions::deleteById); }
    @Transactional public void administrativeReset(Accounts.Account actor,UUID id,long rowVersion,String next) {
        if(!actor.systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"No tiene permiso para restablecer esta cuenta.");
        if(actor.id().equals(id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use Cambiar contraseña para su propia cuenta.");
        validate(next);
        var rows=jdbc.queryForList("SELECT email,password_hash,row_version FROM app_user WHERE id=? AND active FOR UPDATE",id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario activo no disponible.");
        var row=rows.getFirst();
        if(((Number)row.get("row_version")).longValue()!=rowVersion) throw new ResponseStatusException(HttpStatus.CONFLICT,"La cuenta cambió. Recargue antes de restablecerla.");
        if(encoder.matches(next,(String)row.get("password_hash"))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use una contraseña temporal diferente.");
        jdbc.update("UPDATE app_user SET password_hash=?,must_change_password=true,row_version=row_version+1 WHERE id=?",encoder.encode(next),id);
        jdbc.update("DELETE FROM password_reset WHERE user_id=?",id);revoke((String)row.get("email"));
        audit.record(actor.id(),"ADMIN_PASSWORD_RESET",id);
    }
    @Transactional public void change(Accounts.Account account, String current, String next) {
        validate(next);
        String hash=jdbc.queryForObject("SELECT password_hash FROM app_user WHERE id=? FOR UPDATE",String.class,account.id());
        if (current == null || current.length()>128 || !encoder.matches(current,hash)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La contraseña actual es incorrecta.");
        if (encoder.matches(next,hash)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Use una contraseña diferente.");
        update(account.id(),account.email(),next,"PASSWORD_CHANGED");
    }
    private void update(UUID id, String email, String password, String action) {
        jdbc.update("UPDATE app_user SET password_hash=?,must_change_password=false WHERE id=?",encoder.encode(password),id);
        jdbc.update("DELETE FROM password_reset WHERE user_id=?",id);
        revoke(email);
        audit.record(id,action,id);
    }
    @Async("recoveryExecutor") @Transactional public void forgot(String email) {
        var users=jdbc.query("SELECT id FROM app_user WHERE email=? AND active=true",(rs,n)->rs.getObject(1,UUID.class),Accounts.normalize(email));
        if (users.isEmpty()) return;
        byte[] bytes=new byte[32]; new SecureRandom().nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        UUID id=users.getFirst();
        jdbc.update("DELETE FROM password_reset WHERE user_id=? OR expires_at<now()",id);
        jdbc.update("INSERT INTO password_reset(token_hash,user_id,expires_at) VALUES (?,?,now()+interval '20 minutes')",AuthThrottle.digest(token),id);
        var message=new SimpleMailMessage(); message.setFrom(mailFrom); message.setTo(Accounts.normalize(email));
        message.setSubject("Recuperación de acceso a UTAPED");
        message.setText("Para cambiar su contraseña abra este enlace (vence en 20 minutos):\n"+publicUrl+"/#reset="+token+"\nSi no lo solicitó, ignore este mensaje.");
        try { mail.send(message); } catch (org.springframework.mail.MailException error) {
            jdbc.update("DELETE FROM password_reset WHERE user_id=?",id);
            audit.record(null,"PASSWORD_RESET_DELIVERY_FAILED",id);
        }
    }
    @Transactional public void reset(String token,String password) {
        validate(password);
        var ids=jdbc.query("SELECT user_id FROM password_reset WHERE token_hash=? AND expires_at>now()",(rs,n)->rs.getObject(1,UUID.class),AuthThrottle.digest(token));
        if (ids.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace expiró o ya fue utilizado.");
        UUID id=ids.getFirst();
        jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",id);
        if (jdbc.queryForObject("SELECT count(*) FROM password_reset WHERE token_hash=? AND expires_at>now()",Integer.class,AuthThrottle.digest(token))==0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace expiró o ya fue utilizado.");
        var emails=jdbc.queryForList("SELECT email FROM app_user WHERE id=? AND active=true",String.class,id);
        if (emails.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace expiró o ya fue utilizado.");
        String email=emails.getFirst();
        update(id,email,password,"PASSWORD_RESET");
    }
}
