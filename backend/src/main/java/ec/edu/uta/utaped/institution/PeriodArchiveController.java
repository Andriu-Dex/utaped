package ec.edu.uta.utaped.institution;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PeriodArchiveController {
    private final PeriodArchiveService service;
    public PeriodArchiveController(PeriodArchiveService service) { this.service=service; }
    @GetMapping("/admin/periods/{period}/control") public Map<String,Object> get(@PathVariable UUID period,Principal p) { return service.overview(period,p.getName()); }
    @PostMapping("/admin/periods/{period}/close") public Map<String,Object> close(@PathVariable UUID period,@Valid @RequestBody PeriodArchiveService.Close data,Principal p) { return service.close(period,p.getName(),data); }
    @GetMapping("/periods/{period}/archive") public ResponseEntity<byte[]> archive(@PathVariable UUID period,Principal p) {
        var bytes=service.download(period,p.getName());
        return ResponseEntity.ok().contentType(MediaType.parseMediaType("application/zip")).cacheControl(CacheControl.noStore())
            .header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"utaped-period-"+period+".zip\"")
            .header("X-Content-SHA256",ec.edu.uta.utaped.documents.StoredFiles.hash(bytes)).body(bytes);
    }
}
