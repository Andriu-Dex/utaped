package ec.edu.uta.utaped.notifications;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeadlineNoticeService {
    private final JdbcTemplate jdbc;private final ZoneId zone;private final int nearDays,repeatDays;private final boolean enabled;
    public DeadlineNoticeService(JdbcTemplate jdbc,@Value("${app.planning.time-zone:America/Guayaquil}") String zone,
        @Value("${app.notifications.deadline-near-days:3}") int nearDays,@Value("${app.notifications.missing-repeat-days:1}") int repeatDays,
        @Value("${app.notifications.deadline-enabled:true}") boolean enabled) {
        if(nearDays<0||nearDays>30||repeatDays<1||repeatDays>30) throw new IllegalArgumentException("Invalid deadline notification configuration");
        this.jdbc=jdbc;this.zone=ZoneId.of(zone);this.nearDays=nearDays;this.repeatDays=repeatDays;this.enabled=enabled;
    }
    public Map<String,Object> configuration() { return Map.of("enabled",enabled,"nearDays",nearDays,"repeatDays",repeatDays,"timeZone",zone.toString()); }
    public LocalDate today() { return LocalDate.now(zone); }
    @Transactional public int generate(LocalDate today) {
        // Follow the mutation lock order so closure cannot interleave with delivery.
        var periods=jdbc.queryForList("SELECT id FROM academic_period WHERE status='ACTIVE' AND starts_on<=? AND ends_on>=? ORDER BY id FOR SHARE",UUID.class,today,today);
        int sent=0;
        for(var period:periods) {
            jdbc.queryForList("SELECT id FROM organizational_scope WHERE period_id=? ORDER BY id FOR SHARE",period);
            var docs=jdbc.queryForList("SELECT id FROM work_plan WHERE period_id=? AND document_type='T1' AND document_state='APPROVED' AND archived_at IS NULL ORDER BY id FOR SHARE",UUID.class,period);
            for(var doc:docs) {
                var candidates=jdbc.queryForList("""
                    SELECT u.id AS user_id,(a->>'id')::uuid AS activity_id,left(a->>'title',150) AS activity_title,
                      coalesce(e.ends_on,(a->>'endsOn')::date) AS deadline,
                      EXISTS(SELECT 1 FROM generate_series(0,jsonb_array_length(a->'means')-1) i
                        WHERE NOT EXISTS(SELECT 1 FROM execution_evidence v WHERE v.work_plan_id=w.id AND v.activity_id=(a->>'id')::uuid AND v.medium_index=i)) AS missing
                    FROM work_plan w JOIN work_plan_matrix matrix ON matrix.work_plan_id=w.id
                    CROSS JOIN LATERAL jsonb_array_elements(matrix.activities) a
                    CROSS JOIN LATERAL jsonb_array_elements_text(a->'responsibleIds') r
                    JOIN app_user u ON u.id=r.value::uuid AND u.active
                    LEFT JOIN organizational_scope s ON s.id=w.scope_id
                    JOIN institutional_group g ON g.id=w.group_id AND g.active
                    LEFT JOIN deadline_extension e ON e.work_plan_id=w.id AND e.activity_id=(a->>'id')::uuid AND e.user_id=u.id
                    WHERE w.id=? AND jsonb_array_length(a->'means')>0 AND
                     ((s.active AND EXISTS(SELECT 1 FROM scope_member m WHERE m.scope_id=s.id AND m.user_id=u.id AND m.active)
                       AND EXISTS(SELECT 1 FROM document_access d WHERE d.work_plan_id=w.id AND d.user_id=u.id))
                      OR (w.scope_id IS NULL AND w.teacher_id=u.id AND EXISTS(SELECT 1 FROM membership m WHERE m.group_id=w.group_id AND m.user_id=u.id)))
                    """,doc);
                for(var row:candidates) {
                    LocalDate deadline=((java.sql.Date)row.get("deadline")).toLocalDate();
                    if(!(boolean)row.get("missing")||deadline.isAfter(today.plusDays(nearDays))) continue;
                    String kind=deadline.isBefore(today)?"EVIDENCE_MISSING":"ACTIVITY_DEADLINE_NEAR";
                    LocalDate bucket=kind.equals("ACTIVITY_DEADLINE_NEAR")?deadline:LocalDate.ofEpochDay(Math.floorDiv(today.toEpochDay(),repeatDays)*repeatDays);
                    int claimed=jdbc.update("INSERT INTO deadline_notice_delivery(work_plan_id,activity_id,user_id,deadline,kind,sent_on) VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING",doc,row.get("activity_id"),row.get("user_id"),deadline,kind,bucket);
                    if(claimed==0) continue;
                    UUID event=UUID.randomUUID();
                    jdbc.update("INSERT INTO audit_event(id,action,target_id,subject_user_id) VALUES (?,?,?,?)",event,kind,doc,row.get("user_id"));
                    String title=kind.equals("EVIDENCE_MISSING")?"Evidencia pendiente después del plazo":"Actividad próxima a vencer";
                    String message="Actividad: "+Objects.toString(row.get("activity_title"),"")+". Hay medios de verificación sin PDF vigente. Plazo personal: "+deadline+" ("+zone+"). Consulte la actividad en ejecución.";
                    jdbc.update("INSERT INTO personal_notification(id,recipient_id,source_event_id,kind,target_type,target_id,title,message) VALUES (?,?,?,?,'WORK_PLAN',?,?,?)",UUID.randomUUID(),row.get("user_id"),event,kind,doc,title,message);
                    sent++;
                }
            }
        }
        return sent;
    }
}
