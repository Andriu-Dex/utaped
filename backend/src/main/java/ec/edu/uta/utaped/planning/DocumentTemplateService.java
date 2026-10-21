package ec.edu.uta.utaped.planning;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.planning.DocumentTemplateModels.DocumentTemplate;
import ec.edu.uta.utaped.planning.DocumentTemplateModels.ResetInput;
import ec.edu.uta.utaped.planning.DocumentTemplateModels.UpdateInput;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DocumentTemplateService {
    private final JdbcTemplate jdbc;
    private final Accounts accounts;
    private final AuditService audit;

    public DocumentTemplateService(JdbcTemplate jdbc, Accounts accounts, AuditService audit) {
        this.jdbc = jdbc;
        this.accounts = accounts;
        this.audit = audit;
    }

    private DocumentTemplate map(ResultSet rs, int rowNum) throws SQLException {
        return new DocumentTemplate(
            rs.getString("document_type"),
            rs.getString("name"),
            rs.getString("description"),
            rs.getString("justification"),
            rs.getLong("row_version"),
            rs.getObject("updated_at", OffsetDateTime.class),
            rs.getObject("updated_by", UUID.class),
            rs.getString("updated_by_name")
        );
    }

    private static final String SELECT = """
        SELECT t.document_type, t.name, t.description, t.justification, t.row_version, t.updated_at, t.updated_by,
               u.display_name AS updated_by_name
        FROM institutional_document_template t
        LEFT JOIN app_user u ON u.id = t.updated_by
    """;

    public void ensureDefaults() {
        jdbc.update("""
            INSERT INTO institutional_document_template(document_type, name, description, justification)
            VALUES ('T1', 'Plan de Trabajo (Formato T1)', 'Texto predeterminado cargado automáticamente en el campo de Justificación al crear un nuevo Plan de Trabajo.', ?)
            ON CONFLICT (document_type) DO NOTHING
        """, InstitutionalTemplates.DEFAULT_JUSTIFICATION);
    }

    public List<DocumentTemplate> list() {
        var items = jdbc.query(SELECT + " ORDER BY t.document_type", this::map);
        if (items.isEmpty()) {
            ensureDefaults();
            return jdbc.query(SELECT + " ORDER BY t.document_type", this::map);
        }
        return items;
    }

    public DocumentTemplate get(String documentType) {
        var items = jdbc.query(SELECT + " WHERE t.document_type = ?", this::map, documentType.toUpperCase());
        if (items.isEmpty()) {
            if ("T1".equalsIgnoreCase(documentType)) {
                ensureDefaults();
                return jdbc.query(SELECT + " WHERE t.document_type = ?", this::map, "T1")
                    .stream().findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plantilla institucional no encontrada."));
            }
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plantilla institucional no encontrada.");
        }
        return items.getFirst();
    }

    public String getJustificationOrDefault(String documentType) {
        var list = jdbc.queryForList("SELECT justification FROM institutional_document_template WHERE document_type = ?", String.class, documentType.toUpperCase());
        if (list.isEmpty() || list.getFirst() == null || list.getFirst().isBlank()) {
            return InstitutionalTemplates.defaultJustificationFor(documentType);
        }
        return list.getFirst();
    }

    @Transactional
    public DocumentTemplate update(String documentType, String email, UpdateInput input) {
        var actor = accounts.current(email);
        if (!actor.systemRole().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso administrativo requerido.");
        }
        String type = documentType.toUpperCase();
        var current = jdbc.queryForList("SELECT row_version FROM institutional_document_template WHERE document_type = ? FOR UPDATE", type);
        if (current.isEmpty()) {
            ensureDefaults();
            current = jdbc.queryForList("SELECT row_version FROM institutional_document_template WHERE document_type = ? FOR UPDATE", type);
            if (current.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plantilla institucional no encontrada.");
            }
        }
        long version = ((Number) current.getFirst().get("row_version")).longValue();
        if (version != input.rowVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La plantilla cambió en otra sesión. Recargue antes de guardar.");
        }

        int updated = jdbc.update("""
            UPDATE institutional_document_template
            SET justification = ?, row_version = row_version + 1, updated_at = now(), updated_by = ?
            WHERE document_type = ? AND row_version = ?
        """, input.justification().trim(), actor.id(), type, input.rowVersion());

        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La plantilla cambió en otra sesión. Recargue antes de guardar.");
        }

        audit.record(actor.id(), "INSTITUTIONAL_TEMPLATE_UPDATED", null);
        return get(type);
    }

    @Transactional
    public DocumentTemplate reset(String documentType, String email, ResetInput input) {
        var actor = accounts.current(email);
        if (!actor.systemRole().equals("ADMIN")) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acceso administrativo requerido.");
        }
        String type = documentType.toUpperCase();
        var current = jdbc.queryForList("SELECT row_version FROM institutional_document_template WHERE document_type = ? FOR UPDATE", type);
        if (current.isEmpty()) {
            ensureDefaults();
            current = jdbc.queryForList("SELECT row_version FROM institutional_document_template WHERE document_type = ? FOR UPDATE", type);
            if (current.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plantilla institucional no encontrada.");
            }
        }
        long version = ((Number) current.getFirst().get("row_version")).longValue();
        if (version != input.rowVersion()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La plantilla cambió en otra sesión. Recargue antes de guardar.");
        }

        String defaultText = InstitutionalTemplates.defaultJustificationFor(type);
        int updated = jdbc.update("""
            UPDATE institutional_document_template
            SET justification = ?, row_version = row_version + 1, updated_at = now(), updated_by = ?
            WHERE document_type = ? AND row_version = ?
        """, defaultText, actor.id(), type, input.rowVersion());

        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "La plantilla cambió en otra sesión. Recargue antes de guardar.");
        }

        audit.record(actor.id(), "INSTITUTIONAL_TEMPLATE_RESET", null);
        return get(type);
    }
}
