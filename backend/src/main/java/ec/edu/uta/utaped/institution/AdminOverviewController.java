package ec.edu.uta.utaped.institution;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/overview")
public class AdminOverviewController {
    private final AdminOverviewService service;
    public AdminOverviewController(AdminOverviewService service) { this.service=service; }
    @GetMapping public ResponseEntity<AdminOverviewService.Summary> get(Principal actor,@RequestParam(required=false) UUID periodId,@RequestParam(defaultValue="false") boolean allPeriods) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.get(actor.getName(),periodId,allPeriods));
    }
}
