package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.identity.PasswordService;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;
import ec.edu.uta.utaped.workflow.WorkflowModels.*;

/** Explicit development profile. Never runs in the default application profile. */
@Component
@Profile("development-seed")
public class DevelopmentSeed implements ApplicationRunner {
    private final JdbcTemplate jdbc; private final PasswordEncoder encoder; private final String password;
    public DevelopmentSeed(JdbcTemplate jdbc,PasswordEncoder encoder,@Value("${UTAPED_SEED_PASSWORD:}") String password) { this.jdbc=jdbc;this.encoder=encoder;this.password=password; }
    public static UUID id(String key) { return UUID.nameUUIDFromBytes(("utaped-development:"+key).getBytes(StandardCharsets.UTF_8)); }
    @Override @Transactional public void run(ApplicationArguments args) {
        if(!"utaped_dev".equals(jdbc.queryForObject("SELECT current_database()",String.class))) throw new IllegalStateException("Development seeds require a separate database named utaped_dev");
        PasswordService.validate(password);
        for (var person : people()) {
            String role=person.role();
            jdbc.update("""
                INSERT INTO app_user(id,email,username,display_name,first_names,last_names,password_hash,system_role,must_change_password)
                VALUES (?,?,?,?,?,?,?,?,false) ON CONFLICT(id) DO NOTHING
                """,id(role),role+"@utaped.example.invalid","dev."+role,person.displayName(),person.firstNames(),person.lastNames(),encoder.encode(password),role.equals("admin")?"ADMIN":"USER");
            // Upgrade only untouched legacy seed profiles, never administrator-edited names or credentials.
            jdbc.update("""
                UPDATE app_user SET display_name=?,first_names=?,last_names=?,row_version=row_version+1
                WHERE id=? AND username=? AND email=? AND display_name=? AND first_names='Desarrollo' AND last_names=?
                """,person.displayName(),person.firstNames(),person.lastNames(),id(role),"dev."+role,role+"@utaped.example.invalid","Desarrollo "+role,role);
        }
        var reference = DevelopmentReference.load();
        UUID period=id("period");
        upgradeUnusedLegacyReference(reference);
        for (var career : reference.careers()) jdbc.update("INSERT INTO academic_career(id,code,name) VALUES (?,?,?) ON CONFLICT(id) DO NOTHING",id(career.key()),career.code(),career.name());
        jdbc.update("""
            INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on)
            VALUES (?,?,?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING
            """,period,reference.periodName(),reference.startsOn(),reference.endsOn(),reference.startsOn(),reference.endsOn(),reference.startsOn(),reference.endsOn());
        for (var group : reference.groups()) {
            UUID groupId=id(group.key());
            jdbc.update("INSERT INTO institutional_group(id,name,group_type,collective_label) VALUES (?,?,?,?) ON CONFLICT(id) DO NOTHING",groupId,group.name(),group.type(),group.name());
            for (String code : group.careers()) {
                var career=reference.careers().stream().filter(c -> c.code().equals(code)).findFirst().orElseThrow();
                UUID scope=group.key().equals("group") && code.equals("TI") ? id("scope") : id("scope-"+group.key()+"-"+code);
                jdbc.update("INSERT INTO organizational_scope(id,group_id,period_id,career_id,principal_user_id) SELECT ?,?,?,?,? WHERE EXISTS(SELECT 1 FROM academic_period WHERE id=? AND status='ACTIVE' AND workflow_locked_at IS NULL) ON CONFLICT(id) DO NOTHING",scope,groupId,period,id(career.key()),id("principal"),period);
                if (Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM organizational_scope WHERE id=?)",Boolean.class,scope))) seedScope(scope,groupId,period);
            }
        }
        // Example positions until the official table of reviewers/approvers per commission is delivered.
        for(var position:List.of(List.of("UPE_RESPONSIBLE","Responsable de la Unidad de Planificación (UPE)","true"),List.of("CAREER_COORDINATOR","Coordinador/a de carrera","true"),
                List.of("SUBDEAN","Subdecano/a","false"),List.of("DEAN","Decano/a","false")))
            jdbc.update("INSERT INTO institutional_position(id,code,name,career_specific) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",id("position-"+position.get(0)),position.get(0),position.get(1),Boolean.parseBoolean(position.get(2)));
        for(var career:reference.careers())
            jdbc.update("INSERT INTO position_assignment(id,period_id,position_id,career_id,user_id,assigned_by) VALUES (?,?,?,?,?,?) ON CONFLICT DO NOTHING",id("assignment-UPE-"+career.code()),period,id("position-UPE_RESPONSIBLE"),id(career.key()),id("reviewer"),id("admin"));
        jdbc.update("INSERT INTO position_assignment(id,period_id,position_id,career_id,user_id,assigned_by) VALUES (?,?,?,NULL,?,?) ON CONFLICT DO NOTHING",id("assignment-SUBDEAN"),period,id("position-SUBDEAN"),id("approver"),id("admin"));
        for(String kind:List.of("RESOURCE","MEANS")) for (String label : kind.equals("RESOURCE") ? reference.resources() : reference.means())
            jdbc.update("INSERT INTO planning_catalog(id,kind,label,active) VALUES (?,?,?,true) ON CONFLICT DO NOTHING",id("catalog-"+kind+"-"+label),kind,label);
        for (var activity : reference.activities()) jdbc.update("INSERT INTO activity_catalog(id,group_id,title,category,mandatory,active) VALUES (?,?,?,?,false,true) ON CONFLICT(id) DO NOTHING",id(activity.key()),id(activity.groupKey()),activity.title(),activity.category());
        // Optional examples only where no institutional reference or administrator catalog exists.
        for(var group:reference.groups()) if(reference.activities().stream().noneMatch(activity->activity.groupKey().equals(group.key()))
            && jdbc.queryForObject("SELECT count(*) FROM activity_catalog WHERE group_id=? AND active",Integer.class,id(group.key()))==0) {
            int index=0;
            for(String title:List.of("Coordinar la planificación y el seguimiento del trabajo del grupo","Elaborar el informe de resultados del período académico"))
                jdbc.update("INSERT INTO activity_catalog(id,group_id,title,category,mandatory,active) VALUES (?,?,?,'OTHER',false,true) ON CONFLICT(id) DO NOTHING",id("matrix-example-"+group.key()+"-"+index++),id(group.key()),title);
        }
        // Retire only the original generic seed options. Existing matrices preserve their snapshots.
        jdbc.update("UPDATE planning_catalog SET active=false WHERE id IN (?,?) AND label IN ('Equipo de trabajo — Ejemplo','Informe PDF — Ejemplo')",id("RESOURCE"),id("MEANS"));
        jdbc.update("UPDATE activity_catalog SET active=false WHERE id=? AND title='Preparar resultados de ejemplo'",id("activity"));
    }
    // Fictional people for demonstrations; these are not institutional appointments.
    private record Person(String role,String firstNames,String lastNames) {
        String displayName() { return firstNames+" "+lastNames; }
    }
    private static List<Person> people() {
        return List.of(
            new Person("admin","María Fernanda","Salazar Torres"),
            new Person("principal","Andrés Sebastián","Molina Vera"),
            new Person("coordinator","Patricia Elizabeth","Cárdenas Ríos"),
            new Person("reviewer","Carlos Eduardo","Benítez Paredes"),
            new Person("approver","Gabriela Alejandra","Ruiz Medina"),
            new Person("member","Luis Fernando","Acosta León"),
            new Person("outsider","Daniela Isabel","Herrera Viteri")
        );
    }
    private static String position(String role) {
        return switch (role) {
            case "principal" -> "Responsable principal";
            case "coordinator" -> "Coordinador del ámbito";
            default -> "Docente integrante";
        };
    }
    private void upgradeUnusedLegacyReference(DevelopmentReference reference) {
        // Never rewrite a period/group/career already referenced by a document or changed by an administrator.
        jdbc.update("UPDATE academic_career SET code='TI',name='Tecnologías de la Información',row_version=row_version+1 WHERE id=? AND code='DEV-SW' AND name='Carrera de ejemplo — Software' AND NOT EXISTS(SELECT 1 FROM work_plan w JOIN organizational_scope s ON s.id=w.scope_id WHERE s.career_id=?)",id("career"),id("career"));
        jdbc.update("UPDATE institutional_group SET name='Unidad de Titulación',group_type='UNIT',collective_label='Unidad de Titulación' WHERE id=? AND name='Comisión de desarrollo — Ejemplo' AND NOT EXISTS(SELECT 1 FROM work_plan WHERE group_id=?)",id("group"),id("group"));
        jdbc.update("""
            UPDATE academic_period SET name=?,starts_on=?,ends_on=?,preparation_starts_on=?,preparation_ends_on=?,review_starts_on=?,review_ends_on=?,row_version=row_version+1
            WHERE id=? AND name='Período de desarrollo — Ejemplo' AND status='ACTIVE' AND workflow_locked_at IS NULL AND NOT EXISTS(SELECT 1 FROM work_plan WHERE period_id=?)
            """,reference.periodName(),reference.startsOn(),reference.endsOn(),reference.startsOn(),reference.endsOn(),reference.startsOn(),reference.endsOn(),id("period"),id("period"));
    }
    private void seedScope(UUID scope, UUID group, UUID period) {
        for(String role:List.of("principal","coordinator","member")) {
            String membershipRole=role.equals("coordinator")?"COORDINATOR":"MEMBER";
            jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,?) ON CONFLICT DO NOTHING",id(role),group,membershipRole);
            jdbc.update("INSERT INTO scope_member(scope_id,user_id,membership_role,position_label) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",scope,id(role),membershipRole,position(role));
            jdbc.update("UPDATE scope_member SET position_label=?,row_version=row_version+1 WHERE scope_id=? AND user_id=? AND position_label=?",position(role),scope,id(role),"Función de ejemplo: "+role);
        }
        var json=JsonMapper.builder().build();
        for(String type:List.of("T1","T2")) {
            String suffix=group.equals(id("group")) ? type : group+"-"+type;
            UUID configuration=id("configuration-"+suffix),revision=id("revision-"+suffix);
            var definition=new Definition("Revisión y aprobación documental — "+type,List.of(
                new Stage(id("review-stage-"+type),"Revisión documental","REVIEW","PERSON",List.of(id("reviewer")),null,"",false),
                new Stage(id("approval-stage-"+type),"Aprobación del documento","APPROVE","PERSON",List.of(id("approver")),null,"",false)));
            int created=jdbc.update("INSERT INTO workflow_configuration(id,group_id,document_type,draft) VALUES (?,?,?,?::jsonb) ON CONFLICT(id) DO NOTHING",configuration,group,type,json.writeValueAsString(definition));
            if (created==1) {
                jdbc.update("INSERT INTO workflow_revision(id,configuration_id,revision_number,definition,actor_id) VALUES (?,?,1,?::jsonb,?)",revision,configuration,json.writeValueAsString(definition),id("admin"));
                jdbc.update("UPDATE workflow_configuration SET current_revision_id=? WHERE id=?",revision,configuration);
            }
            jdbc.update("INSERT INTO period_workflow_assignment(scope_id,document_type,revision_id,assigned_by) SELECT ?,?,?,? WHERE EXISTS(SELECT 1 FROM academic_period WHERE id=? AND status='ACTIVE' AND workflow_locked_at IS NULL) AND EXISTS(SELECT 1 FROM workflow_configuration WHERE id=? AND current_revision_id=?) ON CONFLICT DO NOTHING",scope,type,revision,id("admin"),period,configuration,revision);
        }
    }
}
