package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.documents.*;
import ec.edu.uta.utaped.planning.*;
import java.io.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.util.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.encryption.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentIntegrationTests {
    @Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;@Autowired WorkPlanService plans;
    @Autowired AttachmentService attachments;@Autowired ArtifactService artifacts;@Autowired ActivityService activities;
    @Autowired StoredFiles files;@Autowired PdfValidation validation;@Autowired PlatformTransactionManager transactions;
    @Autowired ec.edu.uta.utaped.signing.CertificateBindings bindings;
    @Autowired ec.edu.uta.utaped.signing.AuthorSignatureService signatures;
    @Value("${app.documents.storage-root}") String root;
    UUID user,group,period,id;String email="document@example.invalid";
    @BeforeEach void setup() {
        assertEquals("utaped_test",jdbc.queryForObject("SELECT current_database()",String.class));
        jdbc.execute("TRUNCATE stored_file,planning_catalog,planning_holiday,app_user,institutional_group,academic_period,auth_throttle,SPRING_SESSION CASCADE");
        user=UUID.randomUUID();group=UUID.randomUUID();period=UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Docente de prueba',?,'USER',false)",user,email,encoder.encode("Test-only-password-2026"));
        jdbc.update("INSERT INTO institutional_group(id,name,group_type,collective_label) VALUES (?,'Comisión de prueba','COMMISSION','Responsable de la comisión')",group);
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')",user,group);
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Período de prueba',CURRENT_DATE-10,CURRENT_DATE+100,CURRENT_DATE-10,CURRENT_DATE+10,CURRENT_DATE+11,CURRENT_DATE+20)",period);
        id=plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan de Trabajo de prueba")).id();
    }
    long version() { return plans.get(id,email).rowVersion(); }
    void complete(String justification,int count) {
        plans.update(id,email,new WorkPlanModels.Update(version(),"Plan de Trabajo de prueba","FISEI","Ingeniería de Software",justification,"Cumplir las actividades académicas planificadas."));
        var rows=new ArrayList<ActivityModels.Activity>();var date=LocalDate.now(java.time.ZoneId.of("America/Guayaquil"));
        for(int i=0;i<count;i++) rows.add(new ActivityModels.Activity(UUID.randomUUID(),null,"Actividad "+(i+1)+". Verificar el cumplimiento de la planificación institucional y registrar sus resultados.","OTHER",false,date,date,List.of(user),true,
            List.of(new ActivityModels.Choice(null,"Recursos institucionales",null)),List.of(new ActivityModels.Choice(null,"Acta de seguimiento",null))));
        activities.save(id,email,new ActivityModels.Save(version(),"Planificación de la comisión",rows));
    }
    byte[] pdf(int count,boolean encrypted,boolean active) throws Exception {
        try(var document=new PDDocument()) {
            for(int i=0;i<count;i++) { var p=new PDPage(PDRectangle.A4);document.addPage(p);try(var stream=new PDPageContentStream(document,p)) { stream.beginText();stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA),12);stream.newLineAtOffset(60,760);stream.showText("Anexo de prueba "+(i+1));stream.endText(); } }
            if(encrypted) document.protect(new StandardProtectionPolicy("secret-owner","secret-user",new AccessPermission()));
            if(active) document.getDocumentCatalog().setOpenAction(new PDActionJavaScript("app.alert('test')"));
            var out=new ByteArrayOutputStream();document.save(out);return out.toByteArray();
        }
    }
    AttachmentModels.State upload(String title,UUID key,byte[] bytes,long version) { return attachments.upload(id,email,version,key,title,new MockMultipartFile("file","../../original.pdf","application/pdf",bytes)); }
    @Test void attachmentValidationOrderingIsolationIdempotenceAndConcurrency() throws Exception {
        assertFalse(artifacts.readiness(id,email).ready());
        attachments.settings(id,email,new AttachmentModels.Settings(version(),true,false));
        byte[] pdf=pdf(2,false,false);UUID key=UUID.randomUUID();long stale=version();
        var first=upload("Primero",key,pdf,stale);assertEquals("Anexo A",first.items().getFirst().label());assertEquals("original.pdf",first.items().getFirst().originalName());
        assertEquals(first.rowVersion(),upload("Primero",key,pdf,stale).rowVersion());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->upload("Otro",UUID.randomUUID(),pdf,stale)).getStatusCode().value());
        var second=upload("Segundo",UUID.randomUUID(),pdf,version());var a=second.items().get(0);var b=second.items().get(1);
        attachments.reorder(id,email,new AttachmentModels.Reorder(version(),List.of(b.id(),a.id())));
        assertEquals("Segundo",attachments.get(id,email).items().getFirst().title());
        attachments.remove(id,b.id(),email,version());assertEquals("Anexo A",attachments.get(id,email).items().getFirst().label());
        assertArrayEquals(pdf,attachments.content(id,a.id(),email));
        UUID other=plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Otro Plan")).id();
        assertEquals(404,assertThrows(ResponseStatusException.class,()->attachments.content(other,a.id(),email)).getStatusCode().value());
        assertThrows(ResponseStatusException.class,()->attachments.reorder(id,email,new AttachmentModels.Reorder(version(),List.of(a.id(),a.id()))));
        assertThrows(ResponseStatusException.class,()->attachments.settings(id,email,new AttachmentModels.Settings(version(),false,false)));
        long row=version();assertThrows(ResponseStatusException.class,()->upload("Falso",UUID.randomUUID(),"Not a PDF".getBytes(),row));assertEquals(row,version());
        assertThrows(ResponseStatusException.class,()->validation.pages(pdf(1,true,false)));
        assertThrows(ResponseStatusException.class,()->validation.pages(pdf(1,false,true)));
        jdbc.update("UPDATE academic_period SET preparation_ends_on=CURRENT_DATE-1 WHERE id=?",period);
        assertEquals(403,assertThrows(ResponseStatusException.class,()->attachments.remove(id,a.id(),email,version())).getStatusCode().value());
        jdbc.update("DELETE FROM membership WHERE user_id=? AND group_id=?",user,group);
        assertThrows(ResponseStatusException.class,()->attachments.content(id,a.id(),email));
    }
    @Test void immutableTemplatePdfActualPaginationAndHistoricalContent() throws Exception {
        complete("La comisión requiere planificar las actividades y conservar evidencias de cumplimiento.",2);
        attachments.settings(id,email,new AttachmentModels.Settings(version(),true,true));var state=upload("Actas",UUID.randomUUID(),pdf(2,false,false),version());
        assertTrue(artifacts.readiness(id,email).ready());long before=version();
        var artifact=artifacts.generate(id,email,new ArtifactModels.Generate(before));byte[] original=artifacts.content(id,artifact.id(),email);
        Files.createDirectories(Path.of("target/document-qa"));Files.write(Path.of("target/document-qa/t1-short.pdf"),original);
        assertEquals(before,version());assertEquals(artifact.id(),artifacts.generate(id,email,new ArtifactModels.Generate(before)).id());
        try(var document=Loader.loadPDF(original)) {
            assertEquals(document.getNumberOfPages(),artifact.pageCount());String text=new PDFTextStripper().getText(document);
            assertTrue(text.contains("FISEI"),text);assertTrue(text.contains("Actas"),text);assertTrue(text.replaceAll("\\s+"," ").contains("Responsable de la comisión"),text);assertTrue(text.contains("1.0"),text);
            assertFalse(text.contains("@@PN@@"));
            assertFalse(text.contains("vX.0"));assertFalse(text.contains("Revisado por:"));
            for(var page:artifact.pages()) { assertEquals(page.index()+1,page.number());assertTrue(Math.abs(Math.min(page.width(),page.height())-PDRectangle.A4.getWidth())<1);assertTrue(Math.abs(Math.max(page.width(),page.height())-PDRectangle.A4.getHeight())<1); }
            assertEquals(2,artifact.pages().stream().filter(p->p.kind().equals("ANNEX")).count());
            var slot=artifact.signatureSlots().getFirst();var stripper=new PDFTextStripper();stripper.setStartPage(slot.pageNumber());stripper.setEndPage(slot.pageNumber());assertTrue(stripper.getText(document).contains("Docente de prueba"));
        }
        assertTrue(artifacts.page(id,artifact.id(),0,email).length>1000);
        attachments.remove(id,state.items().getFirst().id(),email,version());
        assertArrayEquals(original,artifacts.content(id,artifact.id(),email));assertFalse(artifacts.list(id,email).getFirst().current());
        assertFalse(artifacts.readiness(id,email).ready());
        Files.createDirectories(Path.of("target/document-qa"));Files.write(Path.of("target/document-qa/t1-short.pdf"),original);
    }
    @Test void longContentExpandsPagesAndIndexesFollowRealComposition() throws Exception {
        complete("Justificación académica de la planificación y seguimiento de las actividades institucionales. ".repeat(100),35);
        var artifact=artifacts.generate(id,email,new ArtifactModels.Generate(version()));byte[] bytes=artifacts.content(id,artifact.id(),email);
        Files.createDirectories(Path.of("target/document-qa"));Files.write(Path.of("target/document-qa/t1-long.pdf"),bytes);
        try(var pdf=Loader.loadPDF(bytes)) {
            assertTrue(pdf.getNumberOfPages()>7);var stripper=new PDFTextStripper();String text=stripper.getText(pdf);assertTrue(text.contains("Actividad 35."));
            int matrix=-1;for(int i=0;i<pdf.getNumberOfPages();i++) { stripper.setStartPage(i+1);stripper.setEndPage(i+1);if(stripper.getText(pdf).lines().anyMatch(l->l.trim().replaceAll("^\\d+[.\\s]*","").equals("MATRIZ DE ACTIVIDADES"))) matrix=i+1; }
            assertTrue(matrix>0);assertTrue(text.matches("(?s).*MATRIZ DE ACTIVIDADES \\.{3,} "+matrix+".*"),text);
            assertTrue(artifact.signatureSlots().getFirst().pageNumber()>matrix);
        }
        Files.createDirectories(Path.of("target/document-qa"));Files.write(Path.of("target/document-qa/t1-long.pdf"),bytes);
    }
    @Test void workflowSnapshotStalenessAndHistoricalPreparationDoNotAssignOrApprove() throws Exception {
        complete("Planificar con flujo conservado.",1);
        var initial=artifacts.generate(id,email,new ArtifactModels.Generate(version()));
        var preparation=artifacts.signaturePreparation(id,initial.id(),email);
        assertNull(preparation.workflow());assertFalse(preparation.signingEnabled());assertTrue(preparation.artifactCurrent());
        byte[] original=artifacts.content(id,initial.id(),email);
        UUID configuration=UUID.randomUUID(),revision=UUID.randomUUID(),stage=UUID.randomUUID(),collegiate=UUID.randomUUID();
        var definition=new ec.edu.uta.utaped.workflow.WorkflowModels.Definition("Flujo de prueba",List.of(
            new ec.edu.uta.utaped.workflow.WorkflowModels.Stage(stage,"Revisión técnica","REVIEW","GROUP_ROLE",List.of(),"MEMBER","",true),
            new ec.edu.uta.utaped.workflow.WorkflowModels.Stage(collegiate,"Órgano de prueba","APPROVE","COLLEGIATE",List.of(),null,"Órgano configurado",false)));
        var json=tools.jackson.databind.json.JsonMapper.builder().build();
        jdbc.update("INSERT INTO workflow_configuration(id,group_id,document_type,draft) VALUES (?,?,'T1',?::jsonb)",configuration,group,json.writeValueAsString(definition));
        jdbc.update("INSERT INTO workflow_revision(id,configuration_id,revision_number,definition,actor_id) VALUES (?,?,1,?::jsonb,?)",revision,configuration,json.writeValueAsString(definition),user);
        jdbc.update("UPDATE workflow_configuration SET current_revision_id=? WHERE id=?",revision,configuration);
        assertFalse(artifacts.signaturePreparation(id,initial.id(),email).artifactCurrent());
        var configured=artifacts.generate(id,email,new ArtifactModels.Generate(version()));
        var captured=artifacts.signaturePreparation(id,configured.id(),email);
        assertEquals(revision,captured.workflow().revisionId());assertEquals(user,captured.workflow().stages().getFirst().participants().getFirst().id());
        assertTrue(captured.workflow().stages().get(1).participants().isEmpty());assertFalse(captured.signingEnabled());assertEquals(64,captured.pdfHash().length());
        long events=jdbc.queryForObject("SELECT count(*) FROM audit_event",Long.class);
        jdbc.update("UPDATE app_user SET display_name='Nombre cambiado' WHERE id=?",user);
        var historic=artifacts.signaturePreparation(id,configured.id(),email);
        assertEquals("Docente de prueba",historic.workflow().stages().getFirst().participants().getFirst().name());assertFalse(historic.artifactCurrent());
        assertArrayEquals(original,artifacts.content(id,initial.id(),email));assertEquals("DRAFT",plans.get(id,email).documentState());
        assertEquals(events,jdbc.queryForObject("SELECT count(*) FROM audit_event",Long.class));
        jdbc.update("UPDATE workflow_configuration SET current_revision_id=NULL WHERE id=?",configuration);
        assertEquals(revision,artifacts.signaturePreparation(id,configured.id(),email).workflow().revisionId());
        // Historical snapshots before this delivery deserialize without the optional workflow field.
        jdbc.update("UPDATE document_artifact SET snapshot=snapshot-'workflow' WHERE id=?",initial.id());
        assertNull(artifacts.signaturePreparation(id,initial.id(),email).workflow());
    }
    @Test void signaturePreparationRejectsOtherDocumentsAdminAndRevokedMembershipAndChecksIntegrity() throws Exception {
        complete("Preparación privada.",1);var artifact=artifacts.generate(id,email,new ArtifactModels.Generate(version()));
        UUID other=plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Otro Plan")).id();
        assertEquals(404,assertThrows(ResponseStatusException.class,()->artifacts.signaturePreparation(other,artifact.id(),email)).getStatusCode().value());
        UUID admin=UUID.randomUUID();jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,'other-admin@example.invalid','Admin',?,'ADMIN',false)",admin,encoder.encode("Test-only-password-2026"));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->artifacts.signaturePreparation(id,artifact.id(),"other-admin@example.invalid")).getStatusCode().value());
        jdbc.update("DELETE FROM membership WHERE user_id=? AND group_id=?",user,group);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->artifacts.signaturePreparation(id,artifact.id(),email)).getStatusCode().value());
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')",user,group);
        var key=jdbc.queryForObject("SELECT f.storage_key FROM stored_file f JOIN document_artifact a ON a.file_id=f.id WHERE a.id=?",UUID.class,artifact.id());
        Files.write(Path.of(root).resolve(key.toString()),new byte[]{0});
        assertEquals(409,assertThrows(ResponseStatusException.class,()->artifacts.signaturePreparation(id,artifact.id(),email)).getStatusCode().value());
    }
    @Test void storageRollbackAndIntegrityAndLimitChecks() throws Exception {
        var tx=new TransactionTemplate(transactions);var counts=Files.list(Path.of(root));long before;try(counts) { before=counts.count(); }
        assertThrows(IllegalStateException.class,()->tx.execute(status->{ files.put(new byte[]{1,2,3},"test.pdf",1);throw new IllegalStateException("rollback"); }));
        try(var paths=Files.list(Path.of(root))) { assertEquals(before,paths.count()); }
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM stored_file",Integer.class));
        attachments.settings(id,email,new AttachmentModels.Settings(version(),true,false));var state=upload("Uno",UUID.randomUUID(),pdf(1,false,false),version());
        var fileId=state.items().getFirst().fileId();var key=jdbc.queryForObject("SELECT storage_key FROM stored_file WHERE id=?",UUID.class,fileId);
        Files.write(Path.of(root).resolve(key.toString()),new byte[]{0});assertEquals(409,assertThrows(ResponseStatusException.class,()->files.read(fileId)).getStatusCode().value());
        byte[] oversized=new byte[10485761];assertEquals(413,assertThrows(ResponseStatusException.class,()->upload("Grande",UUID.randomUUID(),oversized,version())).getStatusCode().value());
    }
    record SigningFixture(java.security.KeyPair key,java.security.cert.X509Certificate certificate) {}
    SigningFixture signingFixture() throws Exception {
        var key=java.security.KeyPairGenerator.getInstance("RSA");key.initialize(2048);var pair=key.generateKeyPair();
        var name=new org.bouncycastle.asn1.x500.X500Name("CN=Test-only signer");var now=java.time.Instant.now();
        var builder=new org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder(name,new java.math.BigInteger(120,new java.security.SecureRandom()),Date.from(now.minusSeconds(60)),Date.from(now.plusSeconds(86400)),name,pair.getPublic());
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.basicConstraints,true,new org.bouncycastle.asn1.x509.BasicConstraints(false));
        builder.addExtension(org.bouncycastle.asn1.x509.Extension.keyUsage,true,new org.bouncycastle.asn1.x509.KeyUsage(org.bouncycastle.asn1.x509.KeyUsage.digitalSignature));
        var provider=new org.bouncycastle.jce.provider.BouncyCastleProvider();
        var certificate=new org.bouncycastle.cert.jcajce.JcaX509CertificateConverter().setProvider(provider).getCertificate(builder.build(new org.bouncycastle.operator.jcajce.JcaContentSignerBuilder("SHA256withRSA").setProvider(provider).build(pair.getPrivate())));
        return new SigningFixture(pair,certificate);
    }
    byte[] signingBody(SigningFixture fixture,String suppliedPassword) throws Exception {
        char[] password="Test-only-signing-password".toCharArray();var store=java.security.KeyStore.getInstance("PKCS12");store.load(null,password);
        store.setKeyEntry("signer",fixture.key().getPrivate(),password,new java.security.cert.Certificate[]{fixture.certificate()});var out=new ByteArrayOutputStream();store.store(out,password);Arrays.fill(password,'\0');
        var encoded=suppliedPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8);var container=out.toByteArray();
        var result=java.nio.ByteBuffer.allocate(4+encoded.length+container.length).putInt(encoded.length).put(encoded).put(container).array();Arrays.fill(encoded,(byte)0);Arrays.fill(container,(byte)0);return result;
    }
    String fingerprint(SigningFixture fixture) throws Exception { return StoredFiles.hash(fixture.certificate().getEncoded()); }
    void signingAdmin() { jdbc.update("UPDATE app_user SET system_role='ADMIN' WHERE id=?",user); }
    @Test void certificateBindingsRequireVerifiedIdentityAndPreserveRevokedHistory() throws Exception {
        var fixture=signingFixture();
        assertEquals(403,assertThrows(ResponseStatusException.class,()->bindings.list(user,email)).getStatusCode().value());signingAdmin();
        assertEquals(400,assertThrows(ResponseStatusException.class,()->bindings.register(user,email,new ec.edu.uta.utaped.signing.CertificateBindings.Register(fingerprint(fixture),false,"Test identity evidence",null))).getStatusCode().value());
        var bound=bindings.register(user,email,new ec.edu.uta.utaped.signing.CertificateBindings.Register(fingerprint(fixture),true,"Test identity evidence",null));
        assertEquals(bound.id(),bindings.register(user,email,new ec.edu.uta.utaped.signing.CertificateBindings.Register(fingerprint(fixture),true,"Test identity evidence",bound.id())).id());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->bindings.register(user,email,new ec.edu.uta.utaped.signing.CertificateBindings.Register(fingerprint(fixture),true,"Test identity evidence",null))).getStatusCode().value());
        UUID other=UUID.randomUUID();jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,'binding-other@example.invalid','Other',?,'USER',false)",other,encoder.encode("Test-only-password-2026"));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->bindings.register(other,email,new ec.edu.uta.utaped.signing.CertificateBindings.Register(fingerprint(fixture),true,"Other identity evidence",null))).getStatusCode().value());
        bindings.revoke(user,bound.id(),email);assertNull(bindings.current(user));assertFalse(bindings.list(user,email).getFirst().active());
        long events=jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='SIGNING_CERTIFICATE_UNBOUND'",Long.class);bindings.revoke(user,bound.id(),email);
        assertEquals(events,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='SIGNING_CERTIFICATE_UNBOUND'",Long.class));
    }
    @Test void visibleAuthorSignatureIsRealPrivateImmutableAndIdempotent() throws Exception {
        complete("Firma personal del artefacto exacto.",2);attachments.settings(id,email,new AttachmentModels.Settings(version(),true,false));upload("Anexo para ubicación dinámica",UUID.randomUUID(),pdf(2,false,false),version());
        var artifact=artifacts.generate(id,email,new ArtifactModels.Generate(version()));var fixture=signingFixture();signingAdmin();
        var binding=bindings.register(user,email,new ec.edu.uta.utaped.signing.CertificateBindings.Register(fingerprint(fixture),true,"Test public certificate identity",null));
        var state=signatures.state(id,artifact.id(),email,true);assertTrue(state.signingEnabled(),state.signingBlockers().toString());
        byte[] original=artifacts.content(id,artifact.id(),email),body=signingBody(fixture,"Test-only-signing-password");UUID key=UUID.randomUUID();long version=version();
        var signed=signatures.sign(id,artifact.id(),email,version,state.preparation().pdfHash(),key,body,true);assertArrayEquals(new byte[body.length],body);
        byte[] bytes=signatures.content(id,signed.id(),email);assertArrayEquals(original,Arrays.copyOf(bytes,original.length));assertArrayEquals(original,artifacts.content(id,artifact.id(),email));
        assertEquals("NOT_CHECKED",signed.revocationCheck());assertEquals("DRAFT",plans.get(id,email).documentState());
        assertFalse(signatures.state(id,artifact.id(),email,true).signingEnabled());
        try(var pdf=Loader.loadPDF(bytes)) {
            assertEquals(artifact.pageCount(),pdf.getNumberOfPages());assertEquals(1,pdf.getSignatureDictionaries().size());
            var field=(org.apache.pdfbox.pdmodel.interactive.form.PDSignatureField)pdf.getDocumentCatalog().getAcroForm().getField("UTAPED_AUTHOR");assertNotNull(field);assertNotNull(field.getSignature());
            var widget=field.getWidgets().getFirst();assertNotNull(widget.getAppearance().getNormalAppearance());
            var slot=artifact.signatureSlots().getFirst();assertEquals(slot.bounds().x(),widget.getRectangle().getLowerLeftX(),0.1);assertEquals(slot.bounds().width(),widget.getRectangle().getWidth(),0.1);
            assertTrue(pdf.getPage(slot.pageIndex()).getAnnotations().stream().anyMatch(a->a.getCOSObject().equals(widget.getCOSObject())));
            assertFalse(new PDFTextStripper().getText(pdf).contains("@@SIG"));
        }
        var retry=signingBody(fixture,"Test-only-signing-password");assertEquals(signed.id(),signatures.sign(id,artifact.id(),email,version,state.preparation().pdfHash(),key,retry,true).id());assertArrayEquals(new byte[retry.length],retry);
        var reused=signingBody(fixture,"Test-only-signing-password");assertEquals(409,assertThrows(ResponseStatusException.class,()->signatures.sign(id,artifact.id(),email,version,"0".repeat(64),key,reused,true)).getStatusCode().value());assertArrayEquals(new byte[reused.length],reused);
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM signed_document_artifact",Integer.class));
        Files.createDirectories(Path.of("target/document-qa"));Files.write(Path.of("target/document-qa/t1-signed.pdf"),bytes);Files.write(Path.of("target/document-qa/t1-signed-page.png"),signatures.page(id,signed.id(),signed.signaturePageIndex(),email));
        UUID other=plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Other Plan")).id();
        assertEquals(404,assertThrows(ResponseStatusException.class,()->signatures.content(other,signed.id(),email)).getStatusCode().value());
        plans.update(id,email,new WorkPlanModels.Update(version(),"Borrador modificado","FISEI","Ingeniería de Software","Nueva justificación","Nuevo objetivo"));
        assertArrayEquals(bytes,signatures.content(id,signed.id(),email));assertFalse(artifacts.list(id,email).getFirst().current());
        bindings.revoke(user,binding.id(),email);assertArrayEquals(bytes,signatures.content(id,signed.id(),email));
        jdbc.update("DELETE FROM membership WHERE user_id=? AND group_id=?",user,group);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->signatures.content(id,signed.id(),email)).getStatusCode().value());
    }
    @Test void signatureFailuresNeverPersistSecretsArtifactsOrAdvanceState() throws Exception {
        complete("Controles de firma.",1);var artifact=artifacts.generate(id,email,new ArtifactModels.Generate(version()));var fixture=signingFixture();signingAdmin();
        assertFalse(signatures.state(id,artifact.id(),email,true).signingEnabled());
        var bound=bindings.register(user,email,new ec.edu.uta.utaped.signing.CertificateBindings.Register(fingerprint(fixture),true,"Test public identity",null));
        var state=signatures.state(id,artifact.id(),email,true);long filesBefore=jdbc.queryForObject("SELECT count(*) FROM stored_file",Long.class);
        var invalid=signingBody(fixture,"Wrong password");assertEquals(400,assertThrows(ResponseStatusException.class,()->signatures.sign(id,artifact.id(),email,version(),state.preparation().pdfHash(),UUID.randomUUID(),invalid,true)).getStatusCode().value());assertArrayEquals(new byte[invalid.length],invalid);
        var other=signingBody(signingFixture(),"Test-only-signing-password");assertEquals(400,assertThrows(ResponseStatusException.class,()->signatures.sign(id,artifact.id(),email,version(),state.preparation().pdfHash(),UUID.randomUUID(),other,true)).getStatusCode().value());assertArrayEquals(new byte[other.length],other);
        var insecure=signingBody(fixture,"Test-only-signing-password");assertEquals(403,assertThrows(ResponseStatusException.class,()->signatures.sign(id,artifact.id(),email,version(),state.preparation().pdfHash(),UUID.randomUUID(),insecure,false)).getStatusCode().value());assertArrayEquals(new byte[insecure.length],insecure);
        var stale=signingBody(fixture,"Test-only-signing-password");assertEquals(409,assertThrows(ResponseStatusException.class,()->signatures.sign(id,artifact.id(),email,version()+1,state.preparation().pdfHash(),UUID.randomUUID(),stale,true)).getStatusCode().value());assertArrayEquals(new byte[stale.length],stale);
        bindings.revoke(user,bound.id(),email);var revoked=signingBody(fixture,"Test-only-signing-password");assertEquals(409,assertThrows(ResponseStatusException.class,()->signatures.sign(id,artifact.id(),email,version(),state.preparation().pdfHash(),UUID.randomUUID(),revoked,true)).getStatusCode().value());assertArrayEquals(new byte[revoked.length],revoked);
        assertEquals(filesBefore,jdbc.queryForObject("SELECT count(*) FROM stored_file",Long.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM signed_document_artifact",Integer.class));assertEquals("DRAFT",plans.get(id,email).documentState());
    }
}
