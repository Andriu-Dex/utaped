package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.ArtifactModels.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/work-plans/{targetDocId}/artifacts")
public class ArtifactController {
    private final ArtifactService service;
    public ArtifactController(ArtifactService service) { this.service=service; }
    @GetMapping("/readiness") public Readiness readiness(@PathVariable UUID targetDocId,Principal p) { return service.readiness(targetDocId,p.getName()); }
    @GetMapping public List<Artifact> list(@PathVariable UUID targetDocId,Principal p) { return service.list(targetDocId,p.getName()); }
    @PostMapping public Artifact generate(@PathVariable UUID targetDocId,@Valid @RequestBody Generate input,Principal p) { return service.generate(targetDocId,p.getName(),input); }
    @GetMapping("/{artifactId}/signature-preparation") public ResponseEntity<SignaturePreparation> signaturePreparation(@PathVariable UUID targetDocId,@PathVariable UUID artifactId,Principal p) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(service.signaturePreparation(targetDocId,artifactId,p.getName()));
    }
    @GetMapping("/{artifactId}/content") public ResponseEntity<byte[]> content(@PathVariable UUID targetDocId,@PathVariable UUID artifactId,Principal p) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"plan-trabajo-t1.pdf\"").body(service.content(targetDocId,artifactId,p.getName()));
    }
    @GetMapping("/{artifactId}/pages/{pageIndex}") public ResponseEntity<byte[]> page(@PathVariable UUID targetDocId,@PathVariable UUID artifactId,@PathVariable int pageIndex,Principal p) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).cacheControl(CacheControl.noStore()).body(service.page(targetDocId,artifactId,pageIndex,p.getName()));
    }
}
