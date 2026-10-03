package ec.edu.uta.utaped.workflow;

import static ec.edu.uta.utaped.workflow.WorkflowModels.*;
import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class WorkflowService {
    private final JdbcTemplate jdbc;private final Accounts accounts;private final AuditService audit;
    private final JsonMapper json=JsonMapper.builder().build();
    public WorkflowService(JdbcTemplate jdbc,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.audit=audit; }
    private void admin(String email) { if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo administración puede configurar flujos."); }
    private boolean group(UUID id) { return jdbc.query("SELECT active FROM institutional_group WHERE id=?",(rs,n)->rs.getBoolean(1),id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Grupo no disponible.")); }
    private void type(String type) { if(!Set.of("T1","T2").contains(type)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Tipo documental no disponible."); }
    private List<String> blockers(UUID groupId,Definition definition) {
        var result=new ArrayList<String>();var ids=new HashSet<UUID>();
        if(definition.stages().isEmpty()) result.add("Agregue al menos una etapa.");
        for(var stage:definition.stages()) {
            if(!ids.add(stage.id())) result.add("No repita identificadores de etapa.");
            var recipients=new HashSet<>(stage.assigneeIds());
            if(recipients.size()!=stage.assigneeIds().size()) result.add(stage.label()+": no repita participantes.");
            for(var user:recipients) if(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id=? AND active",Integer.class,user)==0) result.add(stage.label()+": seleccione participantes activos.");
            switch(stage.recipientKind()) {
                case "PERSON" -> { if(recipients.isEmpty() || stage.groupRole()!=null || !stage.recipientLabel().isBlank()) result.add(stage.label()+": seleccione personas, sin rol ni órgano colegiado."); }
                case "GROUP_ROLE" -> {
                    if(stage.groupRole()==null || !recipients.isEmpty() || !stage.recipientLabel().isBlank()) result.add(stage.label()+": seleccione un rol de pertenencia, sin personas ni órgano colegiado.");
                    else if(jdbc.queryForObject("SELECT count(*) FROM membership m JOIN app_user u ON u.id=m.user_id WHERE m.group_id=? AND m.membership_role=? AND u.active",Integer.class,groupId,stage.groupRole())==0) result.add(stage.label()+": el rol no tiene integrantes activos.");
                }
                case "COLLEGIATE" -> {
                    if(stage.recipientLabel().isBlank() || stage.groupRole()!=null) result.add(stage.label()+": indique el órgano colegiado, sin rol de pertenencia.");
                    if(stage.requiresSignature() && recipients.isEmpty()) result.add(stage.label()+": una firma personal requiere identificar representantes.");
                    if(!stage.requiresSignature() && !recipients.isEmpty()) result.add(stage.label()+": un órgano sin firma personal no requiere representantes individuales.");
                }
                default -> result.add("Tipo de destinatario no disponible.");
            }
        }
        return List.copyOf(result);
    }
    public State get(UUID groupId,String type,String email) {
        admin(email);type(type);boolean active=group(groupId);
        var roots=jdbc.queryForList("SELECT id,row_version,draft::text,current_revision_id FROM workflow_configuration WHERE group_id=? AND document_type=?",groupId,type);
        if(roots.isEmpty()) return new State(0,active,new Definition("",List.of()),null,List.of(),List.of("Guarde la configuración inicial."));
        var root=roots.getFirst();var draft=json.readValue((String)root.get("draft"),Definition.class);
        var revisions=jdbc.query("SELECT r.*,u.display_name FROM workflow_revision r JOIN app_user u ON u.id=r.actor_id WHERE configuration_id=? ORDER BY revision_number DESC",(rs,n)->new Revision(rs.getObject("id",UUID.class),rs.getInt("revision_number"),json.readValue(rs.getString("definition"),Definition.class),rs.getString("display_name"),rs.getObject("configured_at",OffsetDateTime.class)),root.get("id"));
        var current=revisions.stream().filter(r->r.id().equals(root.get("current_revision_id"))).findFirst().orElse(null);
        return new State(((Number)root.get("row_version")).longValue(),active,draft,current,revisions,blockers(groupId,draft));
    }
    private UUID lock(UUID groupId,String type,String email,long version) {
        admin(email);type(type);
        jdbc.queryForList("SELECT id FROM institutional_group WHERE id=? FOR UPDATE",groupId);
        if(!group(groupId)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Reactive el grupo antes de cambiar su configuración.");
        jdbc.update("INSERT INTO workflow_configuration(id,group_id,document_type,draft) VALUES (?,?,?,?::jsonb) ON CONFLICT(group_id,document_type) DO NOTHING",UUID.randomUUID(),groupId,type,json.writeValueAsString(new Definition("",List.of())));
        var root=jdbc.queryForMap("SELECT id,row_version FROM workflow_configuration WHERE group_id=? AND document_type=? FOR UPDATE",groupId,type);
        if(((Number)root.get("row_version")).longValue()!=version) throw new ResponseStatusException(HttpStatus.CONFLICT,"El flujo cambió en otra sesión. Recargue antes de guardar.");
        return (UUID)root.get("id");
    }
    @Transactional public State save(UUID groupId,String type,String email,Save input) {
        UUID id=lock(groupId,type,email,input.rowVersion());
        if(new HashSet<>(input.definition().stages().stream().map(Stage::id).toList()).size()!=input.definition().stages().size()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No repita identificadores de etapa.");
        jdbc.update("UPDATE workflow_configuration SET draft=?::jsonb,row_version=row_version+1 WHERE id=?",json.writeValueAsString(input.definition()),id);
        audit.record(accounts.current(email).id(),"WORKFLOW_DRAFT_SAVED",id);return get(groupId,type,email);
    }
    @Transactional public State configure(UUID groupId,String type,String email,Version input) {
        UUID id=lock(groupId,type,email,input.rowVersion());var state=get(groupId,type,email);
        if(!state.blockers().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,String.join(" ",state.blockers()));
        // Technical revision only: no document transitions, assignments or simulated signatures.
        UUID revision=UUID.randomUUID();int number=state.revisions().stream().mapToInt(Revision::revisionNumber).max().orElse(0)+1;
        jdbc.update("INSERT INTO workflow_revision(id,configuration_id,revision_number,definition,actor_id) VALUES (?,?,?,?::jsonb,?)",revision,id,number,json.writeValueAsString(state.draft()),accounts.current(email).id());
        jdbc.update("UPDATE workflow_configuration SET current_revision_id=?,row_version=row_version+1 WHERE id=?",revision,id);
        audit.record(accounts.current(email).id(),"WORKFLOW_REVISION_CONFIGURED",id);return get(groupId,type,email);
    }
    @Transactional public State disable(UUID groupId,String type,String email,Version input) {
        UUID id=lock(groupId,type,email,input.rowVersion());jdbc.update("UPDATE workflow_configuration SET current_revision_id=NULL,row_version=row_version+1 WHERE id=?",id);
        audit.record(accounts.current(email).id(),"WORKFLOW_CONFIGURATION_DISABLED",id);return get(groupId,type,email);
    }
}
