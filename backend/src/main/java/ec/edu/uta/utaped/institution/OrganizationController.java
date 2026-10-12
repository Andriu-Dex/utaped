package ec.edu.uta.utaped.institution;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class OrganizationController {
    private final OrganizationService service;
    public OrganizationController(OrganizationService service) { this.service=service; }
    @GetMapping("/careers") public List<Map<String,Object>> careers() { return service.careers(); }
    @GetMapping("/organization/scopes") public List<Map<String,Object>> scopes(Principal p) { return service.scopes(p.getName()); }
    @GetMapping("/organization/scopes/{id}/members") public List<Map<String,Object>> members(@PathVariable UUID id,Principal p) { return service.members(id,p.getName()); }
    @PostMapping("/admin/careers") public Map<String,UUID> career(@Valid @RequestBody OrganizationService.CareerInput input,Principal p) { return Map.of("id",service.createCareer(p.getName(),input)); }
    @PostMapping("/admin/organization/scopes") public Map<String,UUID> scope(@Valid @RequestBody OrganizationService.ScopeInput input,Principal p) { return Map.of("id",service.createScope(p.getName(),input)); }
    @PutMapping("/admin/organization/scopes/{id}/members") public void member(@PathVariable UUID id,@Valid @RequestBody OrganizationService.MemberInput input,Principal p) { service.member(id,p.getName(),input); }
    @PutMapping("/admin/organization/scopes/{id}/principal") public void principal(@PathVariable UUID id,@Valid @RequestBody OrganizationService.PrincipalInput input,Principal p) { service.principal(id,p.getName(),input); }
}
