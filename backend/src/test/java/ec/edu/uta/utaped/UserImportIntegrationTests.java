package ec.edu.uta.utaped;

import static org.junit.jupiter.api.Assertions.*;
import ec.edu.uta.utaped.identity.UserImportService;
import ec.edu.uta.utaped.identity.UserImportMaintenance;
import ec.edu.uta.utaped.institution.OrganizationService;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class UserImportIntegrationTests extends WorkPlanIntegrationTests {
    @Autowired UserImportService imports;
    @Autowired UserImportMaintenance maintenance;
    @Autowired OrganizationService organization;
    final String administrator="admin@example.invalid";
    MockMultipartFile csv(String text) { return new MockMultipartFile("file","usuarios.csv","text/csv",text.getBytes(StandardCharsets.UTF_8)); }
    String header="email,username,first_names,last_names\n";
    @Test void previewCommitIsAtomicPrivateAndOneTime() {
        var batch=imports.preview(administrator,csv("\uFEFF"+header+"new@example.invalid,New.User,Ana,Pérez\nsecond@example.invalid,second.user,Luis,Lopez\n"));
        assertEquals(2,batch.validCount());assertEquals("new.user",batch.rows().getFirst().data().username());
        assertEquals(3,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
        var credentials=new String(imports.commit(batch.id(),administrator),StandardCharsets.UTF_8);
        assertEquals(3,credentials.lines().count());String password=credentials.lines().skip(1).findFirst().orElseThrow().split(",")[2];
        assertTrue(encoder.matches(password,jdbc.queryForObject("SELECT password_hash FROM app_user WHERE username='new.user'",String.class)));
        assertTrue(jdbc.queryForObject("SELECT must_change_password AND system_role='USER' FROM app_user WHERE username='new.user'",Boolean.class));
        assertEquals(2,imports.get(batch.id(),administrator).importedCount());assertTrue(imports.get(batch.id(),administrator).rows().isEmpty());
        assertEquals(409,assertThrows(ResponseStatusException.class,()->imports.commit(batch.id(),administrator)).getStatusCode().value());
        assertFalse(jdbc.queryForObject("SELECT rows::text FROM user_import_batch WHERE id=?",String.class,batch.id()).contains(password));
        assertEquals(2,jdbc.queryForObject("SELECT count(*) FROM audit_event WHERE action='USER_IMPORTED'",Integer.class));
    }
    @Test void duplicatesAndConcurrentChangesNeverPartiallyImport() {
        var invalid=imports.preview(administrator,csv(header+"new@example.invalid,duplicate,Ana,Perez\nnew@example.invalid,duplicate,Luis,Lopez\nuser@example.invalid,existing,Other,Person\n"));
        assertEquals(2,invalid.errorCount());assertEquals(409,assertThrows(ResponseStatusException.class,()->imports.commit(invalid.id(),administrator)).getStatusCode().value());
        var valid=imports.preview(administrator,csv(header+"new@example.invalid,new.user,Ana,Perez\nchanged@example.invalid,changed,Luis,Lopez\n"));
        add(UUID.randomUUID(),"changed@example.invalid","USER",false);
        assertEquals(409,assertThrows(ResponseStatusException.class,()->imports.commit(valid.id(),administrator)).getStatusCode().value());
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM app_user WHERE email='new@example.invalid'",Integer.class));
    }
    @Test void optionalMembershipCannotGrantAdminPrincipalOrPasswordReset() {
        openPeriod();UUID scope=organization.createScope(administrator,new OrganizationService.ScopeInput(group,period,null));
        var batch=imports.preview(administrator,csv("email;username;first_names;last_names;scope_id;membership_role;position_label\nnew@example.invalid;new.user;Ana;Perez;"+scope+";COORDINATOR;Coordinación\n"));
        assertEquals(1,batch.validCount());imports.commit(batch.id(),administrator);
        assertTrue(jdbc.queryForObject("SELECT active AND membership_role='COORDINATOR' AND NOT password_reset_authorized FROM scope_member WHERE scope_id=?",Boolean.class,scope));
        assertNull(jdbc.queryForObject("SELECT principal_user_id FROM organizational_scope WHERE id=?",UUID.class,scope));
        assertEquals(0,jdbc.queryForObject("SELECT count(*) FROM document_access",Integer.class));
        assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM scope_membership_history WHERE scope_id=?",Integer.class,scope));
    }
    @Test void malformedCsvPrivilegesOwnershipExpiryAndCleanup() {
        for(String text:new String[]{"email,email,first_names,last_names\nx,x,x,x\n",header+"x,y,z\n","email,username,first_names,last_names,password\nx,x,x,x,secret\n",header})
            assertEquals(400,assertThrows(ResponseStatusException.class,()->imports.preview(administrator,csv(text))).getStatusCode().value());
        assertEquals(400,assertThrows(ResponseStatusException.class,()->imports.preview(administrator,new MockMultipartFile("file","x.csv","text/csv",new byte[]{(byte)0xff}))).getStatusCode().value());
        assertEquals(403,assertThrows(ResponseStatusException.class,()->imports.preview("user@example.invalid",csv(header+"new@example.invalid,new,Ana,Perez\n"))).getStatusCode().value());
        var batch=imports.preview(administrator,csv(header+"new@example.invalid,new,Ana,Perez\n"));
        add(UUID.randomUUID(),"admin2@example.invalid","ADMIN",false);
        assertEquals(404,assertThrows(ResponseStatusException.class,()->imports.get(batch.id(),"admin2@example.invalid")).getStatusCode().value());
        jdbc.update("UPDATE user_import_batch SET expires_at=now()-interval '1 minute' WHERE id=?",batch.id());
        assertEquals(410,assertThrows(ResponseStatusException.class,()->imports.commit(batch.id(),administrator)).getStatusCode().value());
        maintenance.purge();assertTrue(imports.get(batch.id(),administrator).rows().isEmpty());
    }
    @Test void concurrentBatchesCannotOverwriteOrLeavePartialAccounts() throws Exception {
        var first=imports.preview(administrator,csv(header+"shared@example.invalid,shared,Ana,Perez\nfirst@example.invalid,first,Ana,Uno\n"));
        var second=imports.preview(administrator,csv(header+"shared@example.invalid,shared,Ana,Perez\nsecond@example.invalid,second,Ana,Dos\n"));
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var latch=new java.util.concurrent.CountDownLatch(1);
            var work=new java.util.ArrayList<java.util.concurrent.Future<Boolean>>();
            for(var batch:java.util.List.of(first,second)) work.add(pool.submit(()->{latch.await();try {imports.commit(batch.id(),administrator);return true;}catch(ResponseStatusException|org.springframework.dao.DataIntegrityViolationException conflict){return false;}}));
            latch.countDown();int successes=0;for(var result:work) if(result.get(60,java.util.concurrent.TimeUnit.SECONDS)) successes++;
            assertEquals(1,successes);assertEquals(5,jdbc.queryForObject("SELECT count(*) FROM app_user",Integer.class));
            assertEquals(1,jdbc.queryForObject("SELECT count(*) FROM user_import_batch WHERE committed_at IS NOT NULL",Integer.class));
        }
    }

}
