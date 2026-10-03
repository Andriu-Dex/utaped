package ec.edu.uta.utaped.workflow;
import static ec.edu.uta.utaped.workflow.WorkflowModels.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/groups/{groupId}/workflows/{documentType}")
public class WorkflowController {
    private final WorkflowService service;
    public WorkflowController(WorkflowService service) { this.service=service; }
    @GetMapping public State get(@PathVariable UUID groupId,@PathVariable String documentType,Principal p) { return service.get(groupId,documentType,p.getName()); }
    @PutMapping public State save(@PathVariable UUID groupId,@PathVariable String documentType,@Valid @RequestBody Save body,Principal p) { return service.save(groupId,documentType,p.getName(),body); }
    @PostMapping("/revisions") public State configure(@PathVariable UUID groupId,@PathVariable String documentType,@Valid @RequestBody Version body,Principal p) { return service.configure(groupId,documentType,p.getName(),body); }
    @PostMapping("/disable") public State disable(@PathVariable UUID groupId,@PathVariable String documentType,@Valid @RequestBody Version body,Principal p) { return service.disable(groupId,documentType,p.getName(),body); }
}
