package ec.edu.uta.utaped.identity;

import ec.edu.uta.utaped.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.jdbc.JdbcIndexedSessionRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;

@RestController
@Validated
@RequestMapping("/api/admin/users")
public class UserAdminController {
    private final JdbcTemplate jdbc;
    private final Accounts accounts;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final JdbcIndexedSessionRepository sessions;
    public UserAdminController(JdbcTemplate jdbc,Accounts accounts,PasswordEncoder encoder,AuditService audit,JdbcIndexedSessionRepository sessions) {
        this.jdbc=jdbc;this.accounts=accounts;this.encoder=encoder;this.audit=audit;this.sessions=sessions;
    }
    private static final String FIELDS="id,email,display_name,system_role,active,must_change_password,row_version";
    @GetMapping public List<Map<String,Object>> list() { return jdbc.queryForList("SELECT "+FIELDS+" FROM app_user ORDER BY display_name,id LIMIT 200"); }
    public record Directory(List<Map<String,Object>> items,long total,int page,int size) {}
    @GetMapping("/directory") public Directory directory(@RequestParam(defaultValue="") @Size(max=200) String query,
        @RequestParam(required=false) Boolean active,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,
        @RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        String where=" WHERE position(lower(?) in lower(display_name||' '||email))>0 AND (?::boolean IS NULL OR active=?)";
        long total=jdbc.queryForObject("SELECT count(*) FROM app_user"+where,Long.class,query,active,active);
        var items=jdbc.queryForList("SELECT "+FIELDS+" FROM app_user"+where+" ORDER BY display_name,id LIMIT ? OFFSET ?",query,active,active,size,(long)page*size);
        return new Directory(items,total,page,size);
    }
    @GetMapping("/{id}") public Map<String,Object> get(@PathVariable UUID id) {
        return jdbc.queryForList("SELECT "+FIELDS+" FROM app_user WHERE id=?",id).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no disponible."));
    }
    public record ProfileInput(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=160) String displayName,
        @NotBlank @Pattern(regexp="ADMIN|USER") String systemRole) {}
    @PutMapping("/{id}") @Transactional public void profile(@PathVariable UUID id,@Valid @RequestBody ProfileInput data,Principal principal) {
        var actor=accounts.current(principal.getName());
        jdbc.queryForList("SELECT id FROM app_user WHERE system_role='ADMIN' ORDER BY id FOR UPDATE");
        var target=jdbc.queryForList("SELECT email,system_role,active,row_version FROM app_user WHERE id=? FOR UPDATE",id);
        if(target.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no disponible.");
        var current=target.getFirst();
        if(((Number)current.get("row_version")).longValue()!=data.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El usuario cambió en otra sesión. Recargue antes de guardar.");
        if(current.get("system_role").equals("ADMIN") && (boolean)current.get("active") && data.systemRole().equals("USER") && jdbc.queryForObject("SELECT count(*) FROM app_user WHERE active AND system_role='ADMIN'",Integer.class)<=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Debe conservar al menos un administrador activo.");
        jdbc.update("UPDATE app_user SET display_name=?,system_role=?,row_version=row_version+1 WHERE id=?",data.displayName().trim(),data.systemRole(),id);
        if(!current.get("system_role").equals(data.systemRole())) sessions.findByPrincipalName((String)current.get("email")).keySet().forEach(sessions::deleteById);
        audit.record(actor.id(),"USER_PROFILE_UPDATED",id);
    }
    public record UserInput(@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(max=160) String displayName,
        @NotBlank @Pattern(regexp="ADMIN|USER") String systemRole,@NotBlank @Size(max=72) String temporaryPassword) {}
    @PostMapping @Transactional public Map<String,UUID> create(@Valid @RequestBody UserInput data,Principal principal) {
        PasswordService.validate(data.temporaryPassword()); UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role) VALUES (?,?,?,?,?)",id,Accounts.normalize(data.email()),data.displayName().trim(),encoder.encode(data.temporaryPassword()),data.systemRole());
        audit.record(accounts.current(principal.getName()).id(),"USER_CREATED",id); return Map.of("id",id);
    }
    public record ActiveInput(@NotNull Boolean active,@PositiveOrZero Long rowVersion) {}
    @PatchMapping("/{id}/active") @Transactional public void active(@PathVariable UUID id,@Valid @RequestBody ActiveInput data,Principal principal) {
        var actor=accounts.current(principal.getName());
        // Serialize administrative status changes to preserve at least one active administrator.
        jdbc.queryForList("SELECT id FROM app_user WHERE system_role='ADMIN' ORDER BY id FOR UPDATE");
        var target=jdbc.queryForList("SELECT email,system_role,active,row_version FROM app_user WHERE id=? FOR UPDATE",id);
        if(target.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuario no disponible.");
        if(data.rowVersion()!=null && ((Number)target.getFirst().get("row_version")).longValue()!=data.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El usuario cambió en otra sesión. Recargue antes de cambiar su acceso.");
        if(!data.active() && target.getFirst().get("system_role").equals("ADMIN") && jdbc.queryForObject("SELECT count(*) FROM app_user WHERE system_role='ADMIN' AND active",Integer.class)<=1)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Debe conservar al menos un administrador activo.");
        jdbc.update("UPDATE app_user SET active=?,row_version=row_version+1 WHERE id=?",data.active(),id);
        if(!data.active()) sessions.findByPrincipalName((String)target.getFirst().get("email")).keySet().forEach(sessions::deleteById);
        audit.record(actor.id(),"USER_STATUS_CHANGED",id);
    }
}
