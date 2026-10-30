package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.workflow.ReviewReassignmentService;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class ReviewReassignmentIntegrationTests extends ReviewExecutionIntegrationTests {
    @Autowired ReviewReassignmentService replacements;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    UUID participant(UUID scope,String address,String role) {
        UUID person=UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,'unused','USER',false)",person,address,address);
        organization.member(scope,email,new ec.edu.uta.utaped.institution.OrganizationService.MemberInput(person,role,"Integrante",true,0L));
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?)",id,person,user);return person;
    }
    @Test void replacementSignsOriginalSlotWithOwnIdentityAndPreservesPreviousSignedBytes() throws Exception {
        UUID review=submit(true),scope=plans.get(id,email).scopeId();
        participant(scope,"coordinator-replacement@example.invalid","COORDINATOR");
        UUID next=participant(scope,"replacement@example.invalid","MEMBER");
        jdbc.update("DELETE FROM document_access WHERE work_plan_id=? AND user_id=?",id,next);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->plans.get(id,"replacement@example.invalid")).getStatusCode().value());
        var task=reviews.inbox(reviewerEmail).getFirst();
        assertFalse(replacements.options(task.id(),"coordinator-replacement@example.invalid").canReassign());
        jdbc.update("UPDATE app_user SET active=false WHERE id=?",reviewer);
        UUID file=jdbc.queryForObject("SELECT current_file_id FROM document_review WHERE id=?",UUID.class,review);byte[] original=files.read(file);
        var input=new ReviewReassignmentService.Replace(task.rowVersion(),next,"Titular no disponible",UUID.randomUUID());
        var replaced=replacements.replace(task.id(),"coordinator-replacement@example.invalid",input);
        assertEquals(next,replaced.actorId());assertEquals(1,replaced.history().size());
        assertEquals(1,replacements.replace(task.id(),"coordinator-replacement@example.invalid",input).history().size());
        assertArrayEquals(original,files.read(file));
        var pending=reviews.inbox("replacement@example.invalid").getFirst();assertEquals(task.id(),pending.id());assertEquals(task.rowVersion()+1,pending.rowVersion());
        var certificate=signingFixture();
        reviews.sign(task.id(),"replacement@example.invalid",pending.rowVersion(),reviews.state(review,email).pdfHash(),UUID.randomUUID(),signingBody(certificate,"Test-only-signing-password"),true);
        UUID output=jdbc.queryForObject("SELECT current_file_id FROM document_review WHERE id=?",UUID.class,review);byte[] signed=files.read(output);
        assertArrayEquals(original,Arrays.copyOf(signed,original.length));
        try(var parsed=org.apache.pdfbox.Loader.loadPDF(signed)) {
            assertTrue(parsed.getSignatureDictionaries().getLast().getReason().contains("Sustitución"));
        }
        assertEquals("replacement@example.invalid",jdbc.queryForObject("SELECT signature_actor_name FROM review_task WHERE id=?",String.class,task.id()));
        assertEquals(1,reviews.inbox(approverEmail).size());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->replacements.replace(task.id(),"coordinator-replacement@example.invalid",new ReviewReassignmentService.Replace(pending.rowVersion(),reviewer,"Otra sustitución",UUID.randomUUID()))).getStatusCode().value());
        periodArchives.close(period,email,new ec.edu.uta.utaped.institution.PeriodArchiveService.Close(1L,"Custodia de sustitución"));
        boolean conserved=false;
        try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(periodArchives.custody(period,email)))) {
            java.util.zip.ZipEntry entry;
            while((entry=zip.getNextEntry())!=null) if(entry.getName().equals("documents/"+id+"/manifest.json")) {
                String manifest=new String(zip.readAllBytes(),java.nio.charset.StandardCharsets.UTF_8);
                assertTrue(manifest.contains("reviewReassignments"));assertTrue(manifest.contains("Titular no disponible"));conserved=true;
            }
        }
        assertTrue(conserved);

    }
    @Test void replacementRejectsUnauthorizedDuplicateAndStaleRequestsWithoutChangingTask() throws Exception {
        submit(false);UUID scope=plans.get(id,email).scopeId();
        participant(scope,"coordinator-replacement@example.invalid","COORDINATOR");
        UUID next=participant(scope,"replacement@example.invalid","MEMBER");var task=reviews.inbox(reviewerEmail).getFirst();
        var input=new ReviewReassignmentService.Replace(task.rowVersion(),next,"Sustitución",UUID.randomUUID());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->replacements.replace(task.id(),email,input)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->replacements.replace(task.id(),"coordinator-replacement@example.invalid",input)).getStatusCode().value());
        jdbc.update("UPDATE app_user SET active=false WHERE id=?",reviewer);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->replacements.replace(task.id(),"coordinator-replacement@example.invalid",new ReviewReassignmentService.Replace(task.rowVersion(),approver,"No duplicar etapas",UUID.randomUUID()))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->replacements.replace(task.id(),"coordinator-replacement@example.invalid",new ReviewReassignmentService.Replace(task.rowVersion()+1,next,"Solicitud vieja",UUID.randomUUID()))).getStatusCode().value());
        assertEquals(reviewer,jdbc.queryForObject("SELECT actor_id FROM review_task WHERE id=?",UUID.class,task.id()));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM review_task_reassignment",Integer.class));
    }
    @Test void waitingFormerActorCannotActAfterReassignmentEvenWithNextVersionAndHistoricalAccess() throws Exception {
        UUID review=submit(false),scope=plans.get(id,email).scopeId();
        participant(scope,"coordinator-replacement@example.invalid","COORDINATOR");
        UUID next=participant(scope,"replacement@example.invalid","MEMBER");
        var task=reviews.inbox(reviewerEmail).getFirst();
        organization.member(scope,email,new ec.edu.uta.utaped.institution.OrganizationService.MemberInput(reviewer,"MEMBER","Participante anterior",false,0L));
        UUID previous=UUID.randomUUID();
        jdbc.update("UPDATE document_review SET round_number=2 WHERE id=?",review);
        jdbc.update("UPDATE work_plan SET review_round=2 WHERE id=?",id);
        jdbc.update("INSERT INTO document_review(id,work_plan_id,artifact_id,author_signature_id,current_file_id,round_number,status,closed_at) SELECT ?,work_plan_id,artifact_id,author_signature_id,current_file_id,1,'RETURNED',now() FROM document_review WHERE id=?",previous,review);
        jdbc.update("INSERT INTO review_task(id,review_id,actor_id,actor_name,stage_label,stage_action,position,requires_signature,signature_action,status,acted_at) SELECT ?,?,actor_id,actor_name,stage_label,stage_action,0,requires_signature,signature_action,'RETURNED',now() FROM review_task WHERE id=?",UUID.randomUUID(),previous,task.id());
        var executor=java.util.concurrent.Executors.newSingleThreadExecutor();
        try {
            var pending=new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{
                jdbc.queryForList("SELECT id FROM document_review WHERE id=? FOR UPDATE",review);
                var request=executor.submit(()->reviews.observe(task.id(),reviewerEmail,new ec.edu.uta.utaped.workflow.ReviewService.Observation(task.rowVersion()+1,"Petición del actor anterior",null,null,null,null,null)));
                long limit=System.nanoTime()+java.util.concurrent.TimeUnit.SECONDS.toNanos(10);boolean waiting=false;
                while(System.nanoTime()<limit) {
                    jdbc.execute("SELECT pg_stat_clear_snapshot()");
                    waiting=jdbc.queryForObject("SELECT count(*) FROM pg_stat_activity WHERE pid<>pg_backend_pid() AND cardinality(pg_blocking_pids(pid))>0 AND query ILIKE '%document_review%'",Integer.class)>0;
                    if(waiting) break;
                    try { Thread.sleep(10); } catch(InterruptedException e) { Thread.currentThread().interrupt();throw new IllegalStateException(e); }
                }
                assertTrue(waiting,"The former actor must be waiting for the review lock");
                replacements.replace(task.id(),"coordinator-replacement@example.invalid",new ReviewReassignmentService.Replace(task.rowVersion(),next,"Titular retirado",UUID.randomUUID()));
                return request;
            });
            var failure=assertThrows(java.util.concurrent.ExecutionException.class,()->pending.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(404,((ResponseStatusException)failure.getCause()).getStatusCode().value());
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM review_observation WHERE task_id=?",Integer.class,task.id()));
            assertNotNull(plans.get(id,reviewerEmail)); // Read permission from a previous round is not action permission.
        } finally { executor.shutdownNow(); }
    }

}
