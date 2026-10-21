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
            case "ADMIN_PASSWORD_RESET", "COORDINATOR_PASSWORD_RESET" -> { recipient=subject;type="ACCOUNT";title="Acceso restablecido";message="Su contraseña fue restablecida. Debe cambiar la contraseña temporal al ingresar. Si no solicitó esta acción, contacte al administrador."; }
            case "MEMBERSHIP_ASSIGNED" -> { recipient=subject;type="GROUP";title="Pertenencia actualizada";message="Su acceso o rol de pertenencia al grupo fue actualizado."; }
            case "MEMBERSHIP_REMOVED" -> { recipient=subject;type="GROUP";title="Pertenencia retirada";message="Su pertenencia fue retirada. El acceso al grupo y sus documentos se comprueba con sus permisos actuales."; }
            case "T1_PREVIEW_GENERATED" -> { recipient=actor;type="WORK_PLAN";title="Previsualización T1 guardada";message="Se guardó una previsualización del Plan. No constituye firma ni aprobación."; }
            case "T2_PREVIEW_GENERATED" -> { recipient=actor;type="WORK_PLAN";title="Previsualización T2 guardada";message="Se guardó una previsualización del Informe con sus evidencias conservadas."; }
            case "ACTIVITY_EXTENSION_GRANTED" -> { recipient=subject;type="WORK_PLAN";title="Prórroga individual concedida";message="Consulte el nuevo plazo personal de su actividad en ejecución."; }
            case "DOCUMENT_ARCHIVED" -> { recipient=subject;type="WORK_PLAN";title="Documento archivado";message="El documento permanece disponible como historial de solo lectura."; }
            case "AUTHOR_SIGNATURE_REQUESTED" -> { recipient=subject;type="WORK_PLAN";title="Documento pendiente de su firma";message="Es su turno como elaborador. Abra el documento y continúe en Firma y Finalización; el contenido ya está congelado."; }
            case "REVIEW_TASK_ASSIGNED" -> { recipient=subject;type="WORK_PLAN";title="Documento pendiente de revisión";message="Tiene una etapa activa. Revise el documento firmado en su bandeja de revisión."; }
            case "DOCUMENT_RETURNED" -> { recipient=subject;type="WORK_PLAN";title="Documento devuelto";message="Consulte las observaciones, corrija y firme un nuevo artefacto para la siguiente ronda."; }
            case "DOCUMENT_APPROVED" -> { recipient=subject;type="WORK_PLAN";title="Documento aprobado";message="Todas las etapas configuradas han concluido. El artefacto y su historial se conservan."; }
            default -> { return; }
        }
        if(recipient==null || target==null) return;
        String table=type.equals("GROUP")?"institutional_group":"work_plan",column=type.equals("GROUP")?"name":"title";
        String objectName=type.equals("ACCOUNT")?"":jdbc.queryForList("SELECT "+column+" FROM "+table+" WHERE id=?",String.class,target).stream().findFirst().orElse("");
        message+=(objectName.isBlank()?"":" Objeto: "+objectName);
        jdbc.update("INSERT INTO personal_notification(id,recipient_id,source_event_id,kind,target_type,target_id,title,message) SELECT ?,id,?,?,?,?,?,? FROM app_user WHERE id=? AND active ON CONFLICT(source_event_id,recipient_id) DO NOTHING",UUID.randomUUID(),event,action,type,target,title,message,recipient);
    }
}
