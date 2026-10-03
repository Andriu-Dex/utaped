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
class ActivityIntegrationTests {
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
            csrf(); var response=request("POST","/api/auth/login","username="+email+"&password="+password,true);csrf();return response;
        }
    }
    @BeforeEach void seed() {
        String database=jdbc.queryForObject("SELECT current_database()",String.class);
        assertEquals("utaped_test",database,"Tests must never use the development database");
        jdbc.execute("TRUNCATE planning_catalog,planning_holiday,app_user,institutional_group,academic_period,auth_throttle,SPRING_SESSION CASCADE");
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
        jdbc.update("INSERT INTO academic_period(id,name,starts_on,ends_on,preparation_starts_on,preparation_ends_on,review_starts_on,review_ends_on) VALUES (?,'Current',CURRENT_DATE-10,CURRENT_DATE+100,CURRENT_DATE-10,CURRENT_DATE+10,CURRENT_DATE+11,CURRENT_DATE+20)",period);
    }
    String createBody(UUID key) { return "{\"groupId\":\""+group+"\",\"periodId\":\""+period+"\",\"requestKey\":\""+key+"\",\"title\":\"Plan inicial\"}"; }
    String updateBody(long version,String title) { return "{\"rowVersion\":"+version+",\"title\":\""+title+"\",\"institutionalUnit\":\"FISEI\",\"career\":\"\",\"justification\":\"Contenido persistente\",\"objective\":\"\"}"; }

    UUID required,resource,means;
    String planId;
    Browser setupMatrix() throws Exception {
        openPeriod();required=UUID.randomUUID();resource=UUID.randomUUID();means=UUID.randomUUID();
        jdbc.update("INSERT INTO activity_catalog(id,group_id,title,category,mandatory) VALUES (?,?,'Required action','POA',true)",required,group);
        jdbc.update("INSERT INTO planning_catalog(id,kind,label) VALUES (?,'RESOURCE','Original resource')",resource);
        jdbc.update("INSERT INTO planning_catalog(id,kind,label) VALUES (?,'MEANS','Original means')",means);
        jdbc.update("UPDATE institutional_group SET collective_label='Configured collective' WHERE id=?",group);
        Browser b=new Browser();b.login("user@example.invalid",PASSWORD);
        planId=JsonPath.read(b.request("POST","/api/work-plans",createBody(UUID.randomUUID()),true).body(),"$.id");
        return b;
    }
    String matrixBody(long version,UUID responsible,String other) {
        String day=java.time.LocalDate.now(java.time.ZoneId.of("America/Guayaquil")).toString();
        return "{\"rowVersion\":"+version+",\"source\":\"Source\",\"activities\":[{\"id\":\""+required+"\",\"catalogId\":\""+required+"\",\"title\":\"Forged title\",\"category\":\"OTHER\",\"mandatory\":false,\"startsOn\":\""+day+"\",\"endsOn\":\""+day+"\",\"responsibleIds\":[\""+responsible+"\"],\"collective\":true,\"resources\":[{\"catalogId\":\""+resource+"\",\"label\":\"Forged resource\"}],\"means\":[{\"catalogId\":null,\"other\":\""+other+"\"}]}]}";
    }
    @Test void requiredActivitiesSnapshotsPersistenceAndAggregateConcurrency() throws Exception {
        Browser b=setupMatrix();String path="/api/work-plans/"+planId+"/matrix";
        assertEquals(1,JsonPath.<Integer>read(b.request("GET",path,null,false).body(),"$.activities.length()"));
        var response=b.request("PUT",path,matrixBody(0,user,"Custom evidence"),true);
        assertEquals(200,response.statusCode(),response.body());
        assertEquals("Required action",JsonPath.read(response.body(),"$.activities[0].title"));
        assertEquals("POA",JsonPath.read(response.body(),"$.activities[0].category"));
        assertEquals(true,JsonPath.read(response.body(),"$.activities[0].mandatory"));
        assertEquals("Original resource",JsonPath.read(response.body(),"$.activities[0].resources[0].label"));
        assertEquals(user.toString(),JsonPath.read(response.body(),"$.activities[0].responsibleIds[0]"));
        assertEquals(409,b.request("PUT",path,matrixBody(0,user,"Stale"),true).statusCode());
        assertEquals(409,b.request("PUT","/api/work-plans/"+planId,updateBody(0,"Stale metadata"),true).statusCode());
        jdbc.update("UPDATE activity_catalog SET title='Changed activity',mandatory=false,active=false WHERE id=?",required);
        jdbc.update("UPDATE planning_catalog SET label='Changed resource',active=false WHERE id=?",resource);
        Browser fresh=new Browser();fresh.login("user@example.invalid",PASSWORD);
        var current=fresh.request("GET",path,null,false);
        assertEquals("Required action",JsonPath.read(current.body(),"$.activities[0].title"));
        assertEquals(200,fresh.request("PUT",path,matrixBody(1,user,"Custom evidence"),true).statusCode());
        String second=JsonPath.read(b.request("POST","/api/work-plans",createBody(UUID.randomUUID()),true).body(),"$.id");
        assertEquals(0,JsonPath.<Integer>read(b.request("GET","/api/work-plans/"+second+"/matrix",null,false).body(),"$.activities.length()"));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='WORK_PLAN_MATRIX_SAVED'",Integer.class));
    }
    @Test void mandatoryMembershipOtherDatesHolidaysAndAuthorization() throws Exception {
        Browser b=setupMatrix();String path="/api/work-plans/"+planId+"/matrix";
        assertEquals(400,b.request("PUT",path,"{\"rowVersion\":0,\"source\":\"\",\"activities\":[]}",true).statusCode());
        assertEquals(400,b.request("PUT",path,matrixBody(0,user,"   "),true).statusCode());
        assertEquals(400,b.request("PUT",path,matrixBody(0,outsider,"Evidence"),true).statusCode());
        String day=java.time.LocalDate.now(java.time.ZoneId.of("America/Guayaquil")).toString();
        assertEquals(400,b.request("PUT",path,matrixBody(0,user,"Evidence").replace(day,"2000-01-01"),true).statusCode());
        jdbc.update("INSERT INTO planning_holiday(holiday_date,label) VALUES (?::date,'Configured holiday')",day);
        jdbc.update("UPDATE academic_period SET restrict_holiday_endpoints=true WHERE id=?",period);
        assertEquals(400,b.request("PUT",path,matrixBody(0,user,"Evidence"),true).statusCode());
        String crossing=matrixBody(0,user,"Evidence").replace("\"startsOn\":\""+day,"\"startsOn\":\""+java.time.LocalDate.parse(day).minusDays(1)).replace("\"endsOn\":\""+day,"\"endsOn\":\""+java.time.LocalDate.parse(day).plusDays(1));
        assertEquals(200,b.request("PUT",path,crossing,true).statusCode());
        jdbc.update("UPDATE academic_period SET restrict_holiday_endpoints=false WHERE id=?",period);
        assertEquals(200,b.request("PUT",path,matrixBody(1,user,"Evidence"),true).statusCode());
        Browser other=new Browser();other.login("other@example.invalid",PASSWORD);
        assertEquals(404,other.request("GET",path,null,false).statusCode());
        assertEquals(404,other.request("PUT",path,matrixBody(1,outsider,"Evidence"),true).statusCode());
        assertEquals(403,b.request("GET","/api/admin/planning/catalogs",null,false).statusCode());
        assertEquals(403,b.request("PUT",path,matrixBody(1,user,"Evidence"),false).statusCode());
        jdbc.update("UPDATE academic_period SET preparation_ends_on=CURRENT_DATE-1 WHERE id=?",period);
        assertEquals(false,JsonPath.read(b.request("GET",path,null,false).body(),"$.editable"));
        jdbc.update("INSERT INTO activity_catalog(id,group_id,title,category,mandatory) VALUES (?,?,'Late required','POA',true)",UUID.randomUUID(),group);
        assertEquals(1,JsonPath.<Integer>read(b.request("GET",path,null,false).body(),"$.activities.length()"));
        assertEquals(403,b.request("PUT",path,matrixBody(1,user,"Evidence"),true).statusCode());
    }
    @Test void catalogsAdminValidationAndNoAutomaticSeedData() throws Exception {
        Browser a=new Browser();a.login("admin@example.invalid",PASSWORD);
        assertEquals("[]",a.request("GET","/api/admin/planning/catalogs",null,false).body());
        var created=a.request("POST","/api/admin/planning/catalogs","{\"kind\":\"RESOURCE\",\"label\":\"Configured\",\"active\":true}",true);
        assertEquals(200,created.statusCode());String id=JsonPath.read(created.body(),"$.id");
        assertEquals(200,a.request("PUT","/api/admin/planning/catalogs/"+id,"{\"kind\":\"RESOURCE\",\"label\":\"Renamed\",\"active\":false}",true).statusCode());
        assertEquals(400,a.request("POST","/api/admin/planning/catalogs","{\"kind\":\"INVALID\",\"label\":\"Bad\"}",true).statusCode());
        assertEquals(200,a.request("PUT","/api/admin/planning/groups/"+group+"/collective-label","{\"label\":\"Configured name\"}",true).statusCode());
        assertEquals(200,a.request("POST","/api/admin/planning/holidays","{\"date\":\"2026-10-03\",\"label\":\"Configured day\"}",true).statusCode());
        assertEquals(200,a.request("DELETE","/api/admin/planning/holidays/2026-10-03",null,true).statusCode());
    }
}
