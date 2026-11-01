package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import java.util.UUID;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;
import ec.edu.uta.utaped.documents.AttachmentModels;

class ImageAttachmentIntegrationTests extends DocumentIntegrationTests {
    @org.springframework.beans.factory.annotation.Autowired ec.edu.uta.utaped.institution.PeriodArchiveService archives;
    @Test void imageAnnexKeepsItsOriginalAndProducesAnIndependentA4PdfWithIdempotentRetries() throws Exception {
        var state=attachments.settings(id,email,new AttachmentModels.Settings(version(),true,false));
        var image=new BufferedImage(160,90,BufferedImage.TYPE_INT_RGB);var output=new ByteArrayOutputStream();ImageIO.write(image,"png",output);
        byte[] original=output.toByteArray();var upload=new MockMultipartFile("file","captura.png","image/png",original);var key=UUID.randomUUID();
        var saved=attachments.upload(id,email,state.rowVersion(),key,"Captura institucional",upload);assertEquals(1,saved.items().size());
        var item=saved.items().getFirst();assertEquals("image/png",item.originalMediaType());assertEquals("captura.png",item.sourceName());
        assertArrayEquals(original,attachments.original(id,item.id(),email));
        assertEquals(saved,attachments.upload(id,email,state.rowVersion(),key,"Captura institucional",upload));
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(attachments.content(id,item.id(),email))) { assertEquals(1,pdf.getNumberOfPages());assertEquals(org.apache.pdfbox.pdmodel.common.PDRectangle.A4.getWidth(),pdf.getPage(0).getMediaBox().getWidth(),.1f); }
        assertEquals(404,assertThrows(ResponseStatusException.class,()->attachments.original(UUID.randomUUID(),item.id(),email)).getStatusCode().value());
        var invalid=new MockMultipartFile("file","falso.png","image/png","Texto sin imagen".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->attachments.upload(id,email,saved.rowVersion(),UUID.randomUUID(),"Inválido",invalid)).getStatusCode().value());
        assertEquals(1,attachments.get(id,email).items().size());
        jdbc.update("UPDATE app_user SET system_role='ADMIN' WHERE id=?",user);
        long periodVersion=jdbc.queryForObject("SELECT row_version FROM academic_period WHERE id=?",Long.class,period);
        archives.close(period,email,new ec.edu.uta.utaped.institution.PeriodArchiveService.Close(periodVersion,"Custodia de anexo de imagen"));
        boolean found=false;try(var zip=new java.util.zip.ZipInputStream(new java.io.ByteArrayInputStream(archives.custody(period,email)))) { java.util.zip.ZipEntry entry;while((entry=zip.getNextEntry())!=null) if(entry.getName().endsWith(item.originalFileId()+".png")) { assertArrayEquals(original,zip.readAllBytes());found=true; } }assertTrue(found);

    }
    @Test void generatedLinksAndTablesAreBoundedPaginatedAndIdempotent() throws Exception {
        var state=attachments.settings(id,email,new AttachmentModels.Settings(version(),true,false));
        var input=new AttachmentModels.GeneratedInput(state.rowVersion(),UUID.randomUUID(),"Referencia externa","URL","https://repositorio.example.invalid/referencia",null);
        var linked=attachments.generate(id,email,input);assertEquals(linked,attachments.generate(id,email,input));
        var attachment=linked.items().getFirst();
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(attachments.content(id,attachment.id(),email))) {
            assertTrue(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).contains("https://repositorio.example.invalid/referencia"));assertFalse(pdf.getPage(0).getAnnotations().isEmpty());
        }
        assertEquals(400,assertThrows(ResponseStatusException.class,()->attachments.generate(id,email,new AttachmentModels.GeneratedInput(linked.rowVersion(),UUID.randomUUID(),"No permitido","URL","javascript:alert(1)",null))).getStatusCode().value());
        var rows=new java.util.ArrayList<java.util.List<String>>();rows.add(java.util.List.of("Actividad","Responsable"));
        for(int i=0;i<99;i++) rows.add(java.util.List.of("Fila "+i+" de la planificación institucional. "+"Contenido de seguimiento. ".repeat(5),"Persona responsable "+i));
        var table=attachments.generate(id,email,new AttachmentModels.GeneratedInput(linked.rowVersion(),UUID.randomUUID(),"Consolidado de actividades","TABLE",null,rows));
        var item=table.items().getLast();
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(attachments.content(id,item.id(),email))) {
            assertTrue(pdf.getNumberOfPages()>1);String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains("Fila 98"));
            for(var page:pdf.getPages()) assertEquals(org.apache.pdfbox.pdmodel.common.PDRectangle.A4.getWidth(),page.getMediaBox().getWidth(),.1f);
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImageWithDPI(0,110),"png",java.nio.file.Path.of("target/document-qa/generated-table.png").toFile());
        }
    }
}
