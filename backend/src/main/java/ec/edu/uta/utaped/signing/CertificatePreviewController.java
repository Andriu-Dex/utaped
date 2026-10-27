package ec.edu.uta.utaped.signing;

import ec.edu.uta.utaped.identity.Accounts;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.time.Instant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/signing")
public class CertificatePreviewController {
    private final Accounts accounts;private final boolean allowHttp;
    private final java.util.concurrent.Semaphore inspections=new java.util.concurrent.Semaphore(2);
    public CertificatePreviewController(Accounts accounts,@Value("${app.signing.allow-http:false}") boolean allowHttp) { this.accounts=accounts;this.allowHttp=allowHttp; }
    @PostMapping(value="/certificate-preview",consumes=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Pkcs12PdfSigner.CertificateDetails> inspect(Principal principal,HttpServletRequest request) throws IOException {
        accounts.current(principal.getName());
        if(!request.isSecure()&&!allowHttp) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"La firma requiere HTTPS.");
        if(request.getContentLengthLong()>1049092) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"El archivo supera el límite permitido.");
        if(!inspections.tryAcquire()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Hay otra comprobación en curso. Intente nuevamente.");
        byte[] body=null;
        try {
            body=request.getInputStream().readNBytes(1049093);
            try(var material=new SigningMaterial(body)) {
                return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new Pkcs12PdfSigner().inspect(material.container,material.password,Instant.now()));
            }
        } catch(Pkcs12PdfSigner.SigningException e) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST,e.getMessage()); }
        finally { if(body!=null) java.util.Arrays.fill(body,(byte)0);inspections.release(); }
    }
}
