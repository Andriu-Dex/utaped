package ec.edu.uta.utaped.audit;

import jakarta.validation.constraints.*;
import java.security.Principal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
public class AuditController {
    private final AuditQueryService service;
    public AuditController(AuditQueryService service) { this.service=service; }
    @GetMapping("/api/admin/audit-events/actions") public List<String> actions(Principal p) { return service.actions(p.getName()); }
    @GetMapping("/api/admin/audit-events") public AuditQueryService.Page administration(Principal p,
        @RequestParam(defaultValue="") @Size(max=80) String action,@RequestParam(required=false) UUID actorId,@RequestParam(required=false) UUID targetId,
        @RequestParam(required=false) OffsetDateTime from,@RequestParam(required=false) OffsetDateTime to,
        @RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return service.administration(p.getName(),action,actorId,targetId,from,to,page,size);
    }
    @GetMapping("/api/work-plans/{targetDocId}/history") public AuditQueryService.Page history(Principal p,@PathVariable UUID targetDocId,
        @RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        return service.history(p.getName(),targetDocId,page,size);
    }
}
