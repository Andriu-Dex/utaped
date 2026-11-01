package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.uta.utaped.planning.*;
import ec.edu.uta.utaped.writing.AiProvider;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class HolidayAssistanceIntegrationTests extends WorkPlanIntegrationTests {
    @Autowired HolidayAssistance assistance;
    @MockitoBean AiProvider provider;
    @Test void suggestionsNeverWriteCalendarAndRequireReviewedSourcesBeforeApplying() {
        when(provider.configuration()).thenReturn(new AiProvider.Configuration(true,"GROQ",8000));
        when(provider.holidays(2026)).thenReturn("{\"items\":[{\"date\":\"2026-01-01\",\"label\":\"Fecha de prueba\",\"sourceUrl\":\"https://calendario.example.invalid/referencia\"}]}");
        var before=jdbc.queryForObject("SELECT count(*) FROM planning_holiday",Integer.class);
        var proposal=assistance.suggest("admin@example.invalid",new HolidayAssistance.Query(2026));assertEquals(1,proposal.items().size());
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM planning_holiday",Integer.class));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->assistance.apply("admin@example.invalid",new HolidayAssistance.Apply(2026,proposal.items(),false,false))).getStatusCode().value());
        assistance.apply("admin@example.invalid",new HolidayAssistance.Apply(2026,proposal.items(),true,true));
        assertEquals("https://calendario.example.invalid/referencia",jdbc.queryForObject("SELECT source_url FROM planning_holiday WHERE holiday_date='2026-01-01'",String.class));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->assistance.apply("admin@example.invalid",new HolidayAssistance.Apply(2026,proposal.items(),true,false))).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->assistance.apply("admin@example.invalid",new HolidayAssistance.Apply(2027,proposal.items(),true,true))).getStatusCode().value());
    }
    @Test void unauthorizedRequestsAndMalformedResponsesCannotCreateHolidays() {
        assertEquals(403,assertThrows(ResponseStatusException.class,()->assistance.suggest("user@example.invalid",new HolidayAssistance.Query(2026))).getStatusCode().value());verifyNoInteractions(provider);
        when(provider.configuration()).thenReturn(new AiProvider.Configuration(true,"OPENAI",8000));when(provider.holidays(2026)).thenReturn("Respuesta sin fuente");
        assertEquals(503,assertThrows(ResponseStatusException.class,()->assistance.suggest("admin@example.invalid",new HolidayAssistance.Query(2026))).getStatusCode().value());
        var invalid=new HolidayAssistance.Item(LocalDate.of(2026,1,2),"Prueba","javascript:alert(1)");
        assertEquals(400,assertThrows(ResponseStatusException.class,()->assistance.apply("admin@example.invalid",new HolidayAssistance.Apply(2026,List.of(invalid),true,false))).getStatusCode().value());
    }
}
