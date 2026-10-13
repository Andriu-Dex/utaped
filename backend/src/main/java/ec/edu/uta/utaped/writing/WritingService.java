package ec.edu.uta.utaped.writing;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.identity.AuthThrottle;
import ec.edu.uta.utaped.planning.WorkPlanService;
import jakarta.validation.constraints.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class WritingService {
    public record Request(@NotNull @PositiveOrZero Long rowVersion,
        @NotBlank @Pattern(regexp="JUSTIFICATION|OBJECTIVE|BACKGROUND|CONCLUSIONS|IMPROVEMENTS") String field,
        @NotBlank @Pattern(regexp="CLEAR|FORMAL|CONCISE") String tone,
        @NotBlank @Size(max=50000) String text, @NotNull @AssertTrue Boolean consent) {}
    public record Suggestion(String text, String field, long rowVersion) {}
    private final WorkPlanService plans; private final Accounts accounts; private final AuditService audit;
    private final AuthThrottle throttle; private final AiProvider provider; private final int userLimit;
    private final Semaphore concurrent;
    public WritingService(WorkPlanService plans, Accounts accounts, AuditService audit, AuthThrottle throttle, AiProvider provider,
        @Value("${app.ai-writing.user-limit:15}") int userLimit, @Value("${app.ai-writing.concurrency:2}") int concurrency) {
        if(userLimit<1 || userLimit>100 || concurrency<1 || concurrency>20) throw new IllegalArgumentException("Límites de redacción inválidos.");
        this.plans=plans;this.accounts=accounts;this.audit=audit;this.throttle=throttle;this.provider=provider;
        this.userLimit=userLimit;this.concurrent=new Semaphore(concurrency);
    }
    public AiProvider.Configuration configuration() { return provider.configuration(); }
    private void authorize(UUID doc, String email, Request data) {
        var plan=plans.get(doc,email);
        if(!plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La redacción asistida solo está disponible en borradores editables.");
        if(plan.rowVersion()!=data.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió. Recargue antes de solicitar otra sugerencia.");
        var fields=plan.documentType().equals("T1") ? Set.of("JUSTIFICATION","OBJECTIVE") : Set.of("BACKGROUND","CONCLUSIONS","IMPROVEMENTS");
        if(!fields.contains(data.field()) || !Set.of("CLEAR","FORMAL","CONCISE").contains(data.tone())
            || !Boolean.TRUE.equals(data.consent()) || data.text()==null || data.text().isBlank()
            || data.text().length()>configuration().maxInputCharacters())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revise el campo, el texto y la autorización de envío.");
    }
    // No transaction or document lock is kept open while waiting for an external provider.
    public Suggestion suggest(UUID doc, String email, Request data) {
        authorize(doc,email,data);
        if(!configuration().available()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La asistencia de redacción no está habilitada. Puede editar manualmente.");
        var actor=accounts.current(email).id(); throttle.check("writing:"+actor,userLimit);
        if(!concurrent.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"La asistencia está ocupada. Vuelva a intentarlo en unos momentos.");
        try {
            audit.record(actor,"WRITING_REQUESTED",doc);
            String text=provider.suggest(data.field(),data.tone(),data.text());
            authorize(doc,email,data);
            if(text==null || text.isBlank() || text.length()>50000) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se obtuvo una sugerencia completa. Su texto se conserva.");
            audit.record(actor,"WRITING_SUGGESTED",doc);
            return new Suggestion(text,data.field(),data.rowVersion());
        } catch(ResponseStatusException error) { audit.record(actor,"WRITING_FAILED",doc);throw error; }
        catch(RuntimeException error) { audit.record(actor,"WRITING_FAILED",doc);throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo obtener una sugerencia. Su texto se conserva."); }
        finally { concurrent.release(); }
    }
}
