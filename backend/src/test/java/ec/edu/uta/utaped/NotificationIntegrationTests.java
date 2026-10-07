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
class NotificationIntegrationTests {
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
    @Autowired ec.edu.uta.utaped.audit.AuditService events;
    @Autowired ec.edu.uta.utaped.notifications.NotificationWriter writer;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactions;
    String plan(Browser u) throws Exception {
        UUID period=UUID.randomUUID();jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Period',(now() at time zone 'America/Guayaquil')::date-10,(now() at time zone 'America/Guayaquil')::date+100,(now() at time zone 'America/Guayaquil')::date-10,(now() at time zone 'America/Guayaquil')::date+10,(now() at time zone 'America/Guayaquil')::date+11,(now() at time zone 'America/Guayaquil')::date+20)",period);
        return JsonPath.read(u.request("POST","/api/work-plans",Map.of("groupId",group,"periodId",period,"requestKey",UUID.randomUUID(),"title","Exact target"),true).body(),"$.id");
    }
    @Test void membershipNotificationsArePersonalIdempotentAndRespectCurrentAccess() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");var u=new Browser();u.login("user@example.invalid");String path="/api/admin/groups/"+group+"/members";
        var input=Map.of("userId",user,"membershipRole","COORDINATOR");assertEquals(200,b.request("POST",path,input,true).statusCode());assertEquals(200,b.request("POST",path,input,true).statusCode());
        var result=u.request("GET","/api/notifications",null,false);assertEquals(1,JsonPath.<Integer>read(result.body(),"$.total"));assertEquals(true,JsonPath.read(result.body(),"$.items[0].targetAvailable"));String id=JsonPath.read(result.body(),"$.items[0].id");
        assertEquals(0,JsonPath.<Integer>read(b.request("GET","/api/notifications",null,false).body(),"$.total"));assertEquals(404,b.request("POST","/api/notifications/"+id+"/read",null,true).statusCode());assertEquals(403,u.request("POST","/api/notifications/"+id+"/read",null,false).statusCode());
        var opened=u.request("POST","/api/notifications/"+id+"/open",null,true);assertEquals(200,opened.statusCode());assertEquals(group.toString(),JsonPath.read(opened.body(),"$.id"));assertEquals("GROUP",JsonPath.read(opened.body(),"$.type"));
        assertEquals(0,JsonPath.<Integer>read(u.request("GET","/api/notifications/unread-count",null,false).body(),"$.unread"));
        assertEquals(200,b.request("DELETE",path+"/"+user,null,true).statusCode());assertEquals(200,b.request("DELETE",path+"/"+user,null,true).statusCode());
        var unavailable=u.request("GET","/api/notifications",null,false);assertEquals(2,JsonPath.<Integer>read(unavailable.body(),"$.total"));assertEquals(false,JsonPath.read(unavailable.body(),"$.items[0].targetAvailable"));assertEquals(404,u.request("POST","/api/notifications/"+id+"/open",null,true).statusCode());
    }
    @Test void documentTargetsPaginationReadingAndDedupeStayWithinRecipient() throws Exception {
        var u=new Browser();u.login("user@example.invalid");String target=plan(u);for(int n=0;n<25;n++) events.record(user,"T1_PREVIEW_GENERATED",UUID.fromString(target));
        var result=u.request("GET","/api/notifications?read=false&size=10&page=2",null,false);assertEquals(25,JsonPath.<Integer>read(result.body(),"$.total"));assertEquals(5,JsonPath.<Integer>read(result.body(),"$.items.length()"));String id=JsonPath.read(result.body(),"$.items[0].id");
        UUID event=jdbc.queryForObject("SELECT source_event_id FROM personal_notification WHERE id=?",UUID.class,UUID.fromString(id));writer.fromEvent(event,user,"T1_PREVIEW_GENERATED",UUID.fromString(target),null);assertEquals(25,jdbc.queryForObject("SELECT count(*) FROM personal_notification",Integer.class));
        var opened=u.request("POST","/api/notifications/"+id+"/open",null,true);assertEquals(target,JsonPath.read(opened.body(),"$.id"));assertEquals("WORK_PLAN",JsonPath.read(opened.body(),"$.type"));
        assertEquals(24,JsonPath.<Integer>read(u.request("POST","/api/notifications/read-all",null,true).body(),"$.updated"));assertEquals(0,JsonPath.<Integer>read(u.request("POST","/api/notifications/read-all",null,true).body(),"$.updated"));assertEquals(0,JsonPath.<Integer>read(u.request("GET","/api/notifications?read=false",null,false).body(),"$.total"));
        jdbc.update("UPDATE institutional_group SET active=false WHERE id=?",group);assertEquals(404,u.request("POST","/api/notifications/"+id+"/open",null,true).statusCode());assertEquals(25,JsonPath.<Integer>read(u.request("GET","/api/notifications?read=true",null,false).body(),"$.total"));assertEquals(400,u.request("GET","/api/notifications?size=101",null,false).statusCode());
    }
    @Test void eventAndNotificationRollbackTogetherWithoutRetroactiveGeneration() throws Exception {
        var u=new Browser();u.login("user@example.invalid");String target=plan(u);
        new org.springframework.transaction.support.TransactionTemplate(transactions).execute(status->{events.record(user,"T1_PREVIEW_GENERATED",UUID.fromString(target));status.setRollbackOnly();return null;});
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM personal_notification",Integer.class));assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='T1_PREVIEW_GENERATED'",Integer.class));
        assertEquals(401,new Browser().request("GET","/api/notifications",null,false).statusCode());
    }
}


