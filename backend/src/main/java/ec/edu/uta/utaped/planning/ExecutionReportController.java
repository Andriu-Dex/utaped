package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ExecutionReportController {
    private final ExecutionReportService service;
    public ExecutionReportController(ExecutionReportService service) { this.service=service; }
    @GetMapping("/execution-reports/options") public List<ExecutionReportService.Option> options(Principal p) { return service.options(p.getName()); }
    @PostMapping("/work-plans/{id}/execution-report") public WorkPlanModels.Plan create(@PathVariable UUID id,Principal p) { return service.create(id,p.getName()); }
    @GetMapping("/execution-reports/{id}") public ExecutionReportService.State get(@PathVariable UUID id,Principal p) { return service.get(id,p.getName()); }
    @PutMapping("/execution-reports/{id}") public ExecutionReportService.State save(@PathVariable UUID id,@Valid @RequestBody ExecutionReportService.Save data,Principal p) { return service.save(id,p.getName(),data); }
}
