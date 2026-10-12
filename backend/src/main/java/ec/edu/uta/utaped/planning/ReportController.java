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
    @PostMapping("/work-plans/{id}/report") public WorkPlanModels.Plan create(@PathVariable UUID id,Principal p) { return service.create(id,p.getName()); }
    @GetMapping("/reports/{id}") public ReportService.State get(@PathVariable UUID id,Principal p) { return service.get(id,p.getName()); }
    @PutMapping("/reports/{id}") public ReportService.State save(@PathVariable UUID id,@Valid @RequestBody ReportService.Save data,Principal p) { return service.save(id,p.getName(),data); }
}
