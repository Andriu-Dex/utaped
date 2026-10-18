package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentAuthors {
    public record Author(UUID id,String name,String position) {}
    public record State(long rowVersion,boolean editable,List<Author> authors,List<Author> candidates) {}
    public record Save(@NotNull @PositiveOrZero Long rowVersion,@NotNull @Size(max=19) List<@NotNull UUID> coauthorIds) {}
    private final JdbcTemplate jdbc; private final WorkPlanService plans; private final Accounts accounts; private final AuditService audit;
    public DocumentAuthors(JdbcTemplate jdbc,WorkPlanService plans,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.plans=plans;this.accounts=accounts;this.audit=audit; }
    public List<Author> authors(UUID doc,String email) {
        var plan=plans.get(doc,email);var result=new ArrayList<Author>();
        result.add(person(plan.teacherId(),plan.scopeId(),"Responsable principal"));
        for(var id:jdbc.queryForList("SELECT user_id FROM document_author WHERE work_plan_id=? ORDER BY sort_order",UUID.class,doc))
            if(!id.equals(plan.teacherId())) result.add(person(id,plan.scopeId(),""));
        return List.copyOf(result);
    }
    private Author person(UUID id,UUID scope,String fallback) {
        return jdbc.query("SELECT u.id,u.display_name,coalesce(nullif(m.position_label,''),?) AS position FROM app_user u LEFT JOIN scope_member m ON m.user_id=u.id AND m.scope_id=? WHERE u.id=?",
            (rs,n)->new Author(id,rs.getString("display_name"),rs.getString("position")),fallback,scope,id).getFirst();
    }
    public State get(UUID doc,String email) {
        var plan=plans.get(doc,email);
        var candidates=plan.scopeId()==null?List.<Author>of():jdbc.query("SELECT u.id,u.display_name,m.position_label FROM scope_member m JOIN app_user u ON u.id=m.user_id WHERE m.scope_id=? AND m.active AND u.active ORDER BY u.display_name,u.id",
            (rs,n)->new Author(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3)),plan.scopeId());
        return new State(plan.rowVersion(),plan.editable(),authors(doc,email),candidates);
    }
    @Transactional public State save(UUID doc,String email,Save input) {
        var plan=plans.lock(doc,email);
        if(!plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable principal puede configurar elaboradores antes de firmar.");
        if(plan.rowVersion()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió. Recargue antes de guardar elaboradores.");
        var ids=new LinkedHashSet<>(input.coauthorIds());var candidates=get(doc,email).candidates().stream().map(Author::id).toList();
        if(ids.size()!=input.coauthorIds().size()||ids.contains(plan.teacherId())||!candidates.containsAll(ids))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione coautores activos del ámbito, sin repetir al responsable principal.");
        jdbc.update("DELETE FROM document_author WHERE work_plan_id=?",doc);int order=0;
        for(var id:ids) {
            jdbc.update("INSERT INTO document_author(work_plan_id,user_id,sort_order) VALUES (?,?,?)",doc,id,order++);
            jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?) ON CONFLICT DO NOTHING",doc,id,accounts.current(email).id());
        }
        jdbc.update("UPDATE work_plan SET row_version=row_version+1,updated_at=now() WHERE id=?",doc);
        audit.record(accounts.current(email).id(),"DOCUMENT_AUTHORS_UPDATED",doc);return get(doc,email);
    }
}
@RestController
@RequestMapping("/api/work-plans/{targetDocId}/authors")
class DocumentAuthorsController {
    private final DocumentAuthors service;
    DocumentAuthorsController(DocumentAuthors service) { this.service=service; }
    @GetMapping DocumentAuthors.State get(@PathVariable UUID targetDocId,Principal principal) { return service.get(targetDocId,principal.getName()); }
    @PutMapping DocumentAuthors.State save(@PathVariable UUID targetDocId,@Valid @RequestBody DocumentAuthors.Save input,Principal principal) { return service.save(targetDocId,principal.getName(),input); }
}
