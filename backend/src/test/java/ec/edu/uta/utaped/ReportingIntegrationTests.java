package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.planning.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest
class ReportingIntegrationTests {
    @Autowired JdbcTemplate jdbc;@Autowired WorkPlanService plans;@Autowired ReportingService reporting;
    UUID user,group,period,plan,a,b,c;LocalDate today;String email="metrics@example.invalid";
    @BeforeEach void setup() {
        assertEquals("utaped_test",jdbc.queryForObject("SELECT current_database()",String.class));
        jdbc.execute("TRUNCATE app_user,institutional_group,academic_period,auth_throttle,SPRING_SESSION CASCADE");
        user=UUID.randomUUID();group=UUID.randomUUID();period=UUID.randomUUID();a=UUID.randomUUID();b=UUID.randomUUID();c=UUID.randomUUID();today=LocalDate.now(ZoneId.of("America/Guayaquil"));
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Analista','unused','USER',false)",user,email);
        jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,'Grupo de indicadores','COMMISSION')",group);
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')",user,group);
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Período de indicadores',?,?,?,?,?,?)",period,today.minusDays(30),today.plusDays(60),today.minusDays(30),today.plusDays(30),today,today.plusDays(60));
        plan=createPlan();
        matrix(plan,"["+activity(a,today.plusDays(2),2)+","+activity(b,today.minusDays(1),1)+","+activity(c,today.plusDays(20),1)+"]");
        evidence(a,0,1);evidence(a,0,2);evidence(c,0,1);
    }
    UUID createPlan() {
        UUID id=plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan de indicadores")).id();
        jdbc.update("UPDATE work_plan SET document_state='APPROVED' WHERE id=?",id);return id;
    }
    String activity(UUID id,LocalDate end,int media) {
        return "{\"id\":\""+id+"\",\"title\":\"Actividad de análisis\",\"endsOn\":\""+end+"\",\"responsibleIds\":[\""+user+"\"],\"means\":"+(media==2?"[{},{}]":"[{}]")+"}";
    }
    void matrix(UUID id,String json) { jdbc.update("INSERT INTO work_plan_matrix(work_plan_id,activities) VALUES (?,?::jsonb)",id,json); }
    void evidence(UUID activity,int medium,int version) { jdbc.update("INSERT INTO execution_evidence(id,work_plan_id,activity_id,medium_index,version_number,uploaded_by,evidence_kind,external_url,reference_title) VALUES (?,?,?,?,?,?,'URL','https://example.invalid/evidence','Referencia')",UUID.randomUUID(),plan,activity,medium,version,user); }
    UUID report(UUID base,String type,String state,int sequence,String results) {
        UUID id=UUID.randomUUID(),file=UUID.randomUUID(),artifact=UUID.randomUUID();
        jdbc.update("INSERT INTO stored_file(id,storage_key,sha256,size_bytes,original_name,page_count) VALUES (?,?,?,1,'fixture.pdf',1)",file,UUID.randomUUID(),"a".repeat(64));
        jdbc.update("INSERT INTO document_artifact(id,work_plan_id,actor_id,source_row_version,source_hash,template_version,file_id,snapshot,pages,signature_slots) VALUES (?,?,?,0,?,'metric-fixture',?,'{}','[]','[]')",artifact,base,user,UUID.randomUUID().toString().replace("-","").repeat(2),file);
        jdbc.update("INSERT INTO work_plan(id,teacher_id,group_id,period_id,request_key,creation_title,title,preparation_date,document_type,base_plan_id,document_state) VALUES (?,?,?,?,?,'Informe','Informe',?,?,?,?)",id,user,group,period,UUID.randomUUID(),today,type,base,state);
        if(type.equals("T2")) jdbc.update("INSERT INTO work_report(id,base_plan_id,base_artifact_id,base_formal_version,results) VALUES (?,?,?,'1.0',?::jsonb)",id,base,artifact,results);
        else jdbc.update("INSERT INTO execution_report(id,base_plan_id,base_artifact_id,base_formal_version,sequence_number,results) VALUES (?,?,?,'1.0',?,?::jsonb)",id,base,artifact,sequence,results);
        return id;
    }
    String result(UUID activity,int percentage) { return "{\"activityId\":\""+activity+"\",\"percentage\":"+percentage+"}"; }
    void percent(String expected,BigDecimal actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual)); }
    @Test void pendingCountsRespectVersionsExtensionsFiltersAndPrivateAccess() {
        var summary=reporting.get(email,group,period,false);
        assertEquals(3,summary.activities());assertEquals(1,summary.completedActivities());assertEquals(2,summary.pendingEvidence());assertEquals(1,summary.dueSoonActivities());assertEquals(1,summary.overdueActivities());
        assertEquals(1,summary.upcoming().size());assertEquals(a,summary.upcoming().getFirst().activityId());assertNull(summary.declaredExecution());percent("0",summary.declarationCoverage());
        jdbc.update("INSERT INTO deadline_extension(work_plan_id,activity_id,user_id,ends_on,reason,granted_by) VALUES (?,?,?,?,'Prórroga de prueba',?)",plan,a,user,today.plusDays(12),user);
        assertEquals(0,reporting.get(email,group,period,false).dueSoonActivities());
        jdbc.update("INSERT INTO deadline_extension(work_plan_id,activity_id,user_id,ends_on,reason,granted_by) VALUES (?,?,?,?,'Prórroga de prueba',?)",plan,b,user,today.plusDays(3),user);
        var extended=reporting.get(email,group,period,false);assertEquals(0,extended.overdueActivities());assertEquals(1,extended.dueSoonActivities());assertEquals(b,extended.upcoming().getFirst().activityId());
        assertTrue(reporting.get(email,UUID.randomUUID(),period,false).rows().isEmpty());
        String admin="global@example.invalid",outsider="outsider@example.invalid";
        for(String role:List.of("ADMIN","USER")) jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Sin ámbito','unused',?,false)",UUID.randomUUID(),role.equals("ADMIN")?admin:outsider,role);
        assertTrue(reporting.get(outsider,null,null,false).rows().isEmpty());
        var global=reporting.get(admin,null,null,false);assertEquals(2,global.pendingEvidence());assertTrue(global.upcoming().isEmpty());
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->plans.get(plan,admin)).getStatusCode().value());
        jdbc.update("UPDATE academic_period SET status='CLOSED' WHERE id=?",period);
        assertEquals(0,reporting.get(admin,null,null,false).pendingEvidence());assertEquals(0,reporting.get(admin,null,null,false).dueSoonActivities());
    }
    @Test void declaredProgressUsesApprovedResultsFinalPrecedenceAndActivityWeightedMean() {
        report(plan,"T3","APPROVED",1,"["+result(a,0)+","+result(b,100)+"]");
        var initial=reporting.get(email,group,period,false);percent("50",initial.declaredExecution());assertEquals(2,initial.declaredActivities());percent("66.67",initial.declarationCoverage());
        report(plan,"T3","DRAFT",2,"["+result(a,100)+"]");percent("50",reporting.get(email,group,period,false).declaredExecution());
        report(plan,"T2","APPROVED",1,"["+result(a,80)+"]");percent("90",reporting.get(email,group,period,false).declaredExecution());
        UUID second=createPlan(),activity=UUID.randomUUID();matrix(second,"["+activity(activity,today.plusDays(5),1)+"]");report(second,"T2","APPROVED",1,"["+result(activity,20)+"]");
        var weighted=reporting.get(email,group,period,false);percent("66.67",weighted.declaredExecution());assertEquals(3,weighted.declaredActivities());assertEquals(4,weighted.activities());percent("75",weighted.declarationCoverage());
        assertEquals(0,reporting.get(email,UUID.randomUUID(),period,false).declaredActivities());
    }
    @Test void archivedDocumentsAreHistoricalAndCsvIncludesCoverageAndEscapesFormulas() {
        jdbc.update("UPDATE institutional_group SET name='=SUM(A1)' WHERE id=?",group);
        String csv=new String(reporting.csv(email,group,period,false),java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(csv.contains("\"'=SUM(A1)\""));assertTrue(csv.contains("Cobertura de resultados (%)"));assertTrue(csv.contains("Medios sin evidencia"));
        jdbc.update("UPDATE work_plan SET archived_at=now() WHERE id=?",plan);
        assertEquals(0,reporting.get(email,group,period,false).plans());
        var history=reporting.get(email,group,period,true);assertEquals(1,history.plans());assertEquals(3,history.activities());assertEquals(0,history.pendingEvidence());assertTrue(history.upcoming().isEmpty());
    }
}
