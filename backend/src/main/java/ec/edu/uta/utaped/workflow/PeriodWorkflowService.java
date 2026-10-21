package ec.edu.uta.utaped.workflow;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.documents.DocumentWorkflowSnapshot;
import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.constraints.*;
import jakarta.validation.Valid;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PeriodWorkflowService {
    public record Assign(@NotNull UUID revisionId,@NotNull @PositiveOrZero Long rowVersion) {}
    public record Version(@NotNull @PositiveOrZero Long rowVersion) {}
    public record AssignmentInput(@NotBlank @Pattern(regexp="T1|T2|T3") String type,@NotNull UUID revisionId,@NotNull @PositiveOrZero Long rowVersion) {}
    public record Batch(@NotNull @Size(min=1,max=3) List<@NotNull @Valid AssignmentInput> assignments) {}
    private final JdbcTemplate jdbc;
    private final Accounts accounts;
    private final AuditService audit;
    private final DocumentWorkflowSnapshot snapshots;
    private final JsonMapper json=JsonMapper.builder().build();
    public PeriodWorkflowService(JdbcTemplate jdbc,Accounts accounts,AuditService audit,DocumentWorkflowSnapshot snapshots) {
        this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.snapshots=snapshots;
    }
    private void admin(String email) {
        if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido.");
    }
    private Map<String,Object> scope(UUID id) {
        return jdbc.queryForList("SELECT s.*,p.status,p.workflow_locked_at FROM organizational_scope s JOIN academic_period p ON p.id=s.period_id WHERE s.id=?",id).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ámbito no disponible."));
    }
    public List<Map<String,Object>> assignments(UUID scope,String email) {
        admin(email);scope(scope);
        return jdbc.queryForList("SELECT a.*,r.revision_number,r.definition FROM period_workflow_assignment a JOIN workflow_revision r ON r.id=a.revision_id WHERE scope_id=? ORDER BY document_type",scope);
    }
    @Transactional public void assign(UUID scopeId,String type,String email,Assign input) {
        admin(email);
        if(!Set.of("T1","T2","T3").contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo documental no disponible.");
        var scope=scope(scopeId);
        jdbc.queryForList("SELECT id FROM academic_period WHERE id=? FOR UPDATE",scope.get("period_id"));
        scope=scope(scopeId);
        boolean late=scope.get("workflow_locked_at")!=null;
        if(!scope.get("status").equals("ACTIVE") || late&&(!type.equals("T3")||jdbc.queryForObject("SELECT count(*) FROM period_workflow_assignment WHERE scope_id=? AND document_type=?",Integer.class,scopeId,type)>0)) throw new ResponseStatusException(HttpStatus.CONFLICT,"El flujo del período está bloqueado; solo puede habilitar por primera vez el formato de ejecución.");
        if(!(boolean)scope.get("active")) throw new ResponseStatusException(HttpStatus.CONFLICT,"El ámbito está inactivo.");
        if(jdbc.queryForObject("SELECT count(*) FROM workflow_revision r JOIN workflow_configuration c ON c.id=r.configuration_id WHERE r.id=? AND c.group_id=? AND c.document_type=?",Integer.class,input.revisionId(),scope.get("group_id"),type)==0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione una revisión publicada del grupo y tipo documental correspondientes.");
        var previous=jdbc.queryForList("SELECT row_version FROM period_workflow_assignment WHERE scope_id=? AND document_type=?",scopeId,type);
        long version=previous.isEmpty()?0:((Number)previous.getFirst().get("row_version")).longValue();
        if(version!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"La asignación cambió. Recargue antes de guardar.");
        jdbc.update("INSERT INTO period_workflow_assignment(scope_id,document_type,revision_id,assigned_by) VALUES (?,?,?,?) ON CONFLICT(scope_id,document_type) DO UPDATE SET revision_id=excluded.revision_id,assigned_by=excluded.assigned_by,assigned_at=now(),row_version=period_workflow_assignment.row_version+1",scopeId,type,input.revisionId(),accounts.current(email).id());
        if(late) {
            var workflow=snapshots.captureNewType((UUID)scope.get("group_id"),scopeId,type);
            if(workflow==null||workflow.stages().isEmpty()||scope.get("principal_user_id")==null)throw new ResponseStatusException(HttpStatus.CONFLICT,"Configure el responsable y los participantes del nuevo formato.");
            var seen=new HashSet<UUID>();for(var stage:workflow.stages()) { if(stage.participants().isEmpty())throw new ResponseStatusException(HttpStatus.CONFLICT,"El nuevo flujo requiere participantes asignados.");for(var actor:stage.participants())if(!actor.active()||actor.id().equals(scope.get("principal_user_id"))||!seen.add(actor.id()))throw new ResponseStatusException(HttpStatus.CONFLICT,"Seleccione participantes activos, independientes y sin duplicados."); }
            jdbc.update("UPDATE period_workflow_assignment SET frozen_workflow=?::jsonb,row_version=row_version+1 WHERE scope_id=? AND document_type=?",json.writeValueAsString(workflow),scopeId,type);
            audit.record(accounts.current(email).id(),"PERIOD_DOCUMENT_TYPE_ENABLED",scopeId);
        }
        audit.record(accounts.current(email).id(),"PERIOD_WORKFLOW_ASSIGNED",scopeId);
    }
    @Transactional public void assignBatch(UUID scope,String email,Batch input) {
        if(input.assignments().stream().map(AssignmentInput::type).distinct().count()!=input.assignments().size())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No repita tipos documentales.");
        for(var item:input.assignments()) assign(scope,item.type(),email,new Assign(item.revisionId(),item.rowVersion()));
    }
    @Transactional public void activate(UUID period,String email,Version input) {
        admin(email);
        var rows=jdbc.queryForList("SELECT * FROM academic_period WHERE id=? FOR UPDATE",period);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Período no disponible.");
        var current=rows.getFirst();
        if(!current.get("status").equals("ACTIVE")||current.get("workflow_locked_at")!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"El período ya está bloqueado o cerrado.");
        if(((Number)current.get("row_version")).longValue()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El período cambió. Recargue antes de activar.");
        var scopes=jdbc.queryForList("SELECT s.id,s.group_id,s.principal_user_id FROM organizational_scope s JOIN institutional_group g ON g.id=s.group_id AND g.active WHERE s.period_id=? AND s.active ORDER BY s.id FOR SHARE OF s",period);
        if(scopes.isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Configure al menos un ámbito antes de activar los flujos.");
        for(var scope:scopes) for(String type:List.of("T1","T2","T3")) {
            // Execution reports (T3) are optional: only scopes with an assigned T3 flow get one frozen.
            if(type.equals("T3")&&jdbc.queryForObject("SELECT count(*) FROM period_workflow_assignment WHERE scope_id=? AND document_type='T3'",Integer.class,scope.get("id"))==0) continue;
            var workflow=snapshots.capture((UUID)scope.get("group_id"),(UUID)scope.get("id"),type);
            if(scope.get("principal_user_id")==null||workflow==null||workflow.stages().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Cada ámbito requiere responsable principal y flujos T1/T2 asignados.");
            var seen=new HashSet<UUID>();
            for(var stage:workflow.stages()) {
                if(stage.participants().isEmpty()) throw new ResponseStatusException(HttpStatus.CONFLICT,stage.label()+": asigne ejecutores individuales antes de activar.");
                for(var actor:stage.participants()) if(!actor.active()||actor.id().equals(scope.get("principal_user_id"))||!seen.add(actor.id()))
                    throw new ResponseStatusException(HttpStatus.CONFLICT,stage.label()+": revise participantes activos, independientes del elaborador y sin duplicados.");
            }
            jdbc.update("UPDATE period_workflow_assignment SET frozen_workflow=?::jsonb,row_version=row_version+1 WHERE scope_id=? AND document_type=?",json.writeValueAsString(workflow),scope.get("id"),type);
        }
        jdbc.update("UPDATE academic_period SET workflow_locked_at=now(),workflow_locked_by=?,row_version=row_version+1 WHERE id=?",accounts.current(email).id(),period);
        audit.record(accounts.current(email).id(),"PERIOD_WORKFLOWS_ACTIVATED",period);
    }
}
