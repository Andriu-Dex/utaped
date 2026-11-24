package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.documents.ArtifactModels.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;

class EventDocumentIntegrationTests extends DocumentIntegrationTests {
    @Autowired EventDocuments events;
    EventDocuments.Data sample() { return sample(user); }
    static EventDocuments.Data sample(UUID user) {
        var today=LocalDate.now(ZoneId.of("America/Guayaquil"));
        return new EventDocuments.Data("Jornada académica","ACADEMIC","",today,today,LocalTime.of(9,0),LocalTime.of(12,0),"PRESENTIAL","","Auditorio",100,2,10,60,0,"Proyector y equipos","No aplica","Canales institucionales","Compartir los resultados académicos.","Presentar resultados.\nRecoger propuestas.","Se cumplieron los objetivos.","Comisión organizadora",false,"",List.of(new EventDocuments.Action(UUID.randomUUID(),"Presentación de resultados",today,LocalTime.of(9,0),LocalTime.of(10,0),List.of(user),"Nombre enviado que no debe confiarse","Equipos institucionales",null,"")));
    }
    UUID createEvent() { return plans.create(email,new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan de evento",null,true)).id(); }
    @Test void eventPlansHaveIndependentDraftsAndUseTheOfficialTemplate() throws Exception {
        UUID doc=createEvent(),other=createEvent();var plan=plans.get(doc,email);
        var saved=events.save(doc,email,new EventDocuments.Save(plan.rowVersion(),true,sample(),"Justificación del evento."));
        assertTrue(saved.enabled());assertTrue(events.snapshot(other).actions().isEmpty());assertNull(events.snapshot(id));
        assertEquals("Docente de prueba",saved.data().actions().getFirst().responsibleDisplay());
        var settings=attachments.settings(doc,email,new ec.edu.uta.utaped.documents.AttachmentModels.Settings(saved.rowVersion(),false,false));
        assertTrue(artifacts.readiness(doc,email).ready(),artifacts.readiness(doc,email).blockers().toString());
        var artifact=artifacts.generate(doc,email,new Generate(plans.get(doc,email).rowVersion()));
        assertTrue(artifact.templateVersion().startsWith("EVENT-T1-"));assertEquals(artifact.pages().size(),artifact.pageCount());
        var file=jdbc.queryForObject("SELECT file_id FROM document_artifact WHERE id=?",UUID.class,artifact.id());
        try(var pdf=Loader.loadPDF(files.read(file))) {
            String text=new PDFTextStripper().getText(pdf);
            for(int page=0;page<pdf.getNumberOfPages();page++){var stripper=new PDFTextStripper();stripper.setStartPage(page+1);stripper.setEndPage(page+1);assertTrue(stripper.getText(pdf).contains("UNIVERSIDAD TÉCNICA DE AMBATO"),"Encabezado faltante en página "+(page+1));}
            assertTrue(text.contains("DATOS INFORMATIVOS DEL EVENTO"));assertTrue(text.contains("DESARROLLO DEL EVENTO"));
            assertTrue(text.contains("OBJETIVOS ESPECÍFICOS"));assertTrue(text.contains("Jornada académica"));
            assertFalse(text.contains("Nombre enviado que no debe confiarse"));
            assertTrue(Math.abs(pdf.getPage(0).getMediaBox().getWidth()-595.3)<2);
            java.nio.file.Files.createDirectories(java.nio.file.Path.of("target/event-qa"));
            java.nio.file.Files.write(java.nio.file.Path.of("target/event-qa/event-plan.pdf"),files.read(file));
            javax.imageio.ImageIO.write(new org.apache.pdfbox.rendering.PDFRenderer(pdf).renderImageWithDPI(Math.min(2,pdf.getNumberOfPages()-1),120),"png",java.nio.file.Path.of("target/event-qa/event-plan.png").toFile());
        }
        assertEquals(1,artifact.signatureSlots().stream().filter(s->s.action().equals("ELABORADO_POR")).count());
        assertThrows(ResponseStatusException.class,()->events.save(doc,email,new EventDocuments.Save(plan.rowVersion(),true,sample())));
        assertThrows(ResponseStatusException.class,()->events.save(id,email,new EventDocuments.Save(plans.get(id,email).rowVersion(),true,sample())));
    }
    @Test void incompleteEventDraftCannotBeSignedAndCannotChangeItsVariant() {
        UUID doc=createEvent();assertFalse(artifacts.readiness(doc,email).ready());
        assertThrows(ResponseStatusException.class,()->events.save(doc,email,new EventDocuments.Save(plans.get(doc,email).rowVersion(),false,EventDocuments.empty())));
    }
}
