package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.workflow.*;
import java.time.OffsetDateTime;
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

    @Test
    void relatedDerivedReportsWithoutAccessAreExcludedFromTimeline() {
        // Create second user who belongs to the same group as base plan, so can view base plan
        String secondUserEmail = "collaborator@example.invalid";
        UUID secondUserId = UUID.randomUUID();
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,'Docente colaborador',?,'USER',false)",
            secondUserId, secondUserEmail, encoder.encode("Test-password-2026"));
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')", secondUserId, group);

        // Verify second user has access to base plan
        assertNotNull(plans.get(id, secondUserEmail));

        // Create a separate restricted group where only primary user is member
        UUID privateGroup = UUID.randomUUID();
        jdbc.update("INSERT INTO institutional_group(id,name,group_type,collective_label) VALUES (?,'Comisión privada','COMMISSION','Responsable')", privateGroup);
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')", user, privateGroup);

        // Insert a derived report belonging to privateGroup and created by primary user
        UUID privateReportId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state)
            VALUES (?,?,?,?,?,?,'Informe derivado privado','Unidad','Carrera','T2',?,'1.0','APPROVED')
            """, privateReportId, user, user, privateGroup, period, UUID.randomUUID(), id);

        // Second user does NOT have access to privateReportId
        assertThrows(ResponseStatusException.class, () -> plans.get(privateReportId, secondUserEmail));

        // When second user queries timeline of base plan: private report MUST NOT appear
        var secondUserTimeline = timeline.timeline(id, secondUserEmail, "asc");
        assertTrue(secondUserTimeline.stream().noneMatch(item -> privateReportId.equals(item.linkedDocumentId())),
            "Un informe derivado al que el usuario no tiene acceso no debe figurar en la línea de tiempo");

        // Primary user DOES see the derived report
        var primaryUserTimeline = timeline.timeline(id, email, "asc");
        assertTrue(primaryUserTimeline.stream().anyMatch(item -> privateReportId.equals(item.linkedDocumentId())),
            "El usuario autorizado debe ver los informes derivados a los que tiene acceso");
    }

    @Test
    void tasksWithoutActedAtOrApprovalWithoutClosedAtDoNotFabricateDates() {
        // Seed artifact and signature so we can create a review
        UUID artifactId = UUID.randomUUID();
        UUID fileId = UUID.randomUUID();
        jdbc.update("INSERT INTO stored_file(id,original_name,media_type,size_bytes,sha256,storage_path) VALUES (?,'doc.pdf','application/pdf',100,'hash','path')", fileId);
        jdbc.update("INSERT INTO document_artifact(id,work_plan_id,row_version,current_file_id) VALUES (?,?,0,?)", artifactId, id, fileId);
        UUID signatureId = UUID.randomUUID();
        jdbc.update("INSERT INTO signed_document_artifact(id,work_plan_id,artifact_id,signed_file_id,signer_name) VALUES (?,?,?,?,'Firmante')",
            signatureId, id, artifactId, fileId);

        // Review with status APPROVED but closed_at is NULL
        UUID reviewId = UUID.randomUUID();
        OffsetDateTime roundCreated = OffsetDateTime.now().minusDays(5);
        jdbc.update("INSERT INTO document_review(id,work_plan_id,artifact_id,author_signature_id,current_file_id,round_number,status,created_at,closed_at) VALUES (?,?,?,?,?,1,'APPROVED',?,NULL)",
            reviewId, id, artifactId, signatureId, fileId, roundCreated);

        // Review task with status COMPLETED but acted_at is NULL
        UUID taskId = UUID.randomUUID();
        jdbc.update("INSERT INTO review_task(id,review_id,actor_id,actor_name,stage_label,stage_action,position,requires_signature,signature_action,status,acted_at) VALUES (?,?,?,?,'Revisión','REVIEW',0,false,'NONE','COMPLETED',NULL)",
            taskId, reviewId, user, "Revisor de prueba");

        var items = timeline.timeline(id, email, "asc");

        // Find task item and verify its occurredAt is null (not fabricated as roundCreated)
        var taskItem = items.stream().filter(i -> ("review-task-" + taskId).equals(i.id())).findFirst().orElseThrow();
        assertNull(taskItem.occurredAt(), "Si acted_at es null, no debe fabricarse la fecha de creación de la ronda");

        // Find approval item and verify its occurredAt is null (not fabricated as roundCreated)
        var approvalItem = items.stream().filter(i -> ("review-approval-" + reviewId).equals(i.id())).findFirst().orElseThrow();
        assertNull(approvalItem.occurredAt(), "Si closed_at es null, no debe fabricarse la fecha de creación de la ronda");
    }

    @Test
    void derivedReportsAccuratelyReflectAllDocumentStates() {
        // Test multiple states: DRAFT, IN_REVIEW, APPROVED, ARCHIVED, DISCARDED
        UUID draftRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state) VALUES (?,?,?,?,?,?,'Informe Borrador','U','C','T2',?,'1.0','DRAFT')",
            draftRep, user, user, group, period, UUID.randomUUID(), id);

        UUID inReviewRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state) VALUES (?,?,?,?,?,?,'Informe En Revision','U','C','T3',?,'1.0','IN_REVIEW')",
            inReviewRep, user, user, group, period, UUID.randomUUID(), id);

        UUID approvedRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state) VALUES (?,?,?,?,?,?,'Informe Aprobado','U','C','T2',?,'1.0','APPROVED')",
            approvedRep, user, user, group, period, UUID.randomUUID(), id);

        UUID archivedRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state,archived_at) VALUES (?,?,?,?,?,?,'Informe Archivado','U','C','T3',?,'1.0','APPROVED',now())",
            archivedRep, user, user, group, period, UUID.randomUUID(), id);

        UUID discardedRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state,archived_at,discarded) VALUES (?,?,?,?,?,?,'Informe Descartado','U','C','T2',?,'1.0','DRAFT',now(),true)",
            discardedRep, user, user, group, period, UUID.randomUUID(), id);

        var items = timeline.timeline(id, email, "asc");

        var draftItem = items.stream().filter(i -> draftRep.equals(i.linkedDocumentId())).findFirst().orElseThrow();
        assertEquals("DRAFT", draftItem.status());
        assertTrue(draftItem.detail().contains("Estado: Borrador"));

        var reviewItem = items.stream().filter(i -> inReviewRep.equals(i.linkedDocumentId())).findFirst().orElseThrow();
        assertEquals("IN_REVIEW", reviewItem.status());
        assertTrue(reviewItem.detail().contains("Estado: En revisión"));

        var approvedItem = items.stream().filter(i -> approvedRep.equals(i.linkedDocumentId())).findFirst().orElseThrow();
        assertEquals("APPROVED", approvedItem.status());
        assertTrue(approvedItem.detail().contains("Estado: Aprobado"));

        var archivedItem = items.stream().filter(i -> archivedRep.equals(i.linkedDocumentId())).findFirst().orElseThrow();
        assertEquals("ARCHIVED", archivedItem.status());
        assertTrue(archivedItem.detail().contains("Estado: Archivado"));

        var discardedItem = items.stream().filter(i -> discardedRep.equals(i.linkedDocumentId())).findFirst().orElseThrow();
        assertEquals("DISCARDED", discardedItem.status());
        assertTrue(discardedItem.detail().contains("Estado: Descartado"));
    }
}
