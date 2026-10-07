package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import static org.awaitility.Awaitility.await;
import com.jayway.jsonpath.JsonPath;
import ec.edu.uta.utaped.identity.AuthThrottle;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT)
class WorkPlanIntegrationTests {
    @Value("${local.server.port}") int port;
    @Autowired JdbcTemplate jdbc;
    @Autowired PasswordEncoder encoder;
    UUID admin,user,outsider,group;
    static final String PASSWORD="Test-only-password-2026";
    class Browser {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).build();
        String token;
        HttpResponse<String> request(String method,String path,String body,boolean csrf) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if(body!=null) builder.header("Content-Type",path.endsWith("/login")?"application/x-www-form-urlencoded":"application/json");
            if(csrf) builder.header("X-CSRF-TOKEN",token);
            return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        }
        void csrf() throws Exception { token=JsonPath.read(request("GET","/api/auth/csrf",null,false).body(),"$.token"); }
        HttpResponse<String> login(String email,String password) throws Exception {
            csrf(); var response=request("POST","/api/auth/login","username="+email+"&password="+password+"&captcha="+LoginChallenges.answer(client,port,jdbc),true);csrf();return response;
        }
    }
    @BeforeEach void seed() {
        String database=jdbc.queryForObject("SELECT current_database()",String.class);
        assertEquals("utaped_test",database,"Tests must never use the development database");
        jdbc.execute("TRUNCATE app_user,institutional_group,academic_period,auth_throttle,SPRING_SESSION CASCADE");
        admin=UUID.randomUUID();user=UUID.randomUUID();outsider=UUID.randomUUID();group=UUID.randomUUID();
        add(admin,"admin@example.invalid","ADMIN",false);
        add(user,"user@example.invalid","USER",false);
        add(outsider,"other@example.invalid","USER",false);
        jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,'Test group','COMMISSION')",group);
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'MEMBER')",user,group);
    }
    void add(UUID id,String email,String role,boolean temporary) {
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,?,?)",id,email,email,encoder.encode(PASSWORD),role,temporary);
    }

    UUID period;
    void openPeriod() {
        period=UUID.randomUUID();
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Current',(now() at time zone 'America/Guayaquil')::date-10,(now() at time zone 'America/Guayaquil')::date+100,(now() at time zone 'America/Guayaquil')::date-10,(now() at time zone 'America/Guayaquil')::date+10,(now() at time zone 'America/Guayaquil')::date+11,(now() at time zone 'America/Guayaquil')::date+20)",period);
    }
    String createBody(UUID key) { return "{\"groupId\":\""+group+"\",\"periodId\":\""+period+"\",\"requestKey\":\""+key+"\",\"title\":\"Plan inicial\"}"; }
    String updateBody(long version,String title) { return "{\"rowVersion\":"+version+",\"title\":\""+title+"\",\"institutionalUnit\":\"FISEI\",\"career\":\"\",\"justification\":\"Contenido persistente\",\"objective\":\"\"}"; }
    @Test void draftPersistenceIsolationIdempotencyAndConcurrency() throws Exception {
        openPeriod();Browser b=new Browser();b.login("user@example.invalid",PASSWORD);
        UUID key=UUID.randomUUID();String body=createBody(key);
        var created=b.request("POST","/api/work-plans",body,true);assertEquals(200,created.statusCode(),created.body());
        String id=JsonPath.read(created.body(),"$.id");
        assertEquals(user.toString(),JsonPath.read(created.body(),"$.teacherId"));
        assertEquals(id,JsonPath.read(b.request("POST","/api/work-plans",body,true).body(),"$.id"));
        String second=JsonPath.read(b.request("POST","/api/work-plans",createBody(UUID.randomUUID()),true).body(),"$.id");
        assertEquals(200,b.request("PUT","/api/work-plans/"+id,updateBody(0,"Plan corregido"),true).statusCode());
        assertEquals(409,b.request("PUT","/api/work-plans/"+id,updateBody(0,"Obsoleto"),true).statusCode());
        assertEquals(id,JsonPath.read(b.request("POST","/api/work-plans",body,true).body(),"$.id"));
        assertEquals("Plan inicial",JsonPath.read(b.request("GET","/api/work-plans/"+second,null,false).body(),"$.title"));
        Browser fresh=new Browser();fresh.login("user@example.invalid",PASSWORD);
        var persisted=fresh.request("GET","/api/work-plans/"+id,null,false);
        assertEquals("Contenido persistente",JsonPath.read(persisted.body(),"$.justification"));
        assertEquals("1.0",JsonPath.read(persisted.body(),"$.formalVersion"));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='WORK_PLAN_DRAFT_SAVED'",Integer.class));
        assertEquals(1,JsonPath.<Integer>read(b.request("GET","/api/work-plans?query=corregido&size=1",null,false).body(),"$.total"));
    }
    @Test void authorizationWindowAndValidation() throws Exception {
        openPeriod(); Browser b=new Browser();b.login("user@example.invalid",PASSWORD);
        String id=JsonPath.read(b.request("POST","/api/work-plans",createBody(UUID.randomUUID()),true).body(),"$.id");
        Browser outsiderBrowser=new Browser();outsiderBrowser.login("other@example.invalid",PASSWORD);
        assertEquals(403,outsiderBrowser.request("POST","/api/work-plans",createBody(UUID.randomUUID()),true).statusCode());
        assertEquals(404,outsiderBrowser.request("GET","/api/work-plans/"+id,null,false).statusCode());
        assertEquals(404,outsiderBrowser.request("PUT","/api/work-plans/"+id,updateBody(0,"Intrusion"),true).statusCode());
        Browser a=new Browser();a.login("admin@example.invalid",PASSWORD);
        assertEquals(404,a.request("GET","/api/work-plans/"+id,null,false).statusCode());
        assertEquals(403,b.request("PUT","/api/work-plans/"+id,updateBody(0,"No CSRF"),false).statusCode());
        assertEquals(400,b.request("GET","/api/work-plans?size=51",null,false).statusCode());
        assertEquals(400,b.request("POST","/api/work-plans","{}",true).statusCode());
        jdbc.update("UPDATE academic_period SET preparation_starts_on=(now() at time zone 'America/Guayaquil')::date-10,preparation_ends_on=(now() at time zone 'America/Guayaquil')::date-1 WHERE id=?",period);
        assertEquals(false,JsonPath.read(b.request("GET","/api/work-plans/"+id,null,false).body(),"$.editable"));
        assertEquals(403,b.request("PUT","/api/work-plans/"+id,updateBody(0,"Closed"),true).statusCode());
        assertEquals(403,b.request("POST","/api/work-plans",createBody(UUID.randomUUID()),true).statusCode());
        jdbc.update("DELETE FROM membership WHERE user_id=?",user);
        assertEquals(404,b.request("GET","/api/work-plans/"+id,null,false).statusCode());
    }
}
