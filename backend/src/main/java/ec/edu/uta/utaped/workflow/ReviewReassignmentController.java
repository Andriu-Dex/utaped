package ec.edu.uta.utaped.workflow;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews/tasks/{taskId}/reassignment")
public class ReviewReassignmentController {
    private final ReviewReassignmentService service;
    public ReviewReassignmentController(ReviewReassignmentService service) { this.service=service; }
    @GetMapping public ResponseEntity<ReviewReassignmentService.Options> options(@PathVariable UUID taskId,Principal actor) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.options(taskId,actor.getName()));
    }
    @PostMapping public ReviewReassignmentService.Options replace(@PathVariable UUID taskId,Principal actor,@Valid @RequestBody ReviewReassignmentService.Replace input) {
        return service.replace(taskId,actor.getName(),input);
    }
}
