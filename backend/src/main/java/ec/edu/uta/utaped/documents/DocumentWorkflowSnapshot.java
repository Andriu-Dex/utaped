package ec.edu.uta.utaped.documents;

import ec.edu.uta.utaped.workflow.WorkflowModels.Definition;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Captures configuration, not execution assignments or institutional approval. */
@Component
public class DocumentWorkflowSnapshot {
    public record Participant(UUID id,String name,boolean active) {}
    public record Stage(UUID id,String label,String action,String recipientKind,String recipientLabel,
        String groupRole,boolean requiresSignature,List<Participant> participants) {}
    public record Workflow(UUID revisionId,int revisionNumber,String name,List<Stage> stages) {}
    private final JdbcTemplate jdbc;
    private final JsonMapper json=JsonMapper.builder().build();
    public DocumentWorkflowSnapshot(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public static String signatureAction(String action) {
        return switch(action) { case "APPROVE" -> "APROBADO_POR";case "VALIDATE" -> "VALIDADO_POR";default -> "REVISADO_POR"; };
    }

    // Caller must first authorize the explicit document; this component exposes no API.
    public Workflow capture(UUID groupId) {
        return capture(groupId,null);
    }
    public Workflow capture(UUID groupId,UUID scopeId) {
        return capture(groupId,scopeId,"T1");
    }
    public Workflow capture(UUID groupId,UUID scopeId,String type) {
        var revisions=jdbc.queryForList("SELECT r.id,r.revision_number,r.definition::text FROM workflow_configuration c JOIN workflow_revision r ON r.id=c.current_revision_id AND r.configuration_id=c.id WHERE c.group_id=? AND c.document_type=?",groupId,type);
        if(revisions.isEmpty()) return null;
        var revision=revisions.getFirst();var definition=json.readValue((String)revision.get("definition"),Definition.class);
        var stages=definition.stages().stream().map(stage->{
            List<Participant> people;
            if(stage.recipientKind().equals("GROUP_ROLE")) {
                people=scopeId==null?jdbc.query("SELECT u.id,u.display_name,u.active FROM membership m JOIN app_user u ON u.id=m.user_id WHERE m.group_id=? AND m.membership_role=? AND u.active ORDER BY u.id",
                    (rs,n)->new Participant(rs.getObject(1,UUID.class),rs.getString(2),rs.getBoolean(3)),groupId,stage.groupRole())
                    :jdbc.query("SELECT u.id,u.display_name,u.active FROM scope_member m JOIN app_user u ON u.id=m.user_id WHERE m.scope_id=? AND m.active AND m.membership_role=? AND u.active ORDER BY u.id",
                    (rs,n)->new Participant(rs.getObject(1,UUID.class),rs.getString(2),rs.getBoolean(3)),scopeId,stage.groupRole());
            } else {
                people=stage.assigneeIds().stream().map(id->jdbc.query("SELECT id,display_name,active FROM app_user WHERE id=?",
                    (rs,n)->new Participant(rs.getObject(1,UUID.class),rs.getString(2),rs.getBoolean(3)),id).stream().findFirst()
                    .orElse(new Participant(id,"Participante no disponible",false))).toList();
            }
            return new Stage(stage.id(),stage.label(),stage.action(),stage.recipientKind(),stage.recipientLabel(),stage.groupRole(),stage.requiresSignature(),people);
        }).toList();
        return new Workflow((UUID)revision.get("id"),((Number)revision.get("revision_number")).intValue(),definition.name(),stages);
    }
}
