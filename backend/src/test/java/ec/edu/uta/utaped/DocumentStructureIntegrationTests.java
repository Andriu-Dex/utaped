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
        }
        java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/document-qa"));java.nio.file.Files.write(java.nio.file.Path.of("target/document-qa/structured-plan.pdf"),bytes);
        structures.save("T1",email,new DocumentStructures.Save(1L,initial.sections()));assertEquals(1,structures.snapshot(id).revision());
        assertArrayEquals(bytes,artifacts.content(id,artifact.id(),email));assertEquals(2,structures.template("T1",email).revision());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->structures.save("T1",email,new DocumentStructures.Save(1L,ordered))).getStatusCode().value());
    }
}
