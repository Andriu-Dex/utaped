package ec.edu.uta.utaped.identity;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import java.util.Map;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final Accounts accounts;
    private final PasswordService passwords;
    private final AuthThrottle throttle;
    private final CaptchaService captcha;
    public AuthController(Accounts accounts,PasswordService passwords,AuthThrottle throttle,CaptchaService captcha) { this.accounts=accounts;this.passwords=passwords;this.throttle=throttle;this.captcha=captcha; }
    @GetMapping(value="/captcha",produces="image/png") public org.springframework.http.ResponseEntity<byte[]> captcha(HttpServletRequest request) throws java.io.IOException {
        throttle.check("captcha-ip:"+request.getRemoteAddr(),100);
        var challenge=captcha.issue(request);
        return org.springframework.http.ResponseEntity.ok().header("Cache-Control","no-store, private").header("Vary","Cookie")
            .header("X-Captcha-Id",challenge.id().toString()).body(challenge.image());
    }
    @GetMapping("/csrf") public Map<String,String> csrf(CsrfToken token) { return Map.of("headerName",token.getHeaderName(),"token",token.getToken()); }
    @GetMapping("/me") public Accounts.Account me(Principal principal) { return accounts.current(principal.getName()); }
    public record Change(@NotBlank @Size(max=128) String currentPassword, @NotBlank @Size(max=72) String newPassword) {}
    @PostMapping("/change-password") public void change(@Valid @RequestBody Change data,Principal principal,HttpServletRequest request) {
        passwords.change(accounts.current(principal.getName()),data.currentPassword(),data.newPassword());
        request.getSession().invalidate();
    }
    public record Forgot(@NotBlank @Email @Size(max=254) String email) {}
    @PostMapping("/forgot-password") public Map<String,String> forgot(@Valid @RequestBody Forgot data,HttpServletRequest request) {
        throttle.check("forgot:"+Accounts.normalize(data.email()),3);
        throttle.check("forgot-ip:"+request.getRemoteAddr(),30);
        try { passwords.forgot(data.email()); }
        catch (org.springframework.core.task.TaskRejectedException error) {
            // Keep the same public response when the bounded delivery queue is saturated.
        }
        return Map.of("message","Si el correo está registrado, recibirá un enlace de recuperación.");
    }
    public record Reset(@NotBlank @Size(min=43,max=43) String token,@NotBlank @Size(max=72) String newPassword) {}
    @PostMapping("/reset-password") public void reset(@Valid @RequestBody Reset data,HttpServletRequest request) {
        throttle.check("reset-ip:"+request.getRemoteAddr(),30);
        passwords.reset(data.token(),data.newPassword());
    }
}
