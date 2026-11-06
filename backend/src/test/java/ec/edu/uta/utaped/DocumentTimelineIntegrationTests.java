package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.workflow.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class DocumentTimelineIntegrationTests extends DocumentIntegrationTests {
    @Autowired DocumentTimelineService timeline;
    @Autowired ReviewService reviews;
    @Autowired WorkflowService workflows;

    @Test
    void newlyCreatedDocumentHasCreationMilestone() {
        var items = timeline.timeline(id, email, "asc");
        assertFalse(items.isEmpty());
        var first = items.getFirst();
        assertEquals("CREATION", first.kind());
        assertEquals("Elaboración iniciada", first.title());
        assertNotNull(first.occurredAt());
    }

    @Test
    void unauthorizedUserCannotAccessDocumentTimeline() {
        String unauthorized = "unauthorized@example.invalid";
        UUID otherUser = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Otro docente',?,'USER',false)",
            otherUser, unauthorized, encoder.encode("Test-password-2026"));

        assertThrows(ResponseStatusException.class, () -> timeline.timeline(id, unauthorized, "asc"));
    }

    @Test
    void descendingOrderInvertsMilestoneSequence() {
        var asc = timeline.timeline(id, email, "asc");
        var desc = timeline.timeline(id, email, "desc");
        assertEquals(asc.size(), desc.size());
        if (!asc.isEmpty()) {
            assertEquals(asc.getFirst().id(), desc.getLast().id());
        }
    }

    @Test
    void documentsEventsAreIsolatedBetweenDocuments() {
        UUID secondDocId = plans.create(email, new WorkPlanModels.Create(group, period, UUID.randomUUID(), "Segundo Plan")).id();
        var itemsFirst = timeline.timeline(id, email, "asc");
        var itemsSecond = timeline.timeline(secondDocId, email, "asc");

        for (var item : itemsFirst) {
            assertTrue(itemsSecond.stream().noneMatch(other -> other.id().equals(item.id())),
                "Los eventos de un documento no deben filtrarse en otro");
        }
    }
}
