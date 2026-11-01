package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.planning.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class InstitutionalActivityCatalogIntegrationTests extends OrganizationIntegrationTests {
    @Autowired InstitutionalActivityCatalog reference;
    @Autowired ActivityService activities;
    @Test void importsOfficialHierarchyWithoutDuplicatingOrOverwriting() {
        var data=reference.reference("admin@example.invalid");
        assertEquals(14,data.entries().stream().filter(e->e.category().equals("POA")).count());
        assertEquals(67,data.entries().stream().filter(e->e.category().equals("IMPROVEMENT_PLAN")).count());
        var entry=data.entries().stream().filter(e->e.category().equals("IMPROVEMENT_PLAN")).findFirst().orElseThrow();
        var input=new InstitutionalActivityCatalog.Import(List.of(new InstitutionalActivityCatalog.Selection(entry.key(),group,null,null)));
        assertEquals(new InstitutionalActivityCatalog.Result(1,0),reference.apply(input,"admin@example.invalid"));
        var row=activities.definitions(group).stream().filter(d->d.context()!=null).findFirst().orElseThrow();
        assertEquals(entry.context().indicator(),row.context().indicator());assertNotNull(row.context().objective());
        activities.saveDefinition(row.id(),new ActivityModels.DefinitionInput(group,"Texto revisado",row.category(),false,true),"admin@example.invalid");
        assertEquals(new InstitutionalActivityCatalog.Result(0,1),reference.apply(input,"admin@example.invalid"));
        assertEquals("Texto revisado",activities.definitions(group).stream().filter(d->d.id().equals(row.id())).findFirst().orElseThrow().title());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->reference.apply(input,"user@example.invalid")).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->reference.apply(new InstitutionalActivityCatalog.Import(List.of(input.selections().getFirst(),input.selections().getFirst())),"admin@example.invalid")).getStatusCode().value());
    }
    @Test void filtersOfficialReferencesByCareerAndPeriodAndRejectsForeignIds() {
        var scope=scope();var plan=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan",scope));
        var career=organization.createCareer("admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.CareerInput("TEST","Carrera distinta"));
        var context=new ActivityModels.Context("career-test","Fuente oficial","Objetivo",null,"Indicador",null,null,null,null,period,career);
        var definition=activities.saveDefinition(null,new ActivityModels.DefinitionInput(group,"Actividad de otra carrera","IMPROVEMENT_PLAN",false,true,context),"admin@example.invalid");
        assertTrue(activities.get(plan.id(),"user@example.invalid").definitions().stream().noneMatch(d->d.id().equals(definition.id())));
        var choice=new ActivityModels.Choice(null,"Recurso",null);
        var row=new ActivityModels.Activity(UUID.randomUUID(),definition.id(),definition.title(),definition.category(),false,LocalDate.now(),LocalDate.now(),List.of(user),false,List.of(choice),List.of(choice));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->activities.save(plan.id(),"user@example.invalid",new ActivityModels.Save(plan.rowVersion(),"Fuente",List.of(row)))).getStatusCode().value());
        assertTrue(activities.get(plan.id(),"user@example.invalid").activities().isEmpty());
    }
    @Test void savesEditableNoteWithConcurrencyAndPreservesItOnLegacyRequests() {
        var scope=scope();var plan=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan",scope));
        var saved=activities.save(plan.id(),"user@example.invalid",new ActivityModels.Save(plan.rowVersion(),"Fuente",List.of(),"Nota institucional revisada"));
        assertEquals("Nota institucional revisada",saved.note());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->activities.save(plan.id(),"user@example.invalid",new ActivityModels.Save(plan.rowVersion(),"Anterior",List.of(),"Sobrescritura"))).getStatusCode().value());
        var legacy=activities.save(plan.id(),"user@example.invalid",new ActivityModels.Save(saved.rowVersion(),"Actualizada",List.of()));
        assertEquals(saved.note(),legacy.note());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->activities.save(plan.id(),"user@example.invalid",new ActivityModels.Save(legacy.rowVersion(),"Fuente",List.of()," "))).getStatusCode().value());
    }
    @Test void tiProposalsKeepTheirCareerWhenImportedAndCannotBeAssignedToAnotherCareer() {
        var entry=reference.reference("admin@example.invalid").entries().stream().filter(e->"TI".equals(e.careerCode())).findFirst().orElseThrow();
        var ti=jdbc.queryForList("SELECT id FROM academic_career WHERE code='TI'",UUID.class);
        UUID tiId=ti.isEmpty()?organization.createCareer("admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.CareerInput("TI","Tecnologías de la Información")):ti.getFirst();
        UUID wrong=organization.createCareer("admin@example.invalid",new ec.edu.uta.utaped.institution.OrganizationService.CareerInput("OTHER_TEST","Otra carrera de prueba"));
        assertEquals(400,assertThrows(ResponseStatusException.class,()->reference.apply(new InstitutionalActivityCatalog.Import(List.of(new InstitutionalActivityCatalog.Selection(entry.key(),group,null,wrong))),"admin@example.invalid")).getStatusCode().value());
        reference.apply(new InstitutionalActivityCatalog.Import(List.of(new InstitutionalActivityCatalog.Selection(entry.key(),group,null,null))),"admin@example.invalid");
        assertEquals(tiId,activities.definitions(group).stream().filter(d->d.context()!=null && entry.key().equals(d.context().sourceKey())).findFirst().orElseThrow().context().careerId());
    }
}
