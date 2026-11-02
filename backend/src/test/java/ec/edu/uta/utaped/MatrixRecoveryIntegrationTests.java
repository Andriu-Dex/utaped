package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.planning.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class MatrixRecoveryIntegrationTests extends OrganizationIntegrationTests {
    @Autowired MatrixRecovery recovery;
    @Autowired ActivityService activities;
    @Autowired ec.edu.uta.utaped.documents.ArtifactService artifacts;
    private MatrixRecovery.Content incomplete(String title) {
        var row=new MatrixRecovery.Row(UUID.randomUUID(),null,title,"OTHER",false,null,null,List.of(),false,List.of(),List.of(),null);
        return new MatrixRecovery.Content(new MatrixRecovery.Draft("Fuente parcial","Nota",List.of(row)),"","",List.of());
    }
    @Test void incompleteRecoveryIsPrivateIsolatedAndNeverBecomesCanonicalUntilValidated() {
        UUID scope=scope();var first=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan uno",scope));
        var career=organization.createCareer("admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.CareerInput("RECOVERY", "Carrera de prueba - Recuperación"));
        var secondScope=organization.createScope("admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.ScopeInput(group,period,career));
        organization.member(secondScope,"admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.MemberInput(user,"MEMBER","Responsable",true,0L));
        organization.principal(secondScope,"admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.PrincipalInput(user,0L));
        var second=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan dos",secondScope));
        var content=incomplete("Actividad incompleta");
        var saved=recovery.save(first.id(),"user@example.invalid",new MatrixRecovery.Input(first.rowVersion(),null,content)).recovery();
        assertEquals(content,recovery.read(first.id(),"user@example.invalid").recovery().content());
        assertNull(recovery.read(second.id(),"user@example.invalid").recovery());
        assertTrue(activities.get(first.id(),"user@example.invalid").activities().isEmpty());
        assertTrue(artifacts.readiness(first.id(),"user@example.invalid").blockers().stream().anyMatch(blocker->blocker.message().contains("cambios pendientes")));
        assertThrows(ResponseStatusException.class,()->recovery.read(first.id(),"other@example.invalid"));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->recovery.save(first.id(),"user@example.invalid",new MatrixRecovery.Input(first.rowVersion(),null,content))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->activities.save(first.id(),"user@example.invalid",new ActivityModels.Save(first.rowVersion(),"",List.of()))).getStatusCode().value());
        var updated=recovery.save(first.id(),"user@example.invalid",new MatrixRecovery.Input(first.rowVersion(),saved.revision(),content)).recovery();
        assertEquals(409,assertThrows(ResponseStatusException.class,()->recovery.discard(first.id(),"user@example.invalid",saved.revision())).getStatusCode().value());
        var changed=plans.update(first.id(),"user@example.invalid",new WorkPlanModels.Update(first.rowVersion(),"Plan actualizado",first.institutionalUnit(),first.career(),first.justification(),first.objective()));
        assertEquals(409,assertThrows(ResponseStatusException.class,()->recovery.save(first.id(),"user@example.invalid",new MatrixRecovery.Input(first.rowVersion(),updated.revision(),content))).getStatusCode().value());
        assertEquals(first.rowVersion(),recovery.read(first.id(),"user@example.invalid").recovery().baseVersion());
        var canonical=activities.save(first.id(),"user@example.invalid",new ActivityModels.Save(changed.rowVersion(),"Fuente definitiva",List.of(),"Nota",updated.revision()));
        assertEquals("Fuente definitiva",canonical.source());assertNull(recovery.read(first.id(),"user@example.invalid").recovery());
        assertTrue(activities.get(second.id(),"user@example.invalid").activities().isEmpty());
        recovery.save(first.id(),"user@example.invalid",new MatrixRecovery.Input(canonical.rowVersion(),null,content));
        organization.member(scope,"admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.MemberInput(outsider,"MEMBER","Nuevo responsable",true,0L));
        long scopeVersion=jdbc.queryForObject("SELECT row_version FROM organizational_scope WHERE id=?",Long.class,scope);
        organization.principal(scope,"admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.PrincipalInput(outsider,scopeVersion));
        assertNull(recovery.read(first.id(),"other@example.invalid").recovery());
        assertTrue(artifacts.readiness(first.id(),"other@example.invalid").blockers().stream().noneMatch(blocker->blocker.message().contains("cambios pendientes")));
    }
}
