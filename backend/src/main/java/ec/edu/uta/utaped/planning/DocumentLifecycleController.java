package ec.edu.uta.utaped.planning;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/work-plans/{doc}")
public class DocumentLifecycleController {
    private final DocumentLifecycleService service;
    public DocumentLifecycleController(DocumentLifecycleService service) { this.service=service; }
    @GetMapping("/versions") public List<Map<String,Object>> history(@PathVariable UUID doc,Principal p) { return service.history(doc,p.getName()); }
    @PostMapping("/archive") public WorkPlanModels.Plan archive(@PathVariable UUID doc,@Valid @RequestBody DocumentLifecycleService.Change data,Principal p) { return service.archive(doc,p.getName(),data); }
    @PostMapping("/versions") public WorkPlanModels.Plan next(@PathVariable UUID doc,@Valid @RequestBody DocumentLifecycleService.Change data,Principal p) { return service.nextVersion(doc,p.getName(),data); }
}
