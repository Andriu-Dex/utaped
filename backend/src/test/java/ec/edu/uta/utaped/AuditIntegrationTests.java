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
class AuditIntegrationTests {
    @Value("${local.server.port}") int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;
    UUID admin,user,group;final JsonMapper json=JsonMapper.builder().build();
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String token;
        HttpResponse<String> request(String method,String path,Object body,boolean csrf) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));if(body!=null) builder.header("Content-Type",path.endsWith("/login")?"application/x-www-form-urlencoded":"application/json");if(csrf) builder.header("X-CSRF-TOKEN",token);
            return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body instanceof String s?s:json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        void login(String email) throws Exception { token=JsonPath.read(request("GET","/api/auth/csrf",null,false).body(),"$.token");assertEquals(204,request("POST","/api/auth/login","username="+email+"&password=Test-only-password-2026&captcha="+LoginChallenges.answer(client,port,jdbc),true).statusCode());token=JsonPath.read(request("GET","/api/auth/csrf",null,false).body(),"$.token"); }
    }
    @BeforeEach void seed() {
        assertEquals("utaped_test",jdbc.queryForObject("SELECT current_database()",String.class));jdbc.execute("TRUNCATE app_user,institutional_group,academic_period,auth_throttle,SPRING_SESSION CASCADE");admin=UUID.randomUUID();user=UUID.randomUUID();group=UUID.randomUUID();
        for(var pair:Map.of(admin,"admin",user,"user").entrySet()) jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,?,false)",pair.getKey(),pair.getValue()+"@example.invalid",pair.getValue(),encoder.encode("Test-only-password-2026"),pair.getKey().equals(admin)?"ADMIN":"USER");
        jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,'Group','COMMISSION')",group);jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')",user,group);
    }

    @Test void filteredPaginationIsStableAndContainsNoSecrets() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");var u=new Browser();u.login("user@example.invalid");UUID target=UUID.randomUUID();
        for(int n=0;n<25;n++) jdbc.update("INSERT INTO audit_event(id,actor_id,action,target_id,occurred_at) VALUES (?,?,'TEST_EVENT',?,'2026-01-01T12:00:00Z')",UUID.randomUUID(),user,target);
        String path="/api/admin/audit-events?action=TEST_EVENT&actorId="+user+"&targetId="+target+"&size=10";
        var first=b.request("GET",path,null,false);assertEquals(200,first.statusCode());assertEquals(25,JsonPath.<Integer>read(first.body(),"$.total"));
        var ids=JsonPath.<List<String>>read(first.body(),"$.items[*].id");var second=b.request("GET",path+"&page=1",null,false);assertTrue(Collections.disjoint(ids,JsonPath.<List<String>>read(second.body(),"$.items[*].id")));
        assertEquals(ids,JsonPath.<List<String>>read(b.request("GET",path,null,false).body(),"$.items[*].id"));assertFalse(first.body().contains("password"));assertFalse(first.body().contains("@example.invalid"));
        assertEquals(25,JsonPath.<Integer>read(b.request("GET",path+"&from=2026-01-01T12%3A00%3A00Z&to=2026-01-01T12%3A00%3A00Z",null,false).body(),"$.total"));
        assertEquals(0,JsonPath.<Integer>read(b.request("GET",path+"&from=2026-01-02T00%3A00%3A00Z",null,false).body(),"$.total"));
        assertEquals(400,b.request("GET",path+"&from=2026-01-02T00%3A00%3A00Z&to=2026-01-01T00%3A00%3A00Z",null,false).statusCode());
        assertEquals(400,b.request("GET","/api/admin/audit-events?size=101",null,false).statusCode());assertEquals(400,b.request("GET","/api/admin/audit-events?actorId=invalid",null,false).statusCode());
        assertEquals(0,JsonPath.<Integer>read(b.request("GET","/api/admin/audit-events?action=%27%20OR%201%3D1--",null,false).body(),"$.total"));
        assertEquals(403,u.request("GET",path,null,false).statusCode());assertEquals(403,u.request("GET","/api/admin/audit-events/actions",null,false).statusCode());
        assertTrue(b.request("GET","/api/admin/audit-events/actions",null,false).body().contains("TEST_EVENT"));
    }
    @Test void newEventsPreserveActorNamesAndLegacyEventsRemainExplicit() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");jdbc.update("UPDATE app_user SET display_name='Renamed' WHERE id=?",admin);
        var events=b.request("GET","/api/admin/audit-events?action=LOGIN_SUCCEEDED",null,false);
        assertEquals("admin",JsonPath.read(events.body(),"$.items[0].actorName"));assertEquals(true,JsonPath.read(events.body(),"$.items[0].historicalActorName"));
        jdbc.update("INSERT INTO audit_event(id,actor_id,action) VALUES (?,?,'LEGACY')",UUID.randomUUID(),admin);
        var legacy=b.request("GET","/api/admin/audit-events?action=LEGACY",null,false);assertEquals("Renamed",JsonPath.read(legacy.body(),"$.items[0].actorName"));assertEquals(false,JsonPath.read(legacy.body(),"$.items[0].historicalActorName"));
        jdbc.update("INSERT INTO audit_event(id,action) VALUES (?,'SYSTEM_EVENT')",UUID.randomUUID());assertNull(JsonPath.read(b.request("GET","/api/admin/audit-events?action=SYSTEM_EVENT",null,false).body(),"$.items[0].actorName"));
    }
    @Test void documentHistoryUsesExactIdentityAndRevocableScope() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");var u=new Browser();u.login("user@example.invalid");UUID period=UUID.randomUUID();
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Current',(now() at time zone 'America/Guayaquil')::date-10,(now() at time zone 'America/Guayaquil')::date+100,(now() at time zone 'America/Guayaquil')::date-10,(now() at time zone 'America/Guayaquil')::date+10,(now() at time zone 'America/Guayaquil')::date+11,(now() at time zone 'America/Guayaquil')::date+20)",period);
        String id=JsonPath.read(u.request("POST","/api/work-plans",Map.of("groupId",group,"periodId",period,"requestKey",UUID.randomUUID(),"title","History one"),true).body(),"$.id");
        String other=JsonPath.read(u.request("POST","/api/work-plans",Map.of("groupId",group,"periodId",period,"requestKey",UUID.randomUUID(),"title","History two"),true).body(),"$.id");
        var result=u.request("GET","/api/work-plans/"+id+"/history",null,false);assertEquals(200,result.statusCode());assertEquals(1,JsonPath.<Integer>read(result.body(),"$.total"));assertEquals(id,JsonPath.read(result.body(),"$.items[0].targetId"));assertFalse(result.body().contains(other));assertFalse(result.body().contains("LOGIN_SUCCEEDED"));
        assertEquals(404,b.request("GET","/api/work-plans/"+id+"/history",null,false).statusCode());assertEquals(404,u.request("GET","/api/work-plans/"+UUID.randomUUID()+"/history",null,false).statusCode());
        jdbc.update("DELETE FROM membership WHERE user_id=? AND group_id=?",user,group);assertEquals(404,u.request("GET","/api/work-plans/"+id+"/history",null,false).statusCode());assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='WORK_PLAN_CREATED'",Integer.class));
    }
}

