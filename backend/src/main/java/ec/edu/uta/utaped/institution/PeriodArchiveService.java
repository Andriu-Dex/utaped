package ec.edu.uta.utaped.institution;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.documents.StoredFiles;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.planning.WorkPlanService;
import jakarta.validation.constraints.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

@Service
public class PeriodArchiveService {
    public record Close(@NotNull @PositiveOrZero Long rowVersion,@NotBlank @Size(max=1000) String reason) {}
    public record Purge(@NotBlank @Size(max=1000) String reason) {}
    private final JdbcTemplate jdbc;
    private final Accounts accounts;
    private final AuditService audit;
    private final WorkPlanService plans;
    private final StoredFiles files;
    private final long maxBytes;
    private final int purgeGraceDays;
    private final JsonMapper json=JsonMapper.builder().build();
    public PeriodArchiveService(JdbcTemplate jdbc,Accounts accounts,AuditService audit,WorkPlanService plans,StoredFiles files,
        @Value("${app.periods.archive-max-bytes:134217728}") long maxBytes,@Value("${app.periods.purge-grace-days:30}") int purgeGraceDays) {
        if(purgeGraceDays<0) throw new IllegalArgumentException("Invalid period purge grace days");
        if(maxBytes<1048576||maxBytes>536870912) throw new IllegalArgumentException("Invalid period archive byte limit");
        this.jdbc=jdbc;this.accounts=accounts;this.audit=audit;this.plans=plans;this.files=files;this.maxBytes=maxBytes;this.purgeGraceDays=purgeGraceDays;
    }
    private void admin(String email) {
        if(!accounts.current(email).systemRole().equals("ADMIN")) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Solo administración puede cerrar períodos.");
    }
    public Map<String,Object> overview(UUID period,String email) {
        admin(email);
        var rows=jdbc.queryForList("SELECT p.*,a.document_count,a.archive_sha256,a.archive_size,a.custody_downloaded_at,a.purged_at,a.purge_reason,p.closed_at+make_interval(days=>?) AS purge_available_at,COALESCE(p.closed_at+make_interval(days=>?)<=now(),false) AS purge_ready FROM academic_period p LEFT JOIN period_archive a ON a.period_id=p.id WHERE p.id=?",purgeGraceDays,purgeGraceDays,period);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Período no disponible.");
        var result=new LinkedHashMap<>(rows.getFirst());
        result.replaceAll((key,value)->value instanceof java.sql.Date date ? date.toLocalDate().toString() : value);
        result.put("purge_grace_days",purgeGraceDays);
        result.put("states",jdbc.queryForList("SELECT document_type,document_state,count(*) AS count FROM work_plan WHERE period_id=? GROUP BY document_type,document_state ORDER BY document_type,document_state",period));
        return result;
    }
    private Object rows(String sql,UUID id) {
        return jdbc.query("SELECT row_to_json(item)::text FROM ("+sql+") item",(rs,n)->json.readValue(rs.getString(1),Object.class),id);
    }
    private Set<UUID> fileIds(UUID document) {
        return new LinkedHashSet<>(jdbc.queryForList("""
            SELECT file_id FROM document_artifact WHERE work_plan_id=?
            UNION SELECT file_id FROM signed_document_artifact WHERE work_plan_id=?
            UNION SELECT file_id FROM document_attachment WHERE work_plan_id=?
            UNION SELECT file_id FROM execution_evidence WHERE work_plan_id=? AND file_id IS NOT NULL
            UNION SELECT current_file_id FROM document_review WHERE work_plan_id=?
            UNION SELECT t.output_file_id FROM review_task t JOIN document_review r ON r.id=t.review_id WHERE r.work_plan_id=? AND t.output_file_id IS NOT NULL
            ORDER BY 1
            """,UUID.class,document,document,document,document,document,document));
    }
    private Map<String,Object> documentMetadata(UUID document) {
        var data=new LinkedHashMap<String,Object>();
        data.put("document",rows("SELECT * FROM work_plan WHERE id=?",document));
        data.put("matrix",rows("SELECT * FROM work_plan_matrix WHERE work_plan_id=?",document));
        data.put("report",rows("SELECT * FROM work_report WHERE id=?",document));
        data.put("artifacts",rows("SELECT * FROM document_artifact WHERE work_plan_id=? ORDER BY created_at,id",document));
        data.put("authorSignatures",rows("SELECT * FROM signed_document_artifact WHERE work_plan_id=? ORDER BY signed_at,id",document));
        data.put("attachments",rows("SELECT * FROM document_attachment WHERE work_plan_id=? ORDER BY sort_order,id",document));
        data.put("evidence",rows("SELECT * FROM execution_evidence WHERE work_plan_id=? ORDER BY uploaded_at,id",document));
        data.put("extensions",rows("SELECT * FROM deadline_extension_history WHERE work_plan_id=? ORDER BY created_at,id",document));
        data.put("reviews",rows("SELECT * FROM document_review WHERE work_plan_id=? ORDER BY round_number",document));
        data.put("tasks",rows("SELECT t.* FROM review_task t JOIN document_review r ON r.id=t.review_id WHERE r.work_plan_id=? ORDER BY r.round_number,t.position",document));
        data.put("observations",rows("SELECT o.* FROM review_observation o JOIN document_review r ON r.id=o.review_id WHERE r.work_plan_id=? ORDER BY o.created_at,o.id",document));
        data.put("audit",rows("SELECT * FROM audit_event WHERE target_id=? ORDER BY occurred_at,id",document));
        data.put("scope",rows("SELECT s.* FROM organizational_scope s JOIN work_plan w ON w.scope_id=s.id WHERE w.id=?",document));
        data.put("scopeMembershipHistory",rows("SELECT h.* FROM scope_membership_history h JOIN work_plan w ON w.scope_id=h.scope_id WHERE w.id=? ORDER BY h.created_at,h.id",document));
        data.put("scopePrincipalHistory",rows("SELECT h.* FROM scope_principal_history h JOIN work_plan w ON w.scope_id=h.scope_id WHERE w.id=? ORDER BY h.created_at,h.id",document));
        data.put("workflowAssignments",rows("SELECT a.* FROM period_workflow_assignment a JOIN work_plan w ON w.scope_id=a.scope_id WHERE w.id=? ORDER BY a.document_type",document));
        data.put("periodAudit",rows("SELECT e.* FROM audit_event e JOIN work_plan w ON w.period_id=e.target_id WHERE w.id=? ORDER BY e.occurred_at,e.id",document));
        data.put("files",fileIds(document).stream().map(files::metadata).toList());
        return data;
    }
    private byte[] metadata(Object data) { return json.writeValueAsString(data).getBytes(StandardCharsets.UTF_8); }
    private void entry(ZipOutputStream zip,String name,byte[] bytes,long[] total) throws IOException {
        total[0]+=bytes.length;
        if(total[0]>maxBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"El archivo histórico supera el límite técnico. El período no se cerró; ajuste el límite o solicite una exportación operativa.");
        var entry=new ZipEntry(name);entry.setTime(0);zip.putNextEntry(entry);zip.write(bytes);zip.closeEntry();
    }
    @Transactional public Map<String,Object> close(UUID period,String email,Close input) {
        admin(email);
        var rows=jdbc.queryForList("SELECT * FROM academic_period WHERE id=? FOR UPDATE",period);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Período no disponible.");
        var current=rows.getFirst();
        if(!current.get("status").equals("ACTIVE")) throw new ResponseStatusException(HttpStatus.CONFLICT,"El período ya está cerrado; no se vuelve a generar su archivo.");
        if(((Number)current.get("row_version")).longValue()!=input.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El período cambió. Recargue antes de cerrar.");
        var actor=accounts.current(email).id();
        jdbc.update("UPDATE academic_period SET status='CLOSED',closed_at=now(),closed_by=?,closure_reason=?,row_version=row_version+1 WHERE id=?",actor,input.reason().trim(),period);
        // Preserve document/review states: closure never invents approval or cancels history.
        jdbc.update("UPDATE work_plan SET archived_at=now(),archived_by=?,archive_reason=?,row_version=row_version+1,updated_at=now() WHERE period_id=? AND archived_at IS NULL",actor,"Cierre de período: "+input.reason().trim(),period);
        audit.record(actor,"PERIOD_CLOSED",period);
        var documents=jdbc.queryForList("SELECT id FROM work_plan WHERE period_id=? ORDER BY id",UUID.class,period);
        try(var output=new ByteArrayOutputStream();var zip=new ZipOutputStream(output)) {
            var total=new long[]{0};
            entry(zip,"period.json",metadata(rows("SELECT * FROM academic_period WHERE id=?",period)),total);
            for(var document:documents) {
                String prefix="documents/"+document+"/";
                entry(zip,prefix+"manifest.json",metadata(documentMetadata(document)),total);
                for(var file:fileIds(document)) entry(zip,prefix+"files/"+file+".pdf",files.read(file),total);
            }
            zip.finish();
            var stored=files.put(output.toByteArray(),"period-"+period+".zip",0);
            jdbc.update("INSERT INTO period_archive(period_id,file_id,document_count,created_by,archive_sha256,archive_size) VALUES (?,?,?,?,?,?)",period,stored.id(),documents.size(),actor,stored.sha256(),stored.sizeBytes());
        } catch(IOException failure) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo generar el archivo histórico. El cierre se revirtió.",failure); }
        return overview(period,email);
    }
    /** Full internal ZIP for institutional custody; recording this download is a precondition for the purge. */
    @Transactional public byte[] custody(UUID period,String email) {
        admin(email);
        var rows=jdbc.queryForList("SELECT file_id FROM period_archive WHERE period_id=? FOR UPDATE",period);
        if(rows.isEmpty()||rows.getFirst().get("file_id")==null) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Copia de custodia no disponible.");
        byte[] bytes=files.read((UUID)rows.getFirst().get("file_id"));var actor=accounts.current(email).id();
        jdbc.update("UPDATE period_archive SET custody_downloaded_at=now(),custody_downloaded_by=? WHERE period_id=?",actor,period);
        audit.record(actor,"PERIOD_ARCHIVE_CUSTODY_DOWNLOADED",period);
        return bytes;
    }
    /** Deletes every document, file and the internal ZIP of a closed period, keeping an inventory and the audit trail. */
    @Transactional public Map<String,Object> purge(UUID period,String email,Purge input) {
        admin(email);
        var rows=jdbc.queryForList("SELECT a.*,p.status,p.closed_at,p.closed_at+make_interval(days=>?)<=now() AS grace_elapsed FROM period_archive a JOIN academic_period p ON p.id=a.period_id WHERE a.period_id=? FOR UPDATE OF a,p",purgeGraceDays,period);
        if(rows.isEmpty()||!"CLOSED".equals(rows.getFirst().get("status"))) throw new ResponseStatusException(HttpStatus.CONFLICT,"Solo se elimina la documentación de un período cerrado con archivo.");
        var archive=rows.getFirst();
        if(archive.get("purged_at")!=null) throw new ResponseStatusException(HttpStatus.CONFLICT,"La documentación de este período ya fue eliminada.");
        if(archive.get("custody_downloaded_at")==null) throw new ResponseStatusException(HttpStatus.CONFLICT,"Descargue primero la copia de custodia del período.");
        if(!(boolean)archive.get("grace_elapsed")) throw new ResponseStatusException(HttpStatus.CONFLICT,"El período de gracia de "+purgeGraceDays+" días desde el cierre todavía no termina.");
        var documents=jdbc.queryForList("SELECT id FROM work_plan WHERE period_id=?",UUID.class,period);
        if(jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE period_id<>? AND (base_plan_id IN (SELECT id FROM work_plan WHERE period_id=?) OR previous_version_id IN (SELECT id FROM work_plan WHERE period_id=?))",Integer.class,period,period,period)>0)
            throw new ResponseStatusException(HttpStatus.CONFLICT,"Hay documentos de otros períodos que dependen de este período; no se eliminó nada.");
        var inventory=jdbc.queryForList("SELECT w.id,w.document_type,w.title,w.document_state,w.formal_version,g.name AS group_name FROM work_plan w JOIN institutional_group g ON g.id=w.group_id WHERE w.period_id=? ORDER BY g.name,w.document_type,w.formal_version,w.id",period);
        var fileIds=new LinkedHashSet<UUID>();for(var document:documents) fileIds.addAll(fileIds(document));fileIds.add((UUID)archive.get("file_id"));
        String reviews="SELECT r.id FROM document_review r JOIN work_plan w ON w.id=r.work_plan_id WHERE w.period_id=?";
        jdbc.update("DELETE FROM review_observation WHERE review_id IN ("+reviews+")",period);
        jdbc.update("DELETE FROM review_task WHERE review_id IN ("+reviews+")",period);
        jdbc.update("DELETE FROM document_review WHERE work_plan_id IN (SELECT id FROM work_plan WHERE period_id=?)",period);
        jdbc.update("DELETE FROM work_report WHERE id IN (SELECT id FROM work_plan WHERE period_id=?)",period);
        for(String table:List.of("signed_document_artifact","document_artifact","document_attachment","execution_evidence","activity_extension","deadline_extension","deadline_extension_history","deadline_notice_delivery","document_author","document_access","work_plan_matrix"))
            jdbc.update("DELETE FROM "+table+" WHERE work_plan_id IN (SELECT id FROM work_plan WHERE period_id=?)",period);
        jdbc.update("DELETE FROM work_plan WHERE period_id=?",period);
        var actor=accounts.current(email).id();
        jdbc.update("UPDATE period_archive SET file_id=NULL,purged_at=now(),purged_by=?,purge_reason=?,purge_inventory=?::jsonb WHERE period_id=?",actor,input.reason().trim(),json.writeValueAsString(inventory),period);
        fileIds.remove(null);files.delete(fileIds);
        audit.record(actor,"PERIOD_DOCUMENTS_PURGED",period);
        return overview(period,email);
    }
    @Transactional public byte[] download(UUID period,String email) {
        var archives=jdbc.queryForList("SELECT a.file_id,p.name,p.closed_at FROM period_archive a JOIN academic_period p ON p.id=a.period_id WHERE a.period_id=? AND p.status='CLOSED'",period);
        if(archives.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Archivo histórico no disponible.");
        if(archives.getFirst().get("file_id")==null) throw new ResponseStatusException(HttpStatus.GONE,"La documentación de este período fue eliminada. Solicite la copia de custodia a administración.");
        var permitted=new HashSet<UUID>();
        for(var document:jdbc.queryForList("SELECT id FROM work_plan WHERE period_id=?",UUID.class,period)) {
            try { plans.get(document,email);permitted.add(document); }
            catch(ResponseStatusException denied) { if(denied.getStatusCode()!=HttpStatus.NOT_FOUND) throw denied; }
        }
        if(permitted.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"No tiene documentos autorizados en este archivo histórico.");
        var downloadable=new HashMap<UUID,Set<UUID>>();
        for(var document:permitted) downloadable.put(document,new HashSet<>(jdbc.queryForList("""
            SELECT current_file_id FROM document_review WHERE work_plan_id=? AND status='APPROVED'
            UNION SELECT file_id FROM execution_evidence WHERE work_plan_id=? AND file_id IS NOT NULL
            UNION SELECT file_id FROM document_attachment WHERE work_plan_id=? AND removed_at IS NULL
            UNION SELECT a.file_id FROM document_artifact a JOIN work_plan w ON w.id=a.work_plan_id WHERE w.id=? AND w.scope_id IS NULL
            UNION SELECT a.file_id FROM signed_document_artifact a JOIN work_plan w ON w.id=a.work_plan_id WHERE w.id=? AND w.scope_id IS NULL
            """,UUID.class,document,document,document,document,document)));
        // ADMIN is not a document grant. Re-authorize at every download; never expose the full internal ZIP.
        var archive=archives.getFirst();
        try(var source=new ZipInputStream(new ByteArrayInputStream(files.read((UUID)archive.get("file_id"))));var output=new ByteArrayOutputStream();var zip=new ZipOutputStream(output)) {
            var total=new long[]{0};
            entry(zip,"period.json",metadata(Map.of("id",period,"name",archive.get("name"),"closedAt",archive.get("closed_at").toString(),"authorizedDocuments",permitted.stream().sorted().toList(),"scope","Solo documentos autorizados para la cuenta al descargar", "filePolicy","PDFs finales aprobados, evidencias y anexos vigentes. Preliminares por ámbito y anexos retirados permanecen en el archivo interno; no se habilita su descarga por esta ruta.")),total);
            for(var document:permitted) entry(zip,"documents/"+document+"/download.json",metadata(Map.of("includedFileIds",downloadable.get(document).stream().sorted().toList(),"note","El manifest conserva el historial completo; algunos archivos históricos pueden estar excluidos de esta descarga por su estado.")),total);
            ZipEntry item;
            while((item=source.getNextEntry())!=null) {
                var parts=item.getName().split("/");
                if(parts.length<3||!parts[0].equals("documents")) continue;
                UUID document=UUID.fromString(parts[1]);
                if(permitted.contains(document)) {
                    if(parts.length==4&&parts[2].equals("files")&&!downloadable.get(document).contains(UUID.fromString(parts[3].replace(".pdf","")))) continue;
                    entry(zip,item.getName(),source.readAllBytes(),total);
                }
            }
            zip.finish();audit.record(accounts.current(email).id(),"PERIOD_ARCHIVE_DOWNLOADED",period);
            return output.toByteArray();
        } catch(IOException failure) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo leer el archivo histórico.",failure); }
    }
}
