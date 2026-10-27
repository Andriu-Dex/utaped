package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.security.Principal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api")
public class InstitutionalUnits {
    public static final UUID FISEI=UUID.fromString("d091bc85-0823-4977-8b45-493cb466f858");
    private final JdbcTemplate jdbc; private final Accounts accounts; private final AuditService audit;
    public InstitutionalUnits(JdbcTemplate jdbc,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.audit=audit; }
    public record Input(@NotBlank @Size(max=200) String name,@NotBlank @Pattern(regexp="ACADEMIC|ADMINISTRATIVE") String unitType,@NotNull Boolean active,@NotNull @PositiveOrZero Long rowVersion) {}
    @GetMapping("/institutional-units") public List<Map<String,Object>> list(Principal p) {
        return jdbc.queryForList("SELECT * FROM institutional_unit WHERE active OR ? ORDER BY name,id",accounts.current(p.getName()).systemRole().equals("ADMIN"));
    }
    private void admin(Principal p) { if(!accounts.current(p.getName()).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido."); }
    @PostMapping("/admin/institutional-units") @Transactional public Map<String,UUID> create(@Valid @RequestBody Input input,Principal p) { admin(p);UUID id=UUID.randomUUID();write(id,input,p,false);return Map.of("id",id); }
    @PutMapping("/admin/institutional-units/{id}") @Transactional public void update(@PathVariable UUID id,@Valid @RequestBody Input input,Principal p) { admin(p);write(id,input,p,true); }
    private void write(UUID id,Input input,Principal p,boolean updating) {
        if(input.name().isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Indique el nombre de la unidad.");
        if(updating) {
            var rows=jdbc.queryForList("SELECT * FROM institutional_unit WHERE id=? FOR UPDATE",id);
            if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Unidad no disponible.");
            var prior=rows.getFirst();
            if(((Number)prior.get("row_version")).longValue()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"La unidad cambió. Recargue antes de guardar.");
            if(!prior.get("unit_type").equals(input.unitType()) && jdbc.queryForObject("SELECT (SELECT count(*) FROM academic_career WHERE institutional_unit_id=?)+(SELECT count(*) FROM organizational_scope WHERE institutional_unit_id=?)",Integer.class,id,id)>0)
                throw new ResponseStatusException(HttpStatus.CONFLICT,"No cambie el tipo de una unidad con carreras o ámbitos. Registre otra unidad.");
        }
        if(jdbc.queryForObject("SELECT count(*) FROM institutional_unit WHERE lower(name)=lower(?) AND id<>?",Integer.class,input.name().trim(),id)>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"La unidad ya está registrada.");
        if(updating) jdbc.update("UPDATE institutional_unit SET name=?,unit_type=?,active=?,row_version=row_version+1 WHERE id=?",input.name().trim(),input.unitType(),input.active(),id);
        else jdbc.update("INSERT INTO institutional_unit(id,name,unit_type,active) VALUES (?,?,?,?)",id,input.name().trim(),input.unitType(),input.active());
        audit.record(accounts.current(p.getName()).id(),updating?"INSTITUTIONAL_UNIT_UPDATED":"INSTITUTIONAL_UNIT_CREATED",id);
    }
}
