package ec.edu.uta.utaped.notifications;

import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/notifications")
public class NotificationController {
    private final NotificationService service;
    public NotificationController(NotificationService service) { this.service=service; }
    @GetMapping public NotificationService.Page list(Principal p,@RequestParam(required=false) Boolean read,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.list(p.getName(),read,page,size); }
    @GetMapping("/unread-count") public Map<String,Long> unread(Principal p) { return Map.of("unread",service.unread(p.getName())); }
    @PostMapping("/{id}/read") public void read(Principal p,@PathVariable UUID id) { service.read(p.getName(),id); }
    @PostMapping("/read-all") public Map<String,Long> readAll(Principal p) { return Map.of("updated",service.readAll(p.getName())); }
    @PostMapping("/{id}/open") public NotificationService.Target open(Principal p,@PathVariable UUID id) { return service.open(p.getName(),id); }
}
