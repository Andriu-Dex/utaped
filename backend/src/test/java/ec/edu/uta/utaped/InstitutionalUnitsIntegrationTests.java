package ec.edu.uta.utaped;
import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;
import ec.edu.uta.utaped.institution.OrganizationService;
import ec.edu.uta.utaped.planning.WorkPlanService;
import ec.edu.uta.utaped.planning.WorkPlanModels.*;

class InstitutionalUnitsIntegrationTests extends WorkPlanIntegrationTests {
    @Autowired OrganizationService organization;
    @Autowired WorkPlanService plans;
    @Test void administrativeScopeFreezesItsProvenanceAndRejectsCareerSpoofing() {
        openPeriod();UUID unit=UUID.randomUUID();
        jdbc.update("INSERT INTO institutional_unit(id,name,unit_type) VALUES (?,'Unidad administrativa de prueba','ADMINISTRATIVE')",unit);
        var career=organization.createCareer("admin@example.invalid",new OrganizationService.CareerInput("UNITTEST","Carrera de prueba"));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,career,unit))).getStatusCode().value());
        var scope=organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,null,unit));
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(user,"MEMBER","Responsable",true,0L));
        organization.principal(scope,"admin@example.invalid",new OrganizationService.PrincipalInput(user,0L));
        var plan=plans.create("user@example.invalid",new Create(group,period,UUID.randomUUID(),"Procedencia administrativa",scope));
        assertEquals("ADMINISTRATIVE",plan.institutionalUnitType());assertEquals("Unidad administrativa de prueba",plan.institutionalUnit());assertEquals("",plan.career());
        jdbc.update("UPDATE institutional_unit SET name='Nombre modificado' WHERE id=?",unit);
        assertEquals("Unidad administrativa de prueba",plans.get(plan.id(),"user@example.invalid").institutionalUnit());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->plans.update(plan.id(),"user@example.invalid",new Update(plan.rowVersion(),plan.title(),"Nombre modificado","","", ""))).getStatusCode().value());
    }
    @Test void catalogueRequiresAdministratorAndChecksConcurrentChanges() throws Exception {
        Browser owner=new Browser(),admin=new Browser();owner.login("user@example.invalid",PASSWORD);admin.login("admin@example.invalid",PASSWORD);
        String body="{\"name\":\"Unidad de catálogo\",\"unitType\":\"ADMINISTRATIVE\",\"active\":true,\"rowVersion\":0}";
        assertEquals(403,owner.request("POST","/api/admin/institutional-units",body,true).statusCode());
        var response=admin.request("POST","/api/admin/institutional-units",body,true);assertEquals(200,response.statusCode());
        String id=com.jayway.jsonpath.JsonPath.read(response.body(),"$.id");
        assertEquals(200,admin.request("PUT","/api/admin/institutional-units/"+id,body,true).statusCode());
        assertEquals(409,admin.request("PUT","/api/admin/institutional-units/"+id,body,true).statusCode());
        assertEquals(200,owner.request("GET","/api/institutional-units",null,false).statusCode());
    }
    @Test void inactiveAndUnrelatedAcademicUnitsCannotBeAssigned() {
        openPeriod();UUID unit=UUID.randomUUID();jdbc.update("INSERT INTO institutional_unit(id,name,unit_type) VALUES (?,'Otra facultad de prueba','ACADEMIC')",unit);
        var career=organization.createCareer("admin@example.invalid",new OrganizationService.CareerInput("OTHERUNIT","Carrera de otra unidad",unit));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,career,ec.edu.uta.utaped.institution.InstitutionalUnits.FISEI))).getStatusCode().value());
        jdbc.update("UPDATE institutional_unit SET active=false WHERE id=?",unit);
        assertEquals(400,assertThrows(ResponseStatusException.class,()->organization.createCareer("admin@example.invalid",new OrganizationService.CareerInput("INACTIVE","No admitida",unit))).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,career,unit))).getStatusCode().value());
    }
}
