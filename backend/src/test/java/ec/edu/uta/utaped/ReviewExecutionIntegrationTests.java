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
    @Autowired ec.edu.uta.utaped.planning.BusinessCalendar calendar;
    @Autowired ec.edu.uta.utaped.planning.PreparationExtensionService extensions;
    @Autowired ec.edu.uta.utaped.planning.ExecutionReportService executions;
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    @Test void reportCreationOptionsRequireApprovedAccessiblePlansAndPreserveExistingNames() throws Exception {
        UUID review=submit(false);assertEquals(0,reports.options(email,"",0,20).total());approve(review);
        var source=plans.get(id,email);String originalHash=reviews.state(review,email).pdfHash();
        var options=reports.options(email,"",0,1);assertEquals(1,options.total());assertEquals(id,options.items().getFirst().id());assertNull(options.items().getFirst().reportId());
        assertEquals("Informe: "+source.title(),options.items().getFirst().suggestedTitle());
        assertEquals(0,reports.options(reviewerEmail,"",0,20).total());assertEquals(0,reports.options(approverEmail,"",0,20).total());
        String adminEmail="unassigned-admin@example.invalid";jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,'unused','ADMIN',false)",UUID.randomUUID(),adminEmail,"Administrador sin ámbito");assertEquals(0,reports.options(adminEmail,"",0,20).total());
        assertEquals(0,reports.options(email,"%_",0,20).total());assertEquals(0,reports.options(email,"",1,1).items().size());
        assertEquals(1,reports.options(email,"COMISIÓN",0,20).total());
        jdbc.update("UPDATE app_user SET password_hash=? WHERE id=?",encoder.encode("Test-only-password-2026"),user);
        var browser=new DocumentLoadProbe.Browser(port,jdbc,email);var response=browser.request("GET","/api/reports/options?size=1",null,null);assertEquals(200,response.status());
        assertEquals(400,browser.request("GET","/api/reports/options?size=51",null,null).status());
        var report=reports.create(id,email);assertEquals(options.items().getFirst().suggestedTitle(),report.title());
        var draft=reports.data(report.id(),email);reports.save(report.id(),email,new ec.edu.uta.utaped.planning.ReportService.Save(report.rowVersion(),"Informe revisado por el responsable","","","",draft.results(),draft.delegations()));
        var existing=reports.options(email,"",0,20).items().getFirst();assertEquals(report.id(),existing.reportId());assertEquals("Informe revisado por el responsable",existing.suggestedTitle());
        assertEquals(report.id(),reports.create(id,email).id());assertEquals(source.rowVersion(),plans.get(id,email).rowVersion());assertEquals(originalHash,reviews.state(review,email).pdfHash());
        jdbc.update("UPDATE academic_period SET status='CLOSED' WHERE id=?",period);assertEquals(0,reports.options(email,"",0,20).total());
    }
    @Test void boundedHttpLoadGeneratesAndRendersLongDocumentsForThreeIsolatedAuthors() throws Exception {
        var probe=new DocumentLoadProbe();complete("Justificación extensa de planificación y seguimiento. ".repeat(100),35);signingAdmin();
        var source=activities.get(id,email).activities();byte[] annex=pdf(12,false,false);
        var browsers=new ArrayList<DocumentLoadProbe.Browser>();var docs=new ArrayList<UUID>();var versions=new ArrayList<Long>();
        var fixtures=new ArrayList<SigningFixture>();
        for(int i=0;i<3;i++) {
            UUID actor=UUID.randomUUID();String owner="load"+i+"@example.invalid";
            jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,'USER',false)",actor,owner,"Autor de carga "+i,encoder.encode("Test-only-password-2026"));
            UUID ownGroup=UUID.randomUUID();jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,?,'COMMISSION')",ownGroup,"Comisión de carga "+i);
            UUID ownScope=organization.createScope(email,new OrganizationService.ScopeInput(ownGroup,period,null));
            organization.member(ownScope,email,new OrganizationService.MemberInput(actor,"MEMBER","Autor",true,0L));
            organization.principal(ownScope,email,new OrganizationService.PrincipalInput(actor,0L));
            var definition=new Definition("Aprobación ficticia de carga",List.of(new Stage(UUID.randomUUID(),"Aprobación","APPROVE","PERSON",List.of(user),null,"",false)));
            for(String type:List.of("T1","T2")) {
                workflows.save(ownGroup,type,email,new Save(0L,definition));workflows.configure(ownGroup,type,email,new Version(1L));
                periodWorkflows.assign(ownScope,type,email,new PeriodWorkflowService.Assign(workflows.get(ownGroup,type,email).current().id(),0L));
            }
            var fixture=signingFixture();fixtures.add(fixture);bindings.register(actor,email,new CertificateBindings.Register(fingerprint(fixture),true,"Identidad ficticia de carga",null));
            UUID doc=plans.create(owner,new ec.edu.uta.utaped.planning.WorkPlanModels.Create(ownGroup,period,UUID.randomUUID(),"Documento extenso "+i,ownScope)).id();
            var created=plans.get(doc,owner);
            plans.update(doc,owner,new ec.edu.uta.utaped.planning.WorkPlanModels.Update(created.rowVersion(),"Documento extenso "+i,created.institutionalUnit(),created.career(),"Justificación extensa. ".repeat(180),"Objetivo de carga "+i));
            var rows=source.stream().map(a->new ec.edu.uta.utaped.planning.ActivityModels.Activity(UUID.randomUUID(),null,a.title(),a.category(),false,a.startsOn(),a.endsOn(),List.of(actor),false,a.resources(),a.means())).toList();
            activities.save(doc,owner,new ec.edu.uta.utaped.planning.ActivityModels.Save(plans.get(doc,owner).rowVersion(),"Fuente de carga",rows));
            attachments.settings(doc,owner,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(plans.get(doc,owner).rowVersion(),true,false));
            attachments.upload(doc,owner,plans.get(doc,owner).rowVersion(),UUID.randomUUID(),"Anexo extenso",new org.springframework.mock.web.MockMultipartFile("file","anexo.pdf","application/pdf",annex));
            browsers.add(new DocumentLoadProbe.Browser(port,jdbc,owner));docs.add(doc);versions.add(plans.get(doc,owner).rowVersion());
        }
        periodWorkflows.activate(period,email,new PeriodWorkflowService.Version(0L));
        var generating=new ArrayList<java.util.concurrent.Callable<DocumentLoadProbe.Result>>();
        for(int i=0;i<3;i++) {int index=i;generating.add(()->probe.retryBusy(()->browsers.get(index).request("POST","/api/work-plans/"+docs.get(index)+"/artifacts","application/json",("{\"rowVersion\":"+versions.get(index)+"}").getBytes(java.nio.charset.StandardCharsets.UTF_8))));}
        var generated=probe.phase("http-generate-three-authors",generating);var ids=new ArrayList<UUID>();var hashes=new ArrayList<String>();
        for(int i=0;i<3;i++) {
            assertEquals(200,generated.get(i).status());String body=new String(generated.get(i).body(),java.nio.charset.StandardCharsets.UTF_8);
            UUID artifact=UUID.fromString(com.jayway.jsonpath.JsonPath.read(body,"$.id"));ids.add(artifact);
            byte[] bytes=artifacts.content(docs.get(i),artifact,"load"+i+"@example.invalid");hashes.add(ec.edu.uta.utaped.documents.StoredFiles.hash(bytes));
            try(var parsed=org.apache.pdfbox.Loader.loadPDF(bytes)) {assertTrue(parsed.getNumberOfPages()>=20);assertEquals(parsed.getNumberOfPages(),((Number)com.jayway.jsonpath.JsonPath.read(body,"$.pageCount")).intValue());}
            assertEquals(versions.get(i),plans.get(docs.get(i),"load"+i+"@example.invalid").rowVersion());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM document_artifact WHERE work_plan_id=?",Integer.class,docs.get(i)));
        }
        var rendering=new ArrayList<java.util.concurrent.Callable<DocumentLoadProbe.Result>>();
        for(int i=0;i<8;i++) {int index=i%3,page=i/3;rendering.add(()->probe.retryBusy(()->browsers.get(index).request("GET","/api/work-plans/"+docs.get(index)+"/artifacts/"+ids.get(index)+"/pages/"+page,null,null)));}
        for(var result:probe.phase("http-render-eight-readers",rendering)) {assertEquals(200,result.status());assertNotNull(javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(result.body())));}
        var downloads=new ArrayList<java.util.concurrent.Callable<DocumentLoadProbe.Result>>();
        for(int i=0;i<8;i++) {int index=i%3;downloads.add(()->probe.measured(()->browsers.get(index).request("GET","/api/work-plans/"+docs.get(index)+"/artifacts/"+ids.get(index)+"/content",null,null)));}
        var results=probe.phase("http-block-eight-preliminary-downloads",downloads);
        for(var result:results) assertEquals(403,result.status());
        for(int i=0;i<3;i++) assertEquals(hashes.get(i),ec.edu.uta.utaped.documents.StoredFiles.hash(artifacts.content(docs.get(i),ids.get(i),"load"+i+"@example.invalid")));
        assertEquals(404,browsers.get(0).request("GET","/api/work-plans/"+docs.get(1)+"/artifacts/"+ids.get(1)+"/pages/0",null,null).status());
        var signing=new ArrayList<java.util.concurrent.Callable<DocumentLoadProbe.Result>>();
        for(int i=0;i<3;i++) {
            int index=i;UUID key=UUID.randomUUID();
            signing.add(()->probe.retryBusy(()->{
                byte[] material=signingBody(fixtures.get(index),"Test-only-signing-password");
                try {var signed=signatures.sign(docs.get(index),ids.get(index),"load"+index+"@example.invalid",versions.get(index),hashes.get(index),key,material,true);return new DocumentLoadProbe.Result(200,signatures.content(docs.get(index),signed.id(),"load"+index+"@example.invalid"));}
                catch(ResponseStatusException error) {return new DocumentLoadProbe.Result(error.getStatusCode().value(),new byte[0]);}
                finally {assertArrayEquals(new byte[material.length],material);}
            }));
        }
        var signed=probe.phase("service-sign-three-distinct-authors",signing);var reviewIds=new ArrayList<UUID>();
        for(int i=0;i<3;i++) {
            assertEquals(200,signed.get(i).status());byte[] unsigned=artifacts.content(docs.get(i),ids.get(i),"load"+i+"@example.invalid");assertArrayEquals(unsigned,Arrays.copyOf(signed.get(i).body(),unsigned.length));
            try(var pdf=org.apache.pdfbox.Loader.loadPDF(signed.get(i).body())) {assertEquals(1,pdf.getSignatureDictionaries().size());}
            int index=i;var task=reviews.inbox(email).stream().filter(t->t.documentId().equals(docs.get(index))).findFirst().orElseThrow();reviewIds.add(task.reviewId());
            reviews.decide(task.id(),email,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",UUID.randomUUID()));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM signed_document_artifact WHERE work_plan_id=?",Integer.class,docs.get(i)));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM document_review WHERE work_plan_id=?",Integer.class,docs.get(i)));
        }
        downloads.clear();
        for(int i=0;i<8;i++) {int index=i%3;downloads.add(()->probe.measured(()->browsers.get(index).request("GET","/api/reviews/"+reviewIds.get(index)+"/content",null,null)));}
        var approved=probe.phase("http-long-signed-download-eight-readers",downloads);
        for(int i=0;i<8;i++) {assertEquals(200,approved.get(i).status());assertArrayEquals(signed.get(i%3).body(),approved.get(i).body());}
        probe.save("long-documents-http");
    }
    @Test void concurrentEvidenceAndReportSignaturesPreserveVersionsAndSignedPlan() throws Exception {
        var probe=new DocumentLoadProbe();UUID baseReview=submit(false);approve(baseReview);
        jdbc.update("UPDATE app_user SET password_hash=? WHERE id=?",encoder.encode("Test-only-password-2026"),user);
        var browser=new DocumentLoadProbe.Browser(port,jdbc,email);byte[] original=reviews.content(baseReview,email);var plan=plans.get(id,email);
        var downloads=new ArrayList<java.util.concurrent.Callable<DocumentLoadProbe.Result>>();
        for(int i=0;i<8;i++) downloads.add(()->probe.measured(()->browser.request("GET","/api/reviews/"+baseReview+"/content",null,null)));
        for(var result:probe.phase("http-approved-download-eight-readers",downloads)) {assertEquals(200,result.status());assertArrayEquals(original,result.body());}
        var activity=activities.get(id,email).activities().getFirst();byte[] evidencePdf=pdf(12,false,false);
        String boundary="utaped-load-"+UUID.randomUUID();var multipart=new java.io.ByteArrayOutputStream();
        multipart.write(("--"+boundary+"\r\nContent-Disposition: form-data; name=\"file\"; filename=\"evidencia.pdf\"\r\nContent-Type: application/pdf\r\n\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));multipart.write(evidencePdf);multipart.write(("\r\n--"+boundary+"--\r\n").getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        for(int version=0;version<2;version++) {
            int expected=version;var uploading=new ArrayList<java.util.concurrent.Callable<DocumentLoadProbe.Result>>();
            for(int i=0;i<4;i++) uploading.add(()->probe.measured(()->browser.request("POST","/api/work-plans/"+id+"/execution/activities/"+activity.id()+"/media/0?expectedVersion="+expected,"multipart/form-data; boundary="+boundary,multipart.toByteArray())));
            var uploaded=probe.phase("http-evidence-version-"+(version+1),uploading);
            assertEquals(1,uploaded.stream().filter(r->r.status()==200).count());assertEquals(3,uploaded.stream().filter(r->r.status()==409).count());
        }
        var evidenceVersions=evidence.get(id,email).activities().getFirst().media().getFirst().versions();assertEquals(2,evidenceVersions.size());
        for(var item:evidenceVersions) assertArrayEquals(evidencePdf,evidence.content(id,item.id(),email));
        var report=reports.create(id,email);reports.save(report.id(),email,new ec.edu.uta.utaped.planning.ReportService.Save(report.rowVersion(),"Informe extenso de carga","Antecedentes de carga. ".repeat(220),"Conclusiones. ".repeat(100),"Mejoras. ".repeat(100),List.of(new ec.edu.uta.utaped.planning.ReportService.Result(activity.id(),java.math.BigDecimal.valueOf(100),"Cumplida con evidencia")),List.of()));
        var artifact=artifacts.generate(report.id(),email,new Generate(plans.get(report.id(),email).rowVersion()));byte[] unsigned=artifacts.content(report.id(),artifact.id(),email);
        var preparation=signatures.state(report.id(),artifact.id(),email,true).preparation();UUID key=UUID.randomUUID();
        var signing=new ArrayList<java.util.concurrent.Callable<DocumentLoadProbe.Result>>();
        for(int i=0;i<2;i++) signing.add(()->probe.measured(()->{
            byte[] material=signingBody(authorFixture,"Test-only-signing-password");
            try {var signed=signatures.sign(report.id(),artifact.id(),email,preparation.rowVersion(),preparation.pdfHash(),key,material,true);return new DocumentLoadProbe.Result(200,signatures.content(report.id(),signed.id(),email));}
            catch(ResponseStatusException error) {return new DocumentLoadProbe.Result(error.getStatusCode().value(),new byte[0]);}
            catch(org.springframework.dao.ConcurrencyFailureException error) {return new DocumentLoadProbe.Result(409,new byte[0]);}
            finally {assertArrayEquals(new byte[material.length],material);}
        }));
        var signed=probe.phase("service-same-key-signing-two-callers",signing);assertTrue(signed.stream().anyMatch(r->r.status()==200));assertTrue(signed.stream().allMatch(r->r.status()==200||r.status()==409));
        byte[] retry=signingBody(authorFixture,"Test-only-signing-password");var confirmed=signatures.sign(report.id(),artifact.id(),email,preparation.rowVersion(),preparation.pdfHash(),key,retry,true);assertArrayEquals(new byte[retry.length],retry);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM signed_document_artifact WHERE work_plan_id=?",Integer.class,report.id()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM document_review WHERE work_plan_id=?",Integer.class,report.id()));
        byte[] signedBytes=signatures.content(report.id(),confirmed.id(),email);assertArrayEquals(unsigned,Arrays.copyOf(signedBytes,unsigned.length));
        UUID review=reviews.inbox(reviewerEmail).stream().filter(t->t.documentId().equals(report.id())).findFirst().orElseThrow().reviewId();approve(review);
        assertArrayEquals(signedBytes,reviews.content(review,email));assertArrayEquals(original,reviews.content(baseReview,email));assertEquals(plan.rowVersion(),plans.get(id,email).rowVersion());
        probe.save("evidence-and-signing");
    }
    @Autowired ec.edu.uta.utaped.planning.DocumentAuthors authors;
    boolean multipleAuthors,collegiate,requiresQuipux;
    UUID coauthor;
    String coauthorEmail="coauthor@example.invalid";
    SigningFixture authorFixture,coauthorFixture;
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
        if(multipleAuthors) {
            coauthor=UUID.randomUUID();jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,'unused','USER',false)",coauthor,coauthorEmail,"Coautora académica");
            organization.member(scope,email,new OrganizationService.MemberInput(coauthor,"MEMBER","Docente integrante",true,0L));
            assertEquals(404,assertThrows(ResponseStatusException.class,()->authors.save(id,coauthorEmail,new ec.edu.uta.utaped.planning.DocumentAuthors.Save(version(),List.of()))).getStatusCode().value());
            authors.save(id,email,new ec.edu.uta.utaped.planning.DocumentAuthors.Save(version(),List.of(coauthor)));
            var matrix=activities.get(id,email);var a=matrix.activities().getFirst();
            activities.save(id,email,new ec.edu.uta.utaped.planning.ActivityModels.Save(version(),matrix.source(),List.of(new ec.edu.uta.utaped.planning.ActivityModels.Activity(a.id(),a.catalogId(),a.title(),a.category(),a.mandatory(),a.startsOn(),a.endsOn(),a.responsibleIds(),false,a.resources(),a.means()))));
        }
        var definition=new Definition("Circuito de prueba",List.of(new Stage(UUID.randomUUID(),"Revisión","REVIEW",collegiate?"COLLEGIATE":"PERSON",List.of(reviewer),null,collegiate?"Consejo Directivo":"",signatureRequired,"Responsable de revisión"),new Stage(UUID.randomUUID(),"Aprobación","APPROVE","PERSON",List.of(approver),null,"",false)),requiresQuipux);
        workflows.save(group,"T1",email,new Save(0L,definition));workflows.configure(group,"T1",email,new Version(1L));
        workflows.save(group,"T2",email,new Save(0L,definition));workflows.configure(group,"T2",email,new Version(1L));
        workflows.save(group,"T3",email,new Save(0L,definition));workflows.configure(group,"T3",email,new Version(1L));
        for(String type:List.of("T1","T2","T3")) periodWorkflows.assign(scope,type,email,new PeriodWorkflowService.Assign(workflows.get(group,type,email).current().id(),0L));
        periodWorkflows.activate(period,email,new PeriodWorkflowService.Version(0L));
        var artifact=artifacts.generate(id,email,new Generate(version()));var fixture=signingFixture();authorFixture=fixture;bindings.register(user,email,new CertificateBindings.Register(fingerprint(fixture),true,"Identidad pública de prueba",null));
        var state=signatures.state(id,artifact.id(),email,true);assertTrue(state.signingEnabled(),state.signingBlockers().toString());
        signatures.sign(id,artifact.id(),email,version(),state.preparation().pdfHash(),UUID.randomUUID(),signingBody(fixture,"Test-only-signing-password"),true);
        if(multipleAuthors) {
            assertEquals("DRAFT",plans.get(id,email).documentState());assertFalse(plans.get(id,email).editable());
            assertTrue(reviews.inbox(reviewerEmail).isEmpty());
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM personal_notification WHERE recipient_id=? AND kind='AUTHOR_SIGNATURE_REQUESTED'",Integer.class,coauthor));
            assertEquals(403,assertThrows(ResponseStatusException.class,()->plans.update(id,email,new ec.edu.uta.utaped.planning.WorkPlanModels.Update(version(),"Cambio prohibido","FISEI","","Alterar","Alterar"))).getStatusCode().value());
            var first=signatures.list(id,artifact.id(),email).getFirst();byte[] signedBefore=signatures.content(id,first.id(),email);
            var coauthorCert=signingFixture();coauthorFixture=coauthorCert;bindings.register(coauthor,email,new CertificateBindings.Register(fingerprint(coauthorCert),true,"Identidad de coautor de prueba",null));
            var next=signatures.state(id,artifact.id(),coauthorEmail,true);assertTrue(next.signingEnabled(),next.signingBlockers().toString());
            var signed=signatures.sign(id,artifact.id(),coauthorEmail,version(),next.preparation().pdfHash(),UUID.randomUUID(),signingBody(coauthorCert,"Test-only-signing-password"),true);
            byte[] cumulative=signatures.content(id,signed.id(),email);assertArrayEquals(signedBefore,Arrays.copyOf(cumulative,signedBefore.length));
            try(var parsed=org.apache.pdfbox.Loader.loadPDF(cumulative)) { assertEquals(2,parsed.getSignatureDictionaries().size()); }
        }
        assertEquals("IN_REVIEW",plans.get(id,email).documentState());assertFalse(plans.get(id,email).editable());
        return reviews.inbox(reviewerEmail).getFirst().reviewId();
    }
    @Test void coauthorsSignSequentiallyAndOnlyTheLastSignatureStartsReview() throws Exception {
        multipleAuthors=true;UUID review=submit(false);
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM signed_document_artifact WHERE work_plan_id=?",Integer.class,id));
        var saved=reviews.state(review,email);assertTrue(saved.pdfHash()!=null);
        var artifact=artifacts.list(id,email).getFirst();assertEquals(2,artifact.signatureSlots().stream().filter(slot->slot.action().equals("ELABORADO_POR")).count());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM document_review WHERE work_plan_id=?",Integer.class,id));
        approve(review);byte[] planBytes=reviews.content(review,email);
        var report=reports.create(id,email);authors.save(report.id(),email,new ec.edu.uta.utaped.planning.DocumentAuthors.Save(report.rowVersion(),List.of(coauthor)));
        var data=reports.get(report.id(),email);
        reports.save(report.id(),email,new ec.edu.uta.utaped.planning.ReportService.Save(data.document().rowVersion(),data.document().title(),"Antecedentes del informe académico","Conclusiones de ejecución","Mejorar el seguimiento",data.data().results().stream().map(result->new ec.edu.uta.utaped.planning.ReportService.Result(result.activityId(),java.math.BigDecimal.valueOf(100),"Resultado declarado; medios pendientes explicados.")).toList(),List.of()));
        var updated=plans.get(report.id(),email);attachments.settings(report.id(),email,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(updated.rowVersion(),false,false));
        var t2=artifacts.generate(report.id(),email,new Generate(plans.get(report.id(),email).rowVersion()));
        assertEquals(2,t2.signatureSlots().stream().filter(slot->slot.action().equals("ELABORADO_POR")).count());
        for(var author:List.of(email,coauthorEmail)) {
            var next=signatures.state(report.id(),t2.id(),author,true);assertTrue(next.signingEnabled(),next.signingBlockers().toString());
            signatures.sign(report.id(),t2.id(),author,next.preparation().rowVersion(),next.preparation().pdfHash(),UUID.randomUUID(),signingBody(author.equals(email)?authorFixture:coauthorFixture,"Test-only-signing-password"),true);
        }
        assertEquals("IN_REVIEW",plans.get(report.id(),email).documentState());assertEquals("APPROVED",plans.get(id,email).documentState());
        assertArrayEquals(planBytes,reviews.content(review,email));
    }
    @Test void collegiateRecorderActsWithoutAppearingAsPersonalSignatory() throws Exception {
        collegiate=true;UUID review=submit(false);var task=reviews.inbox(reviewerEmail).getFirst();assertFalse(task.requiresSignature());
        byte[] bytes=files.read((UUID)reviews.state(review,email).review().get("current_file_id"));
        try(var parsed=org.apache.pdfbox.Loader.loadPDF(bytes)) {
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(parsed);assertTrue(text.contains("Consejo Directivo"));assertFalse(text.contains(reviewerEmail));
        }
        reviews.decide(task.id(),reviewerEmail,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",UUID.randomUUID()));
        assertFalse(reviews.inbox(approverEmail).isEmpty());
    }
    @Test void savedObservationPreventsCompletionWithOrWithoutSignature() throws Exception {
        UUID review=submit(false);var task=reviews.inbox(reviewerEmail).getFirst();
        reviews.observe(task.id(),reviewerEmail,new ReviewService.Observation(task.rowVersion(),"Corregir antes de aprobar",0,.1,.1,.3,.1));
        var current=reviews.inbox(reviewerEmail).getFirst();
        assertEquals(409,assertThrows(ResponseStatusException.class,()->reviews.decide(current.id(),reviewerEmail,new ReviewService.Decision(current.rowVersion(),"COMPLETE","",UUID.randomUUID()))).getStatusCode().value());
        jdbc.update("UPDATE review_task SET requires_signature=true WHERE id=?",task.id());
        byte[] body=signingBody(signingFixture(),"Test-only-signing-password");
        assertEquals(409,assertThrows(ResponseStatusException.class,()->reviews.sign(current.id(),reviewerEmail,current.rowVersion(),reviews.state(review,reviewerEmail).pdfHash(),UUID.randomUUID(),body,true)).getStatusCode().value());
        assertArrayEquals(new byte[body.length],body);assertEquals("IN_REVIEW",plans.get(id,email).documentState());
    }
    @Test void closedPeriodSuspendsReviewWithoutInventingApprovalOrLosingPdf() throws Exception {
        UUID review=submit(false);var task=reviews.inbox(reviewerEmail).getFirst();var before=reviews.state(review,reviewerEmail);
        byte[] original=files.read((UUID)before.review().get("current_file_id"));
        periodArchives.close(period,email,new ec.edu.uta.utaped.institution.PeriodArchiveService.Close(1L,"Conservar la revisión pendiente"));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->reviews.state(review,reviewerEmail)).getStatusCode().value());
        assertEquals("IN_REVIEW",jdbc.queryForObject("SELECT document_state FROM work_plan WHERE id=?",String.class,id));
        assertEquals("ACTIVE",jdbc.queryForObject("SELECT status FROM review_task WHERE id=?",String.class,task.id()));
        assertArrayEquals(original,files.read((UUID)before.review().get("current_file_id")));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->reviews.decide(task.id(),reviewerEmail,new ReviewService.Decision(task.rowVersion(),"COMPLETE","",UUID.randomUUID()))).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->reviews.observe(task.id(),reviewerEmail,new ReviewService.Observation(task.rowVersion(),"Observación bloqueada",null,null,null,null,null))).getStatusCode().value());
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
    @Test void longT2PreservesEveryNarrativeEvidenceAndSignaturePageWithoutChangingItsPlan() throws Exception {
        UUID baseReview=submit(false);approve(baseReview);
        var base=plans.get(id,email);var baseHash=reviews.state(baseReview,email).pdfHash();
        var activity=activities.get(id,email).activities().getFirst();
        evidence.upload(id,activity.id(),0,0,email,new org.springframework.mock.web.MockMultipartFile("file","evidencia-extensa.pdf","application/pdf",pdf(7,false,false)));
        var report=reports.create(id,email);var data=reports.get(report.id(),email).data();
        String background="Antecedente institucional verificable de la planificación, ejecución y conservación de evidencias. ".repeat(220)+"FIN_ANTECEDENTES_EXTENSOS";
        String conclusions="La ejecución documentada permite identificar resultados y conservar la trazabilidad del trabajo. ".repeat(90)+"FIN_CONCLUSIONES_EXTENSAS";
        String improvements="Organizar el seguimiento de actividades y mejorar la documentación de resultados verificables. ".repeat(90)+"FIN_MEJORAS_EXTENSAS";
        var results=data.results().stream().map(result->new ec.edu.uta.utaped.planning.ReportService.Result(result.activityId(),java.math.BigDecimal.valueOf(100),"Resultados comprobados con evidencia de siete páginas.")).toList();
        reports.save(report.id(),email,new ec.edu.uta.utaped.planning.ReportService.Save(report.rowVersion(),report.title(),background,conclusions,improvements,results,List.of()));
        var artifact=artifacts.generate(report.id(),email,new Generate(plans.get(report.id(),email).rowVersion()));
        byte[] unsigned=artifacts.content(report.id(),artifact.id(),email);
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/t2-long.pdf"),unsigned);
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(unsigned)) {
            assertEquals(pdf.getNumberOfPages(),artifact.pageCount());assertEquals(artifact.pages().size(),artifact.pageCount());assertTrue(artifact.pageCount()>15);
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);
            for(String marker:List.of("FIN_ANTECEDENTES_EXTENSOS","FIN_CONCLUSIONES_EXTENSAS","FIN_MEJORAS_EXTENSAS")) assertTrue(text.contains(marker),marker);
            assertFalse(text.contains("@@PN@@"));assertEquals(7,artifact.pages().stream().filter(page->page.kind().equals("ANNEX")).count());
            for(var slot:artifact.signatureSlots()) {
                assertEquals(slot.pageIndex()+1,slot.pageNumber());assertNotEquals("ANNEX",artifact.pages().get(slot.pageIndex()).kind());assertNotNull(slot.bounds());
                var page=pdf.getPage(slot.pageIndex());assertTrue(slot.bounds().x()+slot.bounds().width()<=page.getMediaBox().getWidth()+1);assertTrue(slot.bounds().y()+slot.bounds().height()<=page.getMediaBox().getHeight()+1);
            }
            var stripper=new org.apache.pdfbox.text.PDFTextStripper();
            for(String section:List.of("ANTECEDENTES","CONCLUSIONES","OPORTUNIDADES DE MEJORA")) {
                int actual=-1;
                for(int page=0;page<pdf.getNumberOfPages();page++) { stripper.setStartPage(page+1);stripper.setEndPage(page+1);if(stripper.getText(pdf).lines().anyMatch(line->line.strip().replaceAll("^\\d+[.\\s]*", "").equals(section))) {actual=page+1;break;} }
                assertTrue(actual>0,section);assertTrue(text.matches("(?s).*"+section+"\\s*\\.{3,}\\s*"+actual+".*"),section+" index");
            }
        }
        var preparation=signatures.state(report.id(),artifact.id(),email,true);
        signatures.sign(report.id(),artifact.id(),email,preparation.preparation().rowVersion(),preparation.preparation().pdfHash(),UUID.randomUUID(),signingBody(authorFixture,"Test-only-signing-password"),true);
        UUID review=reviews.inbox(reviewerEmail).stream().filter(task->task.documentId().equals(report.id())).findFirst().orElseThrow().reviewId();
        byte[] signed=files.read((UUID)reviews.state(review,email).review().get("current_file_id"));assertArrayEquals(unsigned,Arrays.copyOf(signed,unsigned.length));approve(review);
        assertArrayEquals(signed,reviews.content(review,email));assertEquals(baseHash,reviews.state(baseReview,email).pdfHash());assertEquals(base.rowVersion(),plans.get(id,email).rowVersion());
        assertEquals(artifact.pageCount(),reviews.state(review,email).pageCount());
        assertTrue(reviews.page(review,artifact.pageCount()-1,email).length>1000);
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
        try(var rendered=org.apache.pdfbox.Loader.loadPDF(pdf)) { String text=new org.apache.pdfbox.text.PDFTextStripper().getText(rendered);assertFalse(text.contains("INFORME DE: Informe:"));assertHeaderDateIsInTheRightCell(rendered);assertTrue(text.replaceAll("\\s+"," ").contains("UNIDAD ACADÉMICA: Facultad de Ingeniería en Sistemas, Electrónica e Industrial"));assertFalse(text.contains("UNIDAD ACADÉMICA / ADMINISTRATIVA"));assertTrue(text.contains("ANTECEDENTES"));assertTrue(text.contains("CONCLUSIONES"));assertFalse(text.contains("@@SIG"));for(int i=0;i<rendered.getNumberOfPages();i++) java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/t2-page-"+i+".png"),artifacts.renderPage(pdf,i)); }
    }
    @Test void executionReportsCutThePlanAreNumberedAndProduceTheirOwnOfficialPdf() throws Exception {
        UUID review=submit(false);approve(review);var activity=activities.get(id,email).activities().getFirst();
        evidence.upload(id,activity.id(),0,0,email,new org.springframework.mock.web.MockMultipartFile("file","evidencia.pdf","application/pdf",pdf(1,false,false)));
        assertEquals(id,executions.options(email).getFirst().id());assertTrue(executions.options(reviewerEmail).isEmpty());
        var first=executions.create(id,email);assertEquals(first.id(),executions.options(email).getFirst().draftId());assertEquals("T3",first.documentType());assertTrue(first.title().startsWith("Informe de ejecución I —"),first.title());assertEquals(first.id(),executions.create(id,email).id());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->executions.create(id,reviewerEmail)).getStatusCode().value());
        var result=new ec.edu.uta.utaped.planning.ReportService.Result(activity.id(),java.math.BigDecimal.valueOf(75),"Ejecución parcial con evidencia.");
        java.util.function.Function<String,ec.edu.uta.utaped.planning.ExecutionReportService.Save> save=link->new ec.edu.uta.utaped.planning.ExecutionReportService.Save(plans.get(first.id(),email).rowVersion(),first.title(),List.of(activity.id()),List.of(result),
            "Los resultados del corte muestran avance sostenido.","Compromiso del equipo.","Seguimiento limitado.","Herramientas digitales.","Limitaciones presupuestarias.","Se ejecutó el 75 % de lo previsto.","Mantener el seguimiento mensual.",
            List.of(new ec.edu.uta.utaped.planning.ExecutionReportService.Action("Retraso en evidencias","Calendario de carga semanal")),link);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->executions.save(first.id(),email,save.apply("http://inseguro.example.invalid/evidencias"))).getStatusCode().value());
        assertFalse(artifacts.readiness(first.id(),email).ready());
        executions.save(first.id(),email,save.apply("https://repositorio.example.invalid/evidencias"));
        assertTrue(artifacts.readiness(first.id(),email).ready(),artifacts.readiness(first.id(),email).blockers().toString());
        var artifact=artifacts.generate(first.id(),email,new Generate(plans.get(first.id(),email).rowVersion()));assertEquals(1,artifact.pages().stream().filter(p->p.kind().equals("ANNEX")).count());
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(artifacts.content(first.id(),artifact.id(),email))) {
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).replaceAll("\\s+"," ");
            for(String expected:List.of("INFORME DE EJECUCIÓN I","RESULTADOS DEL PLAN DE ACCIÓN","ANÁLISIS FODA","Compromiso del equipo.","75 %","Calendario de carga semanal","https://repositorio.example.invalid/evidencias","UTA-SGC-A-1-1-P4-T2","FIRMAS DE RESPONSABILIDAD"))
                assertTrue(text.contains(expected),expected);
            assertFalse(text.contains("@@"));
            var pageText=new org.apache.pdfbox.text.PDFTextStripper();pageText.setStartPage(1);pageText.setEndPage(1);assertFalse(pageText.getText(pdf).contains("Índice de Contenidos"));
            pageText.setStartPage(2);pageText.setEndPage(2);assertTrue(pageText.getText(pdf).contains("Índice de Contenidos"));
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/t3-execution.pdf"),artifacts.content(first.id(),artifact.id(),email));
        var signing=signatures.state(first.id(),artifact.id(),email,true);assertTrue(signing.signingEnabled(),signing.signingBlockers().toString());
        signatures.sign(first.id(),artifact.id(),email,signing.preparation().rowVersion(),signing.preparation().pdfHash(),UUID.randomUUID(),signingBody(authorFixture,"Test-only-signing-password"),true);
        approve(reviews.inbox(reviewerEmail).stream().filter(t->t.documentId().equals(first.id())).findFirst().orElseThrow().reviewId());
        assertEquals("APPROVED",plans.get(first.id(),email).documentState());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->lifecycle.nextVersion(first.id(),email,new ec.edu.uta.utaped.planning.DocumentLifecycleService.Change(plans.get(first.id(),email).rowVersion(),"No se versiona"))).getStatusCode().value());
        var second=executions.create(id,email);assertNotEquals(first.id(),second.id());assertTrue(second.title().startsWith("Informe de ejecución II —"),second.title());assertEquals(2,executions.data(second.id(),email).sequenceNumber());
        var metric=reporting.get(email,group,period,false).rows().getFirst();assertEquals(0,java.math.BigDecimal.valueOf(75).compareTo(metric.declaredExecution()));assertEquals(0,java.math.BigDecimal.valueOf(100).compareTo(metric.evidenceCoverage()));
        String admin="discard-auditor@example.invalid";jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,'unused','ADMIN',false)",UUID.randomUUID(),admin,"Administración de descartados");
        var finalDraft=reports.create(id,email);lifecycle.archive(finalDraft.id(),email,new ec.edu.uta.utaped.planning.DocumentLifecycleService.Change(finalDraft.rowVersion(),"Descartar Informe final de prueba"));lifecycle.archive(second.id(),email,new ec.edu.uta.utaped.planning.DocumentLifecycleService.Change(second.rowVersion(),"Descartar corte de prueba"));
        assertNull(reports.get(finalDraft.id(),admin).evidence());assertNull(executions.get(second.id(),admin).evidence());assertFalse(artifacts.readiness(finalDraft.id(),admin).ready());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->plans.get(id,admin)).getStatusCode().value());
    }
    @org.springframework.beans.factory.annotation.Autowired ec.edu.uta.utaped.planning.FormalSubmissionService submissions;
    @org.springframework.beans.factory.annotation.Autowired ec.edu.uta.utaped.institution.PositionService positions;
    @Test void approvedQuipuxGateRequiresCoordinatorProofAndNeverChangesSignedBytes() throws Exception {
        requiresQuipux=true;UUID review=submit(false);approve(review);byte[] frozen=reviews.content(review,email);
        assertTrue(submissions.state(id,email).required());assertFalse(evidence.get(id,email).activities().getFirst().canUpload());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->reports.create(id,email)).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->periodArchives.close(period,email,new ec.edu.uta.utaped.institution.PeriodArchiveService.Close(1L,"Constancia pendiente"))).getStatusCode().value());
        var file=new org.springframework.mock.web.MockMultipartFile("file","constancia.pdf","application/pdf",pdf(1,false,false));
        assertEquals(403,assertThrows(ResponseStatusException.class,()->submissions.register(id,email,file)).getStatusCode().value());
        UUID coordinator=UUID.randomUUID();String coordinatorEmail="proof-coordinator@example.invalid";var plan=plans.get(id,email);
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,'unused','USER',false)",coordinator,coordinatorEmail,"Coordinación de constancias");
        jdbc.update("INSERT INTO scope_member(scope_id,user_id,membership_role,position_label) VALUES (?,?,'COORDINATOR','Coordinación')",plan.scopeId(),coordinator);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?)",id,coordinator,user);
        assertTrue(submissions.register(id,coordinatorEmail,file).registered());assertArrayEquals(frozen,reviews.content(review,email));
        assertNotNull(reports.create(id,email));assertTrue(submissions.content(id,coordinatorEmail).length>0);
    }
    @Test void closedDocumentHistoryRequiresAnExplicitCurrentPositionAndArchivedDraftIsAdminOnly() throws Exception {
        UUID review=submit(false);approve(review);byte[] frozen=reviews.content(review,email);
        periodArchives.close(period,email,new ec.edu.uta.utaped.institution.PeriodArchiveService.Close(1L,"Cierre para control histórico"));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->reviews.content(review,reviewerEmail)).getStatusCode().value());
        UUID current=UUID.randomUUID();jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) SELECT ?,'Período vigente',starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on FROM academic_period WHERE id=?",current,period);
        UUID position=positions.create(email,new ec.edu.uta.utaped.institution.PositionService.PositionInput("HISTORY_AUTHORITY","Autoridad con historial",false));
        positions.assign(current,email,new ec.edu.uta.utaped.institution.PositionService.AssignmentInput(position,null,reviewer));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->reviews.content(review,reviewerEmail)).getStatusCode().value());
        positions.historyPermission(position,email,new ec.edu.uta.utaped.institution.PositionService.HistoryPermission(true,0L));
        assertArrayEquals(frozen,reviews.content(review,reviewerEmail));
        positions.historyPermission(position,email,new ec.edu.uta.utaped.institution.PositionService.HistoryPermission(false,1L));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->reviews.content(review,reviewerEmail)).getStatusCode().value());
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
    @Test void businessDaysSkipWeekendsAndRegisteredHolidays() {
        jdbc.update("INSERT INTO planning_holiday(holiday_date,label) VALUES ('2026-10-12','Feriado de prueba')");
        assertEquals(java.time.LocalDate.of(2026,10,15),calendar.addBusinessDays(java.time.LocalDate.of(2026,10,9),3));
        assertEquals(java.time.LocalDate.of(2026,10,9),calendar.addBusinessDays(java.time.LocalDate.of(2026,10,8),1));
    }
    @Test void preparationReopeningIsCoordinatorOnlyBoundedByBusinessDaysAndReopensEditing() {
        signingAdmin();
        UUID scope=organization.createScope(email,new OrganizationService.ScopeInput(group,period,null));
        organization.member(scope,email,new OrganizationService.MemberInput(user,"MEMBER","Elaborador",true,0L));organization.principal(scope,email,new OrganizationService.PrincipalInput(user,0L));
        jdbc.update("UPDATE work_plan SET scope_id=? WHERE id=?",scope,id);jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?)",id,user,user);
        UUID coordinator=UUID.randomUUID();String coordinatorEmail="coordinator-window@example.invalid";
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Coordinación de prueba','unused','USER',false)",coordinator,coordinatorEmail);
        organization.member(scope,email,new OrganizationService.MemberInput(coordinator,"COORDINATOR","Coordinación",true,0L));
        var today=java.time.LocalDate.now(java.time.ZoneId.of("America/Guayaquil"));
        jdbc.update("UPDATE academic_period SET preparation_ends_on=? WHERE id=?",today.minusDays(1),period);
        assertFalse(plans.get(id,email).editable());
        var max=calendar.addBusinessDays(today.minusDays(1),3);var state=extensions.get(scope,coordinatorEmail);
        assertEquals(max,state.maxEndsOn());assertTrue(state.canGrant());assertFalse(extensions.get(scope,email).canGrant());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->extensions.grant(scope,email,new ec.edu.uta.utaped.planning.PreparationExtensionService.Grant(0L,max,"Sin autorización"))).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->extensions.grant(scope,coordinatorEmail,new ec.edu.uta.utaped.planning.PreparationExtensionService.Grant(0L,max.plusDays(1),"Excede el máximo"))).getStatusCode().value());
        var granted=extensions.grant(scope,coordinatorEmail,new ec.edu.uta.utaped.planning.PreparationExtensionService.Grant(0L,max,"Reapertura individual de prueba"));
        assertEquals(max,granted.effectiveEndsOn());assertEquals(1,granted.history().size());assertTrue(plans.get(id,email).editable());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->extensions.grant(scope,coordinatorEmail,new ec.edu.uta.utaped.planning.PreparationExtensionService.Grant(0L,max,"Versión vencida"))).getStatusCode().value());
        assertEquals(today.minusDays(1),jdbc.queryForObject("SELECT preparation_ends_on FROM academic_period WHERE id=?",java.time.LocalDate.class,period));
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
        boolean found=false;try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(periodArchives.custody(period,email)))) {java.util.zip.ZipEntry entry;while((entry=zip.getNextEntry())!=null) {byte[] bytes=zip.readAllBytes();if(entry.getName().equals("documents/"+id+"/manifest.json")) {String manifest=new String(bytes,java.nio.charset.StandardCharsets.UTF_8);assertTrue(manifest.contains(first.externalUrl()));assertTrue(manifest.contains(second.externalUrl()));found=true;}}}assertTrue(found);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->evidence.reference(id,activity.id(),1,email,new ec.edu.uta.utaped.planning.EvidenceService.Reference(2,"Acta","https://example.invalid/acta"))).getStatusCode().value());
    }
}
