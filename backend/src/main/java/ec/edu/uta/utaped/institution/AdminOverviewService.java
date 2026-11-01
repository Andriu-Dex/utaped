package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.planning.ReportingService;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminOverviewService {
    public record Window(String label,LocalDate startsOn,LocalDate endsOn,String status,Long remainingDays) {}
    public record Period(UUID id,String name,LocalDate startsOn,LocalDate endsOn,boolean current,List<Window> windows) {}
    public record DocumentType(String type,long count) {}
    public record Alerts(long scopesWithoutCoordinator,long reviewsOutsideWindow,long pendingEvidence,long overdueActivities) {}
    public record Event(UUID id,String action,String actorName,OffsetDateTime occurredAt) {}
    public record Summary(LocalDate today,OffsetDateTime updatedAt,List<Period> periods,UUID selectedPeriodId,
        long users,long activeUsers,long activeGroups,ReportingService.Summary reporting,List<DocumentType> documentTypes,Alerts alerts,List<Event> events) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final ReportingService reporting;private final ZoneId zone;
    public AdminOverviewService(JdbcTemplate jdbc,Accounts accounts,ReportingService reporting,@Value("${app.planning.time-zone:America/Guayaquil}") String zone) {
        this.jdbc=jdbc;this.accounts=accounts;this.reporting=reporting;this.zone=ZoneId.of(zone);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Summary get(String email,UUID periodId,boolean allPeriods) {
        if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo Administración puede consultar este panel.");
        if(allPeriods&&periodId!=null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un período o el resumen institucional.");
        var today=LocalDate.now(zone);
        var periods=jdbc.query("SELECT id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on FROM academic_period WHERE status='ACTIVE' ORDER BY starts_on DESC,id",(rs,n)->{
            var start=rs.getObject("starts_on",LocalDate.class);var end=rs.getObject("ends_on",LocalDate.class);
            return new Period(rs.getObject("id",UUID.class),rs.getString("name"),start,end,!today.isBefore(start)&&!today.isAfter(end),List.of(
                window("Elaboración",rs.getObject("preparation_starts_on",LocalDate.class),rs.getObject("preparation_ends_on",LocalDate.class),today),
                window("Revisión",rs.getObject("review_starts_on",LocalDate.class),rs.getObject("review_ends_on",LocalDate.class),today)));
        });
        UUID selected=periodId;
        if(selected!=null&&periods.stream().noneMatch(p->p.id().equals(periodId))) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Período activo no disponible. Actualice el panel.");
        var current=periods.stream().filter(Period::current).toList();
        if(selected==null&&!allPeriods&&current.size()==1) selected=current.getFirst().id();
        var metrics=reporting.get(email,null,selected,false);
        // This supervisory endpoint returns aggregates, never the private upcoming-document list.
        metrics=new ReportingService.Summary(metrics.rows(),metrics.plans(),metrics.reports(),metrics.approved(),metrics.activities(),metrics.completedActivities(),metrics.inReview(),metrics.drafts(),metrics.pendingEvidence(),metrics.dueSoonActivities(),metrics.overdueActivities(),metrics.declaredExecution(),metrics.declaredActivities(),metrics.declarationCoverage(),metrics.dueSoonDays(),List.of());
        long users=jdbc.queryForObject("SELECT count(*) FROM app_user",Long.class),active=jdbc.queryForObject("SELECT count(*) FROM app_user WHERE active",Long.class);
        long groups=jdbc.queryForObject("SELECT count(*) FROM institutional_group WHERE active",Long.class);
        long missing=jdbc.queryForObject("""
            SELECT count(*) FROM organizational_scope s JOIN institutional_group g ON g.id=s.group_id
            JOIN academic_period p ON p.id=s.period_id WHERE s.active AND g.active AND p.status='ACTIVE'
            AND (?::uuid IS NULL OR p.id=?::uuid) AND NOT EXISTS(SELECT 1 FROM scope_member m
              JOIN app_user u ON u.id=m.user_id AND u.active WHERE m.scope_id=s.id AND m.active AND m.membership_role='COORDINATOR')
            """,Long.class,selected,selected);
        long delayed=jdbc.queryForObject("""
            SELECT count(DISTINCT w.id) FROM work_plan w JOIN academic_period p ON p.id=w.period_id
            JOIN document_review r ON r.work_plan_id=w.id AND r.status='IN_REVIEW'
            WHERE w.document_state='IN_REVIEW' AND NOT w.discarded AND w.archived_at IS NULL
            AND p.status='ACTIVE' AND p.review_ends_on<? AND (?::uuid IS NULL OR p.id=?::uuid)
            AND EXISTS(SELECT 1 FROM review_task t WHERE t.review_id=r.id AND t.status='ACTIVE')
            """,Long.class,today,selected,selected);
        var counts=jdbc.queryForList("SELECT document_type,count(*) AS total FROM work_plan WHERE NOT discarded AND archived_at IS NULL AND (?::uuid IS NULL OR period_id=?::uuid) GROUP BY document_type",selected,selected);
        var types=new ArrayList<DocumentType>();for(var type:List.of("T1","T2","T3"))types.add(new DocumentType(type,counts.stream().filter(row->type.equals(row.get("document_type"))).mapToLong(row->((Number)row.get("total")).longValue()).sum()));
        var events=jdbc.query("SELECT e.id,e.action,COALESCE(e.actor_display_name,u.display_name) AS actor_name,e.occurred_at FROM audit_event e LEFT JOIN app_user u ON u.id=e.actor_id ORDER BY e.occurred_at DESC,e.id DESC LIMIT 5",(rs,n)->new Event(rs.getObject("id",UUID.class),rs.getString("action"),rs.getString("actor_name"),rs.getObject("occurred_at",OffsetDateTime.class)));
        return new Summary(today,OffsetDateTime.now(zone),periods,selected,users,active,groups,metrics,List.copyOf(types),new Alerts(missing,delayed,metrics.pendingEvidence(),metrics.overdueActivities()),events);
    }
    private static Window window(String label,LocalDate start,LocalDate end,LocalDate today) {
        String status=start==null||end==null?"UNCONFIGURED":today.isBefore(start)?"UPCOMING":today.isAfter(end)?"CLOSED":"OPEN";
        return new Window(label,start,end,status,status.equals("OPEN")?ChronoUnit.DAYS.between(today,end):null);
    }
}
