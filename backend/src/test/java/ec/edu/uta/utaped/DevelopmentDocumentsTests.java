package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import ec.edu.uta.utaped.institution.DevelopmentDocuments;
import ec.edu.uta.utaped.documents.*;

@org.springframework.test.annotation.DirtiesContext(classMode=org.springframework.test.annotation.DirtiesContext.ClassMode.AFTER_CLASS)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,properties={"spring.profiles.active=development-seed","app.development.documents=true","UTAPED_SEED_PASSWORD=Prepared-Examples-Only-2026!"})
class DevelopmentDocumentsTests {
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        String url=System.getenv("DB_URL");
        if(url==null||!url.endsWith("/utaped_test"))throw new IllegalStateException("Requires isolated test server");
        var source=new DriverManagerDataSource(url,System.getenv("DB_USER"),System.getenv("DB_PASSWORD"));
        new JdbcTemplate(source).execute("CREATE DATABASE utaped_dev");
        properties.add("spring.datasource.url",()->url.substring(0,url.length()-"utaped_test".length())+"utaped_dev");
    }
    @AfterAll static void cleanup() {
        var root=new JdbcTemplate(new DriverManagerDataSource(System.getenv("DB_URL"),System.getenv("DB_USER"),System.getenv("DB_PASSWORD")));
        root.execute("DROP DATABASE IF EXISTS utaped_dev WITH (FORCE)");
    }
    @Autowired JdbcTemplate jdbc;@Autowired DevelopmentDocuments seed;@Autowired ArtifactService artifacts;
    @Test void preparedScenariosHaveRealArtifactsEvidenceAndTasksAndNeverResetOwnerEdits() throws Exception {
        var states=jdbc.queryForList("SELECT document_type,document_state,count(*) AS total FROM work_plan GROUP BY document_type,document_state");
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM work_plan",Integer.class));
        assertTrue(states.stream().anyMatch(s->s.get("document_state").equals("APPROVED")),states.toString());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE document_state='IN_REVIEW'",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE document_type='T2' AND document_state='DRAFT'",Integer.class));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM signing_certificate_binding WHERE active",Integer.class));
        assertTrue(jdbc.queryForObject("SELECT count(*) FROM execution_evidence",Integer.class)>0);
        var docs=jdbc.queryForList("SELECT id FROM work_plan");
        for(var row:docs) {
            UUID id=(UUID)row.get("id");var artifact=artifacts.list(id,"principal@utaped.example.invalid").getFirst();
            try(var pdf=org.apache.pdfbox.Loader.loadPDF(artifacts.content(id,artifact.id(),"principal@utaped.example.invalid"))) {
                assertEquals(pdf.getNumberOfPages(),artifact.pageCount());
                assertTrue(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf).contains("Ejemplo"));
            }
        }
        UUID editable=jdbc.queryForObject("SELECT id FROM work_plan WHERE document_type='T1' AND document_state='DRAFT'",UUID.class);
        jdbc.update("UPDATE work_plan SET objective='Cambio manual conservado' WHERE id=?",editable);
        int files=jdbc.queryForObject("SELECT count(*) FROM stored_file",Integer.class);
        seed.run(new DefaultApplicationArguments());
        assertEquals(4,jdbc.queryForObject("SELECT count(*) FROM work_plan",Integer.class));
        assertEquals(files,jdbc.queryForObject("SELECT count(*) FROM stored_file",Integer.class));
        assertEquals("Cambio manual conservado",jdbc.queryForObject("SELECT objective FROM work_plan WHERE id=?",String.class,editable));
    }
}
