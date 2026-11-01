package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import tools.jackson.databind.json.JsonMapper;

@Service
public class InstitutionalActivityCatalog {
    public record Source(String file,String sha256) {}
    public record Entry(String key,String title,String category,List<String> suggestedGroups,ActivityModels.Context context,String careerCode) {}
    public record Reference(String revision,List<Source> sources,List<Entry> entries) {}
    public record Selection(@NotBlank String key,@NotNull UUID groupId,UUID periodId,UUID careerId) {}
    public record Import(@NotEmpty @Size(max=2000) List<@Valid Selection> selections) {}
    public record Result(int created,int unchanged) {}
    private final Accounts accounts;private final ActivityService activities;private final JdbcTemplate jdbc;
    private final Reference reference;
    public InstitutionalActivityCatalog(Accounts accounts,ActivityService activities,JdbcTemplate jdbc) {
        this.accounts=accounts;this.activities=activities;this.jdbc=jdbc;
        try(var input=new ClassPathResource("planning/institutional-activities-2026.json").getInputStream()) {
            reference=JsonMapper.builder().build().readValue(input,Reference.class);
        } catch(java.io.IOException e) { throw new IllegalStateException("No se pudo leer el catálogo institucional de referencia.",e); }
    }
    private void admin(String email) { if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo Administración puede importar actividades."); }
    public Reference reference(String email) { admin(email);return reference; }
    @Transactional public Result apply(Import request,String email) {
        admin(email);var selected=new HashSet<String>();
        var inputs=request.selections().stream().map(input->{
            var entry=reference.entries().stream().filter(e->e.key().equals(input.key())).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.BAD_REQUEST,"Referencia no disponible."));
            UUID career=input.careerId();
            if(entry.careerCode()!=null) {
                var careers=jdbc.queryForList("SELECT id FROM academic_career WHERE code=? AND active",UUID.class,entry.careerCode());
                if(careers.isEmpty() || career!=null && !careers.contains(career)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"La referencia requiere una carrera activa con código "+entry.careerCode()+".");
                career=careers.getFirst();
            }
            if(input.periodId()!=null && entry.context().sourceYear()!=null && jdbc.queryForObject("SELECT count(*) FROM academic_period WHERE id=? AND ? BETWEEN extract(year from starts_on) AND extract(year from ends_on)",Integer.class,input.periodId(),entry.context().sourceYear())!=1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El año de la fuente no corresponde al período seleccionado.");
            return new Selection(input.key(),input.groupId(),input.periodId(),career);
        }).toList();
        for(var input:inputs) {
            if(reference.entries().stream().noneMatch(e->e.key().equals(input.key())) || !selected.add(input.groupId()+":"+input.key()+":"+input.periodId()+":"+input.careerId())) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione referencias válidas sin repetir destinos.");
            if(input.periodId()!=null && jdbc.queryForObject("SELECT count(*) FROM academic_period WHERE id=? AND status='ACTIVE'",Integer.class,input.periodId())!=1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione un período activo.");
            if(input.careerId()!=null && jdbc.queryForObject("SELECT count(*) FROM academic_career WHERE id=? AND active",Integer.class,input.careerId())!=1) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione una carrera activa.");
        }
        int created=0,unchanged=0;
        for(var input:inputs) {
            jdbc.queryForList("SELECT id FROM institutional_group WHERE id=? AND active FOR UPDATE",input.groupId());
            if(jdbc.queryForObject("SELECT count(*) FROM activity_catalog WHERE group_id=? AND context->>'sourceKey'=? AND COALESCE(context->>'periodId','')=? AND COALESCE(context->>'careerId','')=?",Integer.class,input.groupId(),input.key(),input.periodId()==null?"":input.periodId().toString(),input.careerId()==null?"":input.careerId().toString())>0) { unchanged++;continue; }
            var entry=reference.entries().stream().filter(e->e.key().equals(input.key())).findFirst().orElseThrow();var c=entry.context();
            var context=new ActivityModels.Context(c.sourceKey(),c.sourceFile(),c.objective(),c.criterion(),c.indicator(),c.strategy(),c.measure(),c.plannedCompliance(),c.sourceYear(),input.periodId(),input.careerId());
            // Reuse an unscoped legacy reference only when its origin is unambiguous.
            var legacy=input.periodId()==null && (input.careerId()==null || entry.careerCode()!=null) && entry.category().equals("IMPROVEMENT_ACTION")
                ?activities.definitions(input.groupId()).stream().filter(d->d.context()==null && d.title().equals(entry.title()) && d.category().equals(entry.category())).toList():List.<ActivityModels.Definition>of();
            if(legacy.size()==1) {
                var existing=legacy.getFirst();activities.saveDefinition(existing.id(),new ActivityModels.DefinitionInput(input.groupId(),existing.title(),existing.category(),existing.mandatory(),existing.active(),context),email);created++;continue;
            }
            activities.saveDefinition(null,new ActivityModels.DefinitionInput(input.groupId(),entry.title(),entry.category(),false,true,context),email);created++;
        }
        return new Result(created,unchanged);
    }
}
