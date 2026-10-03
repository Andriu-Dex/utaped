package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api")
public class InstitutionController {
    private final JdbcTemplate jdbc;
    private final Accounts accounts;
    private final AuditService audit;
    public InstitutionController(JdbcTemplate jdbc,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.audit=audit; }
    @GetMapping("/groups") public List<Map<String,Object>> groups(Principal principal) {
        var actor=accounts.current(principal.getName());
        return jdbc.queryForList("""
            SELECT g.id,g.name,g.group_type,g.active,g.collective_label,g.row_version,m.membership_role
            FROM institutional_group g LEFT JOIN membership m ON m.group_id=g.id AND m.user_id=?
            WHERE g.active AND (? OR m.user_id IS NOT NULL) ORDER BY g.name
            """,actor.id(),actor.systemRole().equals("ADMIN"));
    }
    @GetMapping("/groups/{id}/members") public List<Map<String,Object>> members(@PathVariable UUID id,Principal principal) {
        var actor=accounts.current(principal.getName());
        boolean allowed=actor.systemRole().equals("ADMIN") || jdbc.queryForObject("SELECT count(*) FROM membership m JOIN institutional_group g ON g.id=m.group_id WHERE m.user_id=? AND m.group_id=? AND g.active",Integer.class,actor.id(),id)>0;
        if (!allowed) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Grupo no disponible.");
        return jdbc.queryForList("SELECT u.id,u.display_name,m.membership_role FROM membership m JOIN app_user u ON u.id=m.user_id WHERE m.group_id=? AND u.active ORDER BY u.display_name",id);
    }
    public record GroupInput(@NotBlank @Size(max=160) String name,@NotBlank @Pattern(regexp="COMMISSION|UNIT|CLUB|OTHER") String groupType) {}
    @GetMapping("/admin/groups") public List<Map<String,Object>> adminGroups() { return jdbc.queryForList("SELECT id,name,group_type,active,collective_label,row_version,NULL::varchar AS membership_role FROM institutional_group ORDER BY name,id"); }
    public record GroupUpdate(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=160) String name,
        @NotBlank @Pattern(regexp="COMMISSION|UNIT|CLUB|OTHER") String groupType,@NotNull Boolean active) {}
    @PutMapping("/admin/groups/{id}") @Transactional public void updateGroup(@PathVariable UUID id,@Valid @RequestBody GroupUpdate data,Principal principal) {
        var actor=accounts.current(principal.getName());
        var current=jdbc.queryForList("SELECT row_version FROM institutional_group WHERE id=? FOR UPDATE",id);
        if(current.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Grupo no disponible.");
        if(((Number)current.getFirst().get("row_version")).longValue()!=data.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El grupo cambió en otra sesión. Recargue antes de guardar.");
        jdbc.update("UPDATE institutional_group SET name=?,group_type=?,active=?,row_version=row_version+1 WHERE id=?",data.name().trim(),data.groupType(),data.active(),id);
        audit.record(actor.id(),"GROUP_PROFILE_UPDATED",id);
    }
    @PostMapping("/admin/groups") @Transactional public Map<String,UUID> createGroup(@Valid @RequestBody GroupInput data,Principal principal) {
        var actor=accounts.current(principal.getName()); UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,?,?)",id,data.name().trim(),data.groupType());
        audit.record(actor.id(),"GROUP_CREATED",id); return Map.of("id",id);
    }
    public record MembershipInput(@NotNull UUID userId,@NotBlank @Pattern(regexp="MEMBER|COORDINATOR") String membershipRole) {}
    @PostMapping("/admin/groups/{id}/members") @Transactional public void addMember(@PathVariable UUID id,@Valid @RequestBody MembershipInput data,Principal principal) {
        var actor=accounts.current(principal.getName());
        if(jdbc.queryForObject("SELECT count(*) FROM institutional_group WHERE id=? AND active",Integer.class,id)==0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un grupo activo.");
        if (jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id=? AND active",Integer.class,data.userId())==0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un usuario activo.");
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,?) ON CONFLICT(user_id,group_id) DO UPDATE SET membership_role=excluded.membership_role",data.userId(),id,data.membershipRole());
        audit.record(actor.id(),"MEMBERSHIP_ASSIGNED",id,data.userId());
    }
    @DeleteMapping("/admin/groups/{id}/members/{userId}") @Transactional public void removeMember(@PathVariable UUID id,@PathVariable UUID userId,Principal principal) {
        jdbc.update("DELETE FROM membership WHERE group_id=? AND user_id=?",id,userId);
        audit.record(accounts.current(principal.getName()).id(),"MEMBERSHIP_REMOVED",id,userId);
    }
    @GetMapping("/periods") public List<Map<String,Object>> periods() {
        var periods=jdbc.queryForList("SELECT * FROM academic_period ORDER BY starts_on DESC");
        periods.forEach(period -> period.replaceAll((key,value) -> value instanceof java.sql.Date date ? date.toLocalDate().toString() : value));
        return periods;
    }
    public record PeriodInput(@NotBlank @Size(max=120) String name,@NotNull LocalDate startsOn,@NotNull LocalDate endsOn,
        @NotNull LocalDate preparationStartsOn,@NotNull LocalDate preparationEndsOn,@NotNull LocalDate reviewStartsOn,@NotNull LocalDate reviewEndsOn) {}
    @PostMapping("/admin/periods") @Transactional public Map<String,UUID> createPeriod(@Valid @RequestBody PeriodInput data,Principal principal) {
        if(data.startsOn().isAfter(data.endsOn()) || data.preparationStartsOn().isAfter(data.preparationEndsOn()) || data.reviewStartsOn().isAfter(data.reviewEndsOn()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Las fechas iniciales no pueden superar las finales.");
        UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,?,?,?,?,?,?,?)",id,data.name().trim(),data.startsOn(),data.endsOn(),data.preparationStartsOn(),data.preparationEndsOn(),data.reviewStartsOn(),data.reviewEndsOn());
        audit.record(accounts.current(principal.getName()).id(),"PERIOD_CREATED",id); return Map.of("id",id);
    }
}
