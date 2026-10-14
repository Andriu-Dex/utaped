package ec.edu.uta.utaped.writing;

import static org.junit.jupiter.api.Assertions.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.json.JsonMapper;

class HttpAiProviderTests {
    HttpServer server; URI endpoint; AtomicReference<String> request=new AtomicReference<>();
    String body; int status=200; long delay;
    @BeforeEach void start() throws Exception {
        server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/",exchange -> {
            request.set(new String(exchange.getRequestBody().readAllBytes(),StandardCharsets.UTF_8));
            assertEquals("Bearer test-only-key",exchange.getRequestHeaders().getFirst("Authorization"));
            try { if(delay>0) Thread.sleep(delay); byte[] bytes=body.getBytes(StandardCharsets.UTF_8); exchange.sendResponseHeaders(status,bytes.length); exchange.getResponseBody().write(bytes); }
            catch(InterruptedException e) { Thread.currentThread().interrupt(); }
            finally { exchange.close(); }
        }); server.start();endpoint=URI.create("http://127.0.0.1:"+server.getAddress().getPort()+"/");
    }
    @AfterEach void stop() { server.stop(0); }
    HttpAiProvider provider(String name) { return new HttpAiProvider(true,name,"test-model","test-only-key",1,8000,2000,endpoint); }
    @Test void openaiUsesStatelessResponsesAndParsesMessageOutput() {
        body="{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\"},{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\" Texto mejorado. \"}]}]}";
        assertEquals("Texto mejorado.",provider("OPENAI").suggest("JUSTIFICATION","CLEAR","Texto inicial."));
        var payload=JsonMapper.builder().build().readTree(request.get()); assertFalse(payload.path("store").asBoolean());
        assertEquals("test-model",payload.path("model").asText());assertEquals(2000,payload.path("max_output_tokens").asInt());
        assertTrue(payload.path("input").asText().contains("Texto inicial."));assertFalse(payload.has("tools"));assertFalse(payload.has("previous_response_id"));
    }
    @Test void groqUsesChatAndRejectsTruncation() {
        body="{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Texto preciso.\"}}]}";
        assertEquals("Texto preciso.",provider("GROQ").suggest("OBJECTIVE","CONCISE","Texto inicial."));
        var payload=JsonMapper.builder().build().readTree(request.get()); assertEquals(2000,payload.path("max_completion_tokens").asInt());assertFalse(payload.has("store"));assertEquals(2,payload.path("messages").size());
        body=body.replace("stop","length");assertEquals(503,assertThrows(ResponseStatusException.class,()->provider("GROQ").suggest("OBJECTIVE","CLEAR","Texto")).getStatusCode().value());
    }
    @Test void invalidRefusedEmptyAndOversizedResponsesHaveOnlySafeErrors() {
        for(String invalid:new String[]{"not-json", "{}", "{\"status\":\"incomplete\"}","{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"refusal\"}]}]}","x".repeat(131073)}) {
            body=invalid;var error=assertThrows(ResponseStatusException.class,()->provider("OPENAI").suggest("BACKGROUND","FORMAL","Texto"));
            assertEquals(503,error.getStatusCode().value());assertFalse(error.getReason().contains("test-only-key"));assertNull(error.getCause());
        }
        status=401;body="Private upstream details";assertThrows(ResponseStatusException.class,()->provider("GROQ").suggest("OBJECTIVE","CLEAR","Texto"));
    }
    @Test void boundedTimeoutAndDisabledConfigurationDoNotFallBackToFakeText() {
        var disabled=new HttpAiProvider(false,"GROQ","test-model","test-only-key",1,8000,2000,endpoint);
        assertFalse(disabled.configuration().available());assertThrows(ResponseStatusException.class,()->disabled.suggest("OBJECTIVE","CLEAR","Texto"));assertNull(request.get());
        delay=1500;body="{}";long start=System.nanoTime();assertThrows(ResponseStatusException.class,()->provider("GROQ").suggest("OBJECTIVE","CLEAR","Texto"));assertTrue(System.nanoTime()-start<2_500_000_000L);
    }
}
