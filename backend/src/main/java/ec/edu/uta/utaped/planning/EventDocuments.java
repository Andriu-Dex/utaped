package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.documents.ArtifactModels.Blocker;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

/** Optional event variant; ordinary T1/T2 documents keep their original structure. */
@Service
public class EventDocuments {
    public record Action(@NotNull UUID id,@NotNull @Size(max=2000) String title,LocalDate date,LocalTime startsAt,LocalTime endsAt,
        @NotNull @Size(max=50) List<@NotNull UUID> responsibleIds,@Size(max=10000) String responsibleDisplay,
        @NotNull @Size(max=4000) String resources,@DecimalMin("0") @DecimalMax("100") BigDecimal percentage,
        @NotNull @Size(max=4000) String observations) {}
    public record Data(@NotNull @Size(max=500) String topic,@NotNull @Pattern(regexp="|ACADEMIC|CULTURAL|SOCIAL|SPORTS|RESEARCH|OTHER") String eventType,
        @NotNull @Size(max=200) String otherType,LocalDate startsOn,LocalDate endsOn,LocalTime startsAt,LocalTime endsAt,
        @NotNull @Pattern(regexp="|PRESENTIAL|VIRTUAL|OTHER") String modality,@NotNull @Size(max=200) String otherModality,
        @NotNull @Size(max=2000) String location,@PositiveOrZero Integer capacity,@PositiveOrZero Integer authorities,
        @PositiveOrZero Integer teachers,@PositiveOrZero Integer students,@PositiveOrZero Integer otherAttendees,
        @NotNull @Size(max=4000) String logistics,@NotNull @Size(max=4000) String certificates,@NotNull @Size(max=4000) String diffusion,
        @NotNull @Size(max=10000) String generalObjective,@NotNull @Size(max=10000) String specificObjectives,
        @NotNull @Size(max=10000) String conclusions,@NotNull @Size(max=2000) String source,
        boolean privacyNoticeEnabled,@NotNull @Size(max=4000) String privacyNotice,
        @NotNull @Size(max=100) List<@NotNull @Valid Action> actions) {}
    public record Save(@NotNull @PositiveOrZero Long rowVersion,boolean enabled,@NotNull @Valid Data data,@Size(max=50000) String justification) { public Save(Long v,boolean e,Data d){this(v,e,d,null);} }
    public record State(long rowVersion,boolean editable,boolean enabled,Data data,List<ActivityModels.Member> members,WorkPlanModels.Plan document) {}
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final ActivityService activities;
    private final Accounts accounts;private final AuditService audit;private final JsonMapper json=JsonMapper.builder().build();
    public EventDocuments(JdbcTemplate jdbc,WorkPlanService plans,ActivityService activities,Accounts accounts,AuditService audit) {
        this.jdbc=jdbc;this.plans=plans;this.activities=activities;this.accounts=accounts;this.audit=audit;
    }
    public static Data empty() { return new Data("","","",null,null,null,null,"","","",null,null,null,null,null,"","","","","","","",false,"",List.of()); }
    public Data snapshot(UUID doc) {
        return jdbc.query("SELECT data::text FROM event_document WHERE work_plan_id=?",(rs,n)->json.readValue(rs.getString(1),Data.class),doc).stream().findFirst().orElse(null);
    }
    public State get(UUID doc,String email) {
        var plan=plans.get(doc,email);if(!Set.of("T1","T2").contains(plan.documentType())) throw invalid("Este formato no corresponde a un documento de evento.");
        var data=snapshot(doc);return new State(plan.rowVersion(),plan.editable(),data!=null,data,data==null?List.of():activities.get(doc,email).members(),plan);
    }
    @Transactional public State save(UUID doc,String email,Save input) {
        var plan=plans.lock(doc,email);if(!plan.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo el responsable principal puede editar un borrador sin firmas.");
        if(plan.rowVersion()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió. Recargue antes de guardar.");
        if(!Set.of("T1","T2").contains(plan.documentType())) throw invalid("Formato de evento no disponible.");
        if(plan.documentType().equals("T2") && (plan.basePlanId()==null || snapshot(plan.basePlanId())==null || !input.enabled())) throw invalid("El informe de evento conserva la variante de su Plan base.");
        if(!input.enabled() || !"EVENT".equals(jdbc.queryForObject("SELECT document_variant FROM work_plan WHERE id=?",String.class,doc))) throw invalid("El formato elegido al crear el documento se conserva durante todo su ciclo.");
        if(input.enabled()) {
            var data=input.data();validateDates(data);
            var members=activities.get(doc,email).members();var names=new HashMap<UUID,String>();members.forEach(m->names.put(m.id(),m.name()));
            var ids=new HashSet<UUID>();var canonical=new ArrayList<Action>();
            for(var a:data.actions()) {
                if(!ids.add(a.id()) || new HashSet<>(a.responsibleIds()).size()!=a.responsibleIds().size() || !names.keySet().containsAll(a.responsibleIds())) throw invalid("Seleccione responsables del grupo y actividades sin duplicados.");
                if(a.startsAt()!=null && a.endsAt()!=null && a.endsAt().isBefore(a.startsAt())) throw invalid("La hora final de una actividad no puede ser anterior a su inicio.");
                if(a.date()!=null && (data.startsOn()!=null && a.date().isBefore(data.startsOn()) || data.endsOn()!=null && a.date().isAfter(data.endsOn()))) throw invalid("Las actividades deben estar dentro de las fechas del evento.");
                canonical.add(new Action(a.id(),a.title(),a.date(),a.startsAt(),a.endsAt(),a.responsibleIds(),String.join("\n",a.responsibleIds().stream().map(names::get).toList()),a.resources(),plan.documentType().equals("T1")?null:a.percentage(),a.observations()));
            }
            var clean=new Data(data.topic(),data.eventType(),data.otherType(),data.startsOn(),data.endsOn(),data.startsAt(),data.endsAt(),data.modality(),data.otherModality(),data.location(),data.capacity(),data.authorities(),data.teachers(),data.students(),data.otherAttendees(),data.logistics(),data.certificates(),data.diffusion(),data.generalObjective(),data.specificObjectives(),data.conclusions(),data.source(),data.privacyNoticeEnabled(),data.privacyNotice(),List.copyOf(canonical));
            jdbc.update("INSERT INTO event_document(work_plan_id,data) VALUES (?,?::jsonb) ON CONFLICT(work_plan_id) DO UPDATE SET data=excluded.data",doc,json.writeValueAsString(clean));
        } else jdbc.update("DELETE FROM event_document WHERE work_plan_id=?",doc);
        String title=input.data().topic().strip();if(!title.isBlank()){title=(plan.documentType().equals("T1")?"Plan de evento: ":"Informe de evento: ")+title;if(title.length()>200)title=title.substring(0,199)+"…";}
        jdbc.update("UPDATE work_plan SET title=CASE WHEN ?='' THEN title ELSE ? END,justification=COALESCE(?,justification),row_version=row_version+1,updated_at=now() WHERE id=?",title,title,input.justification(),doc);
        audit.record(accounts.current(email).id(),"EVENT_DOCUMENT_SAVED",doc);return get(doc,email);
    }
    private void validateDates(Data d) {
        if(d.startsOn()!=null&&d.endsOn()!=null&&d.endsOn().isBefore(d.startsOn())) throw invalid("La fecha final no puede ser anterior a la inicial.");
        if(d.startsAt()!=null&&d.endsAt()!=null&&Objects.equals(d.startsOn(),d.endsOn())&&d.endsAt().isBefore(d.startsAt())) throw invalid("Revise las horas del evento.");
        if(d.eventType().equals("OTHER")&&d.otherType().isBlank() || d.modality().equals("OTHER")&&d.otherModality().isBlank()) throw invalid("Especifique la opción Otro.");
    }
    public List<Blocker> blockers(UUID doc,String type) {
        var d=snapshot(doc);if(d==null)return List.of();var errors=new ArrayList<Blocker>();
        if(d.topic().isBlank()||d.eventType().isBlank()||d.startsOn()==null||d.endsOn()==null||d.startsAt()==null||d.endsAt()==null||d.modality().isBlank()||d.location().isBlank()) errors.add(new Blocker("Evento","Complete tema, tipo, fechas, horas, modalidad y lugar."));
        if(type.equals("T1")&&(d.generalObjective().isBlank()||d.specificObjectives().isBlank())) errors.add(new Blocker("Evento","Complete objetivo general y objetivos específicos."));
        if(type.equals("T2")&&(d.authorities()==null||d.teachers()==null||d.students()==null||d.otherAttendees()==null))errors.add(new Blocker("Evento","Complete las cifras reales de asistencia; use cero cuando corresponda."));
        if(type.equals("T2")&&d.conclusions().isBlank())errors.add(new Blocker("Evento","Complete las conclusiones o recomendaciones."));
        if(d.actions().isEmpty()||d.actions().stream().anyMatch(a->a.title().isBlank()||a.date()==null||a.startsAt()==null||a.endsAt()==null||a.responsibleIds().isEmpty()||a.resources().isBlank()||type.equals("T2")&&a.percentage()==null)) errors.add(new Blocker("Evento","Complete las actividades, horarios, responsables, recursos y resultados que correspondan."));
        if(d.privacyNoticeEnabled()&&d.privacyNotice().isBlank())errors.add(new Blocker("Evento","Complete la nota de protección de datos seleccionada."));
        return List.copyOf(errors);
    }
    public void inherit(UUID target,UUID base) {
        var d=snapshot(base);if(d==null)return;
        var rows=d.actions().stream().map(a->new Action(a.id(),a.title(),a.date(),null,null,a.responsibleIds(),a.responsibleDisplay(),a.resources(),null,"")).toList();
        var report=new Data(d.topic(),d.eventType(),d.otherType(),d.startsOn(),d.endsOn(),null,null,d.modality(),d.otherModality(),d.location(),null,null,null,null,null,"","","",d.generalObjective(),d.specificObjectives(),"",d.source(),d.privacyNoticeEnabled(),d.privacyNotice(),rows);
        jdbc.update("UPDATE work_plan SET document_variant='EVENT' WHERE id=?",target);
        jdbc.update("INSERT INTO event_document(work_plan_id,data) VALUES (?,?::jsonb) ON CONFLICT DO NOTHING",target,json.writeValueAsString(report));
    }
    private ResponseStatusException invalid(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,message); }
}
