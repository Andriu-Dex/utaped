package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import ec.edu.uta.utaped.institution.DevelopmentReference;
import ec.edu.uta.utaped.institution.DevelopmentSeed;
import java.time.LocalDate;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class DevelopmentSeedTests {
    @Test void rejectsAnyDatabaseOtherThanTheExplicitDevelopmentDatabaseBeforeWriting() {
        var jdbc=mock(JdbcTemplate.class);
        when(jdbc.queryForObject("SELECT current_database()",String.class)).thenReturn("utaped_test");
        assertThrows(IllegalStateException.class,() -> new DevelopmentSeed(jdbc,new BCryptPasswordEncoder(),"Seed-Example-Only-2026!").run(new DefaultApplicationArguments()));
        verify(jdbc).queryForObject("SELECT current_database()",String.class); verifyNoMoreInteractions(jdbc);
    }
    @Test void referenceSeedingIsIdempotentAndPreservesAdministratorChanges() {
        String url=System.getenv("DB_URL");
        assertNotNull(url,"Run using the isolated compose.test.yml environment");
        assertTrue(url.endsWith("/utaped_test"),"Never create a fixture database on the sample or principal server");
        String user=System.getenv("DB_USER"),password=System.getenv("DB_PASSWORD");
        var root=new JdbcTemplate(new DriverManagerDataSource(url,user,password));
        root.execute("CREATE DATABASE utaped_dev");
        try {
            var source=new DriverManagerDataSource(url.substring(0,url.length()-"utaped_test".length())+"utaped_dev",user,password);
            Flyway.configure().dataSource(source).load().migrate();
            var jdbc=new JdbcTemplate(source); var seed=new DevelopmentSeed(jdbc,new BCryptPasswordEncoder(),"Seed-Example-Only-2026!");
            seed.run(new DefaultApplicationArguments()); seed.run(new DefaultApplicationArguments());
            assertEquals(23,jdbc.queryForObject("SELECT count(*) FROM institutional_group",Integer.class));
            assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM academic_career",Integer.class));
            assertEquals(30,jdbc.queryForObject("SELECT count(*) FROM organizational_scope",Integer.class));
            assertEquals(7,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
            assertEquals(55,jdbc.queryForObject("SELECT count(*) FROM activity_catalog",Integer.class));
            assertEquals(26,jdbc.queryForObject("SELECT count(*) FROM planning_catalog",Integer.class));
            assertEquals(60,jdbc.queryForObject("SELECT count(*) FROM period_workflow_assignment",Integer.class));
            assertEquals("Julio–Diciembre 2026",jdbc.queryForObject("SELECT name FROM academic_period",String.class));
            assertEquals(LocalDate.of(2026,7,1),jdbc.queryForObject("SELECT starts_on FROM academic_period",LocalDate.class));
            assertEquals(LocalDate.of(2026,12,31),jdbc.queryForObject("SELECT ends_on FROM academic_period",LocalDate.class));
            assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM work_plan",Integer.class));
            var reference=DevelopmentReference.load();
            assertEquals(9,reference.activities().stream().filter(a -> a.source().contains("signed.pdf")).count());
            jdbc.update("UPDATE academic_career SET code='DEV-SW',name='Carrera de ejemplo — Software' WHERE id=?",DevelopmentSeed.id("career"));
            jdbc.update("UPDATE institutional_group SET name='Comisión de desarrollo — Ejemplo' WHERE id=?",DevelopmentSeed.id("group"));
            jdbc.update("UPDATE academic_period SET name='Período de desarrollo — Ejemplo',starts_on='2026-01-01' WHERE id=?",DevelopmentSeed.id("period"));
            seed.run(new DefaultApplicationArguments());
            assertEquals("Unidad de Titulación",jdbc.queryForObject("SELECT name FROM institutional_group WHERE id=?",String.class,DevelopmentSeed.id("group")));
            assertEquals("Tecnologías de la Información",jdbc.queryForObject("SELECT name FROM academic_career WHERE id=?",String.class,DevelopmentSeed.id("career")));
            assertEquals("Julio–Diciembre 2026",jdbc.queryForObject("SELECT name FROM academic_period WHERE id=?",String.class,DevelopmentSeed.id("period")));
            assertEquals(LocalDate.of(2026,7,1),jdbc.queryForObject("SELECT starts_on FROM academic_period WHERE id=?",LocalDate.class,DevelopmentSeed.id("period")));
            jdbc.update("UPDATE institutional_group SET name='Denominación ajustada localmente' WHERE id=?",DevelopmentSeed.id("group"));
            jdbc.update("UPDATE academic_period SET workflow_locked_at=now() WHERE id=?",DevelopmentSeed.id("period"));
            jdbc.update("UPDATE workflow_configuration SET current_revision_id=NULL WHERE id=?",DevelopmentSeed.id("configuration-T1"));
            seed.run(new DefaultApplicationArguments());
            assertEquals("Denominación ajustada localmente",jdbc.queryForObject("SELECT name FROM institutional_group WHERE id=?",String.class,DevelopmentSeed.id("group")));
            assertEquals(30,jdbc.queryForObject("SELECT count(*) FROM organizational_scope",Integer.class));
            assertNull(jdbc.queryForObject("SELECT current_revision_id FROM workflow_configuration WHERE id=?",java.util.UUID.class,DevelopmentSeed.id("configuration-T1")));
        } finally { root.execute("DROP DATABASE utaped_dev"); }
    }
}
