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
    UUID submit(boolean signatureRequired) throws Exception { return submit(signatureRequired,false); }
    UUID submit(boolean signatureRequired,boolean external) throws Exception {
        complete("Justificación de prueba para revisión",1);signingAdmin();
        if(external) {
            var catalog=activities.saveCatalog(null,new ec.edu.uta.utaped.planning.ActivityModels.CatalogInput("MEANS","Repositorio externo",true,"URL"),email);
            var matrix=activities.get(id,email);var a=matrix.activities().getFirst();
            activities.save(id,email,new ec.edu.uta.utaped.planning.ActivityModels.Save(version(),matrix.source(),List.of(new ec.edu.uta.utaped.planning.ActivityModels.Activity(a.id(),a.catalogId(),a.title(),a.category(),a.mandatory(),a.startsOn(),a.endsOn(),a.responsibleIds(),a.collective(),a.resources(),List.of(a.means().getFirst(),new ec.edu.uta.utaped.planning.ActivityModels.Choice(catalog.id(),null,null,"PDF"))))));
            assertEquals("URL",activities.get(id,email).activities().getFirst().means().get(1).evidenceKind());
            activities.saveCatalog(catalog.id(),new ec.edu.uta.utaped.planning.ActivityModels.CatalogInput("MEANS","Repositorio cambiado",true,"PDF"),email);
            assertEquals("URL",activities.get(id,email).activities().getFirst().means().get(1).evidenceKind());
        }
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
    @Test void reviewPaginationExceedsTwoHundredWithStableOrderAndPrivateFilteredMetrics() throws Exception {
        UUID original=submit(false);
        for(int i=2;i<=236;i++) {
            UUID review=UUID.randomUUID();
            jdbc.update("INSERT INTO document_review(id,work_plan_id,artifact_id,author_signature_id,current_file_id,round_number,status,created_at) SELECT ?,work_plan_id,artifact_id,author_signature_id,current_file_id,?,'RETURNED',created_at FROM document_review WHERE id=?",review,i,original);
            jdbc.update("INSERT INTO review_task(id,review_id,actor_id,actor_name,stage_label,stage_action,position,requires_signature,signature_action,status) SELECT ?,?,actor_id,actor_name,stage_label,stage_action,position,requires_signature,signature_action,? FROM review_task WHERE review_id=? AND actor_id=?",UUID.randomUUID(),review,i==236?"RETURNED":"COMPLETED",original,reviewer);
        }
        var seen=new HashSet<UUID>();
        for(int page=0;page<12;page++) {
            var result=reviews.inboxPage(reviewerEmail,"","","",page,20);assertEquals(236,result.total());
            assertEquals(1,result.metrics().active());assertEquals(234,result.metrics().completed());assertEquals(1,result.metrics().returned());
            for(var task:result.items()) assertTrue(seen.add(task.id()),"No duplicate across pages");
            assertEquals(result.items(),reviews.inboxPage(reviewerEmail,"","","",page,20).items());
        }
        assertEquals(236,seen.size());assertEquals(200,reviews.inbox(reviewerEmail).size());
        var filtered=reviews.inboxPage(reviewerEmail,"COMISIÓN","RETURNED","T1",0,10);assertEquals(1,filtered.total());assertEquals(234,filtered.metrics().completed());
        assertEquals(0,reviews.inboxPage(reviewerEmail,"%_","","",0,20).total());assertEquals(0,reviews.inboxPage(reviewerEmail,"","","T2",0,20).total());
        assertEquals(0,reviews.inboxPage(approverEmail,"","","",0,20).total());assertEquals(0,reviews.inboxPage(email,"","","",0,20).total());
        for(int size:new int[]{0,101}) assertEquals(400,assertThrows(ResponseStatusException.class,()->reviews.inboxPage(reviewerEmail,"","","",0,size)).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->reviews.inboxPage(reviewerEmail,"","PENDING","",0,20)).getStatusCode().value());
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
    }
    @Test void externalReferencesRespectMediumPermissionsVersionsAndFrozenT2() throws Exception {
        UUID review=submit(false,true);approve(review);assertFalse(jdbc.queryForObject("SELECT snapshot::text FROM document_artifact WHERE work_plan_id=?",String.class,id).contains("externalReferences"));var activity=activities.get(id,email).activities().getFirst();
        var upload=new org.springframework.mock.web.MockMultipartFile("file","evidencia.pdf","application/pdf",pdf(1,false,false));
        var reference=new ec.edu.uta.utaped.planning.EvidenceService.Reference(0,"Acta externa","https://example.invalid/acta?revision=1");
        assertEquals(400,assertThrows(ResponseStatusException.class,()->evidence.reference(id,activity.id(),0,email,reference)).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->evidence.upload(id,activity.id(),1,0,email,upload)).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->evidence.reference(id,activity.id(),1,reviewerEmail,reference)).getStatusCode().value());
        for(String url:List.of("http://example.invalid/acta","javascript:alert(1)","https://user:password@example.invalid/acta","https://example.invalid/with space"))
            assertEquals(400,assertThrows(ResponseStatusException.class,()->evidence.reference(id,activity.id(),1,email,new ec.edu.uta.utaped.planning.EvidenceService.Reference(0,"Acta",url))).getStatusCode().value());
        var first=evidence.reference(id,activity.id(),1,email,reference);
        assertEquals("URL",first.evidenceKind());assertNull(first.sha256());assertNull(first.originalName());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->evidence.content(id,first.id(),email)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->evidence.reference(id,activity.id(),1,email,reference)).getStatusCode().value());
        assertEquals(1,evidence.get(id,email).coveredMedia());assertFalse(evidence.get(id,email).activities().getFirst().complete());
        evidence.upload(id,activity.id(),0,0,email,upload);assertEquals(2,evidence.get(id,email).coveredMedia());assertTrue(evidence.get(id,email).activities().getFirst().complete());
        var report=reports.create(id,email);var state=reports.get(report.id(),email);
        reports.save(report.id(),email,new ec.edu.uta.utaped.planning.ReportService.Save(report.rowVersion(),report.title(),"Antecedentes de prueba","Conclusiones de prueba","Recomendaciones de prueba",state.data().results().stream().map(r->new ec.edu.uta.utaped.planning.ReportService.Result(r.activityId(),java.math.BigDecimal.valueOf(100),"Cumplimiento documentado")).toList(),List.of()));
        var artifact=artifacts.generate(report.id(),email,new Generate(plans.get(report.id(),email).rowVersion()));
        assertEquals(1,artifact.pages().stream().filter(p->p.kind().equals("ANNEX")).count());
        String snapshot=jdbc.queryForObject("SELECT snapshot::text FROM document_artifact WHERE id=?",String.class,artifact.id());assertTrue(snapshot.contains(first.externalUrl()));
        var signing=signatures.state(report.id(),artifact.id(),email,true);assertTrue(signing.signingEnabled(),signing.signingBlockers().toString());
        signatures.sign(report.id(),artifact.id(),email,signing.preparation().rowVersion(),signing.preparation().pdfHash(),UUID.randomUUID(),signingBody(authorFixture,"Test-only-signing-password"),true);
        UUID reportReview=reviews.inbox(reviewerEmail).stream().filter(t->t.documentId().equals(report.id())).findFirst().orElseThrow().reviewId();approve(reportReview);
        byte[] frozen=reviews.content(reportReview,email);
        try(var rendered=org.apache.pdfbox.Loader.loadPDF(frozen)) {String text=new org.apache.pdfbox.text.PDFTextStripper().getText(rendered);assertTrue(text.contains("Acta externa"));assertTrue(text.contains("https://example.invalid/acta?revision=1"));java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/t2-external.pdf"),frozen);java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/t2-external-first.png"),artifacts.renderPage(frozen,0));}
        var second=evidence.reference(id,activity.id(),1,email,new ec.edu.uta.utaped.planning.EvidenceService.Reference(1,"Acta corregida","https://example.invalid/acta?revision=2"));assertEquals(2,second.version());
        assertEquals(2,evidence.get(id,email).activities().getFirst().media().get(1).versions().size());assertArrayEquals(frozen,reviews.content(reportReview,email));
        assertEquals(snapshot,jdbc.queryForObject("SELECT snapshot::text FROM document_artifact WHERE id=?",String.class,artifact.id()));
        periodArchives.close(period,email,new ec.edu.uta.utaped.institution.PeriodArchiveService.Close(1L,"Cierre de prueba con referencias"));
        boolean found=false;try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(periodArchives.download(period,email)))) {java.util.zip.ZipEntry entry;while((entry=zip.getNextEntry())!=null) {byte[] bytes=zip.readAllBytes();if(entry.getName().equals("documents/"+id+"/manifest.json")) {String manifest=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);assertTrue(manifest.contains(first.externalUrl()));assertTrue(manifest.contains(second.externalUrl()));found=true;}}}assertTrue(found);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->evidence.reference(id,activity.id(),1,email,new ec.edu.uta.utaped.planning.EvidenceService.Reference(2,"Acta","https://example.invalid/acta"))).getStatusCode().value());
    }
}
