package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.institution.OrganizationService;
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
    @Autowired ec.edu.uta.utaped.institution.OrganizationService organization;

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
        signingAdmin();
        UUID scope = organization.createScope(email, new OrganizationService.ScopeInput(group, period, null));
        organization.member(scope, email, new OrganizationService.MemberInput(user, "MEMBER", "Principal", true, 0L));
        organization.member(scope, email, new OrganizationService.MemberInput(secondUserId, "MEMBER", "Colaborador", true, 0L));
        organization.principal(scope, email, new OrganizationService.PrincipalInput(user, 0L));
        jdbc.update("UPDATE work_plan SET scope_id=? WHERE id=?", scope, id);
        jdbc.update("INSERT INTO document_access(work_plan_id,user_id,granted_by) VALUES (?,?,?),(?,?,?)", id, user, user, id, secondUserId, user);

        // Verify second user has access to base plan
        assertNotNull(plans.get(id, secondUserEmail));

        // Create a separate restricted group where only primary user is member
        UUID privateGroup = UUID.randomUUID();
        jdbc.update("INSERT INTO institutional_group(id,name,group_type,collective_label) VALUES (?,'Comisión privada','COMMISSION','Responsable')", privateGroup);
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')", user, privateGroup);

        // Insert a derived report belonging to privateGroup and created by primary user
        UUID privateReportId = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,creation_title,preparation_date,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state)
            VALUES (?,?,?,?,?,?,'Informe derivado privado',CURRENT_DATE,'Informe derivado privado','Unidad','Carrera','T2',?,'1.0','APPROVED')
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
    void derivedReportsAccuratelyReflectAllDocumentStates() {
        signingAdmin();
        // Test multiple states: DRAFT, IN_REVIEW, APPROVED, ARCHIVED, DISCARDED
        UUID draftRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,creation_title,preparation_date,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state) VALUES (?,?,?,?,?,?,'Informe Borrador',CURRENT_DATE,'Informe Borrador','U','C','T3',?,'1.0','DRAFT')",
            draftRep, user, user, group, period, UUID.randomUUID(), id);

        UUID inReviewRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,creation_title,preparation_date,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state) VALUES (?,?,?,?,?,?,'Informe En Revision',CURRENT_DATE,'Informe En Revision','U','C','T3',?,'1.0','IN_REVIEW')",
            inReviewRep, user, user, group, period, UUID.randomUUID(), id);

        UUID approvedRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,creation_title,preparation_date,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state) VALUES (?,?,?,?,?,?,'Informe Aprobado',CURRENT_DATE,'Informe Aprobado','U','C','T2',?,'1.0','APPROVED')",
            approvedRep, user, user, group, period, UUID.randomUUID(), id);

        UUID archivedRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,creation_title,preparation_date,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state,archived_at) VALUES (?,?,?,?,?,?,'Informe Archivado',CURRENT_DATE,'Informe Archivado','U','C','T3',?,'1.0','APPROVED',now())",
            archivedRep, user, user, group, period, UUID.randomUUID(), id);

        UUID discardedRep = UUID.randomUUID();
        jdbc.update("INSERT INTO work_plan(id,teacher_id,created_by,group_id,period_id,request_key,creation_title,preparation_date,title,institutional_unit,career,document_type,base_plan_id,formal_version,document_state,archived_at,discarded) VALUES (?,?,?,?,?,?,'Informe Descartado',CURRENT_DATE,'Informe Descartado','U','C','T2',?,'1.0','DRAFT',now(),true)",
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
