package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.institution.OrganizationService;
import ec.edu.uta.utaped.identity.*;
import ec.edu.uta.utaped.notifications.*;
import ec.edu.uta.utaped.planning.WorkPlanService;
import ec.edu.uta.utaped.planning.WorkPlanModels.Create;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class ContextualAdministrationIntegrationTests extends WorkPlanIntegrationTests {
    @Autowired OrganizationService organization;
    @Autowired PasswordService passwords;
    @Autowired Accounts accounts;
    @Autowired WorkPlanService plans;
    @Autowired DeadlineNoticeService notices;
    @Autowired NotificationService notifications;
    @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
    UUID scope() {
        openPeriod();var id=organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,null));
        organization.member(id,"admin@example.invalid",new OrganizationService.MemberInput(user,"COORDINATOR","Coordinación",true,0L,true));
        organization.member(id,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"MEMBER","Docente",true,0L));
        organization.principal(id,"admin@example.invalid",new OrganizationService.PrincipalInput(user,0L));return id;
    }
    int status(Runnable action) { return assertThrows(ResponseStatusException.class,action::run).getStatusCode().value(); }
    @Test void contextualResetRevokesSessionsAndLinksAndForcesChangeWithPrivateNotice() throws Exception {
        UUID scope=scope();Browser target=new Browser();target.login("other@example.invalid",PASSWORD);
        jdbc.update("INSERT INTO password_reset(token_hash,user_id,expires_at) VALUES ('old-reset',?,now()+interval '10 minutes')",outsider);
        Browser coordinator=new Browser();coordinator.login("user@example.invalid",PASSWORD);
        String next="Temporary-only-password-2026";
        assertEquals(200,coordinator.request("POST","/api/organization/scopes/"+scope+"/accounts/"+outsider+"/temporary-password","{\"rowVersion\":0,\"temporaryPassword\":\""+next+"\"}",true).statusCode());
        assertEquals(401,target.request("GET","/api/auth/me",null,false).statusCode());
        var row=jdbc.queryForMap("SELECT password_hash,must_change_password,row_version FROM app_user WHERE id=?",outsider);
        assertTrue(encoder.matches(next,(String)row.get("password_hash")));assertEquals(true,row.get("must_change_password"));assertEquals(1L,row.get("row_version"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM password_reset WHERE user_id=?",Integer.class,outsider));
        var page=notifications.list("other@example.invalid",null,0,20);assertEquals(1,page.total());assertEquals("ACCOUNT",page.items().getFirst().targetType());assertTrue(page.items().getFirst().targetAvailable());
        assertEquals("ACCOUNT",notifications.open("other@example.invalid",page.items().getFirst().id()).type());
        assertEquals(404,status(()->notifications.open("user@example.invalid",page.items().getFirst().id())));
        assertEquals(409,status(()->passwords.contextualReset("user@example.invalid",scope,outsider,0,next)));
    }
    @Test void coordinatorRecoveryRequiresExplicitAuthorityAndCannotReachOtherScopesOrAdmins() {
        UUID scope=scope();
        assertEquals(400,status(()->passwords.contextualReset("user@example.invalid",scope,user,0,"Temporary-password-2026")));
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(admin,"MEMBER","Administración",true,0L));
        assertEquals(403,status(()->passwords.contextualReset("user@example.invalid",scope,admin,0,"Temporary-password-2026")));
        assertEquals(403,status(()->passwords.contextualReset("other@example.invalid",scope,user,0,"Temporary-password-2026")));
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(user,"COORDINATOR","Coordinación",true,1L,false));
        assertEquals(403,status(()->passwords.contextualReset("user@example.invalid",scope,outsider,0,"Temporary-password-2026")));
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(user,"COORDINATOR","Coordinación",true,2L,true));
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"MEMBER","Docente",false,1L));
        assertEquals(403,status(()->passwords.contextualReset("user@example.invalid",scope,outsider,0,"Temporary-password-2026")));
        jdbc.update("UPDATE academic_period SET status='CLOSED' WHERE id=?",period);
        assertEquals(403,status(()->passwords.contextualReset("user@example.invalid",scope,outsider,0,"Temporary-password-2026")));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='COORDINATOR_PASSWORD_RESET'",Integer.class));
    }
    @Test void roleAndPrincipalHistoryIsAppendOnlyAndClosedScopesAreReadOnly() {
        UUID scope=scope();
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"COORDINATOR","Nueva función",true,1L,true));
        organization.principal(scope,"admin@example.invalid",new OrganizationService.PrincipalInput(outsider,1L));
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(user,"MEMBER","Función anterior",false,1L));
        assertEquals(4,((List<?>)organization.history(scope,"admin@example.invalid").get("memberships")).size());
        assertEquals(2,((List<?>)organization.history(scope,"other@example.invalid").get("principals")).size());
        assertEquals(404,status(()->organization.history(scope,"user@example.invalid")));
        assertEquals(false,jdbc.queryForObject("SELECT password_reset_authorized FROM scope_member WHERE scope_id=? AND user_id=?",Boolean.class,scope,user));
        jdbc.update("UPDATE academic_period SET status='CLOSED' WHERE id=?",period);
        assertEquals(403,status(()->organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"MEMBER","Cambio",true,2L))));
        assertEquals(4,((List<?>)organization.history(scope,"other@example.invalid").get("memberships")).size());
    }
    @Test void careerEditsAreVersionedAndDisablingOnlyPreventsNewScopes() {
        UUID career=organization.createCareer("admin@example.invalid",new OrganizationService.CareerInput("C"+UUID.randomUUID().toString().substring(0,8).toUpperCase(),"Carrera "+UUID.randomUUID()));
        organization.updateCareer(career,"admin@example.invalid",new OrganizationService.CareerUpdate("C"+UUID.randomUUID().toString().substring(0,8).toUpperCase(),"Carrera actualizada "+UUID.randomUUID(),false,0L));
        assertEquals(409,status(()->organization.updateCareer(career,"admin@example.invalid",new OrganizationService.CareerUpdate("CARRERA","Cambio obsoleto",true,0L))));
        assertEquals(403,status(()->organization.updateCareer(career,"user@example.invalid",new OrganizationService.CareerUpdate("CARRERA","Cambio no autorizado",true,1L))));
        openPeriod();assertEquals(400,status(()->organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,career))));
    }
    UUID activity=UUID.randomUUID();
    UUID approved(LocalDate deadline) {
        UUID scope=scope();var doc=plans.create("user@example.invalid",new Create(group,period,UUID.randomUUID(),"Alertas de prueba",scope)).id();
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id) VALUES (?,?) ON CONFLICT DO NOTHING",doc,outsider);
        jdbc.update("UPDATE work_plan SET document_state='APPROVED' WHERE id=?",doc);
        String matrix="[{\"id\":\""+activity+"\",\"title\":\"Actividad\",\"endsOn\":\""+deadline+"\",\"responsibleIds\":[\""+user+"\",\""+outsider+"\"],\"means\":[{\"label\":\"PDF\"}]}]";
        jdbc.update("INSERT INTO work_plan_matrix(work_plan_id,activities) VALUES (?,?::jsonb)",doc,matrix);return doc;
    }
    @Test void noticesUsePersonalDeadlineAndDeduplicateAcrossScansAndDays() {
        LocalDate today=notices.today();UUID doc=approved(today.plusDays(1));
        jdbc.update("INSERT INTO deadline_extension(work_plan_id,activity_id,user_id,ends_on,reason,granted_by) VALUES (?,?,?,?,?,?)",doc,activity,outsider,today.plusDays(10),"Prueba de plazo",user);
        assertEquals(1,notices.generate(today));assertEquals(0,notices.generate(today));assertEquals(0,notices.generate(today.plusDays(1)));
        assertEquals(1,notices.generate(today.plusDays(2)));assertEquals(0,notices.generate(today.plusDays(2)));assertEquals(1,notices.generate(today.plusDays(3)));
        assertEquals(1,notifications.list("user@example.invalid",null,0,20).items().stream().filter(n->n.kind().equals("ACTIVITY_DEADLINE_NEAR")).count());
        assertEquals(0,notifications.list("other@example.invalid",null,0,20).total());
        jdbc.update("UPDATE academic_period SET status='CLOSED' WHERE id=?",period);assertEquals(0,notices.generate(today.plusDays(4)));
    }
    @Test void completeEvidenceAndRemovedAccessSuppressAlerts() {
        LocalDate today=notices.today();UUID doc=approved(today);
        UUID file=UUID.randomUUID();jdbc.update("INSERT INTO stored_file(id,storage_key,original_name,size_bytes,sha256,page_count) VALUES (?,?,'evidence.pdf',1,?,1)",file,UUID.randomUUID(),"a".repeat(64));
        jdbc.update("INSERT INTO execution_evidence(id,work_plan_id,activity_id,medium_index,version_number,file_id,uploaded_by) VALUES (?,?,?,0,1,?,?)",UUID.randomUUID(),doc,activity,file,user);
        assertEquals(0,notices.generate(today));jdbc.update("DELETE FROM execution_evidence WHERE work_plan_id=?",doc);
        jdbc.update("DELETE FROM document_access WHERE work_plan_id=? AND user_id=?",doc,outsider);
        assertEquals(1,notices.generate(today));
        jdbc.update("UPDATE app_user SET active=false WHERE id=?",user);assertEquals(0,notices.generate(today.plusDays(1)));
    }
    @Test void concurrentScansDeliverOneNotificationPerRecipient() throws Exception {
        LocalDate today=notices.today();approved(today);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var a=pool.submit(()->notices.generate(today));var b=pool.submit(()->notices.generate(today));
            assertEquals(2,a.get(20,TimeUnit.SECONDS)+b.get(20,TimeUnit.SECONDS));
        }
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM personal_notification WHERE kind='ACTIVITY_DEADLINE_NEAR'",Integer.class));
    }
    @Test void recoveryDoesNotDeadlockWithDocumentCreationAccountThenScopeLocks() throws Exception {
        UUID scope=scope();var accountLocked=new CountDownLatch(1);var continueCreation=new CountDownLatch(1);
        try(var pool=Executors.newFixedThreadPool(2)) {
            var creation=pool.submit(()->transactions.executeWithoutResult(status->{
                jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",outsider);accountLocked.countDown();
                try { assertTrue(continueCreation.await(10,TimeUnit.SECONDS)); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
                jdbc.queryForList("SELECT id FROM organizational_scope WHERE id=? FOR UPDATE",scope);
            }));
            assertTrue(accountLocked.await(10,TimeUnit.SECONDS));
            var reset=pool.submit(()->passwords.contextualReset("user@example.invalid",scope,outsider,0,"Concurrent-only-password-2026"));
            try {
                org.awaitility.Awaitility.await().atMost(java.time.Duration.ofSeconds(10)).until(()->jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE datname=current_database() AND wait_event_type='Lock' AND query LIKE 'SELECT id FROM app_user WHERE id IN%'",Integer.class)>0);
            } finally { continueCreation.countDown(); }
            creation.get(15,TimeUnit.SECONDS);reset.get(15,TimeUnit.SECONDS);
        }
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='COORDINATOR_PASSWORD_RESET'",Integer.class));
    }
}
