package ec.edu.uta.utaped.identity;

import java.security.Principal;
import java.util.UUID;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/user-imports")
public class UserImportController {
    private final UserImportService service;
    public UserImportController(UserImportService service) { this.service=service; }
    @PostMapping public UserImportService.Preview preview(@RequestParam MultipartFile file,Principal p) { return service.preview(p.getName(),file); }
    @GetMapping("/{id}") public UserImportService.Preview get(@PathVariable UUID id,Principal p) { return service.get(id,p.getName()); }
    @PostMapping("/{id}/commit") public ResponseEntity<byte[]> commit(@PathVariable UUID id,Principal p) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Content-Disposition","attachment; filename=\"credenciales-importadas.csv\"").header("Pragma","no-cache").contentType(MediaType.parseMediaType("text/csv;charset=UTF-8")).body(service.commit(id,p.getName()));
    }
}
