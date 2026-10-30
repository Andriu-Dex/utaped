package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-plans/{sourceId}/career-copies")
public class PlanCopyController {
    private final PlanCopyService service;
    public PlanCopyController(PlanCopyService service) { this.service=service; }
    @GetMapping public ResponseEntity<PlanCopyService.Options> options(@PathVariable UUID sourceId,Principal actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.options(sourceId,actor.getName()));
    }
    @PostMapping public PlanCopyService.Result copy(@PathVariable UUID sourceId,Principal actor,@Valid @RequestBody PlanCopyService.Copy input) {
        return service.copy(sourceId,actor.getName(),input);
    }
}
