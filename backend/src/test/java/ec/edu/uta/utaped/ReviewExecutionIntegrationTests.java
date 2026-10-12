package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.institution.OrganizationService;
import ec.edu.uta.utaped.workflow.*;
import ec.edu.uta.utaped.workflow.WorkflowModels.*;
import ec.edu.uta.utaped.signing.CertificateBindings;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class ReviewExecutionIntegrationTests extends DocumentIntegrationTests {
    @Autowired OrganizationService organization;
    @Autowired ReviewService reviews;
    @Autowired WorkflowService workflows;
    @Autowired PeriodWorkflowService periodWorkflows;
    @Autowired ec.edu.uta.utaped.institution.PeriodArchiveService periodArchives;
    @Autowired ec.edu.uta.utaped.planning.EvidenceService evidence;
    @Autowired ec.edu.uta.utaped.planning.ReportService reports;
    @Autowired ec.edu.uta.utaped.planning.DocumentLifecycleService lifecycle;
    @Autowired ec.edu.uta.utaped.planning.ReportingService reporting;
    SigningFixture authorFixture;
    UUID reviewer,approver; String reviewerEmail="reviewer@example.invalid",approverEmail="approver@example.invalid";
    UUID submit(boolean signatureRequired) throws Exception {
        complete("Justificación de prueba para revisión",1);signingAdmin();
        reviewer=UUID.randomUUID();approver=UUID.randomUUID();
        for(var entry:Map.of(reviewer,reviewerEmail,approver,approverEmail).entrySet()) jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,'unused','USER',false)",entry.getKey(),entry.getValue(),entry.getValue());
        UUID scope=organization.createScope(email,new OrganizationService.ScopeInput(group,period,null));
        organization.member(scope,email,new OrganizationService.MemberInput(user,"MEMBER","Elaborador",true,0L));organization.principal(scope,email,new OrganizationService.PrincipalInput(user,0L));
        jdbc.update("UPDATE work_plan SET scope_id=? WHERE id=?",scope,id);jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?)",id,user,user);
        var definition=new Definition("Circuito de prueba",List.of(new Stage(UUID.randomUUID(),"Revisión","REVIEW","PERSON",List.of(reviewer),null,"",signatureRequired),new Stage(UUID.randomUUID(),"Aprobación","APPROVE","PERSON",List.of(approver),null,"",false)));
        workflows.save(group,"T1",email,new Save(0L,definition));workflows.configure(group,"T1",email,new Version(1L));
        workflows.save(group,"T2",email,new Save(0L,definition));workflows.configure(group,"T2",email,new Version(1L));
        for(String type:List.of("T1","T2")) periodWorkflows.assign(scope,type,email,new PeriodWorkflowService.Assign(workflows.get(group,type,email).current().id(),0L));
        periodWorkflows.activate(period,email,new PeriodWorkflowService.Version(0L));
        var artifact=artifacts.generate(id,email,new Generate(version()));var fixture=signingFixture();authorFixture=fixture;bindings.register(user,email,new CertificateBindings.Register(fingerprint(fixture),true,"Identidad pública de prueba",null));
        var state=signatures.state(id,artifact.id(),email,true);assertTrue(state.signingEnabled(),state.signingBlockers().toString());
        signatures.sign(id,artifact.id(),email,version(),state.preparation().pdfHash(),UUID.randomUUID(),signingBody(fixture,"Test-only-signing-password"),true);
        assertEquals("IN_REVIEW",plans.get(id,email).documentState());assertFalse(plans.get(id,email).editable());
        return reviews.inbox(reviewerEmail).getFirst().reviewId();
    }
    @Test void closedPeriodSuspendsReviewWithoutInventingApprovalOrLosingPdf() throws Exception {
        UUID review=submit(false);var task=reviews.inbox(reviewerEmail).getFirst();var before=reviews.state(review,reviewerEmail);
        byte[] original=files.read((UUID)before.review().get("current_file_id"));
        periodArchives.close(period,email,new ec.edu.uta.utaped.institution.PeriodArchiveService.Close(1L,"Conservar la revisión pendiente"));
        var after=reviews.state(review,reviewerEmail);assertFalse(after.canAct());assertEquals("IN_REVIEW",after.review().get("status"));assertEquals("ACTIVE",after.tasks().getFirst().get("status"));
        assertArrayEquals(original,files.read((UUID)after.review().get("current_file_id")));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->reviews.decide(task.id(),reviewerEmail,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",UUID.randomUUID()))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->reviews.observe(task.id(),reviewerEmail,new ReviewService.Observation(task.rowVersion(),"Observación bloqueada",null,null,null,null,null))).getStatusCode().value());
    }
    @Test void sequentialStagesRejectFutureActorAndRequireEveryDecision() throws Exception {
        UUID review=submit(false);var task=reviews.inbox(reviewerEmail).getFirst();
        assertTrue(reviews.inbox(approverEmail).isEmpty());assertEquals(404,assertThrows(ResponseStatusException.class,()->reviews.state(review,approverEmail)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->reviews.content(review,reviewerEmail)).getStatusCode().value());
        UUID key=UUID.randomUUID();reviews.decide(task.id(),reviewerEmail,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",key));reviews.decide(task.id(),reviewerEmail,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",key));
        assertEquals("IN_REVIEW",plans.get(id,email).documentState());var approval=reviews.inbox(approverEmail).getFirst();
        reviews.decide(approval.id(),approverEmail,new ReviewService.Decision(approval.rowVersion(),"COMPLETE","",UUID.randomUUID()));
        assertEquals("APPROVED",plans.get(id,email).documentState());assertTrue(reviews.content(review,email).length>0);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM review_task WHERE status='COMPLETED'",Integer.class));
    }
    @Test void highlightAndReturnPreserveArtifactAndAdvanceOnlyReviewRound() throws Exception {
        UUID review=submit(false);var task=reviews.inbox(reviewerEmail).getFirst();String hash=reviews.state(review,email).pdfHash();
        reviews.observe(task.id(),reviewerEmail,new ReviewService.Observation(task.rowVersion(),"Corregir esta sección",0,.1,.1,.3,.1));
        var refreshed=reviews.inbox(reviewerEmail).getFirst();reviews.decide(task.id(),reviewerEmail,new ReviewService.Decision(refreshed.rowVersion(),"RETURN","",UUID.randomUUID()));
        var plan=plans.get(id,email);assertEquals("DRAFT",plan.documentState());assertEquals(2,plan.reviewRound());assertEquals("1.0",plan.formalVersion());assertTrue(plan.editable());
        assertEquals(hash,reviews.state(review,email).pdfHash());assertTrue(reviews.inbox(approverEmail).isEmpty());
        assertEquals(1,reviews.state(review,email).observations().size());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM review_task WHERE status='ACTIVE'",Integer.class));
    }
    @Test void requiredReviewerSignatureCannotBeBypassedAndPreservesAuthorBytes() throws Exception {
        UUID review=submit(true);var task=reviews.inbox(reviewerEmail).getFirst();var state=reviews.state(review,reviewerEmail);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->reviews.decide(task.id(),reviewerEmail,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",UUID.randomUUID()))).getStatusCode().value());
        var fixture=signingFixture();bindings.register(reviewer,email,new CertificateBindings.Register(fingerprint(fixture),true,"Revisor de prueba",null));
        var before=files.read((UUID)state.review().get("current_file_id"));var body=signingBody(fixture,"Test-only-signing-password");
        reviews.sign(task.id(),reviewerEmail,task.rowVersion(),state.pdfHash(),UUID.randomUUID(),body,true);assertArrayEquals(new byte[body.length],body);
        var after=files.read((UUID)reviews.state(review,reviewerEmail).review().get("current_file_id"));assertArrayEquals(before,Arrays.copyOf(after,before.length));
        assertEquals(1,reviews.inbox(approverEmail).size());assertEquals("IN_REVIEW",plans.get(id,email).documentState());
    }
    void approve(UUID review) {
        var first=reviews.inbox(reviewerEmail).stream().filter(t->t.reviewId().equals(review)).findFirst().orElseThrow();reviews.decide(first.id(),reviewerEmail,new ReviewService.Decision(first.rowVersion(),"COMPLETE","",UUID.randomUUID()));
        var last=reviews.inbox(approverEmail).stream().filter(t->t.reviewId().equals(review)).findFirst().orElseThrow();reviews.decide(last.id(),approverEmail,new ReviewService.Decision(last.rowVersion(),"COMPLETE","",UUID.randomUUID()));
    }
    @Test void evidenceVersionsAndIndependentT2CompleteTheDocumentCircuit() throws Exception {
        UUID review=submit(false);approve(review);var parent=plans.get(id,email);var activity=activities.get(id,email).activities().getFirst();
        var upload=new org.springframework.mock.web.MockMultipartFile("file","evidencia.pdf","application/pdf",pdf(1,false,false));
        var first=evidence.upload(id,activity.id(),0,0,email,upload);assertEquals(1,first.version());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->evidence.upload(id,activity.id(),0,0,email,upload)).getStatusCode().value());
        var second=evidence.upload(id,activity.id(),0,1,email,upload);assertEquals(2,second.version());assertEquals(2,evidence.get(id,email).activities().getFirst().media().getFirst().versions().size());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->evidence.upload(id,activity.id(),0,2,reviewerEmail,upload)).getStatusCode().value());
        var report=reports.create(id,email);assertNotEquals(id,report.id());assertEquals("T2",report.documentType());assertEquals(report.id(),reports.create(id,email).id());
        var state=reports.get(report.id(),email);var results=state.data().results().stream().map(r->new ec.edu.uta.utaped.planning.ReportService.Result(r.activityId(),java.math.BigDecimal.valueOf(100),"Actividad ejecutada con evidencia de prueba.")).toList();
        reports.save(report.id(),email,new ec.edu.uta.utaped.planning.ReportService.Save(report.rowVersion(),report.title(),"Informe basado en el Plan aprobado, sin resolución ficticia.","Se completó la actividad de prueba.","Mejorar la organización del seguimiento.",results,List.of(new ec.edu.uta.utaped.planning.ReportService.Delegation("Gestión de prueba","Ambato","Unidad de ejemplo","Persona de ejemplo","contacto@example.invalid","Coordinación de actividades","Acuerdo de prueba"))));
        var artifact=artifacts.generate(report.id(),email,new Generate(plans.get(report.id(),email).rowVersion()));assertEquals(1,artifact.pages().stream().filter(p->p.kind().equals("ANNEX")).count());
        var signing=signatures.state(report.id(),artifact.id(),email,true);assertTrue(signing.signingEnabled(),signing.signingBlockers().toString());
        signatures.sign(report.id(),artifact.id(),email,signing.preparation().rowVersion(),signing.preparation().pdfHash(),UUID.randomUUID(),signingBody(authorFixture,"Test-only-signing-password"),true);
        UUID reportReview=reviews.inbox(reviewerEmail).stream().filter(t->t.documentId().equals(report.id())).findFirst().orElseThrow().reviewId();approve(reportReview);
        assertEquals("APPROVED",plans.get(report.id(),email).documentState());assertEquals(parent.rowVersion(),plans.get(id,email).rowVersion());assertEquals(parent.formalVersion(),plans.get(id,email).formalVersion());
        byte[] pdf=reviews.content(reportReview,email);java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/t2-approved.pdf"),pdf);
        try(var rendered=org.apache.pdfbox.Loader.loadPDF(pdf)) { String text=new org.apache.pdfbox.text.PDFTextStripper().getText(rendered);assertTrue(text.contains("ANTECEDENTES"));assertTrue(text.contains("CONCLUSIONES"));assertFalse(text.contains("@@SIG"));for(int i=0;i<rendered.getNumberOfPages();i++) java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/t2-page-"+i+".png"),artifacts.renderPage(pdf,i)); }
    }
    @Test void formalVersionsArchivePriorBytesAndReportingHonorsAccess() throws Exception {
        UUID review=submit(false);approve(review);byte[] original=reviews.content(review,email);var prior=plans.get(id,email);
        var next=lifecycle.nextVersion(id,email,new ec.edu.uta.utaped.planning.DocumentLifecycleService.Change(prior.rowVersion(),"Nueva actividad requerida por actualización material"));
        assertNotEquals(id,next.id());assertEquals("2.0",next.formalVersion());assertEquals(1,next.reviewRound());assertEquals("DRAFT",next.documentState());
        assertEquals("APPROVED",plans.get(id,email).documentState());assertNotNull(plans.get(id,email).archivedAt());assertArrayEquals(original,reviews.content(review,email));
        assertEquals(2,lifecycle.history(next.id(),email).size());assertEquals(1,reporting.get(email,group,period,false).plans());assertEquals(2,reporting.get(email,group,period,true).plans());
        assertEquals(0,reporting.get(approverEmail,group,period,false).plans());assertEquals(1,reporting.get(approverEmail,group,period,true).plans());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->lifecycle.nextVersion(next.id(),email,new ec.edu.uta.utaped.planning.DocumentLifecycleService.Change(next.rowVersion(),"No debe versionar un borrador"))).getStatusCode().value());
        assertTrue(new String(reporting.csv(email,group,period,true),java.nio.charset.StandardCharsets.UTF_8).contains("Comisión de prueba"));
    }
    @Test void archivedDraftReportDoesNotBlockAReplacementOrResetItsFormalVersion() throws Exception {
        UUID review=submit(false);approve(review);var first=reports.create(id,email);
        lifecycle.archive(first.id(),email,new ec.edu.uta.utaped.planning.DocumentLifecycleService.Change(first.rowVersion(),"Descartar borrador incompleto"));
        var next=reports.create(id,email);assertNotEquals(first.id(),next.id());assertEquals(first.formalVersion(),next.formalVersion());
        assertEquals("DRAFT",next.documentState());assertEquals(2,lifecycle.history(next.id(),email).size());
        assertEquals(next.id(),reports.create(id,email).id());assertNotNull(plans.get(first.id(),email).archivedAt());
    }
    @Test void individualExtensionRetainsOriginalDeadlineAndHistory() throws Exception {
        UUID review=submit(false);approve(review);var plan=plans.get(id,email);var activity=evidence.get(id,email).activities().getFirst();
        organization.member(plan.scopeId(),email,new OrganizationService.MemberInput(reviewer,"COORDINATOR","Coordinación de prueba",true,0L));
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?)",id,reviewer,user);
        var grant=new ec.edu.uta.utaped.planning.EvidenceService.Extension(0L,user,activity.endsOn().plusDays(1),"Plazo individual de prueba");
        assertEquals(403,assertThrows(ResponseStatusException.class,()->evidence.extend(id,activity.id(),email,grant)).getStatusCode().value());
        evidence.extend(id,activity.id(),reviewerEmail,grant);
        assertEquals(activity.endsOn().plusDays(1),evidence.get(id,email).activities().getFirst().deadline());
        assertEquals(activity.endsOn(),evidence.get(id,reviewerEmail).activities().getFirst().deadline());
        assertEquals(activity.endsOn(),activities.get(id,email).activities().getFirst().endsOn());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM deadline_extension_history WHERE work_plan_id=? AND user_id=?",Integer.class,id,user));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->evidence.extend(id,activity.id(),reviewerEmail,grant)).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->evidence.extend(id,activity.id(),reviewerEmail,new ec.edu.uta.utaped.planning.EvidenceService.Extension(0L,approver,activity.endsOn().plusDays(1),"No es responsable"))).getStatusCode().value());
    }}
