package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PlanCopyService {
    public record Copy(@NotNull @PositiveOrZero Long rowVersion,@NotNull UUID requestKey,
                       @NotNull @Size(min=1,max=10) List<@NotNull UUID> targetScopeIds) {}
    public record Options(long rowVersion,boolean canCopy,List<WorkPlanModels.ScopeOption> scopes) {}
    public record Result(List<UUID> documentIds) {}
    private final WorkPlanService plans;private final ActivityService activities;private final JdbcTemplate jdbc;
    private final Accounts accounts;private final AuditService audit;private final Validator validator;
    private final JsonMapper json=JsonMapper.builder().build();
    public PlanCopyService(WorkPlanService plans,ActivityService activities,JdbcTemplate jdbc,Accounts accounts,AuditService audit,Validator validator) {
        this.plans=plans;this.activities=activities;this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.validator=validator;
    }
    private boolean single(UUID scope) {
        return scope!=null&&jdbc.queryForObject("SELECT count(*) FROM scope_member m JOIN app_user u ON u.id=m.user_id WHERE m.scope_id=? AND m.active AND u.active",Integer.class,scope)==1;
    }
    private boolean eligible(WorkPlanModels.Plan source) {
        return "T1".equals(source.documentType())&&source.editable()&&source.scopeId()!=null&&single(source.scopeId())
            &&jdbc.queryForObject("SELECT career_id IS NOT NULL FROM organizational_scope WHERE id=?",Boolean.class,source.scopeId());
    }
    @Transactional(readOnly=true) public Options options(UUID sourceId,String email) {
        var source=plans.get(sourceId,email);boolean canCopy=eligible(source);
        var targets=canCopy?plans.options(email).scopes().stream().filter(s->s.canCreate()&&!s.id().equals(source.scopeId())&&s.groupId().equals(source.groupId())&&s.periodId().equals(source.periodId())&&single(s.id())
            &&jdbc.queryForObject("SELECT career_id IS NOT NULL AND career_id<>(SELECT career_id FROM organizational_scope WHERE id=?) FROM organizational_scope WHERE id=?",Boolean.class,source.scopeId(),s.id())).toList():List.<WorkPlanModels.ScopeOption>of();
        return new Options(source.rowVersion(),canCopy&&!targets.isEmpty(),targets);
    }
    @Transactional public Result copy(UUID sourceId,String email,Copy input) {
        UUID actor=accounts.current(email).id();
        jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",actor);
        var source=plans.lock(sourceId,email);
        var targets=input.targetScopeIds().stream().sorted().toList();
        if(new HashSet<>(targets).size()!=targets.size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No repita carreras de destino.");
        String encoded=json.writeValueAsString(targets);
        var previous=jdbc.queryForList("SELECT * FROM plan_copy_batch WHERE request_key=?",input.requestKey());
        if(!previous.isEmpty()) {
            var saved=previous.getFirst();
            if(!actor.equals(saved.get("actor_id"))||!sourceId.equals(saved.get("source_document_id"))||input.rowVersion()!=((Number)saved.get("source_row_version")).longValue()||!Arrays.asList(json.readValue(saved.get("target_scope_ids").toString(),UUID[].class)).equals(targets))
                throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya se utilizó para otra copia.");
            var ids=Arrays.asList(json.readValue(saved.get("document_ids").toString(),UUID[].class));
            ids.forEach(id->plans.get(id,email));return new Result(ids);
        }
        if(!eligible(source))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La copia requiere un borrador editable de una unidad unipersonal con carrera.");
        if(source.rowVersion()!=input.rowVersion())throw new ResponseStatusException(HttpStatus.CONFLICT,"El Plan cambió. Guarde y recargue antes de copiar.");
        // Keep lock order stable; all destinations belong to the already locked academic period.
        for(UUID target:targets)jdbc.queryForList("SELECT id FROM organizational_scope WHERE id=? FOR UPDATE",target);
        var allowed=options(sourceId,email).scopes().stream().map(WorkPlanModels.ScopeOption::id).toList();
        if(!allowed.containsAll(targets))throw new ResponseStatusException(HttpStatus.CONFLICT,"Alguna carrera no está disponible, ya tiene Plan o no pertenece a su unidad unipersonal.");
        var matrix=activities.get(sourceId,email);boolean savedMatrix=jdbc.queryForObject("SELECT count(*) FROM work_plan_matrix WHERE work_plan_id=?",Integer.class,sourceId)>0;
        var ids=new ArrayList<UUID>();
        for(UUID target:targets) {
            UUID key=UUID.nameUUIDFromBytes(("plan-copy:"+input.requestKey()+":"+target).getBytes(StandardCharsets.UTF_8));
            var created=plans.create(email,new WorkPlanModels.Create(source.groupId(),source.periodId(),key,null,target));
            jdbc.update("UPDATE work_plan SET justification=?,objective=?,privacy_notice_enabled=(SELECT privacy_notice_enabled FROM work_plan WHERE id=?),attachments_enabled=false,row_version=row_version+1,updated_at=now() WHERE id=?",source.justification(),source.objective(),sourceId,created.id());
            jdbc.update("UPDATE document_structure target SET revision=source.revision,sections=source.sections,contents=source.contents FROM document_structure source WHERE source.work_plan_id=? AND target.work_plan_id=?",sourceId,created.id());
            if(savedMatrix) {
                var rows=matrix.activities().stream().map(a->new ActivityModels.Activity(UUID.randomUUID(),a.catalogId(),a.title(),a.category(),a.mandatory(),a.startsOn(),a.endsOn(),a.responsibleIds(),a.collective(),a.resources(),a.means())).toList();
                var save=new ActivityModels.Save(plans.get(created.id(),email).rowVersion(),matrix.source(),rows);
                if(!validator.validate(save).isEmpty())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Complete las actividades guardadas del origen antes de copiarlas.");
                activities.save(created.id(),email,save);
            }
            ids.add(created.id());audit.record(actor,"WORK_PLAN_COPIED",created.id());
        }
        jdbc.update("INSERT INTO plan_copy_batch(request_key,source_document_id,actor_id,source_row_version,target_scope_ids,document_ids) VALUES (?,?,?,?,?::jsonb,?::jsonb)",input.requestKey(),sourceId,actor,input.rowVersion(),encoded,json.writeValueAsString(ids));
        return new Result(List.copyOf(ids));
    }
}
