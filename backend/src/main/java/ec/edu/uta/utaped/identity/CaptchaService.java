package ec.edu.uta.utaped.identity;

import jakarta.servlet.http.HttpServletRequest;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CaptchaService {
    private static final String BINDING="utaped.captcha.binding";
    private static final String ALPHABET="ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private final SecureRandom random=new SecureRandom();
    private final JdbcTemplate jdbc;
    public CaptchaService(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public record Challenge(UUID id, byte[] image) {}

    public Challenge issue(HttpServletRequest request) throws IOException {
        var session=request.getSession(true);
        String binding=(String)session.getAttribute(BINDING);
        if(binding==null) { binding=UUID.randomUUID().toString();session.setAttribute(BINDING,binding); }
        StringBuilder text=new StringBuilder();
        for(int i=0;i<6;i++) text.append(ALPHABET.charAt(random.nextInt(ALPHABET.length())));
        UUID id=UUID.randomUUID();
        byte[] image=render(text.toString());
        jdbc.update("DELETE FROM login_captcha WHERE expires_at<=now()");
        jdbc.update("""
            INSERT INTO login_captcha(id,session_hash,answer_hash,expires_at) VALUES (?,?,?,now()+interval '3 minutes')
            ON CONFLICT(session_hash) DO UPDATE SET id=excluded.id,answer_hash=excluded.answer_hash,expires_at=excluded.expires_at
            """,id,AuthThrottle.digest(binding),answerHash(id,text.toString()));
        return new Challenge(id,image);
    }
    public boolean consume(HttpServletRequest request) {
        var session=request.getSession(false);
        if(session==null || !(session.getAttribute(BINDING) instanceof String binding)) return false;
        // DELETE RETURNING makes attempts single-use even across concurrent requests/nodes.
        var rows=jdbc.queryForList("DELETE FROM login_captcha WHERE session_hash=? RETURNING id,answer_hash,expires_at>now() AS valid",AuthThrottle.digest(binding));
        String answer=request.getParameter("captcha");
        if(rows.isEmpty() || answer==null || answer.length()>12 || !Boolean.TRUE.equals(rows.getFirst().get("valid"))) return false;
        var row=rows.getFirst();
        return MessageDigest.isEqual(((String)row.get("answer_hash")).getBytes(StandardCharsets.US_ASCII),
            answerHash((UUID)row.get("id"),answer.trim().toUpperCase(Locale.ROOT)).getBytes(StandardCharsets.US_ASCII));
    }
    private static String answerHash(UUID id,String answer) { return AuthThrottle.digest(id+":"+answer); }
    private byte[] render(String answer) throws IOException {
        var image=new BufferedImage(300,88,BufferedImage.TYPE_INT_RGB);var graphics=image.createGraphics();
        try {
            graphics.setColor(new Color(236,245,248));graphics.fillRect(0,0,300,88);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
            for(int i=0;i<120;i++) { graphics.setColor(new Color(166+random.nextInt(50),196+random.nextInt(35),210+random.nextInt(25)));graphics.fillOval(random.nextInt(300),random.nextInt(88),2,2); }
            graphics.setFont(new Font(Font.SANS_SERIF,Font.BOLD,36));
            for(int i=0;i<answer.length();i++) {
                var transform=graphics.getTransform();graphics.rotate((random.nextDouble()-.5)*.22,34+i*43,48);
                graphics.setColor(new Color(15+random.nextInt(20),65+random.nextInt(25),100+random.nextInt(30)));
                graphics.drawString(answer.substring(i,i+1),24+i*43,58+random.nextInt(9)-4);graphics.setTransform(transform);
            }
            graphics.setColor(new Color(120,163,181));
            for(int i=0;i<3;i++) graphics.drawLine(0,random.nextInt(88),300,random.nextInt(88));
        } finally { graphics.dispose(); }
        var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);return output.toByteArray();
    }
}
