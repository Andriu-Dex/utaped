package ec.edu.uta.utaped.planning;
import java.util.UUID;
import java.security.Principal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.*;
@RestController
@RequestMapping("/api/work-plans/{doc}/formal-submission")
public class FormalSubmissionController {
    private final FormalSubmissionService service;public FormalSubmissionController(FormalSubmissionService service) { this.service=service; }
    @GetMapping public FormalSubmissionService.State state(@PathVariable UUID doc,Principal p) { return service.state(doc,p.getName()); }
    @PostMapping public FormalSubmissionService.State register(@PathVariable UUID doc,@RequestParam MultipartFile file,Principal p) throws java.io.IOException { return service.register(doc,p.getName(),file); }
    @GetMapping("/content") public ResponseEntity<byte[]> content(@PathVariable UUID doc,Principal p) { return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore()).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=constancia-quipux.pdf").body(service.content(doc,p.getName())); }
}
