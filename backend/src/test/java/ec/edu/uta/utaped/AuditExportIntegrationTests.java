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

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditExportIntegrationTests {
    @Value("${local.server.port}") int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;
    UUID admin,user,group;final JsonMapper json=JsonMapper.builder().build();
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String token;
        HttpResponse<String> request(String method,String path,Object body,boolean csrf) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));if(body!=null) builder.header("Content-Type",path.endsWith("/login")?"application/x-www-form-urlencoded":"application/json");if(csrf) builder.header("X-CSRF-TOKEN",token);
            return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body instanceof String s?s:json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        void login(String email) throws Exception { token=JsonPath.read(request("GET","/api/auth/csrf",null,false).body(),"$.token");assertEquals(204,request("POST","/api/auth/login","username="+email+"&password=Test-only-password-2026",true).statusCode());token=JsonPath.read(request("GET","/api/auth/csrf",null,false).body(),"$.token"); }
    }
    @BeforeEach void seed() {
        assertEquals("utaped_test",jdbc.queryForObject("SELECT current_database()",String.class));jdbc.execute("TRUNCATE app_user,institutional_group,academic_period,auth_throttle,SPRING_SESSION CASCADE");admin=UUID.randomUUID();user=UUID.randomUUID();group=UUID.randomUUID();
        for(var pair:Map.of(admin,"admin",user,"user").entrySet()) jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,?,false)",pair.getKey(),pair.getValue()+"@example.invalid",pair.getValue(),encoder.encode("Test-only-password-2026"),pair.getKey().equals(admin)?"ADMIN":"USER");
        jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,'Group','COMMISSION')",group);jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')",user,group);
    }
    @Autowired ec.edu.uta.utaped.audit.AuditService events;
    @Autowired ec.edu.uta.utaped.identity.Accounts accounts;
    @Autowired ec.edu.uta.utaped.planning.WorkPlanService plans;
    @Test void csvUsesAppliedFiltersAllRowsAndSafeHeadersWithoutSecrets() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");UUID target=UUID.randomUUID();for(int n=0;n<30;n++) events.record(user,"TEST_EVENT",target);
        events.record(admin,"OTHER_EVENT",UUID.randomUUID());
        var exported=b.request("GET","/api/admin/audit-events/export?action=TEST_EVENT&targetId="+target,null,false);assertEquals(200,exported.statusCode());assertEquals(31,exported.body().lines().count());assertTrue(exported.body().startsWith("\uFEFF\"Evento\""));assertFalse(exported.body().contains("OTHER_EVENT"));assertFalse(exported.body().contains("password_hash"));assertFalse(exported.body().contains("@example.invalid"));
        assertEquals("text/csv;charset=UTF-8",exported.headers().firstValue("Content-Type").orElse(""));assertTrue(exported.headers().firstValue("Content-Disposition").orElse("").contains("utaped-auditoria.csv"));assertTrue(exported.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='AUDIT_EXPORTED' AND actor_id=?",Integer.class,admin));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM personal_notification",Integer.class));
    }
    @Test void formulaLikeNamesAreMarkedAsTextAndQuotesStayInsideCells() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");String[] names={"=1+2"," \t@SUM(1)","＠SUM(1)","=SUM(1,2)\";ignored","\uFEFF=1+2","Normal, \"Nombre\""};
        for(String name:names) { jdbc.update("UPDATE app_user SET display_name=? WHERE id=?",name,user);events.record(user,"FORMULA_TEST",UUID.randomUUID()); }
        var result=b.request("GET","/api/admin/audit-events/export?action=FORMULA_TEST",null,false);assertEquals(200,result.statusCode());
        for(int n=0;n<5;n++) assertTrue(result.body().contains("\"Texto: "+names[n].replace("\"","\"\"")+"\""));assertTrue(result.body().contains("\"Normal, \"\"Nombre\"\"\""));
    }
    @Test void overLimitAndDeniedExportsDoNotProduceFilesOrSuccessEvents() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");var u=new Browser();u.login("user@example.invalid");for(int n=0;n<3;n++) events.record(user,"LIMIT_TEST",UUID.randomUUID());
        var limited=new ec.edu.uta.utaped.audit.AuditQueryService(jdbc,accounts,plans,events,2);
        var failure=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->limited.export("admin@example.invalid","LIMIT_TEST",null,null,null,null));assertEquals(409,failure.getStatusCode().value());
        assertEquals(403,u.request("GET","/api/admin/audit-events/export",null,false).statusCode());assertEquals(401,new Browser().request("GET","/api/admin/audit-events/export",null,false).statusCode());assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='AUDIT_EXPORTED'",Integer.class));
    }
    @Test void emptyRangesInvalidInputAndBoundsMatchViewerContract() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");jdbc.update("INSERT INTO audit_event(id,actor_id,action,occurred_at) VALUES (?,?,'AT_TIME','2026-01-01T12:00:00Z')",UUID.randomUUID(),user);
        String base="/api/admin/audit-events/export?action=AT_TIME";assertEquals(2,b.request("GET",base+"&from=2026-01-01T12%3A00%3A00Z&to=2026-01-01T12%3A00%3A00Z",null,false).body().lines().count());assertEquals(1,b.request("GET",base+"&from=2026-01-02T12%3A00%3A00Z",null,false).body().lines().count());
        assertEquals(400,b.request("GET",base+"&from=2026-01-02T12%3A00%3A00Z&to=2026-01-01T12%3A00%3A00Z",null,false).statusCode());assertEquals(400,b.request("GET",base+"&actorId=invalid",null,false).statusCode());
    }
}


