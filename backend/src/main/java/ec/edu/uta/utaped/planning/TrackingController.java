package ec.edu.uta.utaped.planning;

import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tracking")
public class TrackingController {
    private final TrackingService service;
    public TrackingController(TrackingService service) { this.service=service; }
    @GetMapping public TrackingService.Summary get(Principal p,@RequestParam(required=false) UUID groupId,@RequestParam(required=false) UUID periodId) { return service.get(p.getName(),groupId,periodId); }
}
