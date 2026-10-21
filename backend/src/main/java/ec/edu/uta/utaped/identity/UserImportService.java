package ec.edu.uta.utaped.identity;

import ec.edu.uta.utaped.audit.AuditService;
import ec.edu.uta.utaped.institution.OrganizationService;
import jakarta.validation.Validator;
import jakarta.validation.constraints.*;
import java.io.*;
import java.nio.*;
import java.nio.charset.*;
import java.security.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.apache.commons.csv.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.ObjectMapper;

@Service
public class UserImportService {
    public record Input(@NotBlank @Email @Size(max=254) String email,@NotBlank @Pattern(regexp="[a-z0-9][a-z0-9._-]{0,99}") String username,
        @NotBlank @Size(max=80) String firstNames,@NotBlank @Size(max=80) String lastNames,UUID scopeId,String membershipRole,@Size(max=160) String positionLabel) {}
    public record Row(int number,Input data,List<String> errors) {}
    public record Preview(UUID id,String sourceName,String sha256,OffsetDateTime expiresAt,OffsetDateTime committedAt,int importedCount,List<Row> rows,int validCount,int errorCount) {}
    private final JdbcTemplate jdbc;private final Accounts accounts;private final Validator validator;private final PasswordEncoder encoder;
    private final OrganizationService organization;private final AuditService audit;private final ObjectMapper json;
    public UserImportService(JdbcTemplate jdbc,Accounts accounts,Validator validator,PasswordEncoder encoder,OrganizationService organization,AuditService audit,ObjectMapper json) {
        this.jdbc=jdbc;this.accounts=accounts;this.validator=validator;this.encoder=encoder;this.organization=organization;this.audit=audit;this.json=json;
    }
    private Accounts.Account admin(String email) { var actor=accounts.current(email);if(!actor.systemRole().equals("ADMIN")) throw problem(HttpStatus.FORBIDDEN,"Acceso administrativo requerido.");return actor; }
    private ResponseStatusException problem(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    private static final Set<String> REQUIRED=Set.of("email","username","first_names","last_names");
    private static final Set<String> OPTIONAL=Set.of("scope_id","membership_role","position_label");
    private String column(CSVRecord row,String name) { return row.isMapped(name)?row.get(name).trim():""; }
    private List<Row> validate(List<Row> source) {
        var emails=new HashSet<String>();var usernames=new HashSet<String>();var result=new ArrayList<Row>();
        for(var row:source) {
            var input=row.data();var errors=new ArrayList<>(row.errors());
            for(var violation:validator.validate(input)) errors.add(switch(violation.getPropertyPath().toString()) { case "email"->"Correo inválido (máximo 254 caracteres).";case "username"->"Usuario inválido: use letras, números, punto, guion o guion bajo (máximo 100).";case "firstNames"->"Complete nombres (máximo 80).";case "lastNames"->"Complete apellidos (máximo 80).";default->"Cargo supera 160 caracteres."; });
            if((input.firstNames()+" "+input.lastNames()).length()>160) errors.add("El nombre completo supera 160 caracteres.");
            if(!emails.add(input.email())) errors.add("Correo repetido en este archivo.");
            if(!usernames.add(input.username())) errors.add("Usuario repetido en este archivo.");
            if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_user WHERE email=?)",Boolean.class,input.email())) errors.add("El correo ya está registrado; no se sobrescribe la cuenta.");
            if(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM app_user WHERE username=?)",Boolean.class,input.username())) errors.add("El usuario ya está registrado.");
            if(input.scopeId()!=null && !jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM organizational_scope s JOIN institutional_group g ON g.id=s.group_id AND g.active JOIN academic_period p ON p.id=s.period_id AND p.status='ACTIVE' WHERE s.id=? AND s.active)",Boolean.class,input.scopeId())) errors.add("Ámbito inexistente, inactivo o de un período cerrado.");
            if(input.scopeId()==null && (!input.membershipRole().isEmpty()||!input.positionLabel().isEmpty())) errors.add("Cargo/rol requieren scope_id.");
            if(input.scopeId()!=null && !Set.of("MEMBER","COORDINATOR").contains(input.membershipRole())) errors.add("membership_role debe ser MEMBER o COORDINATOR.");
            result.add(new Row(row.number(),input,List.copyOf(new LinkedHashSet<>(errors))));
        }
        return List.copyOf(result);
    }
    @Transactional public Preview preview(String email,MultipartFile file) {
        var actor=admin(email);
        if(file.isEmpty()||file.getSize()>1048576) throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"Use un CSV UTF-8 de hasta 1 MB y 500 filas.");
        byte[] bytes;String text;
        try { bytes=file.getBytes();text=StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString(); }
        catch(IOException error) { throw problem(HttpStatus.BAD_REQUEST,"No se pudo leer el CSV UTF-8."); }
        if(text.startsWith("\uFEFF")) text=text.substring(1);
        char separator=text.lines().findFirst().orElse("").contains(";")?';':',';
        var parsed=new ArrayList<Row>();
        try(var parser=CSVFormat.RFC4180.builder().setDelimiter(separator).setHeader().setSkipHeaderRecord(true).setTrim(true).setIgnoreEmptyLines(true).setDuplicateHeaderMode(DuplicateHeaderMode.DISALLOW).get().parse(new StringReader(text))) {
            var headers=parser.getHeaderNames();
            if(!headers.containsAll(REQUIRED)||headers.stream().anyMatch(h->!REQUIRED.contains(h)&&!OPTIONAL.contains(h))) throw problem(HttpStatus.BAD_REQUEST,"Encabezados requeridos: email,username,first_names,last_names. Opcionales: scope_id,membership_role,position_label. No incluya contraseñas ni permisos ADMIN.");
            for(var record:parser) {
                if(parsed.size()==500) throw problem(HttpStatus.PAYLOAD_TOO_LARGE,"El CSV supera 500 filas.");
                if(!record.isConsistent()) throw problem(HttpStatus.BAD_REQUEST,"La fila "+(record.getRecordNumber()+1)+" no coincide con los encabezados.");
                var errors=new ArrayList<String>();UUID scope=null;String raw=column(record,"scope_id");
                if(!raw.isEmpty()) try { scope=UUID.fromString(raw); } catch(IllegalArgumentException error) { errors.add("scope_id debe ser un UUID válido."); }
                parsed.add(new Row((int)record.getRecordNumber()+1,new Input(Accounts.normalize(column(record,"email")),Accounts.normalize(column(record,"username")),column(record,"first_names"),column(record,"last_names"),scope,column(record,"membership_role").toUpperCase(Locale.ROOT),column(record,"position_label")),errors));
            }
        } catch(IOException|IllegalArgumentException|UncheckedIOException error) { throw problem(HttpStatus.BAD_REQUEST,"CSV inválido: revise comillas, encabezados duplicados y columnas."); }
        if(parsed.isEmpty()) throw problem(HttpStatus.BAD_REQUEST,"Agregue al menos una fila de usuarios.");
        UUID id=UUID.randomUUID();String name=Objects.toString(file.getOriginalFilename(),"usuarios.csv").replaceAll("[\\p{Cntrl}/\\\\]","_");if(name.length()>200) name=name.substring(name.length()-200);
        String digest;try { digest=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)); } catch(NoSuchAlgorithmException error) { throw new IllegalStateException(error); }
        var rows=validate(parsed);jdbc.update("INSERT INTO user_import_batch(id,created_by,source_name,source_sha256,rows) VALUES (?,?,?,?,?::jsonb)",id,actor.id(),name,digest,json.writeValueAsString(rows));
        audit.record(actor.id(),"USER_IMPORT_PREVIEWED",id);return get(id,email);
    }
    public Preview get(UUID id,String email) {
        var actor=admin(email);var data=jdbc.queryForList("SELECT *,rows::text AS row_data FROM user_import_batch WHERE id=? AND created_by=?",id,actor.id());
        if(data.isEmpty()) throw problem(HttpStatus.NOT_FOUND,"Importación no disponible.");var row=data.getFirst();
        List<Row> rows=List.of(json.readValue((String)row.get("row_data"),Row[].class));int valid=(int)rows.stream().filter(r->r.errors().isEmpty()).count();
        return new Preview(id,(String)row.get("source_name"),(String)row.get("source_sha256"),((java.sql.Timestamp)row.get("expires_at")).toInstant().atOffset(java.time.ZoneOffset.UTC),row.get("committed_at")==null?null:((java.sql.Timestamp)row.get("committed_at")).toInstant().atOffset(java.time.ZoneOffset.UTC),((Number)row.get("imported_count")).intValue(),rows,valid,rows.size()-valid);
    }
    @Transactional public byte[] commit(UUID id,String email) {
        var actor=admin(email);jdbc.queryForList("SELECT id FROM user_import_batch WHERE id=? AND created_by=? FOR UPDATE",id,actor.id());var preview=get(id,email);
        if(preview.committedAt()!=null) throw problem(HttpStatus.CONFLICT,"La importación ya se aplicó. Las contraseñas no se conservan; use recuperación administrativa si perdió la descarga.");
        if(!preview.expiresAt().isAfter(OffsetDateTime.now())) throw problem(HttpStatus.GONE,"La previsualización expiró. Cargue el CSV nuevamente.");
        var periods=preview.rows().stream().map(r->r.data().scopeId()).filter(Objects::nonNull).distinct().sorted().toList();
        // Lock all referenced periods before accounts/scopes, matching period closure.
        for(var scope:periods) {
            var ids=jdbc.queryForList("SELECT period_id FROM organizational_scope WHERE id=?",UUID.class,scope);
            if(!ids.isEmpty()) new ec.edu.uta.utaped.institution.PeriodGuard(jdbc).writable(ids.getFirst());
        }
        var rows=validate(preview.rows());if(rows.stream().anyMatch(r->!r.errors().isEmpty())) throw problem(HttpStatus.CONFLICT,"Hay errores o los datos cambiaron. Cargue de nuevo para revisar las filas; no se guardó ningún usuario.");
        var credentials=new StringWriter();var random=new SecureRandom();
        try(var csv=new CSVPrinter(credentials,CSVFormat.RFC4180.builder().setHeader("username","email","temporary_password").get())) {
            for(var row:rows) {
                var input=row.data();byte[] entropy=new byte[18];random.nextBytes(entropy);String password="T-"+Base64.getUrlEncoder().withoutPadding().encodeToString(entropy);Arrays.fill(entropy,(byte)0);UUID user=UUID.randomUUID();
                jdbc.update("INSERT INTO app_user(id,email,username,first_names,last_names,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,?,?,?,'USER',true)",user,input.email(),input.username(),input.firstNames(),input.lastNames(),input.firstNames()+" "+input.lastNames(),encoder.encode(password));
                if(input.scopeId()!=null) organization.member(input.scopeId(),email,new OrganizationService.MemberInput(user,input.membershipRole(),input.positionLabel(),true,0L,false));
                audit.record(actor.id(),"USER_IMPORTED",user);csv.printRecord(input.username(),input.email().matches("^[=+@-].*")?"'"+input.email():input.email(),password);
            }
        } catch(IOException error) { throw new IllegalStateException("Unable to compose import result",error); }
        jdbc.update("UPDATE user_import_batch SET committed_at=now(),rows='[]'::jsonb,imported_count=? WHERE id=?",rows.size(),id);audit.record(actor.id(),"USER_IMPORT_COMMITTED",id);
        return credentials.toString().getBytes(StandardCharsets.UTF_8);
    }
}
