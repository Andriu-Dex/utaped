package ec.edu.uta.utaped.signing;

import jakarta.validation.Valid;
import java.security.Principal;
import java.util.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.*;

@RestController
@RequestMapping("/api/admin/users/{userId}/signing-certificates")
public class CertificateBindingController {
    private final CertificateBindings bindings;
    public CertificateBindingController(CertificateBindings bindings) { this.bindings=bindings; }
    @GetMapping public ResponseEntity<List<CertificateBindings.Binding>> list(@PathVariable UUID userId,Principal p) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(bindings.list(userId,p.getName())); }
    @PostMapping public CertificateBindings.Binding register(@PathVariable UUID userId,@Valid @RequestBody CertificateBindings.Register input,Principal p) { return bindings.register(userId,p.getName(),input); }
    @DeleteMapping("/{bindingId}") public void revoke(@PathVariable UUID userId,@PathVariable UUID bindingId,Principal p) { bindings.revoke(userId,bindingId,p.getName()); }
}
