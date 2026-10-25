package ec.edu.uta.utaped;
import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.documents.ArtifactModels;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
class DocumentStructureIntegrationTests extends DocumentIntegrationTests {
    @Autowired DocumentStructures structures;
    @Autowired DocumentStructureController controller;
    @Test void structureVersionsFreezePerDocumentAndRequiredCustomContentReordersA4WithoutChangingHistoricDocuments() throws Exception {
        var historic=id;jdbc.update("UPDATE app_user SET system_role='ADMIN' WHERE id=?",user);
        var initial=structures.template("T1",email);var ordered=new ArrayList<>(initial.sections());Collections.swap(ordered,0,1);
        var extra=new DocumentStructures.Section("CUSTOM_"+UUID.randomUUID(),"SEGUIMIENTO ADICIONAL",true,"");ordered.add(ordered.size()-2,extra);
        structures.save("T1",email,new DocumentStructures.Save(0L,ordered));
        assertEquals(0,structures.snapshot(historic).revision());
        id=plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan con estructura publicada")).id();
        complete("Justificación de la nueva estructura.",2);attachments.settings(id,email,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(version(),false,false));
        assertFalse(artifacts.readiness(id,email).ready());
        controller.values(id,new DocumentStructures.Values(version(),Map.of(extra.key(),"Seguimiento institucional verificado.")),()->email);
        assertTrue(artifacts.readiness(id,email).ready(),artifacts.readiness(id,email).blockers().toString());
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));
        var artifact=artifacts.generate(id,email,new ArtifactModels.Generate(version()));byte[] bytes=artifacts.content(id,artifact.id(),email);
        try(var pdf=org.apache.pdfbox.Loader.loadPDF(bytes)) {
            String text=new org.apache.pdfbox.text.PDFTextStripper().getText(pdf);assertTrue(text.contains("Seguimiento institucional verificado."));
            assertTrue(text.indexOf("OBJETIVO")<text.indexOf("JUSTIFICACIÓN"));assertFalse(text.contains("@@"));
            assertTrue(artifact.pages().stream().anyMatch(page->page.width()>page.height()));
            assertFalse(text.matches("(?s).*\\n\\s*\\d+\\.\\s+FIRMAS DE RESPONSABILIDAD.*"),"Las firmas conservan su título oficial sin numeración.");
            assertFalse(text.matches("(?s).*\\n\\s*\\d+\\.\\s+CONTROL DE HISTORIAL DE CAMBIOS.*"),"El control conserva su título oficial sin numeración.");
            int signaturePage=-1,historyPage=-1;var stripper=new org.apache.pdfbox.text.PDFTextStripper();
            for(int i=0;i<pdf.getNumberOfPages();i++) {
                stripper.setStartPage(i+1);stripper.setEndPage(i+1);String pageText=stripper.getText(pdf);
                if(pageText.lines().anyMatch(line->line.strip().equals("FIRMAS DE RESPONSABILIDAD")))signaturePage=i;
                if(pageText.lines().anyMatch(line->line.strip().equals("CONTROL DE HISTORIAL DE CAMBIOS")))historyPage=i;
            }
            assertTrue(signaturePage>=0);assertEquals(signaturePage,historyPage,"La tabla corta de firmas y su control deben aprovechar la misma página cuando caben.");
            boolean[] customBodyFound={false};
            new org.apache.pdfbox.text.PDFTextStripper() {
                @Override protected void writeString(String value,List<org.apache.pdfbox.text.TextPosition> positions) throws java.io.IOException {
                    if(value.contains("Seguimiento institucional verificado.")) {
                        customBodyFound[0]=true;var position=positions.getFirst();
                        assertEquals(10f,position.getFontSizeInPt(),0.1f,"Una sección adicional conserva la tipografía del cuerpo T1.");
                        String font=position.getFont().getName().toLowerCase(Locale.ROOT);assertFalse(font.contains("bold")||font.contains("italic"),font);
                    }
                    super.writeString(value,positions);
                }
            }.getText(pdf);
            assertTrue(customBodyFound[0]);
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/structured-plan.pdf"),bytes);
        structures.save("T1",email,new DocumentStructures.Save(1L,initial.sections()));assertEquals(1,structures.snapshot(id).revision());
        assertArrayEquals(bytes,artifacts.content(id,artifact.id(),email));assertEquals(2,structures.template("T1",email).revision());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->structures.save("T1",email,new DocumentStructures.Save(1L,ordered))).getStatusCode().value());
    }
}
