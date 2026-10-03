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
class TrackingIntegrationTests {
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
    UUID period(boolean open) {
        UUID id=UUID.randomUUID();jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?, ?,CURRENT_DATE-100,CURRENT_DATE+100,?, ?,CURRENT_DATE+11,CURRENT_DATE+20)",id,"Period "+id,java.time.LocalDate.now().minusDays(10),open?java.time.LocalDate.now().plusDays(10):java.time.LocalDate.now().minusDays(1));return id;
    }
    void plan(UUID owner,UUID scope,UUID period,String title) { jdbc.update("INSERT INTO work_plan(id,teacher_id,group_id,period_id,title,creation_title,preparation_date,request_key) VALUES (?,?,?,?,?,?,CURRENT_DATE,?)",UUID.randomUUID(),owner,scope,period,title,title,UUID.randomUUID()); }
    @Test void trackingCountsDatesFiltersAndRecentObjectsUnderExactOwnership() throws Exception {
        UUID open=period(true),closed=period(false);plan(user,group,open,"Editable");plan(user,group,closed,"Read only");plan(admin,group,open,"Other owner");
        var u=new Browser();u.login("user@example.invalid");var result=u.request("GET","/api/tracking",null,false);assertEquals(200,result.statusCode());assertEquals(2,JsonPath.<Integer>read(result.body(),"$.documents"));assertEquals(1,JsonPath.<Integer>read(result.body(),"$.editableDrafts"));assertEquals(1,JsonPath.<Integer>read(result.body(),"$.readOnlyDocuments"));assertEquals(0,JsonPath.<Integer>read(result.body(),"$.withPreview"));assertFalse(result.body().contains("Other owner"));
        assertEquals(1,JsonPath.<Integer>read(u.request("GET","/api/tracking?periodId="+closed,null,false).body(),"$.documents"));assertEquals(0,JsonPath.<Integer>read(u.request("GET","/api/tracking?groupId="+UUID.randomUUID(),null,false).body(),"$.documents"));
        var b=new Browser();b.login("admin@example.invalid");assertEquals(0,JsonPath.<Integer>read(b.request("GET","/api/tracking",null,false).body(),"$.documents"));
        jdbc.update("DELETE FROM membership WHERE user_id=?",user);assertEquals(0,JsonPath.<Integer>read(u.request("GET","/api/tracking",null,false).body(),"$.documents"));
    }
    @Test void trackingRevocationAndRecentLimitDoNotDeleteDocuments() throws Exception {
        UUID period=period(true);for(int n=0;n<8;n++) plan(user,group,period,"Plan "+n);var u=new Browser();u.login("user@example.invalid");
        assertEquals(8,JsonPath.<Integer>read(u.request("GET","/api/tracking",null,false).body(),"$.documents"));assertEquals(5,JsonPath.<Integer>read(u.request("GET","/api/tracking",null,false).body(),"$.recent.items.length()"));
        jdbc.update("UPDATE institutional_group SET active=false WHERE id=?",group);assertEquals(0,JsonPath.<Integer>read(u.request("GET","/api/tracking",null,false).body(),"$.documents"));assertEquals(8,jdbc.queryForObject("SELECT count(*) FROM work_plan",Integer.class));
        assertEquals(401,new Browser().request("GET","/api/tracking",null,false).statusCode());assertEquals(400,u.request("GET","/api/tracking?groupId=invalid",null,false).statusCode());
    }
}


