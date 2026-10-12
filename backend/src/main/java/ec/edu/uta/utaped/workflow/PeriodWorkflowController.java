package ec.edu.uta.utaped.workflow;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class PeriodWorkflowController {
    private final PeriodWorkflowService service;
    public PeriodWorkflowController(PeriodWorkflowService service) { this.service=service; }
    @GetMapping("/scopes/{scope}/workflows") public List<Map<String,Object>> get(@PathVariable UUID scope,Principal p) { return service.assignments(scope,p.getName()); }
    @PutMapping("/scopes/{scope}/workflows") public void batch(@PathVariable UUID scope,@Valid @RequestBody PeriodWorkflowService.Batch data,Principal p) { service.assignBatch(scope,p.getName(),data); }
    @PutMapping("/scopes/{scope}/workflows/{type}") public void assign(@PathVariable UUID scope,@PathVariable String type,@Valid @RequestBody PeriodWorkflowService.Assign data,Principal p) { service.assign(scope,type,p.getName(),data); }
    @PostMapping("/periods/{period}/activate-workflows") public void activate(@PathVariable UUID period,@Valid @RequestBody PeriodWorkflowService.Version data,Principal p) { service.activate(period,p.getName(),data); }
}
