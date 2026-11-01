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
    @Autowired ec.edu.uta.utaped.institution.AdminOverviewService overview;
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
    String overviewAdmin() {
        String admin="supervision@example.invalid";
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Supervisión','unused','ADMIN',false)",UUID.randomUUID(),admin);
        return admin;
    }
    @Test void administrativeOverviewKeepsAggregatePermissionSeparateFromDocumentAccess() {
        assertEquals(403,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->overview.get(email,null,false)).getStatusCode().value());
        String admin=overviewAdmin();report(plan,"T2","DRAFT",1,"[]");report(plan,"T3","DRAFT",1,"[]");
        var summary=overview.get(admin,null,false);
        assertEquals(period,summary.selectedPeriodId());assertEquals(2,summary.users());assertEquals(2,summary.activeUsers());
        assertEquals(2,summary.alerts().pendingEvidence());assertEquals(1,summary.alerts().overdueActivities());
        assertTrue(summary.reporting().upcoming().isEmpty());assertNull(summary.reporting().declaredExecution());
        assertEquals(3,summary.documentTypes().stream().mapToLong(type->type.count()).sum());
        assertEquals(summary.reporting().plans()+summary.reporting().reports(),summary.documentTypes().stream().mapToLong(type->type.count()).sum());
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->plans.get(plan,admin)).getStatusCode().value());
    }
    @Test void overviewUsesIndependentWindowsAndNeverChoosesAnAmbiguousCurrentPeriod() {
        String admin=overviewAdmin();
        jdbc.update("UPDATE academic_period SET preparation_ends_on=?,review_starts_on=? WHERE id=?",today,today.plusDays(1),period);
        var initial=overview.get(admin,null,false);assertEquals("OPEN",initial.periods().getFirst().windows().getFirst().status());
        assertEquals(0L,initial.periods().getFirst().windows().getFirst().remainingDays());assertEquals("UPCOMING",initial.periods().getFirst().windows().get(1).status());
        jdbc.update("UPDATE academic_period SET preparation_ends_on=? WHERE id=?",today.minusDays(1),period);
        assertEquals("CLOSED",overview.get(admin,period,false).periods().getFirst().windows().getFirst().status());
        UUID second=UUID.randomUUID();jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Segundo período simultáneo',?,?,?,?,?,?)",second,today.minusDays(1),today.plusDays(90),today.minusDays(1),today.plusDays(4),today,today.plusDays(90));
        assertNull(overview.get(admin,null,false).selectedPeriodId());
        assertEquals(0,overview.get(admin,second,false).reporting().plans());
        assertNull(overview.get(admin,null,true).selectedPeriodId());
        assertThrows(org.springframework.web.server.ResponseStatusException.class,()->overview.get(admin,second,true));
        jdbc.update("UPDATE academic_period SET status='CLOSED' WHERE id=?",second);
        assertEquals(404,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->overview.get(admin,second,false)).getStatusCode().value());
    }
    @Test void missingCoordinatorRequiresActiveMembershipAndAccountAndRespectsScopeFilters() {
        String admin=overviewAdmin();UUID scope=UUID.randomUUID();
        jdbc.update("INSERT INTO organizational_scope(id,group_id,period_id) VALUES (?,?,?)",scope,group,period);
        assertEquals(1,overview.get(admin,period,false).alerts().scopesWithoutCoordinator());
        jdbc.update("INSERT INTO scope_member(scope_id,user_id,membership_role,active) VALUES (?,?,'COORDINATOR',false)",scope,user);
        assertEquals(1,overview.get(admin,period,false).alerts().scopesWithoutCoordinator());
        jdbc.update("UPDATE scope_member SET active=true WHERE scope_id=?",scope);
        assertEquals(0,overview.get(admin,period,false).alerts().scopesWithoutCoordinator());
        jdbc.update("UPDATE app_user SET active=false WHERE id=?",user);
        assertEquals(1,overview.get(admin,period,false).alerts().scopesWithoutCoordinator());
        jdbc.update("UPDATE organizational_scope SET active=false WHERE id=?",scope);
        assertEquals(0,overview.get(admin,period,false).alerts().scopesWithoutCoordinator());
    }
    @Test void overviewAuditUsesConservedNamesAndFallsBackOnlyForLegacyEvents() {
        String admin=overviewAdmin();UUID historical=UUID.randomUUID(),legacy=UUID.randomUUID();
        jdbc.update("INSERT INTO audit_event(id,actor_id,action,actor_display_name) VALUES (?,?,'USER_CREATED','Nombre conservado')",historical,user);
        jdbc.update("INSERT INTO audit_event(id,actor_id,action) VALUES (?,?,'LOGIN_SUCCEEDED')",legacy,user);
        jdbc.update("UPDATE app_user SET display_name='Nombre actual' WHERE id=?",user);
        var events=overview.get(admin,period,false).events();
        assertEquals("Nombre conservado",events.stream().filter(event->event.id().equals(historical)).findFirst().orElseThrow().actorName());
        assertEquals("Nombre actual",events.stream().filter(event->event.id().equals(legacy)).findFirst().orElseThrow().actorName());
    }
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
    @Test void repeatedReportingReadsRemainStableAndRecordLocalTimings() {
        String originalJit=jdbc.queryForObject("SELECT current_setting('jit')",String.class);
        var samples=new ArrayList<Long>();
        ReportingService.Summary baseline=null;
        for(int n=0;n<4;n++) {
            long started=System.nanoTime();
            var summary=reporting.get(email,null,null,false);
            samples.add((System.nanoTime()-started)/1_000_000);
            if(baseline==null) baseline=summary; else assertEquals(baseline,summary);
        }
        assertEquals(originalJit,jdbc.queryForObject("SELECT current_setting('jit')",String.class),"The pooled connection must retain its original JIT setting after the transaction");
        System.out.println("REPORTING_LOCAL_TIMINGS_MS="+samples);
    }

    @Test void upcomingDetailIsPagedAndNeverGrantsAggregateAdministratorsPrivateAccess() {
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,'admin-detail@example.invalid','Administración','unused','ADMIN',false)",UUID.randomUUID());
        var detail=reporting.upcoming(email,null,null,0,20);
        assertEquals(detail.items().size(),detail.total());
        assertTrue(reporting.upcoming("admin-detail@example.invalid",null,null,0,20).items().isEmpty());
        assertEquals(400,assertThrows(org.springframework.web.server.ResponseStatusException.class,()->reporting.upcoming(email,null,null,-1,20)).getStatusCode().value());
    }
    @Test void activityDetailTraversesBeyondExecutiveLimitAndSeparatesPendingAndOverdue() {
        UUID extra=createPlan();var rows=new ArrayList<String>();for(int i=0;i<8;i++) rows.add(activity(UUID.randomUUID(),today.plusDays(2),1));matrix(extra,"["+String.join(",",rows)+"]");
        assertEquals(6,reporting.get(email,group,period,false).upcoming().size());
        var seen=new HashSet<UUID>();
        for(int page=0;page<3;page++) { var detail=reporting.activityDetail(email,group,period,"UPCOMING",page,3);assertEquals(9,detail.total());assertEquals(3,detail.items().size());for(var item:detail.items()) assertTrue(seen.add(item.activityId())); }
        assertEquals(9,seen.size());assertEquals(1,reporting.activityDetail(email,group,period,"OVERDUE",0,20).total());
        assertEquals(10,reporting.activityDetail(email,group,period,"PENDING_EVIDENCE",0,20).total());
    }
}
