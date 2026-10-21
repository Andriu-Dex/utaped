package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.documents.StoredFiles;
import ec.edu.uta.utaped.institution.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.workflow.*;
import ec.edu.uta.utaped.workflow.WorkflowModels.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class PeriodControlIntegrationTests extends DocumentIntegrationTests {
    @Autowired OrganizationService organization;
    @Autowired WorkflowService workflows;
    @Autowired PeriodWorkflowService assignments;
    @Autowired PeriodArchiveService archives;
    @Autowired PositionService positions;
    @Autowired ec.edu.uta.utaped.documents.DocumentWorkflowSnapshot snapshots;
    UUID scope,reviewer;
    void configuredScope() {
        signingAdmin();reviewer=UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,'period-reviewer@example.invalid','Revisor','unused','USER',false)",reviewer);
        scope=organization.createScope(email,new OrganizationService.ScopeInput(group,period,null));
        organization.member(scope,email,new OrganizationService.MemberInput(user,"MEMBER","Principal",true,0L));
        organization.principal(scope,email,new OrganizationService.PrincipalInput(user,0L));
        jdbc.update("UPDATE work_plan SET scope_id=? WHERE id=?",scope,id);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?)",id,user,user);
        for(String type:List.of("T1","T2")) {
            var definition=new Definition("Flujo de período "+type,List.of(new Stage(UUID.randomUUID(),"Revisión","REVIEW","PERSON",List.of(reviewer),null,"",false)));
            workflows.save(group,type,email,new Save(0L,definition));workflows.configure(group,type,email,new Version(1L));
            assignments.assign(scope,type,email,new PeriodWorkflowService.Assign(workflows.get(group,type,email).current().id(),0L));
        }
    }
    Map<String,byte[]> entries(byte[] bytes) throws IOException {
        var result=new HashMap<String,byte[]>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(bytes))) { ZipEntry entry;while((entry=zip.getNextEntry())!=null) result.put(entry.getName(),zip.readAllBytes()); }
        return result;
    }
    @Test void activationFreezesPeopleAndDefinitionsAcrossFutureTemplateChanges() {
        configuredScope();assignments.activate(period,email,new PeriodWorkflowService.Version(0L));
        var old=snapshots.capture(group,scope,"T1");
        var newDefinition=new Definition("Nuevo semestre",List.of(new Stage(UUID.randomUUID(),"Nueva etapa","REVIEW","PERSON",List.of(reviewer),null,"",true)));
        workflows.save(group,"T1",email,new Save(2L,newDefinition));workflows.configure(group,"T1",email,new Version(3L));
        assertEquals(old,snapshots.capture(group,scope,"T1"));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->assignments.assign(scope,"T1",email,new PeriodWorkflowService.Assign(workflows.get(group,"T1",email).current().id(),2L))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->assignments.activate(period,email,new PeriodWorkflowService.Version(1L))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->organization.createScope(email,new OrganizationService.ScopeInput(group,period,UUID.randomUUID()))).getStatusCode().value());
    }
    @Test void wrongTypeStaleAssignmentAndBatchFailureDoNotChangePublishedAssignments() {
        configuredScope();var original=snapshots.capture(group,scope,"T1");
        assertEquals(400,assertThrows(ResponseStatusException.class,()->assignments.assign(scope,"T1",email,new PeriodWorkflowService.Assign(workflows.get(group,"T2",email).current().id(),1L))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->assignments.assign(scope,"T1",email,new PeriodWorkflowService.Assign(original.revisionId(),0L))).getStatusCode().value());
        var batch=new PeriodWorkflowService.Batch(List.of(new PeriodWorkflowService.AssignmentInput("T1",original.revisionId(),1L),new PeriodWorkflowService.AssignmentInput("T2",UUID.randomUUID(),1L)));
        assertThrows(ResponseStatusException.class,()->assignments.assignBatch(scope,email,batch));
        assertEquals(1L,((Number)assignments.assignments(scope,email).getFirst().get("row_version")).longValue());
        assertEquals(original,snapshots.capture(group,scope,"T1"));
    }
    @Test void missingAndSelfReviewAssignmentsCannotActivate() {
        configuredScope();jdbc.update("DELETE FROM period_workflow_assignment WHERE scope_id=? AND document_type='T2'",scope);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->assignments.activate(period,email,new PeriodWorkflowService.Version(0L))).getStatusCode().value());
        assertNull(jdbc.queryForObject("SELECT workflow_locked_at FROM academic_period WHERE id=?",Object.class,period));
        assertNull(snapshots.capture(group,scope,"T2"));
    }
    @Test void closurePreservesBytesStatesAndHistoryAndBlocksEveryDocumentWrite() throws Exception {
        complete("Archivo histórico",1);signingAdmin();attachments.settings(id,email,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(version(),true,false));var upload=upload("Anexo conservado",UUID.randomUUID(),pdf(1,false,false),version());
        var original=attachments.content(id,upload.items().getFirst().id(),email);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->archives.close(period,email,new PeriodArchiveService.Close(1L,"Cierre solicitado"))).getStatusCode().value());
        archives.close(period,email,new PeriodArchiveService.Close(0L,"Cierre solicitado"));
        var plan=plans.get(id,email);assertEquals("DRAFT",plan.documentState());assertFalse(plan.editable());assertFalse(plan.canManage());assertNotNull(plan.archivedAt());
        assertFalse(plans.options(email).periods().getFirst().editable());
        assertArrayEquals(original,attachments.content(id,upload.items().getFirst().id(),email));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan bloqueado"))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->plans.update(id,email,new WorkPlanModels.Update(plan.rowVersion(),plan.title(),plan.institutionalUnit(),plan.career(),"Cambio","Cambio"))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->attachments.settings(id,email,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(plan.rowVersion(),false,false))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->archives.close(period,email,new PeriodArchiveService.Close(1L,"Otra vez"))).getStatusCode().value());
        var archive=entries(archives.download(period,email));
        String path="documents/"+id+"/files/"+upload.items().getFirst().fileId()+".pdf";
        assertArrayEquals(original,archive.get(path));
        assertTrue(new String(archive.get("documents/"+id+"/manifest.json"),StandardCharsets.UTF_8).contains(StoredFiles.hash(original)));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM period_archive WHERE period_id=?",Integer.class,period));
    }
    @Test void purgeRequiresCustodyAndGraceThenDeletesOnlyThisPeriodKeepingInventory() throws Exception {
        complete("Documentación a eliminar",1);signingAdmin();attachments.settings(id,email,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(version(),true,false));
        UUID file=upload("Anexo",UUID.randomUUID(),pdf(1,false,false),version()).items().getFirst().fileId();
        UUID key=jdbc.queryForObject("SELECT storage_key FROM stored_file WHERE id=?",UUID.class,file);
        UUID otherPeriod=UUID.randomUUID(),otherPlan=UUID.randomUUID();
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Otro período','2027-01-01','2027-06-30','2027-01-01','2027-01-10','2027-01-11','2027-01-20')",otherPeriod);
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,creation_title,preparation_date) VALUES (?,?,?,?,?,?,'Otro','Otro','2027-01-02')",otherPlan,user,user,group,otherPeriod,UUID.randomUUID());
        var purge=new PeriodArchiveService.Purge("Fin de semestre");
        assertEquals(409,assertThrows(ResponseStatusException.class,()->archives.purge(period,email,purge)).getStatusCode().value());
        archives.close(period,email,new PeriodArchiveService.Close(0L,"Cierre de semestre"));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->archives.purge(period,email,purge)).getStatusCode().value());
        byte[] custody=archives.custody(period,email);
        assertEquals(jdbc.queryForObject("SELECT archive_sha256 FROM period_archive WHERE period_id=?",String.class,period),StoredFiles.hash(custody));
        assertTrue(entries(custody).containsKey("documents/"+id+"/files/"+file+".pdf"));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->archives.purge(period,email,purge)).getStatusCode().value());
        jdbc.update("UPDATE academic_period SET closed_at=now()-interval '31 days' WHERE id=?",period);
        archives.purge(period,email,purge);
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE period_id=?",Integer.class,period));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM stored_file",Integer.class));
        assertFalse(Files.exists(Path.of(root).resolve(key.toString())));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE id=?",Integer.class,otherPlan));
        assertTrue(jdbc.queryForObject("SELECT purge_inventory::text FROM period_archive WHERE period_id=?",String.class,period).contains(id.toString()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='PERIOD_DOCUMENTS_PURGED' AND target_id=?",Integer.class,period));
        assertEquals(410,assertThrows(ResponseStatusException.class,()->archives.download(period,email)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->archives.purge(period,email,purge)).getStatusCode().value());
    }
    @Test void positionStagesResolvePerCareerAndBlockActivationWithoutHolder() {
        signingAdmin();jdbc.execute("TRUNCATE institutional_position CASCADE");String code="P"+UUID.randomUUID().toString().substring(0,8).toUpperCase();
        UUID career=organization.createCareer(email,new OrganizationService.CareerInput(code,"Carrera "+code));
        reviewer=UUID.randomUUID();UUID facultyHolder=UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,username,display_name,password_hash,system_role,must_change_password) VALUES (?,'upe@example.invalid','upe.ti','Responsable UPE','unused','USER',false)",reviewer);
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,'faculty-upe@example.invalid','UPE de facultad','unused','USER',false)",facultyHolder);
        scope=organization.createScope(email,new OrganizationService.ScopeInput(group,period,career));
        organization.member(scope,email,new OrganizationService.MemberInput(user,"MEMBER","Principal",true,0L));organization.principal(scope,email,new OrganizationService.PrincipalInput(user,0L));
        UUID upe=positions.create(email,new PositionService.PositionInput("UPE_RESPONSIBLE","Responsable UPE",true));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->positions.assign(period,email,new PositionService.AssignmentInput(upe,null,facultyHolder))).getStatusCode().value());
        for(String type:List.of("T1","T2")) {
            var definition=new Definition("Flujo por cargo "+type,List.of(new Stage(UUID.randomUUID(),"Revisión UPE","REVIEW","POSITION",List.of(),null,"",false,null,upe)));
            workflows.save(group,type,email,new Save(0L,definition));workflows.configure(group,type,email,new Version(1L));
            assignments.assign(scope,type,email,new PeriodWorkflowService.Assign(workflows.get(group,type,email).current().id(),0L));
        }
        assertTrue(snapshots.capture(group,scope,"T1").stages().getFirst().participants().isEmpty());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->assignments.activate(period,email,new PeriodWorkflowService.Version(0L))).getStatusCode().value());
        var imported=positions.importCsv(period,email,"cargo,carrera,usuario\nUPE_RESPONSIBLE,"+code+",upe.ti\n");
        assertEquals(1,imported.assigned());assertTrue(imported.errors().isEmpty());
        assertFalse(positions.importCsv(period,email,"cargo,carrera,usuario\nNO_EXISTE,"+code+",upe.ti\n").errors().isEmpty());
        var participant=snapshots.capture(group,scope,"T1").stages().getFirst().participants().getFirst();
        assertEquals(reviewer,participant.id());assertEquals("Responsable UPE",participant.position());
        assignments.activate(period,email,new PeriodWorkflowService.Version(0L));
        positions.assign(period,email,new PositionService.AssignmentInput(upe,career,facultyHolder));
        assertEquals(reviewer,snapshots.capture(group,scope,"T1").stages().getFirst().participants().getFirst().id());
    }
    @Test void coordinatorAddsOrdinaryMembersOnlyInOwnScope() {
        configuredScope();UUID coordinator=UUID.randomUUID(),candidate=UUID.randomUUID();String coordinatorEmail="scope-coordinator@example.invalid";
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Coordinación','unused','USER',false)",coordinator,coordinatorEmail);
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,'candidate@example.invalid','Docente nuevo','unused','USER',false)",candidate);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->organization.member(scope,coordinatorEmail,new OrganizationService.MemberInput(candidate,"MEMBER","Docente",true,0L))).getStatusCode().value());
        organization.member(scope,email,new OrganizationService.MemberInput(coordinator,"COORDINATOR","Coordinación",true,0L));
        assertTrue(organization.candidates(scope,coordinatorEmail).stream().anyMatch(c->candidate.equals(c.get("id"))));
        organization.member(scope,coordinatorEmail,new OrganizationService.MemberInput(candidate,"MEMBER","Docente",true,0L));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM scope_member WHERE scope_id=? AND user_id=? AND active",Integer.class,scope,candidate));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->organization.member(scope,coordinatorEmail,new OrganizationService.MemberInput(reviewer,"COORDINATOR","Otra coordinación",true,0L))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->organization.member(scope,coordinatorEmail,new OrganizationService.MemberInput(coordinator,"MEMBER","Autocambio",true,1L))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->organization.candidates(scope,"candidate@example.invalid")).getStatusCode().value());
    }
    @Test void historicDownloadNeverAddsAdminOrNewMemberAccess() throws Exception {
        configuredScope();UUID other=UUID.randomUUID(),pureAdmin=UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,'pure-period-admin@example.invalid','Admin','unused','ADMIN',false)",pureAdmin);
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,creation_title,preparation_date) VALUES (?,?,?,?,?,?,?,'Privado',(now() at time zone 'America/Guayaquil')::date)",other,reviewer,reviewer,group,period,UUID.randomUUID(),"Privado");
        archives.close(period,email,new PeriodArchiveService.Close(0L,"Cierre de prueba"));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->archives.download(period,"pure-period-admin@example.invalid")).getStatusCode().value());
        var names=entries(archives.download(period,email)).keySet();assertTrue(names.contains("documents/"+id+"/manifest.json"));assertFalse(names.stream().anyMatch(n->n.contains(other.toString())));
        jdbc.update("DELETE FROM document_access WHERE work_plan_id=? AND user_id=?",id,user);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->archives.download(period,email)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->organization.member(scope,email,new OrganizationService.MemberInput(reviewer,"MEMBER","Nuevo",true,0L))).getStatusCode().value());
    }
    @Test void corruptStoredFileRollsBackClosureAndArchive() throws Exception {
        complete("Archivo que debe ser íntegro",1);signingAdmin();attachments.settings(id,email,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(version(),true,false));var uploaded=upload("PDF",UUID.randomUUID(),pdf(1,false,false),version());
        UUID file=uploaded.items().getFirst().fileId();UUID key=jdbc.queryForObject("SELECT storage_key FROM stored_file WHERE id=?",UUID.class,file);
        Path path=Path.of(root).resolve(key.toString());var original=Files.readAllBytes(path);
        try {
            Files.write(path,new byte[]{1,2,3});assertEquals(409,assertThrows(ResponseStatusException.class,()->archives.close(period,email,new PeriodArchiveService.Close(0L,"Cierre fallido"))).getStatusCode().value());
            assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM academic_period WHERE id=?",String.class,period));assertNull(plans.get(id,email).archivedAt());
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM period_archive WHERE period_id=?",Integer.class,period));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE target_id=? AND action='PERIOD_CLOSED'",Integer.class,period));
        } finally { Files.write(path,original); }
    }
    @Test void closureWaitsForOngoingMutationAndArchivesItsCommittedContent() throws Exception {
        complete("Contenido inicial",1);signingAdmin();
        var held=new java.util.concurrent.CountDownLatch(1);var release=new java.util.concurrent.CountDownLatch(1);
        try(var workers=java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor()) {
            var write=workers.submit(()->new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{
                var plan=plans.lock(id,email);held.countDown();
                try { if(!release.await(10,java.util.concurrent.TimeUnit.SECONDS)) throw new IllegalStateException("Mutation fixture timed out"); }
                catch(InterruptedException interrupted) { Thread.currentThread().interrupt();throw new IllegalStateException(interrupted); }
                return plans.update(id,email,new WorkPlanModels.Update(plan.rowVersion(),plan.title(),plan.institutionalUnit(),plan.career(),"Contenido confirmado antes del cierre",plan.objective()));
            }));
            assertTrue(held.await(10,java.util.concurrent.TimeUnit.SECONDS));
            var close=workers.submit(()->archives.close(period,email,new PeriodArchiveService.Close(0L,"Cierre concurrente")));
            try { assertThrows(java.util.concurrent.TimeoutException.class,()->close.get(250,java.util.concurrent.TimeUnit.MILLISECONDS)); }
            finally { release.countDown(); }
            write.get(10,java.util.concurrent.TimeUnit.SECONDS);close.get(10,java.util.concurrent.TimeUnit.SECONDS);
            var manifest=entries(archives.download(period,email)).get("documents/"+id+"/manifest.json");
            assertTrue(new String(manifest,StandardCharsets.UTF_8).contains("Contenido confirmado antes del cierre"));
            assertFalse(plans.get(id,email).editable());
        } finally { release.countDown(); }
    }
    @Test void archiveDownloadCannotBypassApprovalGateForScopedPreliminaryPdfs() throws Exception {
        complete("Preliminar conservado sin aprobación ficticia",1);configuredScope();assignments.activate(period,email,new PeriodWorkflowService.Version(0L));
        var artifact=artifacts.generate(id,email,new ec.edu.uta.utaped.documents.ArtifactModels.Generate(version()));
        UUID file=jdbc.queryForObject("SELECT file_id FROM document_artifact WHERE id=?",UUID.class,artifact.id());
        var original=files.read(file);archives.close(period,email,new PeriodArchiveService.Close(1L,"Conservar borrador preliminar"));
        var filtered=entries(archives.download(period,email));String name="documents/"+id+"/files/"+file+".pdf";
        assertFalse(filtered.containsKey(name));assertTrue(filtered.containsKey("documents/"+id+"/manifest.json"));
        UUID archive=jdbc.queryForObject("SELECT file_id FROM period_archive WHERE period_id=?",UUID.class,period);
        assertArrayEquals(original,entries(files.read(archive)).get(name));
        assertTrue(new String(filtered.get("period.json"),StandardCharsets.UTF_8).contains("Preliminares"));
    }
}
