package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.*;
import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.writing.AiProvider;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class HolidayAssistance {
    public record Query(@Min(2000) @Max(2200) int year) {}
    public record Item(@NotNull LocalDate date,@NotBlank @Size(max=200) String label,@NotBlank @Size(max=2048) String sourceUrl) {}
    public record Proposal(List<Item> items) {}
    public record Apply(@Min(2000) @Max(2200) int year,@NotEmpty @Size(max=50) List<@Valid Item> items,@AssertTrue boolean reviewed,boolean overwrite) {}
    private final Accounts accounts;private final AiProvider provider;private final AuthThrottle throttle;private final ActivityService activities;private final AuditService audit;private final JdbcTemplate jdbc;
    private final Semaphore concurrent=new Semaphore(2);
    public HolidayAssistance(Accounts accounts,AiProvider provider,AuthThrottle throttle,ActivityService activities,AuditService audit,JdbcTemplate jdbc) { this.accounts=accounts;this.provider=provider;this.throttle=throttle;this.activities=activities;this.audit=audit;this.jdbc=jdbc; }
    private UUID admin(String email) { var actor=accounts.current(email);if(!actor.systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido.");return actor.id(); }
    public Proposal suggest(String email,Query input) {
        var actor=admin(email);if(input.year()<2000 || input.year()>2200) throw invalid();throttle.check("holiday-assistance:"+actor,5);
        if(!provider.configuration().available()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"La asistencia no está habilitada. Puede mantener el calendario manualmente.");
        if(!concurrent.tryAcquire()) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"La consulta está ocupada. Inténtelo de nuevo en unos momentos.");
        try {
            String response=provider.holidays(input.year());
            if(response==null || response.length()>40000) throw new IllegalArgumentException();
            var proposal=JsonMapper.builder().build().readValue(response,Proposal.class);validate(input.year(),proposal.items());admin(email);
            audit.record(actor,"HOLIDAY_SUGGESTIONS_REVIEW_REQUESTED",null);return proposal;
        } catch(ResponseStatusException error) { throw error; }
        catch(RuntimeException error) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se obtuvo una propuesta verificable. El calendario se conserva."); }
        finally { concurrent.release(); }
    }
    private void validate(int year,List<Item> items) {
        if(year<2000 || year>2200 || items==null || items.size()>50) throw invalid();
        var dates=new HashSet<LocalDate>();
        for(var item:items) {
            if(item==null || item.date()==null || item.date().getYear()!=year || !dates.add(item.date()) || item.label()==null || item.label().isBlank() || item.label().length()>200 || item.sourceUrl()==null || item.sourceUrl().length()>2048) throw invalid();
            try { var uri=new java.net.URI(item.sourceUrl());if(!"https".equalsIgnoreCase(uri.getScheme())||uri.getHost()==null||uri.getUserInfo()!=null||item.sourceUrl().chars().anyMatch(c->Character.isWhitespace(c)||Character.isISOControl(c))) throw invalid(); }
            catch(java.net.URISyntaxException error) { throw invalid(); }
        }
    }
    @Transactional public void apply(String email,Apply input) {
        admin(email);validate(input.year(),input.items());if(!input.reviewed() || input.items().isEmpty()) throw invalid();
        if(!input.overwrite() && input.items().stream().anyMatch(item->jdbc.queryForObject("SELECT count(*) FROM planning_holiday WHERE holiday_date=?",Integer.class,item.date())>0)) throw new ResponseStatusException(HttpStatus.CONFLICT,"Hay fechas ya registradas. Revise la selección o autorice su reemplazo.");
        for(var item:input.items()) activities.holiday(new ActivityModels.Holiday(item.date(),item.label(),item.sourceUrl()),email);
    }
    private ResponseStatusException invalid() { return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Revise fechas, nombres y fuentes HTTPS del año seleccionado."); }
}
