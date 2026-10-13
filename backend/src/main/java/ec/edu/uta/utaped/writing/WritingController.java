package ec.edu.uta.utaped.writing;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class WritingController {
    private final WritingService service;
    public WritingController(WritingService service) { this.service=service; }
    @GetMapping("/writing/configuration") public AiProvider.Configuration configuration() { return service.configuration(); }
    @PostMapping("/work-plans/{doc}/writing") public ResponseEntity<WritingService.Suggestion> suggest(@PathVariable UUID doc, @Valid @RequestBody WritingService.Request data, Principal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.suggest(doc,principal.getName(),data));
    }
}
