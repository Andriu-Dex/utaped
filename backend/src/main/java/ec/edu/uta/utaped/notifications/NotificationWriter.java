package ec.edu.uta.utaped.notifications;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class NotificationWriter {
    private final JdbcTemplate jdbc;
    public NotificationWriter(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public void fromEvent(UUID event,UUID actor,String action,UUID target,UUID subject) {
        UUID recipient;String type,title,message;
        switch(action) {
            case "MEMBERSHIP_ASSIGNED" -> { recipient=subject;type="GROUP";title="Pertenencia actualizada";message="Su acceso o rol de pertenencia al grupo fue actualizado."; }
            case "MEMBERSHIP_REMOVED" -> { recipient=subject;type="GROUP";title="Pertenencia retirada";message="Su pertenencia fue retirada. El acceso al grupo y sus documentos se comprueba con sus permisos actuales."; }
            case "T1_PREVIEW_GENERATED" -> { recipient=actor;type="WORK_PLAN";title="Previsualización T1 guardada";message="Se guardó una previsualización del Plan. No constituye firma ni aprobación."; }
            default -> { return; }
        }
        if(recipient==null || target==null) return;
        String table=type.equals("GROUP")?"institutional_group":"work_plan",column=type.equals("GROUP")?"name":"title";
        String objectName=jdbc.queryForList("SELECT "+column+" FROM "+table+" WHERE id=?",String.class,target).stream().findFirst().orElse("");
        message+=(objectName.isBlank()?"":" Objeto: "+objectName);
        jdbc.update("INSERT INTO personal_notification(id,recipient_id,source_event_id,kind,target_type,target_id,title,message) SELECT ?,id,?,?,?,?,?,? FROM app_user WHERE id=? AND active ON CONFLICT(source_event_id,recipient_id) DO NOTHING",UUID.randomUUID(),event,action,type,target,title,message,recipient);
    }
}
