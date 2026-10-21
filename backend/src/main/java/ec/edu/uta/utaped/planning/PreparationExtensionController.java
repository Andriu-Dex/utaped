package ec.edu.uta.utaped.planning;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organization/scopes/{scope}/preparation-extension")
public class PreparationExtensionController {
    private final PreparationExtensionService service;
    public PreparationExtensionController(PreparationExtensionService service) { this.service=service; }
    @GetMapping public PreparationExtensionService.State get(@PathVariable UUID scope,Principal p) { return service.get(scope,p.getName()); }
    @PutMapping public PreparationExtensionService.State grant(@PathVariable UUID scope,@Valid @RequestBody PreparationExtensionService.Grant input,Principal p) { return service.grant(scope,p.getName(),input); }
}
