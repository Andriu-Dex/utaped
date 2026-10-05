package ec.edu.uta.utaped.signing;

import ec.edu.uta.utaped.documents.*;
import ec.edu.uta.utaped.documents.ArtifactModels.*;
import ec.edu.uta.utaped.planning.WorkPlanService;
import ec.edu.uta.utaped.identity.Accounts;
import ec.edu.uta.utaped.audit.AuditService;
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.Semaphore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthorSignatureService {
    public record Signed(UUID id,UUID artifactId,String signerName,String inputHash,String outputHash,OffsetDateTime signedAt,
        int pageCount,int signaturePageIndex,String identityCheck,String issuerChainCheck,String revocationCheck) {}
    public record State(SignaturePreparation preparation,boolean signingEnabled,List<Blocker> signingBlockers,List<Signed> signed) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final CertificateBindings bindings;
    private final ArtifactService artifacts;private final WorkPlanService plans;private final StoredFiles files;private final AuditService audit;
    private final boolean allowHttp;private final Semaphore operations=new Semaphore(1);
    public AuthorSignatureService(JdbcTemplate jdbc,Accounts accounts,CertificateBindings bindings,ArtifactService artifacts,WorkPlanService plans,
        StoredFiles files,AuditService audit,@Value("${app.signing.allow-http:false}") boolean allowHttp) {
        this.jdbc=jdbc;this.accounts=accounts;this.bindings=bindings;this.artifacts=artifacts;this.plans=plans;this.files=files;this.audit=audit;this.allowHttp=allowHttp;
    }
    private void transport(boolean secure) { if(!secure && !allowHttp) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La firma requiere una conexión HTTPS."); }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public State state(UUID doc,UUID artifact,String email,boolean secure) {
        var preparation=artifacts.signaturePreparation(doc,artifact,email);var plan=plans.get(doc,email);
        var blockers=new ArrayList<Blocker>(preparation.blockers().stream().filter(b->!Set.of("Flujo","Validación institucional","Política de firma").contains(b.section())).toList());
        if(!secure && !allowHttp) blockers.add(new Blocker("Conexión","La firma requiere HTTPS. HTTP solo puede habilitarse explícitamente para desarrollo local aislado."));
        if(bindings.current(plan.teacherId())==null) blockers.add(new Blocker("Identidad","Administración debe vincular previamente la huella de su certificado público después de comprobar su identidad."));
        var source=artifacts.list(doc,email).stream().filter(a->a.id().equals(artifact)).findFirst().orElseThrow();
        if(source.signatureSlots().stream().noneMatch(s->plan.teacherId().equals(s.actorId()) && s.action().equals("ELABORADO_POR") && s.bounds()!=null))
            blockers.add(new Blocker("Ubicación","Genere una nueva previsualización con ubicación visible de firma."));
        var signed=list(doc,artifact,email);
        if(!signed.isEmpty()) blockers.add(new Blocker("Firma","Este artefacto ya tiene una firma del elaborador. Consulte el PDF firmado conservado."));
        return new State(preparation,blockers.isEmpty(),List.copyOf(blockers),signed);
    }
    public List<Signed> list(UUID doc,UUID artifact,String email) {
        plans.get(doc,email);
        var source=artifacts.list(doc,email).stream().filter(a->a.id().equals(artifact)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Artefacto no disponible."));
        return jdbc.query("SELECT s.*,f.page_count FROM signed_document_artifact s JOIN stored_file f ON f.id=s.file_id WHERE s.work_plan_id=? AND s.artifact_id=? ORDER BY s.signed_at DESC",
            (rs,n)->new Signed(rs.getObject("id",UUID.class),artifact,rs.getString("signer_name"),rs.getString("input_hash"),rs.getString("output_hash"),rs.getObject("signed_at",OffsetDateTime.class),rs.getInt("page_count"),
                source.signatureSlots().getFirst().pageIndex(),"ADMINISTRATIVE_CERTIFICATE_PIN","NOT_CHECKED","NOT_CHECKED"),doc,artifact);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public Signed sign(UUID doc,UUID artifact,String email,long rowVersion,String inputHash,UUID requestKey,byte[] body,boolean secure) {
        char[] password=null;byte[] container=null;
        try {
            transport(secure);var actor=accounts.current(email);
            jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",actor.id());
            var plan=plans.get(doc,email);
            jdbc.queryForList("SELECT id FROM work_plan WHERE id=? FOR UPDATE",doc);
            jdbc.queryForList("SELECT m.user_id FROM membership m JOIN institutional_group g ON g.id=m.group_id WHERE m.user_id=? AND m.group_id=? AND g.active FOR SHARE OF m,g",actor.id(),plan.groupId());
            plan=plans.get(doc,email);
            var previous=jdbc.queryForList("SELECT work_plan_id,artifact_id,input_hash,source_row_version FROM signed_document_artifact WHERE actor_id=? AND request_key=?",actor.id(),requestKey);
            if(!previous.isEmpty()) {
                var old=previous.getFirst();
                if(!doc.equals(old.get("work_plan_id")) || !artifact.equals(old.get("artifact_id")) || !Objects.equals(inputHash,old.get("input_hash")) || rowVersion!=((Number)old.get("source_row_version")).longValue())
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya corresponde a otra operación de firma.");
                var result=list(doc,artifact,email).getFirst();files.read(signedFile(doc,result.id(),email));return result;
            }
            if(rowVersion!=plan.rowVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"El Plan cambió. Genere o consulte una previsualización vigente.");
            var state=state(doc,artifact,email,secure);
            if(!state.signingEnabled()) throw new ResponseStatusException(HttpStatus.CONFLICT,"No se puede firmar. Revise los requisitos de firma del artefacto.");
            if(!Objects.equals(inputHash,state.preparation().pdfHash())) throw new ResponseStatusException(HttpStatus.CONFLICT,"El hash no corresponde al artefacto seleccionado.");
            if(body==null || body.length<6 || body.length>1049092) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"Contenedor de firma vacío o demasiado grande.");
            int length=ByteBuffer.wrap(body,0,4).getInt();
            if(length<1 || length>512 || 4+length>=body.length) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Material de firma no válido.");
            var decoded=StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(body,4,length));
            password=new char[decoded.remaining()];decoded.get(password);if(decoded.hasArray()) Arrays.fill(decoded.array(),'\0');
            container=Arrays.copyOfRange(body,4+length,body.length);
            var source=artifacts.list(doc,email).stream().filter(a->a.id().equals(artifact)).findFirst().orElseThrow();
            var slot=source.signatureSlots().stream().filter(s->s.actorId().equals(actor.id()) && s.action().equals("ELABORADO_POR")).findFirst().orElseThrow();
            var binding=bindings.current(actor.id());
            if(!operations.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Hay otra firma en curso. Intente nuevamente.");
            byte[] signed;var now=Instant.now();
            try { signed=new Pkcs12PdfSigner().signPinned(artifacts.content(doc,artifact,email),container,password,binding.fingerprint(),now,slot,actor.displayName()); }
            catch(Pkcs12PdfSigner.SigningException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); }
            finally { operations.release(); }
            var stored=files.put(signed,"plan-t1-firmado.pdf",source.pageCount());UUID id=UUID.randomUUID();
            jdbc.update("INSERT INTO signed_document_artifact(id,work_plan_id,artifact_id,actor_id,binding_id,file_id,request_key,source_row_version,input_hash,output_hash,signer_name,signed_at) VALUES (?,?,?,?,?,?,?,?,?,?,?,?)",
                id,doc,artifact,actor.id(),binding.id(),stored.id(),requestKey,rowVersion,inputHash,stored.sha256(),actor.displayName(),OffsetDateTime.ofInstant(now,ZoneOffset.UTC));
            audit.record(actor.id(),"T1_ARTIFACT_SIGNED",doc);return list(doc,artifact,email).getFirst();
        } catch(java.nio.charset.CharacterCodingException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Contraseña con codificación no válida."); }
        finally { if(body!=null) Arrays.fill(body,(byte)0);if(password!=null) Arrays.fill(password,'\0');if(container!=null) Arrays.fill(container,(byte)0); }
    }
    private UUID signedFile(UUID doc,UUID signedId,String email) {
        plans.get(doc,email);
        return jdbc.query("SELECT file_id FROM signed_document_artifact WHERE id=? AND work_plan_id=?",(rs,n)->rs.getObject(1,UUID.class),signedId,doc).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento firmado no disponible."));
    }
    public byte[] content(UUID doc,UUID signedId,String email) { return files.read(signedFile(doc,signedId,email)); }
    public byte[] page(UUID doc,UUID signedId,int index,String email) { return artifacts.renderPage(content(doc,signedId,email),index); }
}
