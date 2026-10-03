package ec.edu.uta.utaped.documents;

import static ec.edu.uta.utaped.documents.AttachmentModels.*;
import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.planning.WorkPlanService;
import java.io.IOException;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AttachmentService {
    private final JdbcTemplate jdbc;private final WorkPlanService plans;private final StoredFiles files;
    private final PdfValidation pdf;private final AuditService audit;private final Accounts accounts;
    private final long maxFileBytes,maxTotalBytes;private final int maxAttachments;
    public AttachmentService(JdbcTemplate jdbc,WorkPlanService plans,StoredFiles files,PdfValidation pdf,AuditService audit,Accounts accounts,
        @Value("${app.documents.max-file-bytes:10485760}") long maxFileBytes,@Value("${app.documents.max-total-bytes:52428800}") long maxTotalBytes,
        @Value("${app.documents.max-attachments:20}") int maxAttachments) {
        this.jdbc=jdbc;this.plans=plans;this.files=files;this.pdf=pdf;this.audit=audit;this.accounts=accounts;
        this.maxFileBytes=maxFileBytes;this.maxTotalBytes=maxTotalBytes;this.maxAttachments=maxAttachments;
    }
    public static String label(int index) {
        StringBuilder result=new StringBuilder();int value=index+1;
        while(value>0) { value--;result.insert(0,(char)('A'+value%26));value/=26; }
        return "Anexo "+result;
    }
    public State get(UUID targetDocId,String email) {
        var plan=plans.get(targetDocId,email);var settings=jdbc.queryForMap("SELECT attachments_enabled,privacy_notice_enabled FROM work_plan WHERE id=?",targetDocId);
        var items=jdbc.query("""
            SELECT a.*,f.original_name,f.size_bytes,f.page_count,f.sha256 FROM document_attachment a
            JOIN stored_file f ON f.id=a.file_id WHERE a.work_plan_id=? AND a.removed_at IS NULL ORDER BY a.sort_order,a.id
            """,(rs,n)->new Attachment(rs.getObject("id",UUID.class),rs.getObject("file_id",UUID.class),label(n),rs.getString("title"),rs.getString("description"),rs.getString("original_name"),rs.getLong("size_bytes"),rs.getInt("page_count"),rs.getString("sha256")),targetDocId);
        return new State(plan.rowVersion(),plan.editable(),(boolean)settings.get("attachments_enabled"),(boolean)settings.get("privacy_notice_enabled"),maxFileBytes,maxTotalBytes,maxAttachments,items);
    }
    public State lock(UUID targetDocId,String email) {
        plans.get(targetDocId,email);jdbc.queryForList("SELECT id FROM work_plan WHERE id=? FOR UPDATE",targetDocId);return get(targetDocId,email);
    }
    private void requireEditable(State state,long version) {
        if(!state.editable()) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El documento está en modo de solo lectura.");
        if(state.rowVersion()!=version) throw new ResponseStatusException(HttpStatus.CONFLICT,"El documento cambió en otra sesión. Recargue antes de modificar anexos.");
    }
    private void changed(UUID targetDocId,String email,String action) {
        jdbc.update("UPDATE work_plan SET row_version=row_version+1,updated_at=now() WHERE id=?",targetDocId);
        audit.record(accounts.current(email).id(),action,targetDocId);
    }
    @Transactional public State settings(UUID targetDocId,String email,Settings input) {
        var state=lock(targetDocId,email);requireEditable(state,input.rowVersion());
        if(!input.enabled() && !state.items().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Quite los anexos antes de seleccionar No.");
        jdbc.update("UPDATE work_plan SET attachments_enabled=?,privacy_notice_enabled=? WHERE id=?",input.enabled(),input.privacyNoticeEnabled(),targetDocId);
        changed(targetDocId,email,"DOCUMENT_ATTACHMENT_SETTINGS_SAVED");return get(targetDocId,email);
    }
    @Transactional public State upload(UUID targetDocId,String email,long version,UUID requestKey,String title,MultipartFile upload) {
        var state=lock(targetDocId,email);
        if(title==null || title.isBlank() || title.length()>200) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Escriba el título del anexo (máximo 200 caracteres).");
        if(upload.isEmpty() || upload.getSize()>maxFileBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"El archivo está vacío o supera el límite permitido.");
        byte[] bytes;try { bytes=upload.getBytes(); } catch(IOException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"No se pudo leer el archivo."); }
        String sha=StoredFiles.hash(bytes);
        var prior=jdbc.queryForList("SELECT a.removed_at,a.title,f.sha256 FROM document_attachment a JOIN stored_file f ON f.id=a.file_id WHERE a.work_plan_id=? AND a.request_key=?",targetDocId,requestKey);
        if(!prior.isEmpty()) {
            var previous=prior.getFirst();
            if(previous.get("removed_at")!=null || !previous.get("sha256").equals(sha) || !previous.get("title").equals(title.trim())) throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya fue utilizada para otro archivo o título.");
            return state;
        }
        requireEditable(state,version);
        if(!state.enabled()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Seleccione Sí para agregar anexos.");
        if(state.items().size()>=maxAttachments || state.items().stream().mapToLong(Attachment::sizeBytes).sum()+bytes.length>maxTotalBytes) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Se alcanzó el límite de anexos o tamaño total del Plan.");
        int pages=pdf.pages(bytes);String name=Objects.requireNonNullElse(upload.getOriginalFilename(),"anexo.pdf").replace('\\','/');name=name.substring(name.lastIndexOf('/')+1).replaceAll("[\\p{Cntrl}]","");
        if(name.isBlank()) name="anexo.pdf";if(name.length()>200) name=name.substring(name.length()-200);
        var file=files.put(bytes,name,pages);UUID id=UUID.randomUUID();
        jdbc.update("INSERT INTO document_attachment(id,work_plan_id,request_key,file_id,title,sort_order) VALUES (?,?,?,?,?,?)",id,targetDocId,requestKey,file.id(),title.trim(),state.items().size());
        changed(targetDocId,email,"DOCUMENT_ATTACHMENT_ADDED");return get(targetDocId,email);
    }
    private Attachment find(State state,UUID attachmentId) { return state.items().stream().filter(a->a.id().equals(attachmentId)).findFirst().orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Anexo no disponible.")); }
    @Transactional public State update(UUID targetDocId,UUID attachmentId,String email,Update input) {
        var state=lock(targetDocId,email);requireEditable(state,input.rowVersion());find(state,attachmentId);
        jdbc.update("UPDATE document_attachment SET title=?,description=? WHERE id=? AND work_plan_id=? AND removed_at IS NULL",input.title().trim(),input.description(),attachmentId,targetDocId);
        changed(targetDocId,email,"DOCUMENT_ATTACHMENT_UPDATED");return get(targetDocId,email);
    }
    @Transactional public State remove(UUID targetDocId,UUID attachmentId,String email,long version) {
        var state=lock(targetDocId,email);requireEditable(state,version);find(state,attachmentId);
        jdbc.update("UPDATE document_attachment SET removed_at=now() WHERE id=? AND work_plan_id=?",attachmentId,targetDocId);
        int position=0;for(var a:state.items()) if(!a.id().equals(attachmentId)) jdbc.update("UPDATE document_attachment SET sort_order=? WHERE id=?",position++,a.id());
        changed(targetDocId,email,"DOCUMENT_ATTACHMENT_REMOVED");return get(targetDocId,email);
    }
    @Transactional public State reorder(UUID targetDocId,String email,Reorder input) {
        var state=lock(targetDocId,email);requireEditable(state,input.rowVersion());
        var ids=new HashSet<>(input.attachmentIds());var expected=new HashSet<UUID>();state.items().forEach(a->expected.add(a.id()));
        if(ids.size()!=input.attachmentIds().size() || !ids.equals(expected)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"El orden debe incluir exactamente los anexos de este Plan.");
        for(int i=0;i<input.attachmentIds().size();i++) jdbc.update("UPDATE document_attachment SET sort_order=? WHERE id=? AND work_plan_id=?",i,input.attachmentIds().get(i),targetDocId);
        changed(targetDocId,email,"DOCUMENT_ATTACHMENTS_REORDERED");return get(targetDocId,email);
    }
    public Attachment metadata(UUID targetDocId,UUID attachmentId,String email) { return find(get(targetDocId,email),attachmentId); }
    public byte[] content(UUID targetDocId,UUID attachmentId,String email) { return files.read(metadata(targetDocId,attachmentId,email).fileId()); }
}
