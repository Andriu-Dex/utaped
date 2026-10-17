package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ReportService {
    public record Result(@NotNull UUID activityId,@DecimalMin("0") @DecimalMax("100") BigDecimal percentage,@NotNull @Size(max=4000) String observations) {}
    public record Delegation(@NotBlank @Size(max=500) String purpose,@NotNull @Size(max=500) String location,@NotNull @Size(max=500) String institution,
        @NotNull @Size(max=500) String contact,@NotNull @Size(max=500) String contactDetails,@NotNull @Size(max=2000) String topic,@NotNull @Size(max=2000) String agreement) {}
    public record Data(UUID basePlanId,UUID baseArtifactId,String baseFormalVersion,String background,String conclusions,String improvements,List<Result> results,List<Delegation> delegations) {}
    public record State(WorkPlanModels.Plan document,Data data,List<ActivityModels.Activity> activities,EvidenceService.State evidence) {}
    public record Option(UUID id,String title,String groupName,String periodName,String formalVersion,String suggestedTitle,UUID reportId) {}
    public record Options(List<Option> items,long total,int page,int size) {}
    @Transactional(readOnly=true,isolation=org.springframework.transaction.annotation.Isolation.REPEATABLE_READ)
    public Options options(String email,String query,int page,int size) {
        if(page<0||page>100000||size<1||size>50||query==null||query.length()>200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revise la búsqueda y paginación.");
        String from="""
            FROM work_plan w JOIN app_user actor ON actor.id=?
            JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id
            JOIN organizational_scope s ON s.id=w.scope_id
            LEFT JOIN LATERAL (SELECT d.id,d.title,d.archived_at,d.document_state FROM work_report r JOIN work_plan d ON d.id=r.id
                WHERE r.base_plan_id=w.id ORDER BY d.created_at DESC,d.id DESC LIMIT 1) report ON true
            WHERE
            """+WorkPlanService.ACCESS+"""
            AND s.principal_user_id=actor.id AND p.status='ACTIVE' AND w.document_type='T1' AND w.document_state='APPROVED' AND w.archived_at IS NULL
            AND (? BETWEEN p.starts_on AND p.ends_on OR (report.id IS NOT NULL AND (report.archived_at IS NULL OR report.document_state<>'DRAFT')))
            AND position(lower(?) in lower(w.title||' '||g.name||' '||p.name))>0
            """;
        var actor=accounts.current(email).id();var today=LocalDate.now(zone);
        long total=jdbc.queryForObject("SELECT count(*) "+from,Long.class,actor,today,query.trim());
        var items=jdbc.query("SELECT w.id,w.title,g.name AS group_name,p.name AS period_name,w.formal_version,CASE WHEN report.archived_at IS NULL OR report.document_state<>'DRAFT' THEN report.id END AS report_id,report.title AS report_title "+from+" ORDER BY w.created_at DESC,w.id LIMIT ? OFFSET ?",
            (rs,n)->new Option(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("group_name"),rs.getString("period_name"),rs.getString("formal_version"),rs.getObject("report_id",UUID.class)==null?DocumentTitles.report(rs.getString("title")):rs.getString("report_title"),rs.getObject("report_id",UUID.class)),actor,today,query.trim(),size,(long)page*size);
        return new Options(items,total,page,size);
    }
    public record Save(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=200) String title,@NotNull @Size(max=50000) String background,
        @NotNull @Size(max=50000) String conclusions,@NotNull @Size(max=50000) String improvements,@NotNull @Size(max=100) List<@Valid Result> results,@NotNull @Size(max=100) List<@Valid Delegation> delegations) {}
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final ActivityService activities;private final EvidenceService evidence;private final Accounts accounts;private final AuditService audit;private final ZoneId zone;
    private final JsonMapper json=JsonMapper.builder().build();
    public ReportService(JdbcTemplate jdbc,WorkPlanService plans,ActivityService activities,EvidenceService evidence,Accounts accounts,AuditService audit,@Value("${app.planning.time-zone:America/Guayaquil}") String zone) { this.jdbc=jdbc;this.plans=plans;this.activities=activities;this.evidence=evidence;this.accounts=accounts;this.audit=audit;this.zone=ZoneId.of(zone); }
    public Data data(UUID doc,String email) {
        var plan=plans.get(doc,email);if(!plan.documentType().equals("T2")) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Informe no disponible.");
        return jdbc.query("SELECT * FROM work_report WHERE id=?",(rs,n)->new Data(rs.getObject("base_plan_id",UUID.class),rs.getObject("base_artifact_id",UUID.class),rs.getString("base_formal_version"),rs.getString("background"),rs.getString("conclusions"),rs.getString("improvements"),List.of(json.readValue(rs.getString("results"),Result[].class)),List.of(json.readValue(rs.getString("delegations"),Delegation[].class))),doc).getFirst();
    }
    public State get(UUID doc,String email) { var data=data(doc,email);return new State(plans.get(doc,email),data,activities.get(doc,email).activities(),evidence.get(data.basePlanId(),email)); }
    @Transactional public WorkPlanModels.Plan create(UUID base,String email) {
        var parent=plans.lock(base,email);
        var actor=accounts.current(email);
        if(parent.scopeId()==null||!parent.documentType().equals("T1")||!parent.documentState().equals("APPROVED")||parent.archivedAt()!=null||!parent.teacherId().equals(actor.id())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable principal puede crear el Informe de un Plan aprobado y vigente.");
        var existing=jdbc.queryForList("SELECT r.id FROM work_report r JOIN work_plan w ON w.id=r.id WHERE r.base_plan_id=? ORDER BY w.created_at DESC,w.id DESC LIMIT 1",UUID.class,base);
        WorkPlanModels.Plan previous=existing.isEmpty()?null:plans.get(existing.getFirst(),email);
        if(previous!=null && (previous.archivedAt()==null || !previous.documentState().equals("DRAFT"))) return previous;
        if(jdbc.queryForObject("SELECT count(*) FROM academic_period WHERE id=? AND ? BETWEEN starts_on AND ends_on",Integer.class,parent.periodId(),LocalDate.now(zone))==0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"El período no está habilitado para elaborar un Informe nuevo.");
        UUID artifact=jdbc.queryForObject("SELECT artifact_id FROM document_review WHERE work_plan_id=? AND status='APPROVED' ORDER BY round_number DESC LIMIT 1",UUID.class,base);
        UUID id=UUID.randomUUID();
        jdbc.update("""
            INSERT INTO work_plan(id,teacher_id,created_by,scope_id,group_id,period_id,request_key,title,creation_title,preparation_date,institutional_unit,career,document_type,base_plan_id,justification,objective)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'T2',?,?,?)
            """,id,actor.id(),actor.id(),parent.scopeId(),parent.groupId(),parent.periodId(),UUID.randomUUID(),DocumentTitles.report(parent.title()),parent.title(),LocalDate.now(zone),parent.institutionalUnit(),parent.career(),base,parent.justification(),parent.objective());
        jdbc.update("INSERT INTO work_plan_matrix(work_plan_id,source,activities) SELECT ?,source,activities FROM work_plan_matrix WHERE work_plan_id=?",id,base);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) SELECT ?,user_id,? FROM document_access WHERE work_plan_id=?",id,actor.id(),base);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?) ON CONFLICT DO NOTHING",id,actor.id(),actor.id());
        var results=activities.get(id,email).activities().stream().map(a->new Result(a.id(),null,"")).toList();
        jdbc.update("INSERT INTO work_report(id,base_plan_id,base_artifact_id,base_formal_version,results,previous_version_id) VALUES (?,?,?,?,?::jsonb,?)",id,base,artifact,parent.formalVersion(),json.writeValueAsString(results),previous==null?null:previous.id());
        if(previous!=null) jdbc.update("UPDATE work_plan SET previous_version_id=?,formal_version=?,version_reason='Reemplazo de borrador archivado' WHERE id=?",previous.id(),previous.formalVersion(),id);
        audit.record(actor.id(),"REPORT_CREATED",id);return plans.get(id,email);
    }
    @Transactional public State save(UUID doc,String email,Save input) {
        var plan=plans.lock(doc,email);data(doc,email);
        if(!plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El Informe está en modo de solo lectura.");
        if(input.rowVersion()!=plan.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El Informe cambió. Recargue antes de guardar.");
        Set<UUID> ids=new HashSet<>();activities.get(doc,email).activities().forEach(a->ids.add(a.id()));
        if(input.results().size()!=ids.size()||!ids.equals(new HashSet<>(input.results().stream().map(Result::activityId).toList()))) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Los resultados deben corresponder exactamente a las actividades del Plan base.");
        var canonical=activities.get(doc,email).activities().stream().map(a->input.results().stream().filter(r->r.activityId().equals(a.id())).findFirst().orElseThrow()).toList();
        jdbc.update("UPDATE work_report SET background=?,conclusions=?,improvements=?,results=?::jsonb,delegations=?::jsonb WHERE id=?",input.background(),input.conclusions(),input.improvements(),json.writeValueAsString(canonical),json.writeValueAsString(input.delegations()),doc);
        jdbc.update("UPDATE work_plan SET title=?,row_version=row_version+1,updated_at=now() WHERE id=?",input.title().trim(),doc);audit.record(accounts.current(email).id(),"REPORT_DRAFT_SAVED",doc);return get(doc,email);
    }
    public List<Blocker> blockers(UUID doc,String email) {
        var state=get(doc,email);var result=new ArrayList<Blocker>();
        if(state.data().background().isBlank()) result.add(new Blocker("Antecedentes","Complete los antecedentes con el origen verificable del Plan; no invente resoluciones."));
        if(state.data().conclusions().isBlank()) result.add(new Blocker("Conclusiones","Complete las conclusiones del Informe."));
        if(state.data().improvements().isBlank()) result.add(new Blocker("Oportunidades de mejora","Complete las oportunidades de mejora."));
        if(state.data().results().stream().anyMatch(r->r.percentage()==null)) result.add(new Blocker("Resultados","Declare el porcentaje de ejecución de cada actividad. La fórmula institucional todavía debe ratificarse."));
        for(var activity:state.evidence().activities()) if(!activity.complete()&&state.data().results().stream().anyMatch(r->r.activityId().equals(activity.id())&&r.observations().isBlank())) result.add(new Blocker("Evidencias","Explique los medios sin evidencia de: "+activity.title()));
        return List.copyOf(result);
    }
}
