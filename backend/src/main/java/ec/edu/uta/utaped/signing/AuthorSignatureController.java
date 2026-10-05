package ec.edu.uta.utaped.signing;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.security.Principal;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/work-plans/{targetDocId}")
public class AuthorSignatureController {
    private final AuthorSignatureService signatures;
    public AuthorSignatureController(AuthorSignatureService signatures) { this.signatures=signatures; }
    @GetMapping("/artifacts/{artifactId}/signing") public ResponseEntity<AuthorSignatureService.State> state(@PathVariable UUID targetDocId,@PathVariable UUID artifactId,Principal p,HttpServletRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(signatures.state(targetDocId,artifactId,p.getName(),request.isSecure()));
    }
    @PostMapping(value="/artifacts/{artifactId}/signing",consumes=MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<AuthorSignatureService.Signed> sign(@PathVariable UUID targetDocId,@PathVariable UUID artifactId,@RequestParam long rowVersion,@RequestParam String inputHash,@RequestParam UUID requestKey,Principal p,HttpServletRequest request) throws IOException {
        if(request.getContentLengthLong()>1049092) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE,"El material de firma supera el límite técnico.");
        byte[] body=request.getInputStream().readNBytes(1049093);
        try { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(signatures.sign(targetDocId,artifactId,p.getName(),rowVersion,inputHash,requestKey,body,request.isSecure())); }
        finally { Arrays.fill(body,(byte)0); }
    }
    @GetMapping("/signed-artifacts/{signedId}/content") public ResponseEntity<byte[]> content(@PathVariable UUID targetDocId,@PathVariable UUID signedId,Principal p) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.APPLICATION_PDF).header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=\"plan-t1-firmado.pdf\"").body(signatures.content(targetDocId,signedId,p.getName()));
    }
    @GetMapping("/signed-artifacts/{signedId}/pages/{pageIndex}") public ResponseEntity<byte[]> page(@PathVariable UUID targetDocId,@PathVariable UUID signedId,@PathVariable int pageIndex,Principal p) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(signatures.page(targetDocId,signedId,pageIndex,p.getName()));
    }
}
