package ec.edu.uta.utaped.planning;

import java.security.Principal;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-plans/{targetDocId}/timeline")
public class DocumentTimelineController {
    private final DocumentTimelineService service;

    public DocumentTimelineController(DocumentTimelineService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentTimelineService.Item> get(
        @PathVariable UUID targetDocId,
        @RequestParam(defaultValue = "asc") String order,
        Principal principal
    ) {
        return service.timeline(targetDocId, principal.getName(), order);
    }
}

