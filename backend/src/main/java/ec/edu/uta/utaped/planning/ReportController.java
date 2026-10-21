package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ReportController {
    private final ReportService service;
    public ReportController(ReportService service) { this.service=service; }
    @GetMapping("/reports/options") public ReportService.Options options(Principal p,@RequestParam(defaultValue="") String query,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size) { return service.options(p.getName(),query,page,size); }
    @PostMapping("/work-plans/{id}/report") public WorkPlanModels.Plan create(@PathVariable UUID id,Principal p) { return service.create(id,p.getName()); }
    @GetMapping("/reports/{id}") public ReportService.State get(@PathVariable UUID id,Principal p) { return service.get(id,p.getName()); }
    @PutMapping("/reports/{id}") public ReportService.State save(@PathVariable UUID id,@Valid @RequestBody ReportService.Save data,Principal p) { return service.save(id,p.getName(),data); }
}
