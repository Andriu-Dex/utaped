package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Individual reopening of the T1 elaboration window for one organizational scope. */
@Service
public class PreparationExtensionService {
    public record Grant(@NotNull @PositiveOrZero Long rowVersion,@NotNull LocalDate endsOn,@NotBlank @Size(max=1000) String reason) {}
    public record State(LocalDate periodPreparationEndsOn,LocalDate effectiveEndsOn,LocalDate maxEndsOn,int maxBusinessDays,long rowVersion,boolean canGrant,List<Map<String,Object>> history) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final AuditService audit;private final BusinessCalendar calendar;private final int maxDays;
    public PreparationExtensionService(JdbcTemplate jdbc,Accounts accounts,AuditService audit,BusinessCalendar calendar,@Value("${app.planning.max-extension-days:3}") int maxDays) {
        this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.calendar=calendar;this.maxDays=maxDays;
    }
    private Map<String,Object> scope(UUID scope,UUID actor,boolean lock) {
        var rows=jdbc.queryForList("""
            SELECT s.preparation_extended_until,s.preparation_extension_version,p.preparation_ends_on,p.ends_on,p.status,
              EXISTS(SELECT 1 FROM scope_member m WHERE m.scope_id=s.id AND m.user_id=? AND m.active AND m.membership_role='COORDINATOR') AS coordinator,
              EXISTS(SELECT 1 FROM scope_member m WHERE m.scope_id=s.id AND m.user_id=? AND m.active) AS member
            FROM organizational_scope s JOIN academic_period p ON p.id=s.period_id WHERE s.id=? AND s.active
            """+(lock?" FOR UPDATE OF s":""),actor,actor,scope);
        if(rows.isEmpty()||!(boolean)rows.getFirst().get("member")) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ámbito no disponible.");
        return rows.getFirst();
    }
    private static LocalDate date(Object value) { return value==null?null:((java.sql.Date)value).toLocalDate(); }
    public State get(UUID scope,String email) {
        var row=scope(scope,accounts.current(email).id(),false);
        LocalDate base=date(row.get("preparation_ends_on")),extended=date(row.get("preparation_extended_until")),periodEnd=date(row.get("ends_on"));
        LocalDate max=calendar.addBusinessDays(base,maxDays);if(max.isAfter(periodEnd)) max=periodEnd;
        var history=jdbc.queryForList("SELECT h.id,h.old_deadline,h.new_deadline,h.reason,h.created_at,u.display_name AS authorized_by_name FROM preparation_extension_history h JOIN app_user u ON u.id=h.authorized_by WHERE h.scope_id=? ORDER BY h.created_at DESC,h.id",scope);
        return new State(base,extended!=null&&extended.isAfter(base)?extended:base,max,maxDays,((Number)row.get("preparation_extension_version")).longValue(),
            (boolean)row.get("coordinator")&&"ACTIVE".equals(row.get("status")),history);
    }
    @Transactional public State grant(UUID scope,String email,Grant input) {
        var actor=accounts.current(email).id();var row=scope(scope,actor,true);
        if(!(boolean)row.get("coordinator")||!"ACTIVE".equals(row.get("status"))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el coordinador del ámbito puede reabrir la elaboración en un período activo.");
        if(input.rowVersion()!=((Number)row.get("preparation_extension_version")).longValue()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El plazo cambió en otra sesión. Recargue antes de guardar.");
        var state=get(scope,email);
        if(!input.endsOn().isAfter(state.effectiveEndsOn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El nuevo plazo debe ampliar el plazo vigente de elaboración.");
        if(input.endsOn().isAfter(state.maxEndsOn())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La reapertura admite como máximo "+maxDays+" días hábiles acumulados desde el cierre original, dentro del período.");
        jdbc.update("UPDATE organizational_scope SET preparation_extended_until=?,preparation_extension_version=preparation_extension_version+1 WHERE id=?",input.endsOn(),scope);
        jdbc.update("INSERT INTO preparation_extension_history(id,scope_id,old_deadline,new_deadline,reason,authorized_by) VALUES (?,?,?,?,?,?)",UUID.randomUUID(),scope,state.effectiveEndsOn(),input.endsOn(),input.reason().trim(),actor);
        audit.record(actor,"PREPARATION_WINDOW_EXTENDED",null);
        return get(scope,email);
    }
}
