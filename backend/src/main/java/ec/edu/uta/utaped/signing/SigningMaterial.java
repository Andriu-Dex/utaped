package ec.edu.uta.utaped.signing;

import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Request-local sensitive material. Close always clears both decoded and incoming buffers. */
public final class SigningMaterial implements AutoCloseable {
    private final byte[] body; public byte[] container; public char[] password;
    public SigningMaterial(byte[] body) {
        this.body=body;
        try {
            if(body==null||body.length<6||body.length>1049092) throw new IllegalArgumentException();
            int length=ByteBuffer.wrap(body,0,4).getInt();if(length<1||length>512||4+length>=body.length) throw new IllegalArgumentException();
            var decoded=StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(body,4,length));
            password=new char[decoded.remaining()];decoded.get(password);if(decoded.hasArray()) Arrays.fill(decoded.array(),'\0');
            container=Arrays.copyOfRange(body,4+length,body.length);
        } catch(Exception e) { close();throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Material de firma no válido."); }
    }
    @Override public void close() { if(body!=null) Arrays.fill(body,(byte)0);if(password!=null) Arrays.fill(password,'\0');if(container!=null) Arrays.fill(container,(byte)0); }
}
