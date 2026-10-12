package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OrganizationService {
    public record CareerInput(@NotBlank @Pattern(regexp="[A-Z0-9_-]{1,30}") String code,@NotBlank @Size(max=160) String name) {}
    public record ScopeInput(@NotNull UUID groupId,@NotNull UUID periodId,UUID careerId) {}
    public record MemberInput(@NotNull UUID userId,@NotBlank @Pattern(regexp="MEMBER|COORDINATOR") String membershipRole,
        @NotNull @Size(max=160) String positionLabel,@NotNull Boolean active,@NotNull @PositiveOrZero Long rowVersion) {}
    public record PrincipalInput(@NotNull UUID userId,@NotNull @PositiveOrZero Long rowVersion) {}
    private final JdbcTemplate jdbc; private final Accounts accounts; private final AuditService audit;
    public OrganizationService(JdbcTemplate jdbc,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.audit=audit; }
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    private void admin(String email) { if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido."); }
    public List<Map<String,Object>> careers() { return jdbc.queryForList("SELECT * FROM academic_career ORDER BY name,id"); }
    public List<Map<String,Object>> scopes(String email) {
        var actor=accounts.current(email);
        return jdbc.queryForList("""
            SELECT s.*,g.name AS group_name,p.name AS period_name,c.name AS career_name,u.display_name AS principal_name,
              (SELECT count(*) FROM scope_member m JOIN app_user a ON a.id=m.user_id WHERE m.scope_id=s.id AND m.active AND a.active) AS member_count
            FROM organizational_scope s JOIN institutional_group g ON g.id=s.group_id JOIN academic_period p ON p.id=s.period_id
            LEFT JOIN academic_career c ON c.id=s.career_id LEFT JOIN app_user u ON u.id=s.principal_user_id
            WHERE ? OR (s.active AND g.active AND EXISTS (SELECT 1 FROM scope_member m WHERE m.scope_id=s.id AND m.user_id=? AND m.active))
            ORDER BY p.starts_on DESC,g.name,c.name NULLS FIRST,s.id
            """,actor.systemRole().equals("ADMIN"),actor.id());
    }
    public List<Map<String,Object>> members(UUID scope,String email) {
        if(scopes(email).stream().noneMatch(s->scope.equals(s.get("id")))) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Ámbito no disponible.");
        return jdbc.queryForList("SELECT m.*,u.display_name,u.active AS user_active FROM scope_member m JOIN app_user u ON u.id=m.user_id WHERE m.scope_id=? ORDER BY u.display_name,u.id",scope);
    }
    @Transactional public UUID createCareer(String email,CareerInput input) {
        admin(email);UUID id=UUID.randomUUID();
        if(jdbc.queryForObject("SELECT count(*) FROM academic_career WHERE code=? OR name=?",Integer.class,input.code(),input.name().trim())>0) throw invalid("La carrera ya está registrada.");
        jdbc.update("INSERT INTO academic_career(id,code,name) VALUES (?,?,?)",id,input.code(),input.name().trim());audit.record(accounts.current(email).id(),"CAREER_CREATED",id);return id;
    }
    @Transactional public UUID createScope(String email,ScopeInput input) {
        admin(email);
        new PeriodGuard(jdbc).writable(input.periodId());
        if(jdbc.queryForObject("SELECT count(*) FROM academic_period WHERE id=? AND workflow_locked_at IS NOT NULL",Integer.class,input.periodId())>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Los ámbitos del período activado están bloqueados. Configure el siguiente período.");
        jdbc.queryForList("SELECT id FROM institutional_group WHERE id=? FOR UPDATE",input.groupId());
        if(jdbc.queryForObject("SELECT count(*) FROM institutional_group WHERE id=? AND active",Integer.class,input.groupId())==0 || jdbc.queryForObject("SELECT count(*) FROM academic_period WHERE id=?",Integer.class,input.periodId())==0) throw invalid("Seleccione grupo y período válidos.");
        if(input.careerId()!=null && jdbc.queryForObject("SELECT count(*) FROM academic_career WHERE id=? AND active",Integer.class,input.careerId())==0) throw invalid("Seleccione una carrera activa.");
        if(jdbc.queryForObject("SELECT count(*) FROM organizational_scope WHERE group_id=? AND period_id=? AND career_id IS NOT DISTINCT FROM ?::uuid",Integer.class,input.groupId(),input.periodId(),input.careerId())>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ese ámbito ya está registrado.");
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO organizational_scope(id,group_id,period_id,career_id) VALUES (?,?,?,?)",id,input.groupId(),input.periodId(),input.careerId());audit.record(accounts.current(email).id(),"SCOPE_CREATED",id);return id;
    }
    private Map<String,Object> lock(UUID scope) {
        var periods=jdbc.queryForList("SELECT period_id FROM organizational_scope WHERE id=?",UUID.class,scope);
        if(!periods.isEmpty()) new PeriodGuard(jdbc).writable(periods.getFirst());
        return jdbc.queryForList("SELECT * FROM organizational_scope WHERE id=? FOR UPDATE",scope).stream().findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Ámbito no disponible."));
    }
    @Transactional public void member(UUID scope,String email,MemberInput input) {
        admin(email);var current=lock(scope);
        if(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id=? AND active",Integer.class,input.userId())==0) throw invalid("Seleccione un usuario activo.");
        if(!input.active() && input.userId().equals(current.get("principal_user_id"))) throw invalid("Designe otro responsable principal antes de retirar esta pertenencia.");
        var previous=jdbc.queryForList("SELECT row_version FROM scope_member WHERE scope_id=? AND user_id=?",scope,input.userId());
        long version=previous.isEmpty()?0:((Number)previous.getFirst().get("row_version")).longValue();
        if(version!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"La pertenencia cambió. Recargue antes de guardar.");
        jdbc.update("""
            INSERT INTO scope_member(scope_id,user_id,membership_role,position_label,active,row_version) VALUES (?,?,?,?,?,1)
            ON CONFLICT(scope_id,user_id) DO UPDATE SET membership_role=excluded.membership_role,position_label=excluded.position_label,active=excluded.active,row_version=scope_member.row_version+1
            """,scope,input.userId(),input.membershipRole(),input.positionLabel().trim(),input.active());
        // General membership remains a navigation projection, not authority over scoped documents.
        if(input.active()) jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,?) ON CONFLICT(user_id,group_id) DO NOTHING",input.userId(),current.get("group_id"),input.membershipRole());
        audit.record(accounts.current(email).id(),"SCOPE_MEMBERSHIP_UPDATED",scope,input.userId());
    }
    @Transactional public void principal(UUID scope,String email,PrincipalInput input) {
        admin(email);var current=lock(scope);
        if(((Number)current.get("row_version")).longValue()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El ámbito cambió. Recargue antes de guardar.");
        if(jdbc.queryForObject("SELECT count(*) FROM scope_member m JOIN app_user u ON u.id=m.user_id WHERE m.scope_id=? AND m.user_id=? AND m.active AND u.active",Integer.class,scope,input.userId())==0) throw invalid("El responsable principal debe ser un integrante activo del ámbito.");
        jdbc.update("UPDATE organizational_scope SET principal_user_id=?,row_version=row_version+1 WHERE id=?",input.userId(),scope);
        // Explicit principal designation transfers responsibility for this exact scope, with audit.
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) SELECT id,?,? FROM work_plan WHERE scope_id=? ON CONFLICT DO NOTHING",input.userId(),accounts.current(email).id(),scope);
        audit.record(accounts.current(email).id(),"SCOPE_PRINCIPAL_DESIGNATED",scope,input.userId());
    }
}
