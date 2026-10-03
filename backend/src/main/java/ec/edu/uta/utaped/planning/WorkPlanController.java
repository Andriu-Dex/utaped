package ec.edu.uta.utaped.planning;

import static ec.edu.uta.utaped.planning.WorkPlanModels.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import java.security.Principal;
import java.util.UUID;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/work-plans")
public class WorkPlanController {
    private final WorkPlanService service;
    public WorkPlanController(WorkPlanService service) { this.service=service; }
    @GetMapping("/options") public Options options(Principal principal) { return service.options(principal.getName()); }
    @GetMapping public Page list(Principal principal,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(50) int size,@RequestParam(required=false) UUID groupId,
        @RequestParam(required=false) UUID periodId,@RequestParam(defaultValue="") @Size(max=200) String query) {
        return service.list(principal.getName(),page,size,groupId,periodId,query.trim());
    }
    @PostMapping public Plan create(Principal principal,@Valid @RequestBody Create data) { return service.create(principal.getName(),data); }
    @GetMapping("/{targetDocId}") public Plan get(@PathVariable UUID targetDocId,Principal principal) { return service.get(targetDocId,principal.getName()); }
    @PutMapping("/{targetDocId}") public Plan update(@PathVariable UUID targetDocId,Principal principal,@Valid @RequestBody Update data) { return service.update(targetDocId,principal.getName(),data); }
}
