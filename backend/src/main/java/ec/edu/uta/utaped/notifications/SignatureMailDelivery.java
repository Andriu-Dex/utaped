package ec.edu.uta.utaped.notifications;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A small durable delivery queue; SMTP runs outside the signature request. */
@Service
@ConditionalOnProperty(name="app.notifications.signature-mail-enabled",havingValue="true",matchIfMissing=true)
public class SignatureMailDelivery {
    private final JdbcTemplate jdbc;
    private final JavaMailSender mail;
    private final String publicUrl,from;
    public SignatureMailDelivery(JdbcTemplate jdbc,JavaMailSender mail,@Value("${app.public-url}") String publicUrl,@Value("${app.mail-from}") String from) {
        this.jdbc=jdbc;this.mail=mail;this.publicUrl=publicUrl.replaceAll("/+$","");this.from=from;
    }
    @Scheduled(fixedDelayString="${app.notifications.signature-mail-delay-ms:15000}",initialDelayString="${app.notifications.signature-mail-initial-delay-ms:60000}")
    @Transactional public int deliverPending() {
        var pending=jdbc.queryForList("""
            SELECT d.notification_id,n.target_id,n.message,u.email,u.active
            FROM signature_mail_delivery d JOIN personal_notification n ON n.id=d.notification_id
            JOIN app_user u ON u.id=n.recipient_id
            WHERE d.delivered_at IS NULL AND d.available_at<=now() AND d.attempts<8
            ORDER BY d.available_at LIMIT 10 FOR UPDATE OF d SKIP LOCKED
            """);
        int delivered=0;
        for(var row:pending) {
            UUID id=(UUID)row.get("notification_id");
            if(!Boolean.TRUE.equals(row.get("active"))) {
                jdbc.update("UPDATE signature_mail_delivery SET delivered_at=now(),last_error_code='RECIPIENT_INACTIVE' WHERE notification_id=?",id);continue;
            }
            var message=new SimpleMailMessage();message.setFrom(from);message.setTo((String)row.get("email"));
            message.setSubject("UTAPED · Documento pendiente de su firma");
            message.setText(row.get("message")+"\n\nAbra el documento en UTAPED:\n"+publicUrl+"/?view=documents&doc="+row.get("target_id")+"&step=signing&workspace=documents");
            try {
                mail.send(message);
                jdbc.update("UPDATE signature_mail_delivery SET delivered_at=now(),attempts=attempts+1,last_error_code=NULL WHERE notification_id=?",id);delivered++;
            } catch(MailException error) {
                // Do not log message bodies, addresses or SMTP credentials.
                jdbc.update("UPDATE signature_mail_delivery SET attempts=attempts+1,available_at=now()+make_interval(mins=>LEAST(60,(attempts+1)*5)),last_error_code='SMTP_DELIVERY_FAILED' WHERE notification_id=?",id);
            }
        }
        return delivered;
    }
}
