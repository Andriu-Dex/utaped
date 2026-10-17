package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.planning.DocumentTemplateModels.DocumentTemplate;
import ec.edu.uta.utaped.planning.DocumentTemplateModels.ResetInput;
import ec.edu.uta.utaped.planning.DocumentTemplateModels.UpdateInput;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@Validated
@RequestMapping("/api/admin/document-templates")
public class DocumentTemplateController {
    private final DocumentTemplateService service;

    public DocumentTemplateController(DocumentTemplateService service) {
        this.service = service;
    }

    @GetMapping
    public List<DocumentTemplate> list() {
        return service.list();
    }

    @GetMapping("/{type}")
    public DocumentTemplate get(@PathVariable String type) {
        return service.get(type);
    }

    @PutMapping("/{type}")
    public DocumentTemplate update(@PathVariable String type, @Valid @RequestBody UpdateInput input, Principal principal) {
        return service.update(type, principal.getName(), input);
    }

    @PostMapping("/{type}/reset")
    public DocumentTemplate reset(@PathVariable String type, @Valid @RequestBody ResetInput input, Principal principal) {
        return service.reset(type, principal.getName(), input);
    }
}
