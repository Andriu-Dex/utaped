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
class AdministrationIntegrationTests {
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
    @Test void directorySearchPaginationBeyondLegacyLimitAndSafeFields() throws Exception {
        jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) SELECT gen_random_uuid(),'bulk-'||n||'@example.invalid','Person '||lpad(n::text,3,'0'),?,'USER',false FROM generate_series(1,220) n",encoder.encode("Test-only-password-2026"));
        var b=new Browser();b.login("admin@example.invalid");var result=b.request("GET","/api/admin/users/directory?query=Person&page=10&size=20",null,false);
        assertEquals(200,result.statusCode());assertEquals(220,JsonPath.<Integer>read(result.body(),"$.total"));assertEquals(20,JsonPath.<Integer>read(result.body(),"$.items.length()"));assertTrue(result.body().contains("Person 220"));assertFalse(result.body().contains("password_hash"));
        assertEquals(1,JsonPath.<Integer>read(b.request("GET","/api/admin/users/directory?query=bulk-220",null,false).body(),"$.total"));
        assertEquals(400,b.request("GET","/api/admin/users/directory?size=500",null,false).statusCode());
        var u=new Browser();u.login("user@example.invalid");assertEquals(403,u.request("GET","/api/admin/users/directory",null,false).statusCode());
    }
    @Test void profileConcurrencyLastAdministratorAndSessionRevocation() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");var u=new Browser();u.login("user@example.invalid");String path="/api/admin/users/"+user;
        var input=Map.of("rowVersion",0,"displayName","Nombre nuevo","systemRole","ADMIN");
        assertEquals(403,b.request("PUT",path,input,false).statusCode());assertEquals(200,b.request("PUT",path,input,true).statusCode());
        assertEquals(401,u.request("GET","/api/auth/me",null,false).statusCode());assertEquals(409,b.request("PUT",path,input,true).statusCode());
        assertEquals(409,b.request("PATCH",path+"/active",Map.of("active",false,"rowVersion",0),true).statusCode());
        assertEquals(200,b.request("PUT",path,Map.of("rowVersion",1,"displayName","Nombre nuevo","systemRole","USER"),true).statusCode());
        assertEquals(409,b.request("PUT","/api/admin/users/"+admin,Map.of("rowVersion",0,"displayName","Admin","systemRole","USER"),true).statusCode());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM app_user WHERE active AND system_role='ADMIN'",Integer.class));
        assertEquals("user@example.invalid",JsonPath.read(b.request("GET",path,null,false).body(),"$.email"));
    }
    @Test void groupDeactivationPreservesDataAndImmediatelyRevokesDocumentScope() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");var u=new Browser();u.login("user@example.invalid");UUID period=UUID.randomUUID();
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Current',CURRENT_DATE-10,CURRENT_DATE+100,CURRENT_DATE-10,CURRENT_DATE+10,CURRENT_DATE+11,CURRENT_DATE+20)",period);
        var created=u.request("POST","/api/work-plans",Map.of("groupId",group,"periodId",period,"requestKey",UUID.randomUUID(),"title","Preserved plan"),true);assertEquals(200,created.statusCode());String id=JsonPath.read(created.body(),"$.id");
        String path="/api/admin/groups/"+group;var disabled=Map.of("rowVersion",0,"name","Renamed","groupType","UNIT","active",false);
        assertEquals(200,b.request("PUT",path,disabled,true).statusCode());assertEquals(404,u.request("GET","/api/work-plans/"+id,null,false).statusCode());assertEquals("[]",u.request("GET","/api/groups",null,false).body());
        assertEquals(409,b.request("PUT",path,disabled,true).statusCode());assertEquals(400,b.request("POST",path+"/members",Map.of("userId",user,"membershipRole","MEMBER"),true).statusCode());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM work_plan",Integer.class));assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM membership",Integer.class));
        assertEquals(200,b.request("PUT",path,Map.of("rowVersion",1,"name","Renamed","groupType","UNIT","active",true),true).statusCode());assertEquals(200,u.request("GET","/api/work-plans/"+id,null,false).statusCode());
        assertEquals(403,u.request("PUT",path,disabled,true).statusCode());
    }
}
