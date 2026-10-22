package ec.edu.uta.utaped.planning;
import java.security.Principal;
import java.util.*;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api")
public class DocumentStructureController {
    private final DocumentStructures structures;private final WorkPlanService plans;private final JdbcTemplate jdbc;private final ec.edu.uta.utaped.audit.AuditService audit;private final ec.edu.uta.utaped.identity.Accounts accounts;
    public DocumentStructureController(DocumentStructures structures,WorkPlanService plans,JdbcTemplate jdbc,ec.edu.uta.utaped.audit.AuditService audit,ec.edu.uta.utaped.identity.Accounts accounts) { this.structures=structures;this.plans=plans;this.jdbc=jdbc;this.audit=audit;this.accounts=accounts; }
    @GetMapping("/admin/document-structures/{type}") public DocumentStructures.Structure template(@PathVariable String type,Principal p) { return structures.template(type,p.getName()); }
    @PutMapping("/admin/document-structures/{type}") public DocumentStructures.Structure save(@PathVariable String type,@Valid @RequestBody DocumentStructures.Save input,Principal p) { return structures.save(type,p.getName(),input); }
    @GetMapping("/work-plans/{doc}/structure") public Map<String,Object> get(@PathVariable UUID doc,Principal p) { var plan=plans.get(doc,p.getName());var result=new LinkedHashMap<String,Object>();result.put("rowVersion",plan.rowVersion());result.put("editable",plan.editable());result.put("structure",structures.snapshot(doc));return result; }
    @PutMapping("/work-plans/{doc}/structure") @Transactional public Map<String,Object> values(@PathVariable UUID doc,@Valid @RequestBody DocumentStructures.Values input,Principal p) { var plan=plans.lock(doc,p.getName());if(!plan.editable())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Documento de solo lectura.");if(plan.rowVersion()!=input.rowVersion())throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió. Recargue antes de guardar.");structures.values(doc,input);jdbc.update("UPDATE work_plan SET row_version=row_version+1,updated_at=now() WHERE id=?",doc);audit.record(accounts.current(p.getName()).id(),"DOCUMENT_SECTIONS_SAVED",doc);return get(doc,p); }
}
