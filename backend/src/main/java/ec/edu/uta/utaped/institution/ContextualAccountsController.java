package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.identity.*;
import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/organization/scopes/{scope}/accounts")
public class ContextualAccountsController {
    private final JdbcTemplate jdbc;private final Accounts accounts;private final PasswordService passwords;private final OrganizationService organization;
    public ContextualAccountsController(JdbcTemplate jdbc,Accounts accounts,PasswordService passwords,OrganizationService organization) { this.jdbc=jdbc;this.accounts=accounts;this.passwords=passwords;this.organization=organization; }
    @GetMapping public List<Map<String,Object>> list(@PathVariable UUID scope,Principal p) {
        organization.members(scope,p.getName());
        var actor=accounts.current(p.getName());
        return jdbc.queryForList("""
            SELECT u.id,u.display_name,u.username,u.row_version FROM scope_member t JOIN app_user u ON u.id=t.user_id
            JOIN organizational_scope s ON s.id=t.scope_id AND s.active
            JOIN academic_period period ON period.id=s.period_id AND period.status='ACTIVE'
            JOIN institutional_group g ON g.id=s.group_id AND g.active
            WHERE s.id=? AND t.active AND u.active AND u.system_role='USER' AND u.id<>?
              AND EXISTS(SELECT 1 FROM scope_member a WHERE a.scope_id=s.id AND a.user_id=? AND a.active AND a.membership_role='COORDINATOR' AND a.password_reset_authorized)
            ORDER BY u.display_name,u.id
            """,scope,actor.id(),actor.id());
    }
    @PostMapping("/{id}/temporary-password") public void reset(@PathVariable UUID scope,@PathVariable UUID id,@Valid @RequestBody UserAdminController.TemporaryPassword input,Principal p) {
        passwords.contextualReset(p.getName(),scope,id,input.rowVersion(),input.temporaryPassword());
    }
}
