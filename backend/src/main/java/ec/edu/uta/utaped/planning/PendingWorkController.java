package ec.edu.uta.utaped.planning;

import jakarta.servlet.http.HttpServletRequest;
import java.security.Principal;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/pending-work")
public class PendingWorkController {
    private final PendingWorkService service;
    public PendingWorkController(PendingWorkService service) { this.service=service; }
    @GetMapping public ResponseEntity<PendingWorkService.Page> get(Principal actor,HttpServletRequest request,
        @RequestParam(defaultValue="") String kind,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="10") int size) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(actor.getName(),request.isSecure(),kind,page,size));
    }
}
