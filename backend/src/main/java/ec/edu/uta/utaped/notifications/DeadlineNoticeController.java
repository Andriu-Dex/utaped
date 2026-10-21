package ec.edu.uta.utaped.notifications;

import ec.edu.uta.utaped.identity.Accounts;
import java.security.Principal;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/deadline-notices")
public class DeadlineNoticeController {
    private final DeadlineNoticeService service;private final Accounts accounts;
    public DeadlineNoticeController(DeadlineNoticeService service,Accounts accounts) { this.service=service;this.accounts=accounts; }
    private void admin(Principal p) { if(!accounts.current(p.getName()).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido."); }
    @GetMapping public Map<String,Object> configuration(Principal p) { admin(p);return service.configuration(); }
    @PostMapping("/scan") public Map<String,Integer> scan(Principal p) { admin(p);return Map.of("sent",service.generate(service.today())); }
}
