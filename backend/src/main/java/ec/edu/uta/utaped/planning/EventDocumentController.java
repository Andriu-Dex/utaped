package ec.edu.uta.utaped.planning;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/work-plans/{doc}/event")
public class EventDocumentController {
    private final EventDocuments events;
    public EventDocumentController(EventDocuments events) { this.events=events; }
    @GetMapping public EventDocuments.State get(@PathVariable UUID doc,Principal p) { return events.get(doc,p.getName()); }
    @PutMapping public EventDocuments.State save(@PathVariable UUID doc,Principal p,@RequestBody @Valid EventDocuments.Save input) { return events.save(doc,p.getName(),input); }
}
