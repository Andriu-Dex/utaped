package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.Accounts;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReportingService {
    public record Row(UUID groupId,String groupName,String careerName,String periodName,long plans,long reports,long approved,long inReview,long drafts,long activities,long completedActivities,BigDecimal evidenceCoverage,BigDecimal declaredExecution,long declaredActivities,long pendingEvidence,long dueSoonActivities,long overdueActivities) {}
    public record Upcoming(UUID documentId,UUID activityId,String documentTitle,String activityTitle,String groupName,String periodName,LocalDate deadline,long missingEvidence) {}
    public record Summary(List<Row> rows,long plans,long reports,long approved,long activities,long completedActivities,long inReview,long drafts,long pendingEvidence,long dueSoonActivities,long overdueActivities,BigDecimal declaredExecution,long declaredActivities,BigDecimal declarationCoverage,int dueSoonDays,List<Upcoming> upcoming) {}
    private static final String ELIGIBLE="w.document_type='T1' AND w.document_state='APPROVED' AND w.archived_at IS NULL AND p.status='ACTIVE' AND g.active AND (s.id IS NULL OR s.active)";
    private static final String FROM="""
        FROM work_plan w JOIN app_user actor ON actor.id=? JOIN institutional_group g ON g.id=w.group_id
        JOIN academic_period p ON p.id=w.period_id LEFT JOIN organizational_scope s ON s.id=w.scope_id
        LEFT JOIN academic_career c ON c.id=s.career_id LEFT JOIN work_plan_matrix m ON m.work_plan_id=w.id
        CROSS JOIN report_clock clock
        """;
    private static final String FILTER=" AND (? OR w.archived_at IS NULL) AND (?::uuid IS NULL OR w.group_id=?::uuid) AND (?::uuid IS NULL OR w.period_id=?::uuid) ";
    // One row per activity: evidence versions and multiple responsible people never multiply the counts.
    private static final String FACTS="""
        SELECT (a->>'id')::uuid AS activity_id,a->>'title' AS activity_title,
          jsonb_array_length(COALESCE(a->'means','[]'::jsonb)) AS required_media,
          missing.total AS missing_media,deadlines.soon,deadlines.overdue
        FROM jsonb_array_elements(COALESCE(m.activities,'[]'::jsonb)) a
        CROSS JOIN LATERAL (
          SELECT count(*) AS total FROM jsonb_array_elements(COALESCE(a->'means','[]'::jsonb)) WITH ORDINALITY medium(choice,position)
          WHERE NOT EXISTS(SELECT 1 FROM execution_evidence e WHERE e.work_plan_id=w.id AND e.activity_id=(a->>'id')::uuid AND e.medium_index=medium.position-1)
        ) missing
        CROSS JOIN LATERAL (
          SELECT min(coalesce(e.ends_on,(a->>'endsOn')::date)) FILTER (WHERE coalesce(e.ends_on,(a->>'endsOn')::date) BETWEEN clock.today AND clock.today+clock.near_days) AS soon,
            coalesce(bool_or(coalesce(e.ends_on,(a->>'endsOn')::date)<clock.today),false) AS overdue
          FROM jsonb_array_elements_text(COALESCE(a->'responsibleIds','[]'::jsonb)) r
          JOIN app_user u ON u.id=r.value::uuid AND u.active
          LEFT JOIN deadline_extension e ON e.work_plan_id=w.id AND e.activity_id=(a->>'id')::uuid AND e.user_id=u.id
          WHERE ((s.active AND EXISTS(SELECT 1 FROM scope_member member WHERE member.scope_id=s.id AND member.user_id=u.id AND member.active)
            AND EXISTS(SELECT 1 FROM document_access access WHERE access.work_plan_id=w.id AND access.user_id=u.id))
            OR (w.scope_id IS NULL AND EXISTS(SELECT 1 FROM membership member WHERE member.group_id=w.group_id AND member.user_id=u.id)))
        ) deadlines
        """;
    private final JdbcTemplate jdbc;private final Accounts accounts;private final ZoneId zone;private final int nearDays;
    public ReportingService(JdbcTemplate jdbc,Accounts accounts,@Value("${app.planning.time-zone:America/Guayaquil}") String zone,@Value("${app.reporting.due-soon-days:7}") int nearDays) {
        this.jdbc=jdbc;this.accounts=accounts;this.zone=ZoneId.of(zone);this.nearDays=Math.max(1,Math.min(nearDays,30));
    }
    private static BigDecimal percentage(long value,long total) { return total==0?BigDecimal.ZERO:BigDecimal.valueOf(value).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(total),2,RoundingMode.HALF_UP); }
    @Transactional(readOnly=true) public Summary get(String email,UUID group,UUID period,boolean archived) {
        // Interactive aggregates do not amortize PostgreSQL JIT compilation. Scope this
        // setting to the read transaction, never to the pooled connection or server.
        jdbc.execute("SET LOCAL jit = off");
        var actor=accounts.current(email);boolean admin=actor.systemRole().equals("ADMIN");var today=LocalDate.now(zone);
        String clock="WITH report_clock AS (SELECT ?::date AS today, ?::int AS near_days) ";
        var points=new BigDecimal[]{BigDecimal.ZERO};
        var rows=jdbc.query(clock+"""
            SELECT g.id,g.name,COALESCE(c.name,'Facultad') AS career_name,p.name AS period_name,
              count(*) FILTER (WHERE w.document_type='T1') AS plans,count(*) FILTER (WHERE w.document_type<>'T1') AS reports,
              count(*) FILTER (WHERE w.document_state='APPROVED') AS approved,
              count(*) FILTER (WHERE w.document_state='IN_REVIEW') AS in_review,count(*) FILTER (WHERE w.document_state='DRAFT') AS drafts,
              COALESCE(sum(ex.total) FILTER (WHERE w.document_type='T1'),0) AS activities,
              COALESCE(sum(ex.complete) FILTER (WHERE w.document_type='T1'),0) AS complete,
            """+"COALESCE(sum(ex.pending) FILTER (WHERE "+ELIGIBLE+"),0) AS pending, COALESCE(sum(ex.soon) FILTER (WHERE "+ELIGIBLE+"),0) AS soon, COALESCE(sum(ex.overdue) FILTER (WHERE "+ELIGIBLE+"),0) AS overdue,"+"""
              sum(physical.total) FILTER (WHERE w.document_type='T1') AS declared_activities,
              sum(physical.points) FILTER (WHERE w.document_type='T1') AS declared_points,
              sum(physical.points) FILTER (WHERE w.document_type='T1') / NULLIF(sum(physical.total) FILTER (WHERE w.document_type='T1'),0) AS declared_execution
            """+FROM+"""
            LEFT JOIN LATERAL (
              SELECT count(*) AS total,count(*) FILTER (WHERE required_media>0 AND missing_media=0) AS complete,
                COALESCE(sum(missing_media),0) AS pending,count(*) FILTER (WHERE missing_media>0 AND soon IS NOT NULL) AS soon,
                count(*) FILTER (WHERE missing_media>0 AND overdue) AS overdue
              FROM (
            """+FACTS+"""
              ) facts
            ) ex ON true
            LEFT JOIN LATERAL (
              SELECT count(*) AS total,sum((latest.result->>'percentage')::numeric) AS points FROM (
                SELECT DISTINCT ON (result->>'activityId') result
                FROM work_plan report LEFT JOIN work_report final ON final.id=report.id LEFT JOIN execution_report cut ON cut.id=report.id
                CROSS JOIN LATERAL jsonb_array_elements(COALESCE(final.results,cut.results,'[]'::jsonb)) result
                WHERE report.base_plan_id=w.id AND report.document_state='APPROVED' AND report.archived_at IS NULL AND result->>'percentage' IS NOT NULL
                  AND EXISTS(SELECT 1 FROM jsonb_array_elements(COALESCE(m.activities,'[]'::jsonb)) planned WHERE planned->>'id'=result->>'activityId')
                ORDER BY result->>'activityId',(report.document_type='T2') DESC,report.created_at DESC,report.id
              ) latest
            ) physical ON w.document_type='T1'
            WHERE (? OR (
            """+WorkPlanService.ACCESS+"))"+FILTER+" GROUP BY g.id,g.name,c.name,p.id,p.name,p.starts_on ORDER BY p.starts_on DESC,g.name,c.name",
            (rs,n)->{
                long total=rs.getLong("activities"),complete=rs.getLong("complete");
                var raw=rs.getBigDecimal("declared_points");if(raw!=null)points[0]=points[0].add(raw);
                return new Row(rs.getObject("id",UUID.class),rs.getString("name"),rs.getString("career_name"),rs.getString("period_name"),rs.getLong("plans"),rs.getLong("reports"),rs.getLong("approved"),rs.getLong("in_review"),rs.getLong("drafts"),total,complete,percentage(complete,total),rs.getBigDecimal("declared_execution")==null?null:rs.getBigDecimal("declared_execution").setScale(2,RoundingMode.HALF_UP),rs.getLong("declared_activities"),rs.getLong("pending"),rs.getLong("soon"),rs.getLong("overdue"));
            },today,nearDays,actor.id(),admin,archived,group,group,period,period);
        // ADMIN's global aggregate permission does not expose private document titles or activity details.
        var upcoming=jdbc.query(clock+"SELECT w.id,w.title,g.name,p.name AS period_name,facts.* "+FROM+" CROSS JOIN LATERAL ("+FACTS+") facts WHERE ("+WorkPlanService.ACCESS+") AND "+ELIGIBLE+" AND facts.missing_media>0 AND facts.soon IS NOT NULL "+FILTER+" ORDER BY facts.soon,w.id,facts.activity_id LIMIT 6",
            (rs,n)->new Upcoming(rs.getObject("id",UUID.class),rs.getObject("activity_id",UUID.class),rs.getString("title"),rs.getString("activity_title"),rs.getString("name"),rs.getString("period_name"),rs.getObject("soon",LocalDate.class),rs.getLong("missing_media")),today,nearDays,actor.id(),archived,group,group,period,period);
        long activities=rows.stream().mapToLong(Row::activities).sum(),declared=rows.stream().mapToLong(Row::declaredActivities).sum();
        return new Summary(rows,rows.stream().mapToLong(Row::plans).sum(),rows.stream().mapToLong(Row::reports).sum(),rows.stream().mapToLong(Row::approved).sum(),activities,rows.stream().mapToLong(Row::completedActivities).sum(),rows.stream().mapToLong(Row::inReview).sum(),rows.stream().mapToLong(Row::drafts).sum(),rows.stream().mapToLong(Row::pendingEvidence).sum(),rows.stream().mapToLong(Row::dueSoonActivities).sum(),rows.stream().mapToLong(Row::overdueActivities).sum(),declared==0?null:points[0].divide(BigDecimal.valueOf(declared),2,RoundingMode.HALF_UP),declared,percentage(declared,activities),nearDays,upcoming);
    }
    private String cell(String value) { String safe=value.replace('\r',' ').replace('\n',' ');if(safe.matches("^[\\s]*[=+@-].*")) safe="'"+safe;return "\""+safe.replace("\"","\"\"")+"\""; }
    @Transactional(readOnly=true) public byte[] csv(String email,UUID group,UUID period,boolean archived) {
        var data=get(email,group,period,archived);var csv=new StringBuilder("\uFEFFGrupo,Carrera,Periodo,Planes,Informes,Aprobados,En revision,Borradores,Actividades,Completas,Cobertura documental (%),Avance declarado (%),Actividades informadas,Cobertura de resultados (%),Medios sin evidencia,Actividades proximas a vencer,Actividades con plazos vencidos\r\n");
        for(var r:data.rows()) csv.append(cell(r.groupName())).append(',').append(cell(r.careerName())).append(',').append(cell(r.periodName())).append(',').append(r.plans()).append(',').append(r.reports()).append(',').append(r.approved()).append(',').append(r.inReview()).append(',').append(r.drafts()).append(',').append(r.activities()).append(',').append(r.completedActivities()).append(',').append(r.evidenceCoverage().toPlainString()).append(',').append(r.declaredExecution()==null?"":r.declaredExecution().toPlainString()).append(',').append(r.declaredActivities()).append(',').append(percentage(r.declaredActivities(),r.activities()).toPlainString()).append(',').append(r.pendingEvidence()).append(',').append(r.dueSoonActivities()).append(',').append(r.overdueActivities()).append("\r\n");return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
