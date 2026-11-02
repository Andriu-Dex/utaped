package ec.edu.uta.utaped;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.boot.DefaultApplicationArguments;
import ec.edu.uta.utaped.institution.DevelopmentDocuments;
class DevelopmentDocumentsGuardTests {
    @Test void refusesOtherDatabasesBeforeAnyWrite() {
        var jdbc=mock(JdbcTemplate.class);when(jdbc.queryForObject("SELECT current_database()",String.class)).thenReturn("utaped_prod");
        var seed=new DevelopmentDocuments(jdbc,null,null,null,null,null,null,null,null,null,null,null,null);
        assertThrows(IllegalStateException.class,()->seed.run(new DefaultApplicationArguments()));
        verify(jdbc).queryForObject("SELECT current_database()",String.class);verifyNoMoreInteractions(jdbc);
    }
    @Test void refusesWhenRealAdminAccountsExist() {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT current_database()",String.class)).thenReturn("utaped");
        when(jdbc.queryForObject(contains("EXISTS"),eq(Boolean.class))).thenReturn(true);
        var seed=new DevelopmentDocuments(jdbc,null,null,null,null,null,null,null,null,null,null,null,null);
        assertThrows(IllegalStateException.class,()->seed.run(new DefaultApplicationArguments()));
    }
}
