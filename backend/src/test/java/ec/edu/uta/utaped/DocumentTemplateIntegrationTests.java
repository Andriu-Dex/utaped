package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import com.jayway.jsonpath.JsonPath;
import java.net.*;
import java.net.http.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.*;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DocumentTemplateIntegrationTests {
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    UUID admin, user, group, period;
    final JsonMapper json = JsonMapper.builder().build();

    class Browser {
        final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL)).build();
        String token;
        HttpResponse<String> request(String method, String path, Object body, boolean csrf) throws Exception {
            var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
            if (body != null) builder.header("Content-Type", path.endsWith("/login") ? "application/x-www-form-urlencoded" : "application/json");
            if (csrf) builder.header("X-CSRF-TOKEN", token);
            return client.send(builder.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body instanceof String s ? s : json.writeValueAsString(body))).build(), HttpResponse.BodyHandlers.ofString());
        }
        void login(String email) throws Exception {
            token = JsonPath.read(request("GET", "/api/auth/csrf", null, false).body(), "$.token");
            assertEquals(204, request("POST", "/api/auth/login", "username=" + email + "&password=Test-only-password-2026&captcha=" + LoginChallenges.answer(client, port, jdbc), true).statusCode());
            token = JsonPath.read(request("GET", "/api/auth/csrf", null, false).body(), "$.token");
        }
    }

    @BeforeEach void seed() {
        assertEquals("utaped_test", jdbc.queryForObject("SELECT current_database()", String.class));
        jdbc.execute("TRUNCATE app_user, institutional_group, academic_period, auth_throttle, SPRING_SESSION CASCADE");
        admin = UUID.randomUUID();
        user = UUID.randomUUID();
        group = UUID.randomUUID();
        period = UUID.randomUUID();

        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,?,false)",
            admin, "admin@example.invalid", "Admin", encoder.encode("Test-only-password-2026"), "ADMIN");
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,?,false)",
            user, "user@example.invalid", "User", encoder.encode("Test-only-password-2026"), "USER");

        jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,'Comisión Académica','COMMISSION')", group);
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')", user, group);
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Periodo 2026',(now() at time zone 'America/Guayaquil')::date-1,(now() at time zone 'America/Guayaquil')::date+90,(now() at time zone 'America/Guayaquil')::date-1,(now() at time zone 'America/Guayaquil')::date+30,(now() at time zone 'America/Guayaquil')::date+31,(now() at time zone 'America/Guayaquil')::date+60)", period);
    }

    @Test void templateChangesOnlySeedNewDocumentsAndDoNotRewriteExistingContent() throws Exception {
        var a=new Browser();a.login("admin@example.invalid");var u=new Browser();u.login("user@example.invalid");
        var create=Map.of("groupId",group,"periodId",period,"requestKey",UUID.randomUUID(),"title","Plan original");
        var initial=u.request("POST","/api/work-plans",create,true);assertEquals(200,initial.statusCode());
        String id=JsonPath.read(initial.body(),"$.id");String original=JsonPath.read(initial.body(),"$.justification");
        long row=((Number)JsonPath.read(initial.body(),"$.rowVersion")).longValue();
        var clear=Map.of("rowVersion",row,"title","Plan original","institutionalUnit","FISEI — Universidad Técnica de Ambato","career","","justification","","objective","");
        assertEquals(200,u.request("PUT","/api/work-plans/"+id,clear,true).statusCode());
        var template=a.request("GET","/api/admin/document-templates/T1",null,false);
        long version=((Number)JsonPath.read(template.body(),"$.rowVersion")).longValue();
        assertEquals(200,a.request("PUT","/api/admin/document-templates/T1",Map.of("rowVersion",version,"justification","Nueva justificación base"),true).statusCode());
        assertEquals("",JsonPath.read(u.request("GET","/api/work-plans/"+id,null,false).body(),"$.justification"));
        var next=u.request("POST","/api/work-plans",Map.of("groupId",group,"periodId",period,"requestKey",UUID.randomUUID(),"title","Plan nuevo"),true);
        assertEquals(200,next.statusCode());assertEquals("Nueva justificación base",JsonPath.read(next.body(),"$.justification"));
        assertFalse(original.isBlank());
        assertEquals(403,u.request("PUT","/api/admin/document-templates/T1",Map.of("rowVersion",version+1,"justification","No autorizado"),true).statusCode());
        a.request("POST","/api/admin/document-templates/T1/reset",Map.of("rowVersion",version+1),true);
    }

    @Test void adminCanListUpdateAndResetDocumentTemplates() throws Exception {
        var a = new Browser();
        a.login("admin@example.invalid");
        var u = new Browser();
        u.login("user@example.invalid");

        // 1. Regular user gets 403 on admin template endpoint
        var forbidden = u.request("GET", "/api/admin/document-templates", null, false);
        assertEquals(403, forbidden.statusCode());

        // 2. Admin gets list of templates
        var listRes = a.request("GET", "/api/admin/document-templates", null, false);
        assertEquals(200, listRes.statusCode());
        List<Map<String, Object>> templates = JsonPath.read(listRes.body(), "$");
        assertFalse(templates.isEmpty());
        var t1 = templates.stream().filter(t -> "T1".equals(t.get("documentType"))).findFirst().orElseThrow();
        assertEquals("Plan de Trabajo (Formato T1)", t1.get("name"));
        Number rowVersion = (Number) t1.get("rowVersion");
        assertNotNull(rowVersion);

        // 3. User checks work plan options and sees default justification
        var optionsRes = u.request("GET", "/api/work-plans/options", null, false);
        assertEquals(200, optionsRes.statusCode());
        String defaultJust = JsonPath.read(optionsRes.body(), "$.defaultJustification");
        assertTrue(defaultJust.contains("FISEI"));

        // 4. Admin updates the justification with custom institutional text
        String customJustification = "Justificación institucional actualizada por el Administrador para el período 2026.";
        var updatePayload = Map.of("rowVersion", rowVersion.longValue(), "justification", customJustification);

        // Concurrency test: wrong rowVersion should return 409 Conflict
        var conflictRes = a.request("PUT", "/api/admin/document-templates/T1", Map.of("rowVersion", 99999L, "justification", customJustification), true);
        assertEquals(409, conflictRes.statusCode());

        // Successful update
        var updateRes = a.request("PUT", "/api/admin/document-templates/T1", updatePayload, true);
        assertEquals(200, updateRes.statusCode());
        assertEquals(customJustification, JsonPath.read(updateRes.body(), "$.justification"));
        assertEquals(rowVersion.longValue() + 1, ((Number) JsonPath.read(updateRes.body(), "$.rowVersion")).longValue());

        // Verify audit event was logged
        var auditCount = jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='INSTITUTIONAL_TEMPLATE_UPDATED'", Integer.class);
        assertEquals(1, auditCount);

        // 5. User checks work-plans/options again: now receives the administrator's updated text!
        optionsRes = u.request("GET", "/api/work-plans/options", null, false);
        assertEquals(200, optionsRes.statusCode());
        assertEquals(customJustification, JsonPath.read(optionsRes.body(), "$.defaultJustification"));

        // 6. Admin resets the template to factory default
        long newVersion = ((Number) JsonPath.read(updateRes.body(), "$.rowVersion")).longValue();
        var resetRes = a.request("POST", "/api/admin/document-templates/T1/reset", Map.of("rowVersion", newVersion), true);
        assertEquals(200, resetRes.statusCode());
        String resetJust = JsonPath.read(resetRes.body(), "$.justification");
        assertTrue(resetJust.contains("FISEI"));

        // Verify audit event for reset
        var resetAuditCount = jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='INSTITUTIONAL_TEMPLATE_RESET'", Integer.class);
        assertEquals(1, resetAuditCount);
    }
}
