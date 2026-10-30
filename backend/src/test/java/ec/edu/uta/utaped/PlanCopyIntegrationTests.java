package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.institution.OrganizationService;
import ec.edu.uta.utaped.planning.*;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class PlanCopyIntegrationTests extends OrganizationIntegrationTests {
    @Autowired PlanCopyService copies;@Autowired ActivityService activities;
    UUID careerScope(String code) {
        String unique=code+"-"+UUID.randomUUID().toString().substring(0,8);
        UUID career=organization.createCareer("admin@example.invalid",new OrganizationService.CareerInput(unique,"Carrera "+unique));
        UUID scope=organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,career));
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(user,"MEMBER","Responsable",true,0L));
        organization.principal(scope,"admin@example.invalid",new OrganizationService.PrincipalInput(user,0L));return scope;
    }
    @Test void careerCopiesAreIndependentIdempotentAndPreserveSource() {
        openPeriod();UUID sourceScope=careerScope("SW"),target=careerScope("TI");
        var source=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Plan original",sourceScope));
        source=plans.update(source.id(),"user@example.invalid",new WorkPlanModels.Update(source.rowVersion(),source.title(),source.institutionalUnit(),source.career(),"Justificación compartida","Objetivo común"));
        var matrix=activities.get(source.id(),"user@example.invalid");UUID activityId=UUID.randomUUID();
        var choice=new ActivityModels.Choice(null,"Recurso propio",null);
        activities.save(source.id(),"user@example.invalid",new ActivityModels.Save(matrix.rowVersion(),"Fuente original",List.of(new ActivityModels.Activity(activityId,null,"Actividad original","OTHER",false,LocalDate.now().plusDays(1),LocalDate.now().plusDays(4),List.of(user),false,List.of(choice),List.of(choice)))));
        var before=plans.get(source.id(),"user@example.invalid");
        var input=new PlanCopyService.Copy(before.rowVersion(),UUID.randomUUID(),List.of(target));
        var result=copies.copy(source.id(),"user@example.invalid",input);
        assertEquals(result,copies.copy(source.id(),"user@example.invalid",input));
        assertEquals(1,result.documentIds().size());var copied=plans.get(result.documentIds().getFirst(),"user@example.invalid");
        assertEquals(target,copied.scopeId());assertTrue(copied.career().startsWith("Carrera TI-"));assertEquals("DRAFT",copied.documentState());
        assertEquals(before.justification(),copied.justification());assertEquals(before.objective(),copied.objective());
        var rows=activities.get(copied.id(),"user@example.invalid").activities();assertEquals(1,rows.size());assertNotEquals(activityId,rows.getFirst().id());assertEquals(List.of(user),rows.getFirst().responsibleIds());
        assertEquals(before,plans.get(source.id(),"user@example.invalid"));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM document_artifact WHERE work_plan_id=?",Integer.class,copied.id()));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM plan_copy_batch",Integer.class));
    }
    @Test void unavailableDestinationRollsBackWholeBatchAndRejectsOtherUsers() {
        openPeriod();UUID sourceScope=careerScope("SW"),target=careerScope("TI"),occupied=careerScope("SIS");
        var source=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Origen",sourceScope));
        plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Ya existe",occupied));
        long before=jdbc.queryForObject("SELECT count(*) FROM work_plan",Long.class);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->copies.copy(source.id(),"user@example.invalid",new PlanCopyService.Copy(source.rowVersion(),UUID.randomUUID(),List.of(target,occupied)))).getStatusCode().value());
        assertEquals(before,jdbc.queryForObject("SELECT count(*) FROM work_plan",Long.class));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->copies.options(source.id(),"other@example.invalid")).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->copies.copy(source.id(),"user@example.invalid",new PlanCopyService.Copy(source.rowVersion(),UUID.randomUUID(),List.of(target,target)))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->copies.copy(source.id(),"user@example.invalid",new PlanCopyService.Copy(source.rowVersion()+1,UUID.randomUUID(),List.of(target)))).getStatusCode().value());
    }
    @Test void careerCopyHttpRequiresSessionCsrfAndValidRequest() throws Exception {
        openPeriod();UUID from=careerScope("SW"),target=careerScope("TI");
        var source=plans.create("user@example.invalid",new WorkPlanModels.Create(group,period,UUID.randomUUID(),"Origen HTTP",from));
        String path="/api/work-plans/"+source.id()+"/career-copies";
        assertEquals(401,new Browser().request("GET",path,null,false).statusCode());
        Browser browser=new Browser();assertEquals(204,browser.login("user@example.invalid",PASSWORD).statusCode());
        var options=browser.request("GET",path,null,false);assertEquals(200,options.statusCode());
        assertEquals("no-store",options.headers().firstValue("Cache-Control").orElseThrow());
        String body="{\"rowVersion\":"+source.rowVersion()+",\"requestKey\":\""+UUID.randomUUID()+"\",\"targetScopeIds\":[\""+target+"\"]}";
        assertEquals(403,browser.request("POST",path,body,false).statusCode());
        assertEquals(400,browser.request("POST",path,"{}",true).statusCode());
        assertEquals(200,browser.request("POST",path,body,true).statusCode());
        assertEquals(200,browser.request("POST",path,body,true).statusCode());
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM work_plan",Integer.class));
    }

}
