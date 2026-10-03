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
class FoundationIntegrationTests {
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
    @Test void anonymousAndCsrfAreEnforced() throws Exception {
        Browser b=new Browser(); assertEquals(401,b.request("GET","/api/groups",null,false).statusCode());
        assertEquals(403,b.request("POST","/api/auth/login","username=user@example.invalid&password="+PASSWORD,false).statusCode());
        assertEquals(401,b.login("user@example.invalid","wrong-password").statusCode());
    }
    @Test void sessionIdentityAndLogout() throws Exception {
        Browser b=new Browser();assertEquals(204,b.login("USER@EXAMPLE.INVALID",PASSWORD).statusCode());
        var me=b.request("GET","/api/auth/me",null,false);assertEquals(user.toString(),JsonPath.read(me.body(),"$.id"));
        assertFalse(me.body().contains("passwordHash"));
        assertTrue(b.cookies.getCookieStore().getCookies().stream().anyMatch(c->c.isHttpOnly()));
        assertEquals(204,b.request("POST","/api/auth/logout",null,true).statusCode());
        assertEquals(401,b.request("GET","/api/auth/me",null,false).statusCode());
    }
    @Test void groupIsolationAndMembershipRevocation() throws Exception {
        Browser b=new Browser();b.login("other@example.invalid",PASSWORD);
        assertEquals("[]",b.request("GET","/api/groups",null,false).body());
        assertEquals(404,b.request("GET","/api/groups/"+group+"/members",null,false).statusCode());
        b=new Browser();b.login("user@example.invalid",PASSWORD);
        assertTrue(b.request("GET","/api/groups",null,false).body().contains("Test group"));
        jdbc.update("DELETE FROM membership WHERE user_id=?",user);
        assertEquals(404,b.request("GET","/api/groups/"+group+"/members",null,false).statusCode());
    }
    @Test void adminEndpointsAndTemporaryPasswordAreEnforced() throws Exception {
        Browser b=new Browser();b.login("user@example.invalid",PASSWORD);
        assertEquals(403,b.request("GET","/api/admin/users",null,false).statusCode());
        jdbc.update("UPDATE app_user SET must_change_password=true WHERE id=?",user);
        assertEquals(403,b.request("GET","/api/groups",null,false).statusCode());
        assertEquals(200,b.request("GET","/api/auth/me",null,false).statusCode());
    }
    @Test void passwordChangeRevokesAllSessions() throws Exception {
        Browser first=new Browser(),second=new Browser();first.login("user@example.invalid",PASSWORD);second.login("user@example.invalid",PASSWORD);
        assertEquals(400,first.request("POST","/api/auth/change-password","{\"currentPassword\":\"wrong\",\"newPassword\":\"New-test-password-2026\"}",true).statusCode());
        assertEquals(200,first.request("POST","/api/auth/change-password","{\"currentPassword\":\""+PASSWORD+"\",\"newPassword\":\"New-test-password-2026\"}",true).statusCode());
        assertEquals(401,second.request("GET","/api/auth/me",null,false).statusCode());
        assertEquals(204,new Browser().login("user@example.invalid","New-test-password-2026").statusCode());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='PASSWORD_CHANGED'",Integer.class));
    }
    @Test void disabledAccountCannotUseExistingSession() throws Exception {
        Browser b=new Browser();b.login("user@example.invalid",PASSWORD);
        Browser a=new Browser();a.login("admin@example.invalid",PASSWORD);
        assertEquals(200,a.request("PATCH","/api/admin/users/"+user+"/active","{\"active\":false}",true).statusCode());
        assertEquals(401,b.request("GET","/api/groups",null,false).statusCode());
        assertEquals(401,new Browser().login("user@example.invalid",PASSWORD).statusCode());
    }
    @Test void lastAdministratorCannotBeDisabled() throws Exception {
        Browser b=new Browser();b.login("admin@example.invalid",PASSWORD);
        assertEquals(409,b.request("PATCH","/api/admin/users/"+admin+"/active","{\"active\":false}",true).statusCode());
    }
    @Test void usersGroupsAndPeriodsPersistWithAudit() throws Exception {
        Browser b=new Browser();b.login("admin@example.invalid",PASSWORD);
        String input="{\"email\":\"new@example.invalid\",\"displayName\":\"New user\",\"systemRole\":\"USER\",\"temporaryPassword\":\""+PASSWORD+"\"}";
        assertEquals(200,b.request("POST","/api/admin/users",input,true).statusCode());
        assertEquals(409,b.request("POST","/api/admin/users",input,true).statusCode());
        var response=b.request("POST","/api/admin/groups","{\"name\":\"New group\",\"groupType\":\"UNIT\"}",true);
        assertEquals(200,response.statusCode());
        String id=JsonPath.read(response.body(),"$.id");
        assertEquals(200,b.request("POST","/api/admin/groups/"+id+"/members","{\"userId\":\""+user+"\",\"membershipRole\":\"COORDINATOR\"}",true).statusCode());
        String period="{\"name\":\"Test period\",\"startsOn\":\"2026-07-01\",\"endsOn\":\"2026-12-31\",\"preparationStartsOn\":\"2026-07-01\",\"preparationEndsOn\":\"2026-07-31\",\"reviewStartsOn\":\"2026-08-01\",\"reviewEndsOn\":\"2026-08-31\"}";
        assertEquals(200,b.request("POST","/api/admin/periods",period,true).statusCode());
        assertTrue(b.request("GET","/api/periods",null,false).body().contains("Test period"));
        assertTrue(b.request("GET","/api/periods",null,false).body().contains("\"starts_on\":\"2026-07-01\""));
        assertEquals(400,b.request("POST","/api/admin/periods",period.replace("2026-12-31","2026-01-01"),true).statusCode());
    }
    @Test void resetTokenIsOneUseAndHashed() throws Exception {
        Browser b=new Browser();b.csrf();String token="a".repeat(43);
        jdbc.update("INSERT INTO password_reset(token_hash,user_id,expires_at) VALUES (?,?,now()+interval '1 minute')",AuthThrottle.digest(token),user);
        String body="{\"token\":\""+token+"\",\"newPassword\":\"Reset-test-password-2026\"}";
        assertEquals(200,b.request("POST","/api/auth/reset-password",body,true).statusCode());
        assertEquals(400,b.request("POST","/api/auth/reset-password",body,true).statusCode());
        assertEquals(204,new Browser().login("user@example.invalid","Reset-test-password-2026").statusCode());
    }
    @Test void expiredTokenAndShortPasswordAreRejected() throws Exception {
        Browser b=new Browser();b.csrf();String token="b".repeat(43);
        jdbc.update("INSERT INTO password_reset(token_hash,user_id,expires_at) VALUES (?,?,now()-interval '1 minute')",AuthThrottle.digest(token),user);
        assertEquals(400,b.request("POST","/api/auth/reset-password","{\"token\":\""+token+"\",\"newPassword\":\"Valid-test-password\"}",true).statusCode());
        assertEquals(400,b.request("POST","/api/auth/reset-password","{\"token\":\""+token+"\",\"newPassword\":\"short\"}",true).statusCode());
    }
    @Test void recoveryUsesSmtpWithoutAccountEnumeration() throws Exception {
        Browser b=new Browser();b.csrf();
        var known=b.request("POST","/api/auth/forgot-password","{\"email\":\"user@example.invalid\"}",true);
        var unknown=b.request("POST","/api/auth/forgot-password","{\"email\":\"missing@example.invalid\"}",true);
        assertEquals(known.body(),unknown.body());assertEquals(200,known.statusCode());
        await().untilAsserted(() -> assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM password_reset",Integer.class)));
        String url=System.getenv("TEST_MAIL_URL");
        var mail=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url+"/api/v1/messages")).GET().build(),HttpResponse.BodyHandlers.ofString());
        assertTrue(mail.body().contains("user@example.invalid"));
    }
    @Test void loginAttemptsAreThrottled() throws Exception {
        Browser b=new Browser();for(int n=0;n<10;n++) assertEquals(401,b.login("missing@example.invalid","wrong-password").statusCode());
        assertEquals(429,b.login("missing@example.invalid","wrong-password").statusCode());
    }
}
