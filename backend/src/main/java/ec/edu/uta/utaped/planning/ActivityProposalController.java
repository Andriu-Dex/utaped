package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ActivityProposalController {
    private final ActivityProposals service;
    public ActivityProposalController(ActivityProposals service) { this.service=service; }
    @GetMapping("/reports/{doc}/activity-proposals") public List<Map<String,Object>> own(@PathVariable UUID doc,Principal p) { return service.own(doc,p.getName()); }
    @PostMapping("/reports/{doc}/activity-proposals") public UUID create(@PathVariable UUID doc,@Valid @RequestBody ActivityProposals.Create input,Principal p) { return service.create(doc,p.getName(),input); }
    @GetMapping("/admin/planning/groups/{group}/activity-proposals") public List<Map<String,Object>> pending(@PathVariable UUID group,Principal p) { return service.pending(group,p.getName()); }
    @PostMapping("/admin/planning/activity-proposals/{id}/accept") public ActivityModels.Definition accept(@PathVariable UUID id,@Valid @RequestBody ActivityProposals.Accept input,Principal p) { return service.accept(id,p.getName(),input); }
    @PostMapping("/admin/planning/activity-proposals/{id}/reject") public void reject(@PathVariable UUID id,@Valid @RequestBody ActivityProposals.Reject input,Principal p) { service.reject(id,p.getName(),input); }
}
