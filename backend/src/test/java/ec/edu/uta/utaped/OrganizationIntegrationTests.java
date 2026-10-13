package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.institution.OrganizationService;
import ec.edu.uta.utaped.planning.WorkPlanService;
import ec.edu.uta.utaped.planning.WorkPlanModels.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.server.ResponseStatusException;

class OrganizationIntegrationTests extends WorkPlanIntegrationTests {
    @Autowired OrganizationService organization;
    @Autowired WorkPlanService plans;
    UUID scope() {
        openPeriod();var id=organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,null));
        organization.member(id,"admin@example.invalid",new OrganizationService.MemberInput(user,"MEMBER","Responsable",true,0L));
        organization.principal(id,"admin@example.invalid",new OrganizationService.PrincipalInput(user,0L));return id;
    }
    @Test void httpRoleMatrixAndMembershipRevocationKeepSessionsFromBypassingDocumentPermissions() throws Exception {
        UUID scope=scope();organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"MEMBER","Integrante",true,0L));
        var plan=plans.create("user@example.invalid",new Create(group,period,UUID.randomUUID(),"Aceptación por rol",scope));
        Browser owner=new Browser(),member=new Browser(),administrator=new Browser(),anonymous=new Browser();
        owner.login("user@example.invalid",PASSWORD);member.login("other@example.invalid",PASSWORD);administrator.login("admin@example.invalid",PASSWORD);
        String path="/api/work-plans/"+plan.id();
        assertEquals(401,anonymous.request("GET",path,null,false).statusCode());
        assertEquals(200,owner.request("GET",path,null,false).statusCode());assertEquals(200,member.request("GET",path,null,false).statusCode());
        assertEquals(404,administrator.request("GET",path,null,false).statusCode());assertEquals(404,administrator.request("GET",path+"/artifacts",null,false).statusCode());
        assertEquals(403,member.request("PUT",path,updateBody(0,"Intrusión"),true).statusCode());assertEquals(403,member.request("POST",path+"/artifacts","{\"rowVersion\":0}",true).statusCode());
        assertEquals(403,owner.request("GET","/api/admin/users",null,false).statusCode());assertEquals(200,administrator.request("GET","/api/admin/users",null,false).statusCode());
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"MEMBER","Baja",false,1L));
        assertEquals(404,member.request("GET",path,null,false).statusCode());assertEquals(404,member.request("GET",path+"/artifacts",null,false).statusCode());
        assertEquals(404,member.request("GET",path+"/history",null,false).statusCode());assertEquals(200,owner.request("GET",path,null,false).statusCode());
        assertEquals("Aceptación por rol",plans.get(plan.id(),"user@example.invalid").title());assertEquals(0,plans.get(plan.id(),"user@example.invalid").rowVersion());
    }
    @Test void staleMembershipAndGeneralRemovalCannotOverwritePeriodMembership() throws Exception {
        UUID scope=scope();
        assertEquals(409,assertThrows(ResponseStatusException.class,()->organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(user,"MEMBER","Cambio obsoleto",true,0L))).getStatusCode().value());
        Browser browser=new Browser();browser.login("admin@example.invalid",PASSWORD);
        assertEquals(409,browser.request("DELETE","/api/admin/groups/"+group+"/members/"+user,null,true).statusCode());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM membership WHERE group_id=? AND user_id=?",Integer.class,group,user));
    }
    @Test void principalOnlyUniqueScopeAndExplicitHistoricalAccess() {
        UUID scope=scope();
        var plan=plans.create("user@example.invalid",new Create(group,period,UUID.randomUUID(),"Plan organizacional",scope));
        assertEquals(scope,plan.scopeId());assertEquals(user,plan.createdBy());assertTrue(plan.editable());
        var duplicate=assertThrows(ResponseStatusException.class,()->plans.create("user@example.invalid",new Create(group,period,UUID.randomUUID(),"Duplicado",scope)));assertEquals(409,duplicate.getStatusCode().value());
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"MEMBER","Nuevo integrante",true,0L));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->plans.get(plan.id(),"other@example.invalid")).getStatusCode().value());
        organization.principal(scope,"admin@example.invalid",new OrganizationService.PrincipalInput(outsider,1L));
        assertTrue(plans.get(plan.id(),"other@example.invalid").editable());assertFalse(plans.get(plan.id(),"user@example.invalid").editable());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->plans.update(plan.id(),"user@example.invalid",new Update(plan.rowVersion(),plan.title(),plan.institutionalUnit(),plan.career(),"No autorizado",""))).getStatusCode().value());
        assertEquals(404,assertThrows(ResponseStatusException.class,()->plans.get(plan.id(),"admin@example.invalid")).getStatusCode().value());
        organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(user,"MEMBER","Responsable anterior",false,1L));
        assertEquals(404,assertThrows(ResponseStatusException.class,()->plans.get(plan.id(),"user@example.invalid")).getStatusCode().value());
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM work_plan WHERE id=?",Integer.class,plan.id()));
    }
    @Test void memberMayReadButNotEditAndCannotSpoofScopeOrCareer() {
        UUID scope=scope();organization.member(scope,"admin@example.invalid",new OrganizationService.MemberInput(outsider,"MEMBER","Colaborador",true,0L));
        var plan=plans.create("user@example.invalid",new Create(group,period,UUID.randomUUID(),"Plan compartido",scope));
        assertFalse(plans.get(plan.id(),"other@example.invalid").editable());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->plans.create("other@example.invalid",new Create(group,period,UUID.randomUUID(),"Intrusión",scope))).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->plans.update(plan.id(),"user@example.invalid",new Update(plan.rowVersion(),plan.title(),plan.institutionalUnit(),"Carrera falsa","",""))).getStatusCode().value());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->organization.createScope("admin@example.invalid",new OrganizationService.ScopeInput(group,period,null))).getStatusCode().value());
    }
}
