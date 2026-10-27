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
import ec.edu.uta.utaped.workflow.ReviewService;

@Service
public class AuthorSignatureService {
    public record Signed(UUID id,UUID artifactId,String signerName,String inputHash,String outputHash,OffsetDateTime signedAt,
        int pageCount,int signaturePageIndex,String identityCheck,String issuerChainCheck,String revocationCheck,UUID actorId,String actorName,String certificateSubject,String certificateFingerprint) {}
    public record State(SignaturePreparation preparation,boolean signingEnabled,List<Blocker> signingBlockers,List<Signed> signed,boolean automaticSubmission,String documentState,List<ec.edu.uta.utaped.planning.DocumentAuthors.Author> pendingAuthors,boolean actorSigned) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;
    private final ArtifactService artifacts;private final WorkPlanService plans;private final StoredFiles files;private final AuditService audit;
    private final boolean allowHttp;private final Semaphore operations=new Semaphore(1);
    private final ReviewService reviews;
    public AuthorSignatureService(JdbcTemplate jdbc,Accounts accounts,ArtifactService artifacts,WorkPlanService plans,
        StoredFiles files,AuditService audit,@Value("${app.signing.allow-http:false}") boolean allowHttp,ReviewService reviews) {
        this.jdbc=jdbc;this.accounts=accounts;this.artifacts=artifacts;this.plans=plans;this.files=files;this.audit=audit;this.allowHttp=allowHttp;
        this.reviews=reviews;
    }
    private void transport(boolean secure) { if(!secure && !allowHttp) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La firma requiere una conexión HTTPS."); }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
    public State state(UUID doc,UUID artifact,String email,boolean secure) {
        var preparation=artifacts.signaturePreparation(doc,artifact,email);var plan=plans.get(doc,email);
        var actor=accounts.current(email);var signed=list(doc,artifact,email);
        var source=artifacts.list(doc,email).stream().filter(a->a.id().equals(artifact)).findFirst().orElseThrow();
        var snapshot=frozen(doc,artifact);var authors=snapshot.resolvedAuthors();
        var pending=authors.stream().filter(a->signed.stream().noneMatch(signature->signature.actorId().equals(a.id()))).toList();
        var blockers=new ArrayList<Blocker>(preparation.blockers().stream().filter(b->!Set.of("Flujo","Validación institucional","Política de firma","Período").contains(b.section()) && (signed.isEmpty()||!b.section().equals("Artefacto"))).toList());
        if(!plans.signatureWindowOpen(doc,email)) blockers.add(new Blocker("Período","La elaboración o recopilación de firmas no está habilitada."));
        if(source.sourceRowVersion()!=plan.rowVersion()) blockers.add(new Blocker("Versión","El artefacto corresponde a otro contenido o ronda."));
        if(!secure && !allowHttp) blockers.add(new Blocker("Conexión","La firma requiere HTTPS."));
        if(pending.isEmpty()||!pending.getFirst().id().equals(actor.id())) blockers.add(new Blocker("Turno","Solo el siguiente elaborador pendiente puede firmar."));
        if(source.signatureSlots().stream().noneMatch(slot->actor.id().equals(slot.actorId()) && slot.action().equals("ELABORADO_POR") && slot.bounds()!=null)) blockers.add(new Blocker("Ubicación","No existe ubicación de firma para esta persona."));
        blockers.addAll(reviews.submissionBlockers(doc,artifact,email));
        String hash=signed.isEmpty()?preparation.pdfHash():signed.getFirst().outputHash();
        preparation=new SignaturePreparation(preparation.targetDocId(),preparation.artifactId(),plan.rowVersion(),hash,preparation.artifactCurrent(),preparation.workflow(),preparation.mechanism(),blockers.isEmpty(),List.copyOf(blockers));
        return new State(preparation,blockers.isEmpty(),List.copyOf(blockers),signed,plan.scopeId()!=null,plan.documentState(),pending,signed.stream().anyMatch(signature->signature.actorId().equals(actor.id())));
    }
    private Snapshot frozen(UUID doc,UUID artifact) {
        return jdbc.query("SELECT snapshot::text FROM document_artifact WHERE work_plan_id=? AND id=?",(rs,n)->tools.jackson.databind.json.JsonMapper.builder().build().readValue(rs.getString(1),Snapshot.class),doc,artifact).getFirst();
    }

    private int signaturePage(Artifact source,UUID actor) { return source.signatureSlots().stream().filter(slot->actor.equals(slot.actorId()) && slot.action().equals("ELABORADO_POR")).findFirst().orElseThrow().pageIndex(); }
    public List<Signed> list(UUID doc,UUID artifact,String email) {
        plans.get(doc,email);
        var source=artifacts.list(doc,email).stream().filter(a->a.id().equals(artifact)).findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Artefacto no disponible."));
        return jdbc.query("SELECT s.*,f.page_count FROM signed_document_artifact s JOIN stored_file f ON f.id=s.file_id WHERE s.work_plan_id=? AND s.artifact_id=? ORDER BY s.signature_order DESC,s.signed_at DESC",
            (rs,n)->new Signed(rs.getObject("id",UUID.class),artifact,rs.getString("signer_name"),rs.getString("input_hash"),rs.getString("output_hash"),rs.getObject("signed_at",OffsetDateTime.class),rs.getInt("page_count"),
                signaturePage(source,rs.getObject("actor_id",UUID.class)),(rs.getString("certificate_fingerprint")==null?"ADMINISTRATIVE_CERTIFICATE_PIN":"CERTIFICATE_PROVIDED"),"NOT_CHECKED","NOT_CHECKED",rs.getObject("actor_id",UUID.class),rs.getString("actor_name"),rs.getString("certificate_subject"),rs.getString("certificate_fingerprint")),doc,artifact);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public Signed sign(UUID doc,UUID artifact,String email,long rowVersion,String inputHash,UUID requestKey,byte[] body,boolean secure) {
        return sign(doc,artifact,email,rowVersion,inputHash,requestKey,body,secure,null);
    }
    @Transactional(isolation=Isolation.REPEATABLE_READ)
    public Signed sign(UUID doc,UUID artifact,String email,long rowVersion,String inputHash,UUID requestKey,byte[] body,boolean secure,String certificateFingerprint) {
        char[] password=null;byte[] container=null;
        try {
            transport(secure);var actor=accounts.current(email);
            jdbc.queryForList("SELECT id FROM app_user WHERE id=? FOR UPDATE",actor.id());
            var plan=plans.lock(doc,email);
            jdbc.queryForList("SELECT m.user_id FROM membership m JOIN institutional_group g ON g.id=m.group_id WHERE m.user_id=? AND m.group_id=? AND g.active FOR SHARE OF m,g",actor.id(),plan.groupId());
            plan=plans.get(doc,email);
            var previous=jdbc.queryForList("SELECT work_plan_id,artifact_id,input_hash,source_row_version FROM signed_document_artifact WHERE actor_id=? AND request_key=?",actor.id(),requestKey);
            if(!previous.isEmpty()) {
                var old=previous.getFirst();
                if(!doc.equals(old.get("work_plan_id")) || !artifact.equals(old.get("artifact_id")) || !Objects.equals(inputHash,old.get("input_hash")) || rowVersion!=((Number)old.get("source_row_version")).longValue())
                    throw new ResponseStatusException(HttpStatus.CONFLICT,"La solicitud ya corresponde a otra operación de firma.");
                var result=list(doc,artifact,email).stream().filter(signature->signature.actorId().equals(actor.id())).findFirst().orElseThrow();files.read(signedFile(doc,result.id(),email));return result;
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
            Pkcs12PdfSigner.CertificateDetails certificate;
            try(var material=new SigningMaterial(body.clone())) { certificate=material.certificate(certificateFingerprint); }
            catch(Pkcs12PdfSigner.SigningException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); }
            if(!operations.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Hay otra firma en curso. Intente nuevamente.");
            byte[] signed;var now=Instant.now();
            try {
                var previousSignatures=list(doc,artifact,email);
                signed=new Pkcs12PdfSigner().signProvided(previousSignatures.isEmpty()?artifacts.content(doc,artifact,email):content(doc,previousSignatures.getFirst().id(),email),container,password,certificate,now,slot,!previousSignatures.isEmpty());
            }
            catch(Pkcs12PdfSigner.SigningException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); }
            finally { operations.release(); }
            var stored=files.put(signed,"plan-t1-firmado.pdf",source.pageCount());UUID id=UUID.randomUUID();
            jdbc.update("INSERT INTO signed_document_artifact(id,work_plan_id,artifact_id,actor_id,binding_id,file_id,request_key,source_row_version,input_hash,output_hash,signer_name,signed_at,signature_order,actor_name,certificate_subject,certificate_issuer,certificate_serial,certificate_fingerprint,certificate_valid_until) VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id,doc,artifact,actor.id(),null,stored.id(),requestKey,rowVersion,inputHash,stored.sha256(),certificate.holder(),OffsetDateTime.ofInstant(now,ZoneOffset.UTC),state.signed().size(),actor.displayName(),certificate.subject(),certificate.issuer(),certificate.serial(),certificate.fingerprint(),OffsetDateTime.ofInstant(certificate.validUntil(),ZoneOffset.UTC));
            audit.record(actor.id(),plan.documentType()+"_ARTIFACT_SIGNED",doc);if(state.pendingAuthors().size()==1) reviews.start(doc,artifact,id,email);
            else audit.record(actor.id(),"AUTHOR_SIGNATURE_REQUESTED",doc,state.pendingAuthors().get(1).id());return list(doc,artifact,email).getFirst();
        } catch(java.nio.charset.CharacterCodingException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Contraseña con codificación no válida."); }
        finally { if(body!=null) Arrays.fill(body,(byte)0);if(password!=null) Arrays.fill(password,'\0');if(container!=null) Arrays.fill(container,(byte)0); }
    }
    private UUID signedFile(UUID doc,UUID signedId,String email) {
        plans.get(doc,email);
        return jdbc.query("SELECT file_id FROM signed_document_artifact WHERE id=? AND work_plan_id=?",(rs,n)->rs.getObject(1,UUID.class),signedId,doc).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Documento firmado no disponible."));
    }
    public byte[] content(UUID doc,UUID signedId,String email) { return files.read(signedFile(doc,signedId,email)); }
    public byte[] export(UUID doc,UUID signedId,String email) {
        signedFile(doc,signedId,email);
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,"El PDF final se descarga desde la revisión aprobada.");
    }
    public byte[] page(UUID doc,UUID signedId,int index,String email) { return artifacts.renderPage(content(doc,signedId,email),index); }
}
