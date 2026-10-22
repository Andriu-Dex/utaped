package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.jayway.jsonpath.JsonPath;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.writing.*;
import java.util.UUID;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class WritingIntegrationTests extends WorkPlanIntegrationTests {
    @Autowired WorkPlanService plans; @Autowired WritingService writing;
    @MockitoBean AiProvider provider;
    UUID doc;
    @BeforeEach void configure(TestInfo test) {
        if(test.getTestMethod().orElseThrow().getDeclaringClass()!=WritingIntegrationTests.class) return;
        openPeriod(); doc=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan para redacción")).id();
        when(provider.configuration()).thenReturn(new AiProvider.Configuration(true,"GROQ",8000));
        when(provider.suggest(anyString(),anyString(),anyString())).thenReturn("Texto mejorado y verificable.");
    }
    WritingService.Request input(String field,String text) { return new WritingService.Request(0L,field,"CLEAR",text,true); }
    int error(Runnable action) { return assertThrows(ResponseStatusException.class,action::run).getStatusCode().value(); }
    @Test void suggestionsDoNotSaveDraftOrLeakTextIntoAudit() {
        var before=plans.get(doc,"user@example.invalid"); var result=writing.suggest(doc,"user@example.invalid",input("JUSTIFICATION","Texto reservado de prueba."));
        assertEquals("Texto mejorado y verificable.",result.text()); assertEquals(before,plans.get(doc,"user@example.invalid"));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE target_id=? AND action LIKE 'WRITING_%'",Integer.class,doc));
        assertFalse(jdbc.queryForList("SELECT * FROM audit_event WHERE target_id=?",doc).toString().contains("Texto reservado"));
    }
    @Test void outsidersAndAdminsCannotRequestPrivateContentAndFieldsAreTyped() {
        assertEquals(404,error(()->writing.suggest(doc,"other@example.invalid",input("JUSTIFICATION","Texto"))));
        assertEquals(404,error(()->writing.suggest(doc,"admin@example.invalid",input("JUSTIFICATION","Texto"))));
        assertEquals(400,error(()->writing.suggest(doc,"user@example.invalid",input("BACKGROUND","Texto"))));
        for(String text:new String[]{" ","x".repeat(8001)}) assertEquals(400,error(()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE",text))));
        assertEquals(400,error(()->writing.suggest(doc,"user@example.invalid",new WritingService.Request(0L,"OBJECTIVE","CLEAR","Texto",false))));
        verify(provider,never()).suggest(anyString(),anyString(),anyString());
        jdbc.update("UPDATE work_plan SET document_type='T2' WHERE id=?",doc);
        writing.suggest(doc,"user@example.invalid",input("BACKGROUND","Antecedentes reales."));
        assertEquals(400,error(()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto"))));
    }
    @Test void staleVersionSignedClosedAndConcurrentChangesRejectSuggestions() {
        assertEquals(409,error(()->writing.suggest(doc,"user@example.invalid",new WritingService.Request(1L,"OBJECTIVE","CLEAR","Texto",true))));
        jdbc.update("UPDATE work_plan SET document_state='IN_REVIEW' WHERE id=?",doc);assertEquals(403,error(()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto"))));
        jdbc.update("UPDATE work_plan SET document_state='DRAFT' WHERE id=?",doc);
        when(provider.suggest(anyString(),anyString(),anyString())).thenAnswer(call -> { jdbc.update("UPDATE academic_period SET status='CLOSED' WHERE id=?",period);return "Cambio que no debe aplicarse"; });
        assertEquals(404,error(()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto"))));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='WRITING_FAILED'",Integer.class));
        assertEquals(404,error(()->plans.get(doc,"user@example.invalid")));
        assertEquals("",jdbc.queryForObject("SELECT objective FROM work_plan WHERE id=?",String.class,doc));
    }
    @Test void unavailableAndRateLimitedProviderLeavesDraftEditable() {
        when(provider.configuration()).thenReturn(new AiProvider.Configuration(false,"GROQ",8000));assertEquals(503,error(()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto"))));
        when(provider.configuration()).thenReturn(new AiProvider.Configuration(true,"GROQ",8000));
        for(int i=0;i<15;i++) writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto"));
        assertEquals(429,error(()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto"))));
        assertTrue(plans.get(doc,"user@example.invalid").editable());assertEquals(0,plans.get(doc,"user@example.invalid").rowVersion());
    }
    @Test void unexpectedProviderFailureIsSanitizedAndReleasesItsCapacity() {
        when(provider.suggest(anyString(),anyString(),anyString())).thenThrow(new IllegalStateException("Private upstream response"));
        var failure=assertThrows(ResponseStatusException.class,()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto privado")));
        assertEquals(503,failure.getStatusCode().value());assertFalse(failure.getReason().contains("Private"));assertNull(failure.getCause());
        doReturn("", "Texto recuperado").when(provider).suggest(anyString(),anyString(),anyString());
        assertEquals(503,error(()->writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto privado"))));
        assertEquals("Texto recuperado",writing.suggest(doc,"user@example.invalid",input("OBJECTIVE","Texto privado")).text());
        assertEquals(0,plans.get(doc,"user@example.invalid").rowVersion());
    }
    @Test void authenticatedApiValidatesConsentAndCsrfAndReturnsUncachedSuggestion() throws Exception {
        var browser=new Browser();browser.login("user@example.invalid",PASSWORD);
        String path="/api/work-plans/"+doc+"/writing";
        String body="{\"rowVersion\":0,\"field\":\"OBJECTIVE\",\"tone\":\"CLEAR\",\"text\":\"Texto original\",\"consent\":true}";
        assertEquals(403,browser.request("POST",path,body,false).statusCode());
        assertEquals(400,browser.request("POST",path,body.replace("true","false"),true).statusCode());
        assertEquals(400,browser.request("POST",path,body.replace("OBJECTIVE","UNKNOWN"),true).statusCode());
        var result=browser.request("POST",path,body,true);assertEquals(200,result.statusCode(),result.body());assertEquals("no-store",result.headers().firstValue("Cache-Control").orElse(""));
        assertEquals("Texto mejorado y verificable.",JsonPath.read(result.body(),"$.text"));
    }
}
