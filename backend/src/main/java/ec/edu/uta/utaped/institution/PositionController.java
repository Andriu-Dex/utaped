package ec.edu.uta.utaped.institution;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
public class PositionController {
    private final PositionService service;
    public PositionController(PositionService service) { this.service=service; }
    @PutMapping("/positions/{id}/history-permission") public void historyPermission(@PathVariable UUID id,@Valid @RequestBody PositionService.HistoryPermission input,Principal p) { service.historyPermission(id,p.getName(),input); }
    @GetMapping("/positions") public List<Map<String,Object>> positions(Principal p) { return service.positions(p.getName()); }
    @PostMapping("/positions") public Map<String,UUID> create(@Valid @RequestBody PositionService.PositionInput input,Principal p) { return Map.of("id",service.create(p.getName(),input)); }
    @PutMapping("/positions/{id}") public void update(@PathVariable UUID id,@Valid @RequestBody PositionService.PositionUpdate input,Principal p) { service.update(id,p.getName(),input); }
    @GetMapping("/periods/{period}/position-assignments") public List<Map<String,Object>> assignments(@PathVariable UUID period,Principal p) { return service.assignments(period,p.getName()); }
    @PutMapping("/periods/{period}/position-assignments") public void assign(@PathVariable UUID period,@Valid @RequestBody PositionService.AssignmentInput input,Principal p) { service.assign(period,p.getName(),input); }
    @DeleteMapping("/periods/{period}/position-assignments/{assignment}") public void remove(@PathVariable UUID period,@PathVariable UUID assignment,Principal p) { service.remove(period,assignment,p.getName()); }
    @PostMapping("/periods/{period}/position-assignments/import") public PositionService.ImportResult importCsv(@PathVariable UUID period,@Valid @RequestBody PositionService.CsvInput input,Principal p) { return service.importCsv(period,p.getName(),input.csv()); }
}
