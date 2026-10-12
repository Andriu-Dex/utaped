package ec.edu.uta.utaped.planning;
import ec.edu.uta.utaped.identity.Accounts;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service
public class ReportingService {
    public record Row(UUID groupId,String groupName,String careerName,String periodName,long plans,long reports,long approved,long inReview,long drafts,long activities,long completedActivities,BigDecimal evidenceCoverage) {}
    public record Summary(List<Row> rows,long plans,long reports,long approved,long activities,long completedActivities) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;
    public ReportingService(JdbcTemplate jdbc,Accounts accounts) { this.jdbc=jdbc;this.accounts=accounts; }
    @Transactional(readOnly=true) public Summary get(String email,UUID group,UUID period,boolean archived) {
        var actor=accounts.current(email);boolean admin=actor.systemRole().equals("ADMIN");
        var rows=jdbc.query("""
            SELECT g.id,g.name,COALESCE(c.name,'Facultad') AS career_name,p.name AS period_name,
              count(*) FILTER (WHERE w.document_type='T1') AS plans,count(*) FILTER (WHERE w.document_type='T2') AS reports,
              count(*) FILTER (WHERE w.document_state='APPROVED') AS approved,
              count(*) FILTER (WHERE w.document_state='IN_REVIEW') AS in_review,count(*) FILTER (WHERE w.document_state='DRAFT') AS drafts,
              COALESCE(sum(ex.total) FILTER (WHERE w.document_type='T1'),0) AS activities,
              COALESCE(sum(ex.complete) FILTER (WHERE w.document_type='T1'),0) AS complete
            FROM work_plan w JOIN app_user actor ON actor.id=? JOIN institutional_group g ON g.id=w.group_id
            JOIN academic_period p ON p.id=w.period_id LEFT JOIN organizational_scope s ON s.id=w.scope_id LEFT JOIN academic_career c ON c.id=s.career_id
            LEFT JOIN work_plan_matrix m ON m.work_plan_id=w.id
            LEFT JOIN LATERAL (SELECT count(*) AS total,count(*) FILTER (WHERE jsonb_array_length(a->'means')>0 AND NOT EXISTS
              (SELECT 1 FROM jsonb_array_elements(a->'means') WITH ORDINALITY medium(choice,position) WHERE NOT EXISTS
                (SELECT 1 FROM execution_evidence e WHERE e.work_plan_id=w.id AND e.activity_id=(a->>'id')::uuid AND e.medium_index=medium.position-1))) AS complete
              FROM jsonb_array_elements(COALESCE(m.activities,'[]'::jsonb)) a) ex ON true
            WHERE (? OR (
            """+WorkPlanService.ACCESS+"""
            )) AND (? OR w.archived_at IS NULL) AND (?::uuid IS NULL OR w.group_id=?::uuid) AND (?::uuid IS NULL OR w.period_id=?::uuid)
            GROUP BY g.id,g.name,c.name,p.id,p.name,p.starts_on ORDER BY p.starts_on DESC,g.name,c.name
            """,(rs,n)->{
                long total=rs.getLong("activities"),complete=rs.getLong("complete");
                return new Row(rs.getObject("id",UUID.class),rs.getString("name"),rs.getString("career_name"),rs.getString("period_name"),rs.getLong("plans"),rs.getLong("reports"),rs.getLong("approved"),rs.getLong("in_review"),rs.getLong("drafts"),total,complete,total==0?BigDecimal.ZERO:BigDecimal.valueOf(complete*100).divide(BigDecimal.valueOf(total),2,java.math.RoundingMode.HALF_UP));
            },actor.id(),admin,archived,group,group,period,period);
        return new Summary(rows,rows.stream().mapToLong(Row::plans).sum(),rows.stream().mapToLong(Row::reports).sum(),rows.stream().mapToLong(Row::approved).sum(),rows.stream().mapToLong(Row::activities).sum(),rows.stream().mapToLong(Row::completedActivities).sum());
    }
    private String cell(String value) { String safe=value.replace('\r',' ').replace('\n',' ');if(safe.matches("^[\\s]*[=+@-].*")) safe="'"+safe;return "\""+safe.replace("\"","\"\"")+"\""; }
    public byte[] csv(String email,UUID group,UUID period,boolean archived) {
        var data=get(email,group,period,archived);var csv=new StringBuilder("\uFEFFGrupo,Carrera,Periodo,Planes,Informes,Aprobados,En revision,Borradores,Actividades,Completas,Cobertura documental (%)\r\n");
        for(var r:data.rows()) csv.append(cell(r.groupName())).append(',').append(cell(r.careerName())).append(',').append(cell(r.periodName())).append(',').append(r.plans()).append(',').append(r.reports()).append(',').append(r.approved()).append(',').append(r.inReview()).append(',').append(r.drafts()).append(',').append(r.activities()).append(',').append(r.completedActivities()).append(',').append(r.evidenceCoverage().toPlainString()).append("\r\n");return csv.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }
}
