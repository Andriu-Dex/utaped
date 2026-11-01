package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Private incomplete work. It never feeds document composition or readiness. */
@Service
public class MatrixRecovery {
    public record Choice(UUID catalogId,@Size(max=500) String other,@Size(max=500) String label,String evidenceKind) {}
    public record Row(@NotNull UUID id,UUID catalogId,@NotNull @Size(max=500) String title,
        @NotNull @Pattern(regexp="POA|IMPROVEMENT_PLAN|IMPROVEMENT_ACTION|OTHER") String category,boolean mandatory,
        LocalDate startsOn,LocalDate endsOn,@NotNull @Size(max=200) List<@NotNull UUID> responsibleIds,boolean collective,
        @NotNull @Size(max=50) List<@NotNull @Valid Choice> resources,@NotNull @Size(max=50) List<@NotNull @Valid Choice> means,
        @Valid ActivityModels.Context context) {}
    public record Draft(@NotNull @Size(max=500) String source,@NotNull @Size(max=2000) String note,
        @NotNull @Size(max=100) List<@NotNull @Valid Row> activities) {}
    public record Content(@NotNull @Valid Draft draft,@NotNull @Size(max=500) String catalogId,
        @NotNull @Size(max=500) String otherTitle,@NotNull @Size(max=100) List<@NotNull UUID> selectedDefinitions) {}
    public record Input(@NotNull @PositiveOrZero Long baseVersion,@PositiveOrZero Long revision,@NotNull @Valid Content content) {}
    public record Recovery(long baseVersion,long revision,Content content) {}
    public record State(Recovery recovery) {}
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final Accounts accounts;
    private final JsonMapper json=JsonMapper.builder().build();
    public MatrixRecovery(JdbcTemplate jdbc,WorkPlanService plans,Accounts accounts) { this.jdbc=jdbc;this.plans=plans;this.accounts=accounts; }
    private void editable(WorkPlanModels.Plan plan) {
        if(!plan.documentType().equals("T1") || !plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El documento está en modo de solo lectura.");
    }
    public State read(UUID document,String email) {
        var plan=plans.get(document,email);if(!plan.editable() || !plan.documentType().equals("T1")) return new State(null);
        var rows=jdbc.query("SELECT base_version,revision,content::text FROM work_plan_matrix_recovery WHERE work_plan_id=? AND user_id=?",
            (rs,n)->new Recovery(rs.getLong(1),rs.getLong(2),json.readValue(rs.getString(3),Content.class)),document,accounts.current(email).id());
        return new State(rows.isEmpty()?null:rows.getFirst());
    }
    @Transactional public State save(UUID document,String email,Input input) {
        var plan=plans.lock(document,email);editable(plan);
        if(plan.rowVersion()!=input.baseVersion()) throw conflict();
        UUID user=accounts.current(email).id();String content=json.writeValueAsString(input.content());
        if(content.length()>200000) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El borrador supera el tamaño permitido.");
        int changed;
        if(input.revision()==null) changed=jdbc.update("INSERT INTO work_plan_matrix_recovery(work_plan_id,user_id,base_version,content) VALUES (?,?,?,?::jsonb) ON CONFLICT DO NOTHING",document,user,input.baseVersion(),content);
        else changed=jdbc.update("UPDATE work_plan_matrix_recovery SET base_version=?,content=?::jsonb,revision=revision+1,updated_at=now() WHERE work_plan_id=? AND user_id=? AND revision=?",input.baseVersion(),content,document,user,input.revision());
        if(changed!=1) throw conflict();return read(document,email);
    }
    @Transactional public void discard(UUID document,String email,long revision) {
        editable(plans.lock(document,email));
        if(jdbc.update("DELETE FROM work_plan_matrix_recovery WHERE work_plan_id=? AND user_id=? AND revision=?",document,accounts.current(email).id(),revision)!=1) throw conflict();
    }
    private ResponseStatusException conflict() { return new ResponseStatusException(HttpStatus.CONFLICT,"La matriz o el borrador cambió en otra sesión. Revise la versión actual antes de continuar."); }
}
