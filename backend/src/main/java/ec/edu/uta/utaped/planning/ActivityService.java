package ec.edu.uta.utaped.planning;

import static ec.edu.uta.utaped.planning.ActivityModels.*;
import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class ActivityService {
    private final JdbcTemplate jdbc;
    private final WorkPlanService plans;
    private final Accounts accounts;
    private final AuditService audit;
    private final JsonMapper json=JsonMapper.builder().build();
    public ActivityService(JdbcTemplate jdbc,WorkPlanService plans,Accounts accounts,AuditService audit) {
        this.jdbc=jdbc;this.plans=plans;this.accounts=accounts;this.audit=audit;
    }
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
    public List<Catalog> catalogs() { return jdbc.query("SELECT * FROM planning_catalog ORDER BY kind,label",(rs,n)->new Catalog(rs.getObject("id",UUID.class),rs.getString("kind"),rs.getString("label"),rs.getBoolean("active"))); }
    public List<Definition> definitions(UUID groupId) {
        return jdbc.query("SELECT * FROM activity_catalog WHERE group_id=? ORDER BY mandatory DESC,title,id",(rs,n)->new Definition(rs.getObject("id",UUID.class),groupId,rs.getString("title"),rs.getString("category"),rs.getBoolean("mandatory"),rs.getBoolean("active")),groupId);
    }
    public List<Holiday> holidays() { return jdbc.query("SELECT * FROM planning_holiday ORDER BY holiday_date",(rs,n)->new Holiday(rs.getObject("holiday_date",LocalDate.class),rs.getString("label"))); }
    public Matrix get(UUID targetDocId,String email) {
        var plan=plans.get(targetDocId,email);
        var stored=jdbc.queryForList("SELECT source,activities::text FROM work_plan_matrix WHERE work_plan_id=?",targetDocId);
        String source=stored.isEmpty()?"":(String)stored.getFirst().get("source");
        List<Activity> rows=new ArrayList<>();
        if(!stored.isEmpty()) rows.addAll(Arrays.asList(json.readValue((String)stored.getFirst().get("activities"),Activity[].class)));
        var definitions=definitions(plan.groupId());
        // Required entries are materialized for editing; saved snapshots are never rebuilt from the catalog.
        for(var d:definitions) if(plan.editable() && d.active() && d.mandatory() && rows.stream().noneMatch(a->d.id().equals(a.catalogId())))
            rows.add(new Activity(d.id(),d.id(),d.title(),d.category(),true,null,null,List.of(),false,List.of(),List.of()));
        var members=jdbc.query("SELECT u.id,u.display_name FROM membership m JOIN app_user u ON u.id=m.user_id WHERE m.group_id=? AND u.active ORDER BY u.display_name,u.id",(rs,n)->new Member(rs.getObject("id",UUID.class),rs.getString("display_name")),plan.groupId());
        var period=jdbc.queryForMap("SELECT starts_on,ends_on,restrict_holiday_endpoints FROM academic_period WHERE id=?",plan.periodId());
        String label=jdbc.queryForObject("SELECT collective_label FROM institutional_group WHERE id=?",String.class,plan.groupId());
        return new Matrix(plan.rowVersion(),source,plan.groupName(),label,plan.editable(),((java.sql.Date)period.get("starts_on")).toLocalDate(),((java.sql.Date)period.get("ends_on")).toLocalDate(),(boolean)period.get("restrict_holiday_endpoints"),rows,definitions,catalogs(),members,holidays());
    }
    private List<Choice> choices(List<Choice> incoming,List<Choice> old,List<Catalog> catalog,String kind) {
        var result=new ArrayList<Choice>();var selected=new HashSet<UUID>();boolean other=false;
        for(var choice:incoming) {
            if(choice.catalogId()==null) {
                if(other || choice.other()==null || choice.other().isBlank()) throw invalid("Otro requiere una descripción no vacía y no puede repetirse.");
                other=true;result.add(new Choice(null,choice.other().trim(),choice.other().trim()));
            } else {
                if(!selected.add(choice.catalogId())) throw invalid("No repita elementos del catálogo.");
                var previous=old.stream().filter(c->choice.catalogId().equals(c.catalogId())).findFirst();
                if(previous.isPresent()) { result.add(previous.get());continue; }
                var entry=catalog.stream().filter(c->c.id().equals(choice.catalogId()) && c.kind().equals(kind) && c.active()).findFirst().orElseThrow(()->invalid("Seleccione un elemento activo del catálogo correspondiente."));
                result.add(new Choice(entry.id(),null,entry.label()));
            }
        }
        return result;
    }
    @Transactional public Matrix save(UUID targetDocId,String email,Save input) {
        // Lock the document aggregate: metadata and matrix share one concurrency token.
        plans.get(targetDocId,email);
        jdbc.queryForList("SELECT id FROM work_plan WHERE id=? FOR UPDATE",targetDocId);
        var current=get(targetDocId,email);
        if(!current.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El documento está en modo de solo lectura.");
        if(current.rowVersion()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió en otra sesión. Recargue antes de guardar la matriz.");
        Set<UUID> memberIds=new HashSet<>();current.members().forEach(m->memberIds.add(m.id()));
        Set<UUID> required=new HashSet<>();current.activities().stream().filter(Activity::mandatory).forEach(a->required.add(a.catalogId()));
        var result=new ArrayList<Activity>();var rowIds=new HashSet<UUID>();var catalogIds=new HashSet<UUID>();
        for(var row:input.activities()) {
            if(!rowIds.add(row.id())) throw invalid("No repita identificadores de actividad.");
            var previous=current.activities().stream().filter(a->a.id().equals(row.id())).findFirst();
            if(previous.isPresent() && !Objects.equals(previous.get().catalogId(),row.catalogId())) throw invalid("No cambie el origen de una actividad guardada.");
            String title=row.title().trim(),category=row.category();boolean mandatory=false;
            if(row.catalogId()!=null) {
                if(!catalogIds.add(row.catalogId())) throw invalid("Una actividad del catálogo solo puede seleccionarse una vez.");
                var prior=current.activities().stream().filter(a->row.catalogId().equals(a.catalogId())).findFirst();
                if(prior.isPresent()) { title=prior.get().title();category=prior.get().category();mandatory=prior.get().mandatory(); }
                else {
                    var d=current.definitions().stream().filter(a->a.id().equals(row.catalogId()) && a.active()).findFirst().orElseThrow(()->invalid("Actividad no disponible para este grupo."));
                    title=d.title();category=d.category();mandatory=d.mandatory();
                }
            } else if(!category.equals("OTHER")) throw invalid("Las actividades libres deben clasificarse como Otra.");
            if(row.startsOn().isAfter(row.endsOn()) || row.startsOn().isBefore(current.periodStartsOn()) || row.endsOn().isAfter(current.periodEndsOn())) throw invalid("Las fechas deben estar ordenadas y dentro del período.");
            if(current.restrictHolidayEndpoints() && current.holidays().stream().anyMatch(h->h.date().equals(row.startsOn()) || h.date().equals(row.endsOn()))) throw invalid("La configuración del período impide iniciar o finalizar en un feriado.");
            var responsible=new HashSet<>(row.responsibleIds());
            if(responsible.size()!=row.responsibleIds().size() || !memberIds.containsAll(responsible)) throw invalid("Seleccione responsables activos del grupo, sin duplicados.");
            if(row.collective() && (current.collectiveLabel().isBlank() || !responsible.equals(memberIds))) throw invalid("La representación colectiva requiere todos los integrantes y una denominación configurada.");
            var prior=previous.orElse(null);
            result.add(new Activity(row.id(),row.catalogId(),title,category,mandatory,row.startsOn(),row.endsOn(),List.copyOf(row.responsibleIds()),row.collective(),
                choices(row.resources(),prior==null?List.of():prior.resources(),current.catalogs(),"RESOURCE"),choices(row.means(),prior==null?List.of():prior.means(),current.catalogs(),"MEANS")));
        }
        if(!catalogIds.containsAll(required)) throw invalid("No puede quitar actividades obligatorias del Plan.");
        jdbc.update("INSERT INTO work_plan_matrix(work_plan_id,source,activities) VALUES (?,?,?::jsonb) ON CONFLICT(work_plan_id) DO UPDATE SET source=excluded.source,activities=excluded.activities",targetDocId,input.source().trim(),json.writeValueAsString(result));
        jdbc.update("UPDATE work_plan SET row_version=row_version+1,updated_at=now() WHERE id=?",targetDocId);
        audit.record(accounts.current(email).id(),"WORK_PLAN_MATRIX_SAVED",targetDocId);
        return get(targetDocId,email);
    }
    @Transactional public Catalog saveCatalog(UUID id,CatalogInput input,String email) {
        if(id==null) { id=UUID.randomUUID();jdbc.update("INSERT INTO planning_catalog(id,kind,label,active) VALUES (?,?,?,?)",id,input.kind(),input.label().trim(),input.active()); }
        else if(jdbc.update("UPDATE planning_catalog SET label=?,active=? WHERE id=? AND kind=?",input.label().trim(),input.active(),id,input.kind())==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Elemento no disponible.");
        audit.record(accounts.current(email).id(),"PLANNING_CATALOG_SAVED",id);
        UUID target=id;return catalogs().stream().filter(c->c.id().equals(target)).findFirst().orElseThrow();
    }
    @Transactional public Definition saveDefinition(UUID id,DefinitionInput input,String email) {
        if(jdbc.queryForObject("SELECT count(*) FROM institutional_group WHERE id=? AND active",Integer.class,input.groupId())==0) throw invalid("Seleccione un grupo activo.");
        if(id==null) { id=UUID.randomUUID();jdbc.update("INSERT INTO activity_catalog(id,group_id,title,category,mandatory,active) VALUES (?,?,?,?,?,?)",id,input.groupId(),input.title().trim(),input.category(),input.mandatory(),input.active()); }
        else if(jdbc.update("UPDATE activity_catalog SET title=?,category=?,mandatory=?,active=? WHERE id=? AND group_id=?",input.title().trim(),input.category(),input.mandatory(),input.active(),id,input.groupId())==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Actividad no disponible.");
        audit.record(accounts.current(email).id(),"ACTIVITY_CATALOG_SAVED",id);
        UUID target=id;return definitions(input.groupId()).stream().filter(d->d.id().equals(target)).findFirst().orElseThrow();
    }
    @Transactional public void label(UUID groupId,Label input,String email) {
        if(jdbc.update("UPDATE institutional_group SET collective_label=?,row_version=row_version+1 WHERE id=?",input.label().trim(),groupId)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Grupo no disponible.");
        audit.record(accounts.current(email).id(),"GROUP_COLLECTIVE_LABEL_SAVED",groupId);
    }
    @Transactional public void holiday(Holiday input,String email) {
        jdbc.update("INSERT INTO planning_holiday(holiday_date,label) VALUES (?,?) ON CONFLICT(holiday_date) DO UPDATE SET label=excluded.label",input.date(),input.label().trim());
        audit.record(accounts.current(email).id(),"HOLIDAY_SAVED",null);
    }
    @Transactional public void deleteHoliday(LocalDate date,String email) {
        jdbc.update("DELETE FROM planning_holiday WHERE holiday_date=?",date);audit.record(accounts.current(email).id(),"HOLIDAY_REMOVED",null);
    }
    @Transactional public void policy(UUID periodId,Policy input,String email) {
        if(jdbc.update("UPDATE academic_period SET restrict_holiday_endpoints=? WHERE id=?",input.enabled(),periodId)==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Período no disponible.");
        audit.record(accounts.current(email).id(),"PERIOD_HOLIDAY_POLICY_SAVED",periodId);
    }
}
