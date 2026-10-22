package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.constraints.*;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import org.apache.commons.csv.CSVFormat;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** Institutional positions (UPE, coordination, dean's office…) and who holds them in each period and career. */
@Service
public class PositionService {
    public record PositionInput(@NotBlank @Pattern(regexp="[A-Z0-9_]{1,40}") String code,@NotBlank @Size(max=160) String name,@NotNull Boolean careerSpecific) {}
    public record PositionUpdate(@NotBlank @Size(max=160) String name,@NotNull Boolean active,@NotNull @PositiveOrZero Long rowVersion) {}
    public record AssignmentInput(@NotNull UUID positionId,UUID careerId,@NotNull UUID userId) {}
    public record ImportResult(int assigned,List<String> errors) {}
    public record CsvInput(@NotNull @Size(max=200000) String csv) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final AuditService audit;
    public PositionService(JdbcTemplate jdbc,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.audit=audit; }
    private UUID admin(String email) {
        var actor=accounts.current(email);
        if(!actor.systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido.");
        return actor.id();
    }
    public record HistoryPermission(@NotNull Boolean enabled,@NotNull @PositiveOrZero Long rowVersion) {}
    @Transactional public void historyPermission(UUID id,String email,HistoryPermission input) { var actor=admin(email);if(jdbc.update("UPDATE institutional_position SET can_view_history=?,row_version=row_version+1 WHERE id=? AND row_version=?",input.enabled(),id,input.rowVersion())!=1)throw new ResponseStatusException(HttpStatus.CONFLICT,"El cargo cambió. Recargue antes de guardar.");audit.record(actor,"POSITION_HISTORY_PERMISSION_CHANGED",id); }
    public List<Map<String,Object>> positions(String email) {
        admin(email);
        return jdbc.queryForList("SELECT * FROM institutional_position ORDER BY active DESC,name,id");
    }
    @Transactional public UUID create(String email,PositionInput input) {
        var actor=admin(email);UUID id=UUID.randomUUID();
        if(jdbc.queryForObject("SELECT count(*) FROM institutional_position WHERE code=?",Integer.class,input.code())>0) throw new ResponseStatusException(HttpStatus.CONFLICT,"Ya existe un cargo con ese código.");
        jdbc.update("INSERT INTO institutional_position(id,code,name,career_specific) VALUES (?,?,?,?)",id,input.code(),input.name().trim(),input.careerSpecific());
        audit.record(actor,"POSITION_CREATED",id);return id;
    }
    @Transactional public void update(UUID id,String email,PositionUpdate input) {
        var actor=admin(email);
        if(jdbc.update("UPDATE institutional_position SET name=?,active=?,row_version=row_version+1 WHERE id=? AND row_version=?",input.name().trim(),input.active(),id,input.rowVersion())==0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"El cargo cambió o no existe. Recargue antes de guardar.");
        audit.record(actor,"POSITION_UPDATED",id);
    }
    public List<Map<String,Object>> assignments(UUID period,String email) {
        admin(email);
        return jdbc.queryForList("""
            SELECT a.id,a.position_id,a.career_id,a.user_id,a.assigned_at,p.name AS position_name,p.code AS position_code,c.name AS career_name,u.display_name AS user_name,u.active AS user_active
            FROM position_assignment a JOIN institutional_position p ON p.id=a.position_id JOIN app_user u ON u.id=a.user_id LEFT JOIN academic_career c ON c.id=a.career_id
            WHERE a.period_id=? ORDER BY p.name,c.name NULLS FIRST,a.id
            """,period);
    }
    @Transactional public void assign(UUID period,String email,AssignmentInput input) {
        var actor=admin(email);new PeriodGuard(jdbc).writable(period);
        var positions=jdbc.queryForList("SELECT career_specific FROM institutional_position WHERE id=? AND active",Boolean.class,input.positionId());
        if(positions.isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un cargo activo.");
        if(positions.getFirst()!=(input.careerId()!=null)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,positions.getFirst()?"Este cargo se asigna por carrera.":"Este cargo es de facultad; no seleccione carrera.");
        if(input.careerId()!=null&&jdbc.queryForObject("SELECT count(*) FROM academic_career WHERE id=? AND active",Integer.class,input.careerId())==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione una carrera activa.");
        if(jdbc.queryForObject("SELECT count(*) FROM app_user WHERE id=? AND active",Integer.class,input.userId())==0) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un usuario activo.");
        jdbc.update("""
            INSERT INTO position_assignment(id,period_id,position_id,career_id,user_id,assigned_by) VALUES (?,?,?,?,?,?)
            ON CONFLICT(period_id,position_id,career_id) DO UPDATE SET user_id=excluded.user_id,assigned_by=excluded.assigned_by,assigned_at=now()
            """,UUID.randomUUID(),period,input.positionId(),input.careerId(),input.userId(),actor);
        audit.record(actor,"POSITION_ASSIGNED",period,input.userId());
    }
    @Transactional public void remove(UUID period,UUID assignment,String email) {
        var actor=admin(email);new PeriodGuard(jdbc).writable(period);
        if(jdbc.update("DELETE FROM position_assignment WHERE id=? AND period_id=?",assignment,period)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Asignación no disponible.");
        audit.record(actor,"POSITION_UNASSIGNED",period);
    }
    /** CSV columns: cargo (code), carrera (code or empty), usuario (username or email). All rows apply or none. */
    @Transactional public ImportResult importCsv(UUID period,String email,String csv) {
        admin(email);
        var errors=new ArrayList<String>();var inputs=new ArrayList<AssignmentInput>();
        try(var parser=CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).get().parse(new StringReader(csv))) {
            for(var record:parser) {
                long line=record.getRecordNumber()+1;
                if(!record.isMapped("cargo")||!record.isMapped("carrera")||!record.isMapped("usuario")) { errors.add("Use las columnas cargo, carrera y usuario.");break; }
                var position=jdbc.queryForList("SELECT id FROM institutional_position WHERE code=? AND active",UUID.class,record.get("cargo"));
                var career=record.get("carrera").isBlank()?List.<UUID>of():jdbc.queryForList("SELECT id FROM academic_career WHERE code=? AND active",UUID.class,record.get("carrera"));
                var user=jdbc.queryForList("SELECT id FROM app_user WHERE (lower(username)=lower(?) OR lower(email)=lower(?)) AND active",UUID.class,record.get("usuario"),record.get("usuario"));
                if(position.isEmpty()) errors.add("Fila "+line+": cargo no encontrado.");
                if(!record.get("carrera").isBlank()&&career.isEmpty()) errors.add("Fila "+line+": carrera no encontrada.");
                if(user.isEmpty()) errors.add("Fila "+line+": usuario no encontrado o inactivo.");
                if(!position.isEmpty()&&!user.isEmpty()&&(record.get("carrera").isBlank()||!career.isEmpty())) inputs.add(new AssignmentInput(position.getFirst(),career.isEmpty()?null:career.getFirst(),user.getFirst()));
            }
        } catch(IOException|IllegalArgumentException|IllegalStateException error) { errors.add("No se pudo leer el CSV. Verifique el formato y las columnas."); }
        if(inputs.isEmpty()&&errors.isEmpty()) errors.add("El archivo no contiene filas.");
        if(!errors.isEmpty()) return new ImportResult(0,List.copyOf(errors));
        try { for(var input:inputs) assign(period,email,input); }
        catch(ResponseStatusException error) { throw new ResponseStatusException(error.getStatusCode(),"Importación revertida: "+error.getReason()); }
        return new ImportResult(inputs.size(),List.of());
    }
}
