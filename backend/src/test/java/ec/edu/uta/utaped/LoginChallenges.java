package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.identity.AuthThrottle;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;

/** Sets a known answer through the isolated test database; the production API has no bypass. */
final class LoginChallenges {
    static String answer(HttpClient client,int port,JdbcTemplate jdbc) throws Exception {
        return issue(client,port,jdbc,"ABC234").answer();
    }
    record Fixture(UUID id,String answer) {}
    static Fixture issue(HttpClient client,int port,JdbcTemplate jdbc,String answer) throws Exception {
        assertEquals("utaped_test",jdbc.queryForObject("SELECT current_database()",String.class));
        var response=client.send(HttpRequest.newBuilder(URI.create("http://localhost:"+port+"/api/auth/captcha")).GET().build(),HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200,response.statusCode());assertTrue(response.headers().firstValue("Cache-Control").orElse("").contains("no-store"));
        assertEquals("image/png",response.headers().firstValue("Content-Type").orElse(""));
        assertTrue(response.body().length>500);assertEquals((byte)0x89,response.body()[0]);
        UUID id=UUID.fromString(response.headers().firstValue("X-Captcha-Id").orElseThrow());
        assertEquals(1,jdbc.update("UPDATE login_captcha SET answer_hash=? WHERE id=?",AuthThrottle.digest(id+":"+answer),id));
        return new Fixture(id,answer);
    }
}
