package ec.edu.uta.utaped.planning;
import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/reporting")
public class ReportingController {
    private final ReportingService service;
    public ReportingController(ReportingService service) { this.service=service; }
    @GetMapping public ReportingService.Summary get(Principal p,@RequestParam(required=false) UUID groupId,@RequestParam(required=false) UUID periodId,@RequestParam(defaultValue="false") boolean archived) { return service.get(p.getName(),groupId,periodId,archived); }
    @GetMapping("/export") public ResponseEntity<byte[]> export(Principal p,@RequestParam(required=false) UUID groupId,@RequestParam(required=false) UUID periodId,@RequestParam(defaultValue="false") boolean archived) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=seguimiento-utaped.csv").body(service.csv(p.getName(),groupId,periodId,archived)); }
}
