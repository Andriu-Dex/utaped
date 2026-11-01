package ec.edu.uta.utaped.planning;

import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {
    private final TrackingService service;
    public TrackingController(TrackingService service) { this.service=service; }
    @GetMapping("/recent") public ResponseEntity<WorkPlanModels.Page> recent(Principal p,@RequestParam(required=false) UUID groupId,@RequestParam(required=false) UUID periodId) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.recent(p.getName(),groupId,periodId));
    }
    @GetMapping public TrackingService.Summary get(Principal p,@RequestParam(required=false) UUID groupId,@RequestParam(required=false) UUID periodId) { return service.get(p.getName(),groupId,periodId); }
}
