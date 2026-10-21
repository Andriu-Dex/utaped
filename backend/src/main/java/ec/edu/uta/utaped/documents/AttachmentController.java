package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.AttachmentModels.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/work-plans/{targetDocId}/attachments")
public class AttachmentController {
    private final AttachmentService service;
    public AttachmentController(AttachmentService service) { this.service=service; }
    @GetMapping public State get(@PathVariable UUID targetDocId,Principal p) { return service.get(targetDocId,p.getName()); }
    @PutMapping("/settings") public State settings(@PathVariable UUID targetDocId,@Valid @RequestBody Settings body,Principal p) { return service.settings(targetDocId,p.getName(),body); }
    @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public State upload(@PathVariable UUID targetDocId,
        @RequestParam @PositiveOrZero long rowVersion,@RequestParam UUID requestKey,@RequestParam String title,@RequestPart MultipartFile file,Principal p) {
        return service.upload(targetDocId,p.getName(),rowVersion,requestKey,title,file);
    }
    @PutMapping("/order") public State reorder(@PathVariable UUID targetDocId,@Valid @RequestBody Reorder body,Principal p) { return service.reorder(targetDocId,p.getName(),body); }
    @PutMapping("/{attachmentId}") public State update(@PathVariable UUID targetDocId,@PathVariable UUID attachmentId,@Valid @RequestBody Update body,Principal p) { return service.update(targetDocId,attachmentId,p.getName(),body); }
    @DeleteMapping("/{attachmentId}") public State remove(@PathVariable UUID targetDocId,@PathVariable UUID attachmentId,@RequestParam @PositiveOrZero long rowVersion,Principal p) { return service.remove(targetDocId,attachmentId,p.getName(),rowVersion); }
    @GetMapping("/{attachmentId}/content") public ResponseEntity<byte[]> content(@PathVariable UUID targetDocId,@PathVariable UUID attachmentId,Principal p) {
        var meta=service.metadata(targetDocId,attachmentId,p.getName());
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,ContentDisposition.attachment().filename(meta.originalName(),StandardCharsets.UTF_8).build().toString())
            .body(service.content(targetDocId,attachmentId,p.getName()));
    }
}
