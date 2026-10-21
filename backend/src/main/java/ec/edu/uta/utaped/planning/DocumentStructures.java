package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class DocumentStructures {
    public record Section(@NotBlank @Size(max=160) String key,@NotBlank @Size(max=160) String label,boolean required,@NotNull @Size(max=10000) String initialText) {}
    public record Structure(long revision,List<Section> sections,Map<String,String> contents) {}
    public record Save(@NotNull @PositiveOrZero Long revision,@NotNull @Size(max=30) List<@Valid Section> sections) {}
    public record Values(@NotNull @PositiveOrZero Long rowVersion,@NotNull @Size(max=20) Map<String,String> contents) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final AuditService audit;private final JsonMapper json=JsonMapper.builder().build();
    public DocumentStructures(JdbcTemplate jdbc,Accounts accounts,AuditService audit) { this.jdbc=jdbc;this.accounts=accounts;this.audit=audit; }
    public static List<String> nativeSections(String type) { return switch(type) {
        case "T1" -> List.of("JUSTIFICACIÓN","OBJETIVO","MATRIZ DE ACTIVIDADES","ANEXOS","FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS");
        case "T2" -> List.of("ANTECEDENTES","DESARROLLO DE ACTIVIDADES","CONCLUSIONES","OPORTUNIDADES DE MEJORA","REGISTRO DE CONTACTOS Y GESTIONES DE LA DELEGACIÓN","ANEXOS","FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS");
        case "T3" -> List.of("RESULTADOS DEL PLAN DE ACCIÓN","ANÁLISIS DE RESULTADOS","CONCLUSIONES Y/O RECOMENDACIONES","ACCIONES DE MEJORA","ANEXOS","FIRMAS DE RESPONSABILIDAD","CONTROL DE HISTORIAL DE CAMBIOS");
        default -> throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Tipo documental no disponible.");
    }; }
    private void admin(String email) { if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Acceso administrativo requerido."); }
    public Structure template(String type,String email) { admin(email);return configured(type); }
    private Structure configured(String type) {
        var fallback=new Structure(0,nativeSections(type).stream().map(key->new Section(key,key,true,"")).toList(),Map.of());
        return jdbc.query("SELECT * FROM document_structure_template WHERE document_type=?",(rs,n)->new Structure(rs.getLong("revision"),List.of(json.readValue(rs.getString("sections"),Section[].class)),Map.of()),type).stream().findFirst().orElse(fallback);
    }
    @Transactional public Structure save(String type,String email,Save input) {
        admin(email);var nativeKeys=nativeSections(type);var keys=new HashSet<String>();var labels=new HashSet<String>();
        for(var section:input.sections()) {
            if(!keys.add(section.key())||!labels.add(ec.edu.uta.utaped.documents.T1Template.anchor(section.label().strip().toUpperCase(Locale.ROOT)))) throw invalid("No repita identificadores ni títulos de sección.");
            if(nativeKeys.contains(section.key())) { if(!section.label().equals(section.key())||!section.required()||!section.initialText().isEmpty()) throw invalid("Las secciones oficiales conservan su título y obligatoriedad."); }
            else if(!section.key().matches("CUSTOM_[a-f0-9-]{36}")) throw invalid("Identificador de sección adicional no válido.");
        }
        if(input.sections().size()-nativeKeys.size()>20)throw invalid("Máximo 20 secciones adicionales por formato.");
        if(!keys.containsAll(nativeKeys)||input.sections().size()<nativeKeys.size()) throw invalid("Conserve todas las secciones oficiales.");
        var order=input.sections().stream().map(Section::key).toList();
        if(!order.subList(order.size()-2,order.size()).equals(nativeKeys.subList(nativeKeys.size()-2,nativeKeys.size()))) throw invalid("Firmas y control de cambios deben conservarse al final.");
        jdbc.update("INSERT INTO document_structure_template(document_type,sections) VALUES (?,?::jsonb) ON CONFLICT DO NOTHING",type,json.writeValueAsString(configured(type).sections()));
        long current=jdbc.queryForObject("SELECT revision FROM document_structure_template WHERE document_type=? FOR UPDATE",Long.class,type);
        if(current!=input.revision()) throw new ResponseStatusException(HttpStatus.CONFLICT,"La estructura cambió. Recargue antes de guardar.");
        String body=json.writeValueAsString(input.sections());var actor=accounts.current(email).id();
        jdbc.update("UPDATE document_structure_template SET revision=revision+1,sections=?::jsonb,updated_by=?,updated_at=now() WHERE document_type=?",body,actor,type);
        jdbc.update("INSERT INTO document_structure_revision(document_type,revision,sections,changed_by) VALUES (?,?,?::jsonb,?)",type,current+1,body,actor);
        audit.record(actor,"DOCUMENT_STRUCTURE_PUBLISHED",null);return configured(type);
    }
    public void initialize(UUID doc,String type) { var config=configured(type);var values=new LinkedHashMap<String,String>();config.sections().stream().filter(s->s.key().startsWith("CUSTOM_")).forEach(s->values.put(s.key(),s.initialText()));jdbc.update("INSERT INTO document_structure(work_plan_id,revision,sections,contents) VALUES (?,?,?::jsonb,?::jsonb) ON CONFLICT DO NOTHING",doc,config.revision(),json.writeValueAsString(config.sections()),json.writeValueAsString(values)); }
    public Structure snapshot(UUID doc) { return jdbc.query("SELECT * FROM document_structure WHERE work_plan_id=?",(rs,n)->new Structure(rs.getLong("revision"),List.of(json.readValue(rs.getString("sections"),Section[].class)),json.readValue(rs.getString("contents"),new tools.jackson.core.type.TypeReference<Map<String,String>>(){})),doc).stream().findFirst().orElse(null); }
    public List<ec.edu.uta.utaped.documents.ArtifactModels.Blocker> blockers(UUID doc) { var value=snapshot(doc);if(value==null)return List.of();return value.sections().stream().filter(s->s.key().startsWith("CUSTOM_")&&s.required()&&value.contents().getOrDefault(s.key(),"").isBlank()).map(s->new ec.edu.uta.utaped.documents.ArtifactModels.Blocker(s.label(),"Complete la sección adicional obligatoria.")).toList(); }
    public void values(UUID doc,Values input) { var current=snapshot(doc);if(current==null)throw invalid("El documento conserva la estructura histórica.");var allowed=current.sections().stream().filter(s->s.key().startsWith("CUSTOM_")).map(Section::key).toList();if(!allowed.containsAll(input.contents().keySet())||input.contents().values().stream().anyMatch(v->v==null||v.length()>10000))throw invalid("Contenido de sección no válido.");jdbc.update("UPDATE document_structure SET contents=?::jsonb WHERE work_plan_id=?",json.writeValueAsString(input.contents()),doc); }
    private ResponseStatusException invalid(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,text); }
}
