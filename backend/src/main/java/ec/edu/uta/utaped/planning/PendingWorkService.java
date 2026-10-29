package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.signing.AuthorSignatureService;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PendingWorkService {
    public record Item(String id,String kind,UUID documentId,String title,String detail,String groupName,String periodName,
                       LocalDate deadline,boolean overdue,String step,String action) {}
    public record Page(List<Item> items,long total,int page,int size,Map<String,Long> counts) {}
    private final JdbcTemplate jdbc; private final Accounts accounts; private final WorkPlanService plans;
    private final EvidenceService evidence; private final AuthorSignatureService signatures; private final ZoneId zone;
    public PendingWorkService(JdbcTemplate jdbc,Accounts accounts,WorkPlanService plans,EvidenceService evidence,
                              AuthorSignatureService signatures,@Value("${app.planning.time-zone:America/Guayaquil}") String zone) {
        this.jdbc=jdbc;this.accounts=accounts;this.plans=plans;this.evidence=evidence;this.signatures=signatures;this.zone=ZoneId.of(zone);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public Page get(String email,boolean secure,String kind,int page,int size) {
        if(!Set.of("","CORRECTION","REVIEW","SIGNATURE","EVIDENCE").contains(kind)||page<0||page>1000000||size<1||size>50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revise los filtros de pendientes.");
        var actor=accounts.current(email);var today=LocalDate.now(zone);var items=new ArrayList<Item>();
        // The same document ACL applies to personal tasks. ADMIN is not an implicit participant.
        String from=" FROM work_plan w JOIN app_user actor ON actor.id=? JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id LEFT JOIN organizational_scope s ON s.id=w.scope_id WHERE "+WorkPlanService.ACCESS+" AND NOT w.discarded AND w.archived_at IS NULL AND p.status='ACTIVE' AND (s.id IS NULL OR s.active) ";
        var ids=jdbc.queryForList("SELECT w.id"+from+" AND ((w.document_state='DRAFT' AND (actor.id=COALESCE(s.principal_user_id,w.teacher_id) OR EXISTS(SELECT 1 FROM document_author au WHERE au.work_plan_id=w.id AND au.user_id=actor.id))) OR (w.document_type='T1' AND w.document_state='APPROVED' AND EXISTS(SELECT 1 FROM work_plan_matrix m CROSS JOIN LATERAL jsonb_array_elements(m.activities) a WHERE m.work_plan_id=w.id AND a->'responsibleIds' @> to_jsonb(ARRAY[actor.id::text])))) ORDER BY w.id",UUID.class,actor.id());
        for(var id:ids) {
            var plan=plans.get(id,email);
            if(plan.documentState().equals("DRAFT")) {
                if(plan.reviewRound()>1&&plan.editable())
                    items.add(new Item("correction-"+id,"CORRECTION",id,plan.title(),"Devuelto para corrección · Ronda "+plan.reviewRound(),plan.groupName(),plan.periodName(),null,false,"general","Corregir documento"));
                var artifacts=jdbc.queryForList("SELECT id FROM document_artifact WHERE work_plan_id=? AND source_row_version=? ORDER BY created_at DESC,id DESC LIMIT 1",UUID.class,id,plan.rowVersion());
                if(!artifacts.isEmpty()) {
                    var signing=signatures.state(id,artifacts.getFirst(),email,secure);
                    if(!signing.actorSigned()&&!signing.pendingAuthors().isEmpty()&&signing.pendingAuthors().getFirst().id().equals(actor.id()))
                        items.add(new Item("signature-"+id,"SIGNATURE",id,plan.title(),signing.signingEnabled()?"Es tu turno de firma de elaboración":"Tu firma tiene requisitos pendientes",plan.groupName(),plan.periodName(),null,false,plan.documentType().equals("T1")?"signing":plan.documentType().equals("T3")?"6":"preparation",signing.signingEnabled()?"Revisar y firmar":"Consultar requisitos"));
                }
            } else {
                var state=evidence.get(id,email);
                if(state.formalSubmissionPending()) continue;
                for(var activity:state.activities()) {
                    boolean assigned=activity.responsible().stream().anyMatch(person->person.id().equals(actor.id()));
                    long missing=activity.media().stream().filter(medium->medium.versions().isEmpty()).count();
                    if(assigned&&missing>0) {
                        boolean overdue=activity.deadline()!=null&&activity.deadline().isBefore(today);
                        items.add(new Item("evidence-"+id+"-"+activity.id(),"EVIDENCE",id,plan.title(),activity.title()+" · "+missing+(missing==1?" medio pendiente":" medios pendientes"),plan.groupName(),plan.periodName(),activity.deadline(),overdue,"execution",activity.canUpload()?"Registrar evidencia":"Consultar plazo"));
                    }
                }
            }
        }
        var reviewRows=jdbc.queryForList("SELECT t.id,t.requires_signature,t.stage_label,w.id AS document_id,w.title,g.name AS group_name,p.name AS period_name FROM review_task t JOIN document_review r ON r.id=t.review_id JOIN work_plan w ON w.id=r.work_plan_id JOIN app_user actor ON actor.id=? JOIN institutional_group g ON g.id=w.group_id JOIN academic_period p ON p.id=w.period_id LEFT JOIN organizational_scope s ON s.id=w.scope_id WHERE "+WorkPlanService.ACCESS+" AND t.actor_id=actor.id AND t.status='ACTIVE' AND r.status='IN_REVIEW' AND w.archived_at IS NULL AND NOT w.discarded AND p.status='ACTIVE'",actor.id());
        for(var row:reviewRows) items.add(new Item("review-"+row.get("id"),"REVIEW",(UUID)row.get("document_id"),(String)row.get("title"),(String)row.get("stage_label"),(String)row.get("group_name"),(String)row.get("period_name"),null,false,"signing",Boolean.TRUE.equals(row.get("requires_signature"))?"Revisar y firmar":"Revisar documento"));
        items.sort(Comparator.comparing(Item::overdue).reversed().thenComparing(i->priority(i.kind())).thenComparing(Item::deadline,Comparator.nullsLast(Comparator.naturalOrder())).thenComparing(Item::title).thenComparing(Item::id));
        var counts=new LinkedHashMap<String,Long>();for(var category:List.of("CORRECTION","REVIEW","SIGNATURE","EVIDENCE")) counts.put(category,items.stream().filter(i->i.kind().equals(category)).count());
        var filtered=items.stream().filter(i->kind.isEmpty()||kind.equals(i.kind())).toList();
        return new Page(filtered.stream().skip((long)page*size).limit(size).toList(),filtered.size(),page,size,Collections.unmodifiableMap(counts));
    }
    private static int priority(String kind) { return switch(kind) { case "CORRECTION"->0;case "REVIEW"->1;case "SIGNATURE"->2;default->3;}; }
}
