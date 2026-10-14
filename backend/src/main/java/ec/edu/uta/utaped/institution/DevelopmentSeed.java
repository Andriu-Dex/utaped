package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.identity.PasswordService;
import java.nio.charset.StandardCharsets;
import java.time.*;
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
        var roles=List.of("admin","principal","coordinator","reviewer","approver","member","outsider");
        for(String role:roles) jdbc.update("""
            INSERT INTO app_user(id,email,username,display_name,first_names,last_names,password_hash,system_role,must_change_password)
            VALUES (?,?,?,?,?,?,?,?,false) ON CONFLICT(id) DO NOTHING
            """,id(role),role+"@utaped.example.invalid","dev."+role,"Desarrollo "+role,"Desarrollo",role,encoder.encode(password),role.equals("admin")?"ADMIN":"USER");
        UUID group=id("group"),career=id("career"),period=id("period"),scope=id("scope");
        jdbc.update("INSERT INTO academic_career(id,code,name) VALUES (?,'DEV-SW','Carrera de ejemplo — Software') ON CONFLICT(id) DO NOTHING",career);
        jdbc.update("INSERT INTO institutional_group(id,name,group_type,collective_label) VALUES (?,'Comisión de desarrollo — Ejemplo','COMMISSION','Integrantes de la comisión de ejemplo') ON CONFLICT(id) DO NOTHING",group);
        LocalDate today=LocalDate.now(ZoneId.of("America/Guayaquil"));
        jdbc.update("""
            INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on)
            VALUES (?,'Período de desarrollo — Ejemplo',?,?,?,?,?,?) ON CONFLICT(id) DO NOTHING
            """,period,today.minusDays(30),today.plusDays(180),today.minusDays(30),today.plusDays(90),today.minusDays(30),today.plusDays(180));
        jdbc.update("INSERT INTO organizational_scope(id,group_id,period_id,career_id,principal_user_id) VALUES (?,?,?,?,?) ON CONFLICT(id) DO NOTHING",scope,group,period,career,id("principal"));
        for(String role:List.of("principal","coordinator","member")) {
            String membershipRole=role.equals("coordinator")?"COORDINATOR":"MEMBER";
            jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,?) ON CONFLICT DO NOTHING",id(role),group,membershipRole);
            jdbc.update("INSERT INTO scope_member(scope_id,user_id,membership_role,position_label) VALUES (?,?,?,?) ON CONFLICT DO NOTHING",scope,id(role),membershipRole,"Función de ejemplo: "+role);
        }
        for(String kind:List.of("RESOURCE","MEANS")) jdbc.update("INSERT INTO planning_catalog(id,kind,label,active) VALUES (?,?,?,true) ON CONFLICT(id) DO NOTHING",id(kind),kind,kind.equals("RESOURCE")?"Equipo de trabajo — Ejemplo":"Informe PDF — Ejemplo");
        jdbc.update("INSERT INTO activity_catalog(id,group_id,title,category,mandatory,active) VALUES (?,?,'Preparar resultados de ejemplo','OTHER',false,true) ON CONFLICT(id) DO NOTHING",id("activity"),group);
        var json=JsonMapper.builder().build();
        for(String type:List.of("T1","T2")) {
            UUID configuration=id("configuration-"+type),revision=id("revision-"+type);
            var definition=new Definition("Flujo secuencial de desarrollo — "+type,List.of(
                new Stage(id("review-stage-"+type),"Revisión de ejemplo","REVIEW","PERSON",List.of(id("reviewer")),null,"",false),
                new Stage(id("approval-stage-"+type),"Aprobación de ejemplo","APPROVE","PERSON",List.of(id("approver")),null,"",false)));
            jdbc.update("INSERT INTO workflow_configuration(id,group_id,document_type,draft) VALUES (?,?,?,?::jsonb) ON CONFLICT(id) DO NOTHING",configuration,group,type,json.writeValueAsString(definition));
            jdbc.update("INSERT INTO workflow_revision(id,configuration_id,revision_number,definition,actor_id) VALUES (?,?,1,?::jsonb,?) ON CONFLICT(id) DO NOTHING",revision,configuration,json.writeValueAsString(definition),id("admin"));
            jdbc.update("UPDATE workflow_configuration SET current_revision_id=? WHERE id=? AND current_revision_id IS NULL",revision,configuration);
            jdbc.update("INSERT INTO period_workflow_assignment(scope_id,document_type,revision_id,assigned_by) SELECT ?,?,?,? WHERE EXISTS(SELECT 1 FROM academic_period WHERE id=? AND status='ACTIVE' AND workflow_locked_at IS NULL) ON CONFLICT DO NOTHING",scope,type,revision,id("admin"),period);
        }
    }
}
