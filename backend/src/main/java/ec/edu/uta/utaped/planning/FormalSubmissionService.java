package ec.edu.uta.utaped.planning;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import ec.edu.uta.utaped.documents.*;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import tools.jackson.databind.json.JsonMapper;
@Service
public class FormalSubmissionService {
    public record State(boolean required,boolean registered,boolean canRegister,UUID fileId) {}
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final Accounts accounts;private final StoredFiles files;private final PdfValidation validation;private final AuditService audit;
    public FormalSubmissionService(JdbcTemplate jdbc,WorkPlanService plans,Accounts accounts,StoredFiles files,PdfValidation validation,AuditService audit) { this.jdbc=jdbc;this.plans=plans;this.accounts=accounts;this.files=files;this.validation=validation;this.audit=audit; }
    public State state(UUID doc,String email) {
        var plan=plans.get(doc,email);var actor=accounts.current(email);
        var snapshots=jdbc.queryForList("SELECT a.snapshot::text FROM document_review r JOIN document_artifact a ON a.id=r.artifact_id WHERE r.work_plan_id=? AND r.status='APPROVED' ORDER BY r.round_number DESC LIMIT 1",String.class,doc);
        boolean required=!snapshots.isEmpty()&&Optional.ofNullable(JsonMapper.builder().build().readValue(snapshots.getFirst(),ArtifactModels.Snapshot.class).workflow()).map(DocumentWorkflowSnapshot.Workflow::requiresQuipux).orElse(false);
        var existing=jdbc.queryForList("SELECT file_id FROM formal_submission WHERE work_plan_id=?",UUID.class,doc);
        boolean coordinator=plan.scopeId()!=null&&jdbc.queryForObject("SELECT count(*) FROM scope_member WHERE scope_id=? AND user_id=? AND active AND membership_role='COORDINATOR'",Integer.class,plan.scopeId(),actor.id())>0;
        boolean active=jdbc.queryForObject("SELECT status='ACTIVE' FROM academic_period WHERE id=?",Boolean.class,plan.periodId());
        return new State(required,!existing.isEmpty(),required&&active&&plan.archivedAt()==null&&coordinator&&plan.documentState().equals("APPROVED")&&existing.isEmpty(),existing.isEmpty()?null:existing.getFirst());
    }
    public void requireComplete(UUID doc,String email) { var state=state(doc,email);if(state.required()&&!state.registered())throw new ResponseStatusException(HttpStatus.CONFLICT,"La coordinación debe registrar primero la constancia de Quipux del documento aprobado."); }
    @Transactional public State register(UUID doc,String email,MultipartFile file) throws java.io.IOException {
        plans.lock(doc,email);var state=state(doc,email);if(!state.canRegister())throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo la coordinación puede registrar la constancia pendiente de un documento aprobado y vigente.");
        if(file.isEmpty()||file.getSize()>10485760)throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Seleccione un PDF de hasta 10 MB.");byte[] bytes=file.getBytes();int pages=validation.pages(bytes);var stored=files.put(bytes,"constancia-quipux.pdf",pages);jdbc.update("INSERT INTO formal_submission(work_plan_id,file_id,registered_by) VALUES (?,?,?)",doc,stored.id(),accounts.current(email).id());audit.record(accounts.current(email).id(),"QUIPUX_PROOF_REGISTERED",doc);return state(doc,email);
    }
    public byte[] content(UUID doc,String email) { var state=state(doc,email);if(state.fileId()==null)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Constancia no disponible.");return files.read(state.fileId()); }
}
