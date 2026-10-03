package ec.edu.uta.utaped.planning;

import static ec.edu.uta.utaped.planning.ActivityModels.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ActivityController {
    private final ActivityService service;
    public ActivityController(ActivityService service) { this.service=service; }
    @GetMapping("/work-plans/{targetDocId}/matrix") public Matrix matrix(@PathVariable UUID targetDocId,Principal p) { return service.get(targetDocId,p.getName()); }
    @PutMapping("/work-plans/{targetDocId}/matrix") public Matrix save(@PathVariable UUID targetDocId,@Valid @RequestBody Save body,Principal p) { return service.save(targetDocId,p.getName(),body); }
    @GetMapping("/admin/planning/catalogs") public List<Catalog> catalogs() { return service.catalogs(); }
    @PostMapping("/admin/planning/catalogs") public Catalog createCatalog(@Valid @RequestBody CatalogInput body,Principal p) { return service.saveCatalog(null,body,p.getName()); }
    @PutMapping("/admin/planning/catalogs/{id}") public Catalog updateCatalog(@PathVariable UUID id,@Valid @RequestBody CatalogInput body,Principal p) { return service.saveCatalog(id,body,p.getName()); }
    @GetMapping("/admin/planning/groups/{groupId}/activities") public List<Definition> definitions(@PathVariable UUID groupId) { return service.definitions(groupId); }
    @PostMapping("/admin/planning/activities") public Definition createDefinition(@Valid @RequestBody DefinitionInput body,Principal p) { return service.saveDefinition(null,body,p.getName()); }
    @PutMapping("/admin/planning/activities/{id}") public Definition updateDefinition(@PathVariable UUID id,@Valid @RequestBody DefinitionInput body,Principal p) { return service.saveDefinition(id,body,p.getName()); }
    @PutMapping("/admin/planning/groups/{groupId}/collective-label") public void label(@PathVariable UUID groupId,@Valid @RequestBody Label body,Principal p) { service.label(groupId,body,p.getName()); }
    @GetMapping("/admin/planning/holidays") public List<Holiday> holidays() { return service.holidays(); }
    @PostMapping("/admin/planning/holidays") public void holiday(@Valid @RequestBody Holiday body,Principal p) { service.holiday(body,p.getName()); }
    @DeleteMapping("/admin/planning/holidays/{date}") public void deleteHoliday(@PathVariable LocalDate date,Principal p) { service.deleteHoliday(date,p.getName()); }
    @PutMapping("/admin/planning/periods/{periodId}/holiday-policy") public void policy(@PathVariable UUID periodId,@Valid @RequestBody Policy body,Principal p) { service.policy(periodId,body,p.getName()); }
}
