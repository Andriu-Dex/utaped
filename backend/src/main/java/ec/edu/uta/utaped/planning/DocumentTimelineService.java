package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.workflow.ReviewService;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentTimelineService {
    public record Item(
        String id,
        String kind,
        String title,
        String detail,
        String actorName,
        String status,
        Integer roundNumber,
        String formalVersion,
        OffsetDateTime occurredAt,
        UUID reviewId,
        UUID linkedDocumentId,
        String linkedDocumentType
    ) {}

    private final JdbcTemplate jdbc;
    private final WorkPlanService plans;
    private final ZoneId zone;

    public DocumentTimelineService(JdbcTemplate jdbc, WorkPlanService plans,
                                  @Value("${app.planning.time-zone:America/Guayaquil}") String zone) {
        this.jdbc = jdbc;
        this.plans = plans;
        this.zone = ZoneId.of(zone);
    }

    private OffsetDateTime toOffsetDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof OffsetDateTime odt) return odt;
        if (value instanceof java.sql.Timestamp ts) return ts.toInstant().atZone(zone).toOffsetDateTime();
        if (value instanceof java.time.Instant inst) return inst.atZone(zone).toOffsetDateTime();
        return OffsetDateTime.parse(value.toString());
    }

    private boolean canAccess(UUID docId, String email) {
        if (docId == null) return false;
        try {
            plans.get(docId, email);
            return true;
        } catch (ResponseStatusException denied) {
            if (denied.getStatusCode() != HttpStatus.NOT_FOUND && denied.getStatusCode() != HttpStatus.FORBIDDEN) {
                throw denied;
            }
            return false;
        }
    }

    public static String documentStateLabel(String state, Object archivedAt, Boolean discarded) {
        if (Boolean.TRUE.equals(discarded)) return "Descartado";
        if (archivedAt != null) return "Archivado";
        if ("APPROVED".equals(state)) return "Aprobado";
        if ("IN_REVIEW".equals(state)) return "En revisión";
        if ("DRAFT".equals(state)) return "Borrador";
        return state != null && !state.isBlank() ? state : "Sin estado";
    }

    public static String documentStatus(String state, Object archivedAt, Boolean discarded) {
        if (Boolean.TRUE.equals(discarded)) return "DISCARDED";
        if (archivedAt != null) return "ARCHIVED";
        return state != null && !state.isBlank() ? state : "DRAFT";
    }

    @Transactional(readOnly = true)
    public List<Item> timeline(UUID targetDocId, String email, String order) {
        // Enforce document ACL check first
        var plan = plans.get(targetDocId, email);
        var items = new ArrayList<Item>();

        // 1. CREATION milestone
        var creationAudit = jdbc.queryForList("""
            SELECT e.id, e.action, e.occurred_at, COALESCE(e.actor_display_name, u.display_name) AS actor_name
            FROM audit_event e
            LEFT JOIN app_user u ON u.id = e.actor_id
            WHERE e.target_id = ? AND e.action IN ('WORK_PLAN_CREATED', 'WORK_PLAN_COPIED', 'REPORT_CREATED', 'EXECUTION_REPORT_CREATED')
            ORDER BY e.occurred_at ASC, e.id ASC LIMIT 1
            """, targetDocId);

        String docTypeName = "T1".equals(plan.documentType()) ? "Plan de Trabajo" :
                             "T2".equals(plan.documentType()) ? "Informe final" : "Informe de ejecución";

        if (!creationAudit.isEmpty()) {
            var row = creationAudit.getFirst();
            items.add(new Item(
                "creation-" + row.get("id"),
                "CREATION",
                "Elaboración iniciada",
                "Inicio del borrador del " + docTypeName + " institucional.",
                (String) row.get("actor_name"),
                "COMPLETED",
                null,
                plan.formalVersion(),
                toOffsetDateTime(row.get("occurred_at")),
                null,
                null,
                null
            ));
        } else {
            items.add(new Item(
                "creation-" + plan.id(),
                "CREATION",
                "Elaboración iniciada",
                "Inicio del borrador del " + docTypeName + " institucional.",
                plan.teacherName(),
                "COMPLETED",
                null,
                plan.formalVersion(),
                plan.createdAt(),
                null,
                null,
                null
            ));
        }

        // 2. SIGNATURE milestones (Author / Maker signatures)
        var signatures = jdbc.queryForList("""
            SELECT s.id, COALESCE(s.actor_name, s.signer_name) AS signer_name, s.signed_at, s.signature_order
            FROM signed_document_artifact s
            WHERE s.work_plan_id = ?
            ORDER BY s.signed_at ASC, s.signature_order ASC, s.id ASC
            """, targetDocId);
        for (var sig : signatures) {
            int sigOrder = ((Number) sig.get("signature_order")).intValue();
            items.add(new Item(
                "signature-" + sig.get("id"),
                "SIGNATURE",
                "Firma de elaboración",
                "Firma electrónica de autor registrada (orden " + (sigOrder + 1) + ").",
                (String) sig.get("signer_name"),
                "COMPLETED",
                null,
                plan.formalVersion(),
                toOffsetDateTime(sig.get("signed_at")),
                null,
                null,
                null
            ));
        }

        // 3. REVIEW ROUNDS & TASKS
        var reviews = jdbc.queryForList("""
            SELECT r.id, r.round_number, r.status, r.created_at, r.closed_at
            FROM document_review r
            WHERE r.work_plan_id = ?
            ORDER BY r.round_number ASC, r.id ASC
            """, targetDocId);
        for (var rev : reviews) {
            UUID revId = (UUID) rev.get("id");
            int roundNumber = ((Number) rev.get("round_number")).intValue();
            String revStatus = (String) rev.get("status");
            OffsetDateTime createdAt = toOffsetDateTime(rev.get("created_at"));
            OffsetDateTime closedAt = toOffsetDateTime(rev.get("closed_at"));

            // Event: Envío formal a revisión de la ronda
            items.add(new Item(
                "review-submit-" + revId,
                "REVIEW_ROUND_SUBMITTED",
                "Envío a revisión · Ronda " + roundNumber,
                "El documento ingresó al flujo de revisión para la Ronda " + roundNumber + ".",
                null,
                "IN_REVIEW".equals(revStatus) ? "ACTIVE" : "COMPLETED",
                roundNumber,
                null,
                createdAt,
                revId,
                null,
                null
            ));

            // Tasks acted within this round
            var tasks = jdbc.queryForList("""
                SELECT t.id, t.actor_name, t.stage_label, t.stage_action, t.status, t.acted_at, t.requires_signature
                FROM review_task t
                WHERE t.review_id = ? AND t.status IN ('COMPLETED', 'RETURNED')
                ORDER BY t.acted_at ASC, t.position ASC, t.id ASC
                """, revId);
            for (var task : tasks) {
                String taskStatus = (String) task.get("status");
                String stage = ReviewService.displayStageLabel((String) task.get("stage_label"), (String) task.get("stage_action"));
                // Never fabricate or guess the timestamp if acted_at is null
                OffsetDateTime actedAt = toOffsetDateTime(task.get("acted_at"));

                if ("RETURNED".equals(taskStatus)) {
                    items.add(new Item(
                        "review-task-" + task.get("id"),
                        "REVIEW_RETURNED",
                        "Devuelto para corrección · Ronda " + roundNumber,
                        "Devolución con observaciones en la etapa: " + stage + ".",
                        (String) task.get("actor_name"),
                        "RETURNED",
                        roundNumber,
                        null,
                        actedAt,
                        revId,
                        null,
                        null
                    ));
                } else if ("COMPLETED".equals(taskStatus)) {
                    boolean requiresSig = Boolean.TRUE.equals(task.get("requires_signature"));
                    items.add(new Item(
                        "review-task-" + task.get("id"),
                        "REVIEW_TASK_COMPLETED",
                        (requiresSig ? "Firma de revisión" : "Revisión aprobada") + " · Ronda " + roundNumber,
                        "Etapa " + stage + " completada formalmente.",
                        (String) task.get("actor_name"),
                        "COMPLETED",
                        roundNumber,
                        null,
                        actedAt,
                        revId,
                        null,
                        null
                    ));
                }
            }

            // If round closed with APPROVED - use genuine closedAt timestamp without fabricating createdAt
            if ("APPROVED".equals(revStatus)) {
                items.add(new Item(
                    "review-approval-" + revId,
                    "APPROVAL",
                    "Aprobación formal del documento",
                    "Flujo de revisión concluido con aprobación en Ronda " + roundNumber + ". Versión formal: " + plan.formalVersion() + ".",
                    null,
                    "APPROVED",
                    roundNumber,
                    plan.formalVersion(),
                    closedAt,
                    revId,
                    null,
                    null
                ));
            }
        }

        // 4. FORMAL VERSIONS - Each related document's access is strictly validated individually
        var formalVersions = jdbc.queryForList("""
            SELECT w.id, w.title, w.formal_version, w.document_state, w.archived_at, w.discarded, w.version_reason, w.created_at,
                   COALESCE(u.display_name, 'Sistema institucional') AS actor_name
            FROM work_plan w
            LEFT JOIN app_user u ON u.id = w.created_by
            WHERE (w.previous_version_id = ? OR (w.id = ? AND w.previous_version_id IS NOT NULL))
            ORDER BY w.created_at ASC, w.id ASC
            """, targetDocId, targetDocId);
        for (var fv : formalVersions) {
            UUID fvId = (UUID) fv.get("id");
            if (!canAccess(fvId, email)) continue;

            String fvVersion = (String) fv.get("formal_version");
            String reason = (String) fv.get("version_reason");
            String fvState = (String) fv.get("document_state");
            String fvStateLabel = documentStateLabel(fvState, fv.get("archived_at"), (Boolean) fv.get("discarded"));
            String fvStatus = documentStatus(fvState, fv.get("archived_at"), (Boolean) fv.get("discarded"));

            String detail = (reason != null && !reason.isBlank()) ? reason : "Nueva versión formal aprobada del documento.";
            detail += " · Estado: " + fvStateLabel + ".";

            items.add(new Item(
                "formal-version-" + fvId,
                "FORMAL_VERSION",
                "Versión formal " + fvVersion + " creada",
                detail,
                (String) fv.get("actor_name"),
                fvStatus,
                null,
                fvVersion,
                toOffsetDateTime(fv.get("created_at")),
                null,
                fvId,
                null
            ));
        }

        // 5. LINKED DERIVED REPORTS - Each related report is strictly checked for individual user access
        if ("T1".equals(plan.documentType())) {
            var derivedReports = jdbc.queryForList("""
                SELECT r.id, r.title, r.document_type, r.formal_version, r.document_state, r.archived_at, r.discarded, r.created_at,
                       COALESCE(u.display_name, 'Sistema institucional') AS actor_name
                FROM work_plan r
                LEFT JOIN app_user u ON u.id = r.created_by
                WHERE r.base_plan_id = ?
                ORDER BY r.created_at ASC, r.id ASC
                """, targetDocId);
            for (var rep : derivedReports) {
                UUID repId = (UUID) rep.get("id");
                if (!canAccess(repId, email)) continue;

                String repType = (String) rep.get("document_type");
                String state = (String) rep.get("document_state");
                String stateLabel = documentStateLabel(state, rep.get("archived_at"), (Boolean) rep.get("discarded"));
                String status = documentStatus(state, rep.get("archived_at"), (Boolean) rep.get("discarded"));

                items.add(new Item(
                    "linked-report-" + rep.get("id"),
                    "LINKED_REPORT",
                    ("T2".equals(repType) ? "Informe final (T2)" : "Informe de ejecución (T3)") + " derivado",
                    (String) rep.get("title") + " · Estado: " + stateLabel + ".",
                    (String) rep.get("actor_name"),
                    status,
                    null,
                    (String) rep.get("formal_version"),
                    toOffsetDateTime(rep.get("created_at")),
                    null,
                    repId,
                    repType
                ));
            }
        } else if ("T2".equals(plan.documentType()) || "T3".equals(plan.documentType())) {
            if (plan.basePlanId() != null && canAccess(plan.basePlanId(), email)) {
                var basePlans = jdbc.queryForList("""
                    SELECT b.id, b.title, b.document_type, b.formal_version, b.document_state, b.archived_at, b.discarded, b.created_at,
                           COALESCE(u.display_name, 'Sistema institucional') AS actor_name
                    FROM work_plan b
                    LEFT JOIN app_user u ON u.id = b.created_by
                    WHERE b.id = ?
                    """, plan.basePlanId());
                if (!basePlans.isEmpty()) {
                    var base = basePlans.getFirst();
                    String baseState = (String) base.get("document_state");
                    String baseStateLabel = documentStateLabel(baseState, base.get("archived_at"), (Boolean) base.get("discarded"));
                    String baseStatus = documentStatus(baseState, base.get("archived_at"), (Boolean) base.get("discarded"));

                    items.add(new Item(
                        "linked-base-" + base.get("id"),
                        "LINKED_REPORT",
                        "Plan de Trabajo base (T1)",
                        (String) base.get("title") + " · Versión formal: " + base.get("formal_version") + " · Estado: " + baseStateLabel + ".",
                        (String) base.get("actor_name"),
                        baseStatus,
                        null,
                        (String) base.get("formal_version"),
                        toOffsetDateTime(base.get("created_at")),
                        null,
                        (UUID) base.get("id"),
                        "T1"
                    ));
                }
            }
        }

        // 6. ARCHIVED MILESTONE
        if (plan.archivedAt() != null) {
            String archiveReason = "";
            var reasons = jdbc.queryForList("SELECT archive_reason FROM work_plan WHERE id = ?", String.class, targetDocId);
            if (!reasons.isEmpty() && reasons.getFirst() != null) archiveReason = reasons.getFirst().trim();

            items.add(new Item(
                "archived-" + plan.id(),
                "ARCHIVED",
                "Documento archivado",
                !archiveReason.isBlank() ? archiveReason : "El documento ha sido archivado formalmente.",
                null,
                "ARCHIVED",
                null,
                plan.formalVersion(),
                plan.archivedAt(),
                null,
                null,
                null
            ));
        }

        // Sort items stably: non-null dates ordered, null dates placed last, ties broken by id
        if ("desc".equalsIgnoreCase(order)) {
            items.sort((a, b) -> {
                if (a.occurredAt() == null && b.occurredAt() == null) return a.id().compareTo(b.id());
                if (a.occurredAt() == null) return 1;
                if (b.occurredAt() == null) return -1;
                int cmp = b.occurredAt().compareTo(a.occurredAt());
                return cmp != 0 ? cmp : a.id().compareTo(b.id());
            });
        } else {
            items.sort((a, b) -> {
                if (a.occurredAt() == null && b.occurredAt() == null) return a.id().compareTo(b.id());
                if (a.occurredAt() == null) return 1;
                if (b.occurredAt() == null) return -1;
                int cmp = a.occurredAt().compareTo(b.occurredAt());
                return cmp != 0 ? cmp : a.id().compareTo(b.id());
            });
        }

        return Collections.unmodifiableList(items);
    }
}
