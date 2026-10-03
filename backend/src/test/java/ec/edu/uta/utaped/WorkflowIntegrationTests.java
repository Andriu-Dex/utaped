package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import com.jayway.jsonpath.JsonPath;
import ec.edu.uta.utaped.workflow.WorkflowModels.*;
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
class WorkflowIntegrationTests {
    @Value("${local.server.port}") int port;@Autowired JdbcTemplate jdbc;@Autowired PasswordEncoder encoder;
    UUID admin,user,group,other;final JsonMapper json=JsonMapper.builder().build();
    class Browser {
        final HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String token;
        HttpResponse<String> request(String method,String path,Object body,boolean csrf) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path));
            if(body!=null) builder.header("Content-Type",path.endsWith("/login")?"application/x-www-form-urlencoded":"application/json");
            if(csrf) builder.header("X-CSRF-TOKEN",token);
            return client.send(builder.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body instanceof String s?s:json.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        void login(String email) throws Exception {
            token=JsonPath.read(request("GET","/api/auth/csrf",null,false).body(),"$.token");
            assertEquals(204,request("POST","/api/auth/login","username="+email+"&password=Test-only-password-2026",true).statusCode());
            token=JsonPath.read(request("GET","/api/auth/csrf",null,false).body(),"$.token");
        }
    }
    @BeforeEach void seed() {
        assertEquals("utaped_test",jdbc.queryForObject("SELECT current_database()",String.class));
        jdbc.execute("TRUNCATE app_user,institutional_group,academic_period,auth_throttle,SPRING_SESSION CASCADE");
        admin=UUID.randomUUID();user=UUID.randomUUID();group=UUID.randomUUID();other=UUID.randomUUID();
        for(var pair:Map.of(admin,"admin",user,"user").entrySet()) jdbc.update("INSERT INTO app_user(id,email,display_name,password_hash,system_role,must_change_password) VALUES (?,?,?,?,?,false)",pair.getKey(),pair.getValue()+"@example.invalid",pair.getValue(),encoder.encode("Test-only-password-2026"),pair.getKey().equals(admin)?"ADMIN":"USER");
        for(UUID id:List.of(group,other)) jdbc.update("INSERT INTO institutional_group(id,name,group_type) VALUES (?,?,'COMMISSION')",id,id.toString());
        jdbc.update("INSERT INTO membership(user_id,group_id,membership_role) VALUES (?,?,'COORDINATOR')",user,group);
    }
    String path(UUID id,String type) { return "/api/admin/groups/"+id+"/workflows/"+type; }
    Stage person() { return new Stage(UUID.randomUUID(),"Revisión técnica","REVIEW","PERSON",List.of(user),null,"",true); }
    @Test void orderedConfigurationImmutableRevisionsAndDocumentIsolation() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");String path=path(group,"T1");
        var first=List.of(person(),new Stage(UUID.randomUUID(),"Decisión colegiada","APPROVE","COLLEGIATE",List.of(),null,"Órgano configurado",false));
        assertEquals(200,b.request("PUT",path,new Save(0L,new Definition("Flujo inicial",first)),true).statusCode());
        var configured=b.request("POST",path+"/revisions",new Version(1L),true);assertEquals(200,configured.statusCode(),configured.body());
        assertEquals(1,JsonPath.<Integer>read(configured.body(),"$.current.revisionNumber"));
        assertEquals(false,JsonPath.read(configured.body(),"$.current.definition.stages[1].requiresSignature"));
        assertEquals(0,JsonPath.<Integer>read(configured.body(),"$.current.definition.stages[1].assigneeIds.length()"));
        var reversed=List.of(first.get(1),first.get(0));
        assertEquals(200,b.request("PUT",path,new Save(2L,new Definition("Flujo cambiado",reversed)),true).statusCode());
        assertEquals("Flujo inicial",JsonPath.read(b.request("GET",path,null,false).body(),"$.current.definition.name"));
        configured=b.request("POST",path+"/revisions",new Version(3L),true);assertEquals(200,configured.statusCode());
        assertEquals("Flujo inicial",JsonPath.read(configured.body(),"$.revisions[1].definition.name"));
        assertEquals("Decisión colegiada",JsonPath.read(configured.body(),"$.current.definition.stages[0].label"));
        assertNull(JsonPath.read(b.request("GET",path(other,"T1"),null,false).body(),"$.current"));
        assertNull(JsonPath.read(b.request("GET",path(group,"T2"),null,false).body(),"$.current"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM work_plan",Integer.class));
        var disabled=b.request("POST",path+"/disable",new Version(4L),true);assertEquals(200,disabled.statusCode());assertNull(JsonPath.read(disabled.body(),"$.current"));
        assertEquals(2,JsonPath.<Integer>read(disabled.body(),"$.revisions.length()"));
    }
    @Test void authorizationCsrfConcurrencyAndInactiveGroup() throws Exception {
        var anonymous=new Browser();assertEquals(401,anonymous.request("GET",path(group,"T1"),null,false).statusCode());
        var userBrowser=new Browser();userBrowser.login("user@example.invalid");assertEquals(403,userBrowser.request("GET",path(group,"T1"),null,false).statusCode());
        var b=new Browser();b.login("admin@example.invalid");String path=path(group,"T1");var body=new Save(0L,new Definition("Flujo",List.of(person())));
        assertEquals(403,b.request("PUT",path,body,false).statusCode());assertEquals(200,b.request("PUT",path,body,true).statusCode());
        assertEquals(409,b.request("PUT",path,body,true).statusCode());assertEquals(409,b.request("POST",path+"/revisions",new Version(0L),true).statusCode());
        jdbc.update("UPDATE institutional_group SET active=false WHERE id=?",group);
        assertEquals(false,JsonPath.read(b.request("GET",path,null,false).body(),"$.groupActive"));
        assertEquals(403,b.request("PUT",path,new Save(1L,body.definition()),true).statusCode());
        assertEquals(400,b.request("GET",path(group,"T3"),null,false).statusCode());
    }
    @Test void validationRejectsUnresolvedRecipientsAndPreservesDraft() throws Exception {
        var b=new Browser();b.login("admin@example.invalid");String path=path(group,"T1");
        var incomplete=new Stage(UUID.randomUUID(),"Etapa pendiente","REVIEW","PERSON",List.of(),null,"",true);
        assertEquals(200,b.request("PUT",path,new Save(0L,new Definition("Incompleto",List.of(incomplete))),true).statusCode());
        assertEquals(400,b.request("POST",path+"/revisions",new Version(1L),true).statusCode());
        var role=new Stage(UUID.randomUUID(),"Coordinación","VALIDATE","GROUP_ROLE",List.of(),"COORDINATOR","",false);
        assertEquals(200,b.request("PUT",path,new Save(1L,new Definition("Rol",List.of(role))),true).statusCode());
        jdbc.update("DELETE FROM membership WHERE group_id=?",group);
        assertEquals(400,b.request("POST",path+"/revisions",new Version(2L),true).statusCode());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM workflow_revision",Integer.class));
        var duplicate=new Save(2L,new Definition("Duplicados",List.of(person(),person())));
        var same=duplicate.definition().stages().getFirst();
        assertEquals(400,b.request("PUT",path,new Save(2L,new Definition("Duplicados",List.of(same,same))),true).statusCode());
        assertEquals(2,JsonPath.<Integer>read(b.request("GET",path,null,false).body(),"$.rowVersion"));
        assertEquals(400,b.request("PUT",path,new Save(2L,new Definition("",List.of())),true).statusCode());
    }
}
