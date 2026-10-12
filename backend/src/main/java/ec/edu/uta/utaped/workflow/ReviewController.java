package ec.edu.uta.utaped.workflow;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import java.io.IOException;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service=service; }
    @GetMapping("/reviews/tasks") public List<ReviewService.Task> inbox(Principal p) { return service.inbox(p.getName()); }
    @GetMapping("/work-plans/{doc}/reviews") public List<Map<String,Object>> history(@PathVariable UUID doc,Principal p) { return service.history(doc,p.getName()); }
    @GetMapping("/reviews/{id}") public ReviewService.State state(@PathVariable UUID id,Principal p) { return service.state(id,p.getName()); }
    @GetMapping("/reviews/{id}/pages/{page}") public ResponseEntity<byte[]> page(@PathVariable UUID id,@PathVariable int page,Principal p) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(service.page(id,page,p.getName())); }
    @GetMapping("/reviews/{id}/content") public ResponseEntity<byte[]> content(@PathVariable UUID id,Principal p) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).body(service.content(id,p.getName())); }
    @PostMapping("/reviews/tasks/{id}/observations") public void observe(@PathVariable UUID id,@Valid @RequestBody ReviewService.Observation data,Principal p) { service.observe(id,p.getName(),data); }
    @PostMapping("/reviews/tasks/{id}/decisions") public void decide(@PathVariable UUID id,@Valid @RequestBody ReviewService.Decision data,Principal p) { service.decide(id,p.getName(),data); }
    @PostMapping(value="/reviews/tasks/{id}/signing",consumes=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public void sign(@PathVariable UUID id,@RequestParam long rowVersion,@RequestParam String inputHash,@RequestParam UUID requestKey,Principal p,HttpServletRequest request) throws IOException {
        if(request.getContentLengthLong()>1049092) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Material de firma demasiado grande.");
        byte[] body=request.getInputStream().readNBytes(1049093);
        try { service.sign(id,p.getName(),rowVersion,inputHash,requestKey,body,request.isSecure()); } finally { Arrays.fill(body,(byte)0); }
    }
}
