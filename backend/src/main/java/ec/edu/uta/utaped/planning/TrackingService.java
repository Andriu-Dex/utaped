package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.Accounts;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TrackingService {
    public record Scope(UUID id,String name,long documents) {}
    public record Summary(long documents,long editableDrafts,long readOnlyDocuments,long withPreview,List<Scope> groups,List<Scope> periods,WorkPlanModels.Page recent) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final WorkPlanService plans;private final boolean enforce;private final ZoneId zone;
    public TrackingService(JdbcTemplate jdbc,Accounts accounts,WorkPlanService plans,@Value("${app.planning.enforce-preparation-window:true}") boolean enforce,@Value("${app.planning.time-zone:America/Guayaquil}") String zone) { this.jdbc=jdbc;this.accounts=accounts;this.plans=plans;this.enforce=enforce;this.zone=ZoneId.of(zone); }
    @Transactional(readOnly=true)
    public Summary get(String email,UUID groupId,UUID periodId) {
        var actor=accounts.current(email);
        String from=" FROM work_plan w JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id WHERE w.teacher_id=? AND g.active AND EXISTS (SELECT 1 FROM membership m WHERE m.user_id=w.teacher_id AND m.group_id=w.group_id) AND (?::uuid IS NULL OR w.group_id=?::uuid) AND (?::uuid IS NULL OR w.period_id=?::uuid)";
        Object[] filters={actor.id(),groupId,groupId,periodId,periodId};
        var totals=jdbc.queryForMap("SELECT count(*) AS total,count(*) FILTER (WHERE w.document_state='DRAFT' AND (? OR ? BETWEEN p.preparation_starts_on AND p.preparation_ends_on)) AS editable,count(*) FILTER (WHERE EXISTS (SELECT 1 FROM document_artifact a WHERE a.work_plan_id=w.id)) AS preview"+from,!enforce,LocalDate.now(zone),actor.id(),groupId,groupId,periodId,periodId);
        var groups=jdbc.query("SELECT g.id,g.name,count(*) AS documents"+from+" GROUP BY g.id,g.name ORDER BY g.name,g.id",(rs,n)->new Scope(rs.getObject("id",UUID.class),rs.getString("name"),rs.getLong("documents")),filters);
        var periods=jdbc.query("SELECT p.id,p.name,count(*) AS documents"+from+" GROUP BY p.id,p.name,p.starts_on ORDER BY p.starts_on DESC,p.id",(rs,n)->new Scope(rs.getObject("id",UUID.class),rs.getString("name"),rs.getLong("documents")),filters);
        long total=((Number)totals.get("total")).longValue(),editable=((Number)totals.get("editable")).longValue();
        return new Summary(total,editable,total-editable,((Number)totals.get("preview")).longValue(),groups,periods,plans.list(email,0,5,groupId,periodId,""));
    }
}
