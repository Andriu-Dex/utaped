package ec.edu.uta.utaped.documents;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StoredFiles {
    public record File(UUID id,String sha256,long sizeBytes,String originalName,int pageCount) {}
    private final Path root;
    private final JdbcTemplate jdbc;
    public StoredFiles(JdbcTemplate jdbc,@Value("${app.documents.storage-root:./runtime-data/documents}") String root) {
        this.jdbc=jdbc;this.root=Path.of(root).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.root);
            if(Files.getFileStore(this.root).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(this.root,PosixFilePermissions.fromString("rwx------"));
        } catch(IOException e) { throw new IllegalStateException("No se pudo inicializar el almacenamiento documental.",e); }
    }
    public static String hash(byte[] bytes) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); }
        catch(java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public File put(byte[] bytes,String originalName,int pageCount) {
        if(!TransactionSynchronizationManager.isSynchronizationActive()) throw new IllegalStateException("File writes require a transaction.");
        UUID id=UUID.randomUUID(),key=UUID.randomUUID();Path path=root.resolve(key.toString());
        try {
            Files.write(path,bytes,StandardOpenOption.CREATE_NEW,StandardOpenOption.WRITE);
            if(Files.getFileStore(root).supportsFileAttributeView("posix")) Files.setPosixFilePermissions(path,PosixFilePermissions.fromString("rw-------"));
        } catch(IOException e) {
            try { Files.deleteIfExists(path); } catch(IOException cleanup) { e.addSuppressed(cleanup); }
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"No se pudo guardar el archivo.");
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if(status==STATUS_ROLLED_BACK) try { Files.deleteIfExists(path); } catch(IOException e) { throw new IllegalStateException("No se pudo limpiar un archivo revertido.",e); }
            }
        });
        String sha=hash(bytes);
        jdbc.update("INSERT INTO stored_file(id,storage_key,sha256,size_bytes,original_name,page_count) VALUES (?,?,?,?,?,?)",id,key,sha,bytes.length,originalName,pageCount);
        return new File(id,sha,bytes.length,originalName,pageCount);
    }
    public File metadata(UUID id) {
        return jdbc.query("SELECT * FROM stored_file WHERE id=?",(rs,n)->new File(id,rs.getString("sha256"),rs.getLong("size_bytes"),rs.getString("original_name"),rs.getInt("page_count")),id).stream().findFirst()
            .orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Archivo no disponible."));
    }
    public byte[] read(UUID id) {
        var meta=metadata(id);UUID key=jdbc.queryForObject("SELECT storage_key FROM stored_file WHERE id=?",UUID.class,id);
        try {
            byte[] bytes=Files.readAllBytes(root.resolve(key.toString()));
            if(bytes.length!=meta.sizeBytes() || !hash(bytes).equals(meta.sha256())) throw new ResponseStatusException(HttpStatus.CONFLICT,"No se pudo verificar la integridad del archivo.");
            return bytes;
        } catch(IOException e) { throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"El archivo no está disponible en el almacenamiento."); }
    }
}
