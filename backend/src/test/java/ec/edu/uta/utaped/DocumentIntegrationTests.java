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
}
