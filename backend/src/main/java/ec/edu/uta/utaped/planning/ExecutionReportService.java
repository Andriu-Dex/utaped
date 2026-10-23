package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.documents.ArtifactModels.Blocker;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.planning.ReportService.Result;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** T3 execution report: a numbered cut of an approved Plan, with results, FODA analysis and improvement actions. */
@Service
public class ExecutionReportService {
    @org.springframework.beans.factory.annotation.Autowired private FormalSubmissionService submissions;
    @org.springframework.beans.factory.annotation.Autowired private DocumentStructures structures;
    public record Action(@NotBlank @Size(max=2000) String situation,@NotBlank @Size(max=2000) String action) {}
    public record Data(UUID basePlanId,UUID baseArtifactId,String baseFormalVersion,int sequenceNumber,List<UUID> activityIds,List<ReportService.Result> results,
        String analysis,String strengths,String weaknesses,String opportunities,String threats,String conclusions,String recommendations,List<Action> improvementActions,String evidenceLink) {}
    public record State(WorkPlanModels.Plan document,Data data,List<ActivityModels.Activity> activities,EvidenceService.State evidence) {}
    public record Save(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=200) String title,@NotNull @Size(min=1,max=100) List<@NotNull UUID> activityIds,
        @NotNull @Size(max=100) List<@Valid Result> results,@NotNull @Size(max=50000) String analysis,@NotNull @Size(max=20000) String strengths,
        @NotNull @Size(max=20000) String weaknesses,@NotNull @Size(max=20000) String opportunities,@NotNull @Size(max=20000) String threats,
        @NotNull @Size(max=50000) String conclusions,@NotNull @Size(max=50000) String recommendations,@NotNull @Size(max=50) List<@Valid Action> improvementActions,
        @NotNull @Size(max=2000) String evidenceLink) {}
    public record Option(UUID id,String title,String groupName,String periodName,String formalVersion,int reports,UUID draftId) {}
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final ActivityService activities;private final EvidenceService evidence;
    private final Accounts accounts;private final AuditService audit;private final ZoneId zone;
    private final JsonMapper json=JsonMapper.builder().build();
    public ExecutionReportService(JdbcTemplate jdbc,WorkPlanService plans,ActivityService activities,EvidenceService evidence,Accounts accounts,AuditService audit,@Value("${app.planning.time-zone:America/Guayaquil}") String zone) {
        this.jdbc=jdbc;this.plans=plans;this.activities=activities;this.evidence=evidence;this.accounts=accounts;this.audit=audit;this.zone=ZoneId.of(zone);
    }
    public static String roman(int number) {
        var result=new StringBuilder();int[] values={10,9,5,4,1};String[] symbols={"X","IX","V","IV","I"};
        for(int i=0;i<values.length;i++) while(number>=values[i]) { result.append(symbols[i]);number-=values[i]; }
        return result.toString();
    }
    /** Approved, current Plans whose principal is the caller, inside the period dates. */
    public List<Option> options(String email) {
        var actor=accounts.current(email).id();
        return jdbc.query("""
            SELECT w.id,w.title,g.name AS group_name,p.name AS period_name,w.formal_version,
              (SELECT count(*) FROM execution_report e JOIN work_plan d ON d.id=e.id WHERE e.base_plan_id=w.id AND NOT (d.archived_at IS NOT NULL AND d.document_state='DRAFT')) AS reports,
              (SELECT d.id FROM execution_report e JOIN work_plan d ON d.id=e.id WHERE e.base_plan_id=w.id AND d.archived_at IS NULL AND d.document_state='DRAFT' LIMIT 1) AS draft_id
            FROM work_plan w JOIN app_user actor ON actor.id=? JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id
            JOIN organizational_scope s ON s.id=w.scope_id
            WHERE
            """+WorkPlanService.ACCESS+"""
              AND s.principal_user_id=actor.id AND p.status='ACTIVE' AND ? BETWEEN p.starts_on AND p.ends_on
              AND w.document_type='T1' AND w.document_state='APPROVED' AND w.archived_at IS NULL
            ORDER BY w.created_at DESC,w.id LIMIT 50
            """,(rs,n)->new Option(rs.getObject("id",UUID.class),rs.getString("title"),rs.getString("group_name"),rs.getString("period_name"),rs.getString("formal_version"),rs.getInt("reports"),rs.getObject("draft_id",UUID.class)),actor,LocalDate.now(zone));
    }
    public Data data(UUID doc,String email) {
        var plan=plans.get(doc,email);if(!plan.documentType().equals("T3")) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Informe de ejecución no disponible.");
        return jdbc.query("SELECT * FROM execution_report WHERE id=?",(rs,n)->new Data(rs.getObject("base_plan_id",UUID.class),rs.getObject("base_artifact_id",UUID.class),rs.getString("base_formal_version"),rs.getInt("sequence_number"),
            List.of(json.readValue(rs.getString("activity_ids"),UUID[].class)),List.of(json.readValue(rs.getString("results"),ReportService.Result[].class)),
            rs.getString("analysis"),rs.getString("strengths"),rs.getString("weaknesses"),rs.getString("opportunities"),rs.getString("threats"),rs.getString("conclusions"),rs.getString("recommendations"),
            List.of(json.readValue(rs.getString("improvement_actions"),Action[].class)),rs.getString("evidence_link")),doc).getFirst();
    }
    public State get(UUID doc,String email) { var data=data(doc,email);return new State(plans.get(doc,email),data,activities.get(doc,email).activities(),evidence.forReport(doc,data.basePlanId(),email)); }
    @Transactional public WorkPlanModels.Plan create(UUID base,String email) {
        var parent=plans.lock(base,email);submissions.requireComplete(base,email);var actor=accounts.current(email);
        if(parent.scopeId()==null||!parent.documentType().equals("T1")||!parent.documentState().equals("APPROVED")||parent.archivedAt()!=null||!parent.teacherId().equals(actor.id()))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable principal puede crear Informes de ejecución de un Plan aprobado y vigente.");
        var draft=jdbc.queryForList("SELECT e.id FROM execution_report e JOIN work_plan d ON d.id=e.id WHERE e.base_plan_id=? AND d.archived_at IS NULL AND d.document_state='DRAFT'",UUID.class,base);
        if(!draft.isEmpty()) return plans.get(draft.getFirst(),email);
        if(jdbc.queryForObject("SELECT count(*) FROM academic_period WHERE id=? AND status='ACTIVE' AND ? BETWEEN starts_on AND ends_on",Integer.class,parent.periodId(),LocalDate.now(zone))==0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"El período no está habilitado para elaborar un Informe de ejecución.");
        // Discarded drafts do not consume a number: the sequence counts reports that reached review or approval.
        int sequence=jdbc.queryForObject("SELECT count(*) FROM execution_report e JOIN work_plan d ON d.id=e.id WHERE e.base_plan_id=? AND NOT (d.archived_at IS NOT NULL AND d.document_state='DRAFT')",Integer.class,base)+1;
        UUID artifact=jdbc.queryForObject("SELECT artifact_id FROM document_review WHERE work_plan_id=? AND status='APPROVED' ORDER BY round_number DESC LIMIT 1",UUID.class,base);
        UUID id=UUID.randomUUID();String title="Informe de ejecución "+roman(sequence)+" — "+parent.groupName();
        jdbc.update("""
            INSERT INTO work_plan(id,teacher_id,created_by,scope_id,group_id,period_id,request_key,title,creation_title,preparation_date,institutional_unit,career,document_type,base_plan_id,justification,objective)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,'T3',?,?,?)
            """,id,actor.id(),actor.id(),parent.scopeId(),parent.groupId(),parent.periodId(),UUID.randomUUID(),title,parent.title(),LocalDate.now(zone),parent.institutionalUnit(),parent.career(),base,parent.justification(),parent.objective());
        jdbc.update("UPDATE work_plan SET institutional_unit_type=? WHERE id=?",parent.institutionalUnitType(),id);
        jdbc.update("INSERT INTO work_plan_matrix(work_plan_id,source,activities) SELECT ?,source,activities FROM work_plan_matrix WHERE work_plan_id=?",id,base);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) SELECT ?,user_id,? FROM document_access WHERE work_plan_id=?",id,actor.id(),base);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?) ON CONFLICT DO NOTHING",id,actor.id(),actor.id());
        var rows=activities.get(id,email).activities();
        jdbc.update("INSERT INTO execution_report(id,base_plan_id,base_artifact_id,base_formal_version,sequence_number,activity_ids,results) VALUES (?,?,?,?,?,?::jsonb,?::jsonb)",id,base,artifact,parent.formalVersion(),sequence,
            json.writeValueAsString(rows.stream().map(ActivityModels.Activity::id).toList()),json.writeValueAsString(rows.stream().map(a->new ReportService.Result(a.id(),null,"")).toList()));
        structures.initialize(id,"T3");audit.record(actor.id(),"EXECUTION_REPORT_CREATED",id);return plans.get(id,email);
    }
    @Transactional public State save(UUID doc,String email,Save input) {
        var plan=plans.lock(doc,email);data(doc,email);
        if(!plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El Informe de ejecución está en modo de solo lectura.");
        if(input.rowVersion()!=plan.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El Informe cambió. Recargue antes de guardar.");
        var order=activities.get(doc,email).activities().stream().map(ActivityModels.Activity::id).toList();
        var selected=new HashSet<>(input.activityIds());
        if(selected.size()!=input.activityIds().size()||!order.containsAll(selected)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione actividades del Plan base sin repetirlas.");
        if(input.results().size()!=selected.size()||!selected.equals(new HashSet<>(input.results().stream().map(ReportService.Result::activityId).toList())))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Los resultados deben corresponder exactamente a las actividades seleccionadas.");
        String link=input.evidenceLink().trim();
        if(!link.isEmpty()) try {
            var uri=new java.net.URI(link);
            if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||link.chars().anyMatch(Character::isWhitespace)) throw new IllegalArgumentException();
        } catch(java.net.URISyntaxException|IllegalArgumentException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El enlace de evidencias debe ser una URL HTTPS absoluta, sin credenciales ni espacios."); }
        // Keep the Plan order so the PDF table matches the approved matrix.
        var ids=order.stream().filter(selected::contains).toList();
        var results=ids.stream().map(id->input.results().stream().filter(r->r.activityId().equals(id)).findFirst().orElseThrow()).toList();
        jdbc.update("""
            UPDATE execution_report SET activity_ids=?::jsonb,results=?::jsonb,analysis=?,strengths=?,weaknesses=?,opportunities=?,threats=?,conclusions=?,recommendations=?,improvement_actions=?::jsonb,evidence_link=? WHERE id=?
            """,json.writeValueAsString(ids),json.writeValueAsString(results),input.analysis(),input.strengths(),input.weaknesses(),input.opportunities(),input.threats(),input.conclusions(),input.recommendations(),
            json.writeValueAsString(input.improvementActions()),link,doc);
        jdbc.update("UPDATE work_plan SET title=?,row_version=row_version+1,updated_at=now() WHERE id=?",input.title().trim(),doc);
        audit.record(accounts.current(email).id(),"EXECUTION_REPORT_DRAFT_SAVED",doc);return get(doc,email);
    }
    public List<Blocker> blockers(UUID doc,String email) {
        var state=get(doc,email);var data=state.data();var result=new ArrayList<Blocker>();
        if(data.activityIds().isEmpty()) result.add(new Blocker("Resultados","Seleccione al menos una actividad ejecutada."));
        if(data.results().stream().anyMatch(r->r.percentage()==null)) result.add(new Blocker("Resultados","Declare el porcentaje de ejecución de cada actividad seleccionada."));
        if(data.analysis().isBlank()) result.add(new Blocker("Análisis de resultados","Complete el análisis de los resultados de la comisión."));
        if(java.util.stream.Stream.of(data.strengths(),data.weaknesses(),data.opportunities(),data.threats()).anyMatch(String::isBlank)) result.add(new Blocker("Análisis FODA","Complete fortalezas, debilidades, oportunidades y amenazas."));
        if(data.conclusions().isBlank()) result.add(new Blocker("Conclusiones","Complete las conclusiones."));
        if(data.recommendations().isBlank()) result.add(new Blocker("Recomendaciones","Complete las recomendaciones."));
        return List.copyOf(result);
    }
}
