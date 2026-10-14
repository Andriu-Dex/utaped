package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import jakarta.validation.constraints.PositiveOrZero;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Validated
@RequestMapping("/api/work-plans/{doc}/execution")
public class EvidenceController {
    private final EvidenceService service;
    public EvidenceController(EvidenceService service) { this.service=service; }
    @GetMapping public EvidenceService.State get(@PathVariable UUID doc,Principal p) { return service.get(doc,p.getName()); }
    @PostMapping(value="/activities/{activity}/media/{medium}",consumes=MediaType.MULTIPART_FORM_DATA_VALUE)
    public EvidenceService.Evidence upload(@PathVariable UUID doc,@PathVariable UUID activity,@PathVariable @PositiveOrZero int medium,@RequestParam @PositiveOrZero int expectedVersion,@RequestPart MultipartFile file,Principal p) { return service.upload(doc,activity,medium,expectedVersion,p.getName(),file); }
    @PutMapping("/activities/{activity}/extension") public void extend(@PathVariable UUID doc,@PathVariable UUID activity,@Valid @RequestBody EvidenceService.Extension data,Principal p) { service.extend(doc,activity,p.getName(),data); }
    @PostMapping("/activities/{activity}/media/{medium}/reference") public EvidenceService.Evidence reference(@PathVariable UUID doc,@PathVariable UUID activity,@PathVariable int medium,@Valid @RequestBody EvidenceService.Reference input,Principal p) { return service.reference(doc,activity,medium,p.getName(),input); }
    @GetMapping("/extensions/history") public java.util.List<java.util.Map<String,Object>> history(@PathVariable UUID doc,Principal p) { return service.extensionHistory(doc,p.getName()); }
    @GetMapping("/evidence/{id}/content") public ResponseEntity<byte[]> content(@PathVariable UUID doc,@PathVariable UUID id,Principal p) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).body(service.content(doc,id,p.getName())); }
}
