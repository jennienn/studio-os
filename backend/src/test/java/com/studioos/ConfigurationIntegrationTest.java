package com.studioos;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.studioos.security.OperatorPrincipal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers @Import(TestMail.class) @ActiveProfiles("test")
@SpringBootTest @AutoConfigureMockMvc
class ConfigurationIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @Container static final GenericContainer<?> redis=new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    @DynamicPropertySource static void infrastructure(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url",postgres::getJdbcUrl);p.add("spring.datasource.username",postgres::getUsername);
        p.add("spring.datasource.password",postgres::getPassword);p.add("spring.data.redis.host",redis::getHost);
        p.add("spring.data.redis.port",()->redis.getMappedPort(6379));p.add("spring.data.redis.password",()->"");
    }
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    record Fixture(UUID user,UUID studio) { String base(){return "/api/v1/studios/"+studio;} }
    UUID user() {
        UUID id=UUID.randomUUID();
        jdbc.update("insert into users(id,name,status,created_at,updated_at) values (?,'Test','ACTIVE',now(),now())",id);
        return id;
    }
    Fixture fixture() throws Exception {
        UUID user=user();
        var response=perform(user,post("/api/v1/studios").content(mapper.writeValueAsString(Map.of(
            "name","Configured Studio","slug","test-"+UUID.randomUUID(),"timezone","Asia/Seoul"))))
            .andExpect(status().isCreated()).andReturn();
        return new Fixture(user,UUID.fromString(mapper.readTree(response.getResponse().getContentAsString()).get("id").asText()));
    }
    ResultActions perform(UUID user,MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.contentType("application/json").with(csrf()).with(authentication(
            UsernamePasswordAuthenticationToken.authenticated(new OperatorPrincipal(user,0),null,List.of()))));
    }
    ObjectNode body(String category) {
        ObjectNode c=mapper.createObjectNode();
        c.put("version",0).put("businessCategory",category).put("businessType",category.equals("LESSON")?"DANCE":"NAIL");
        var flags=c.putObject("capabilities");
        if(category.equals("LESSON")) flags.put("PRIVATE_LESSON",true).put("GROUP_CLASS",true).put("ATTENDANCE",true).put("PASS_MANAGEMENT",true);
        else flags.put("DEPOSIT",true).put("REVISIT",true);
        flags.put("CUSTOMER_BOOKING",true);
        var hours=c.putArray("businessHours");
        for(int day=1;day<=7;day++) hours.addObject().put("weekday",day).put("closed",false).put("openTime","09:00").put("closeTime","18:00");
        c.putObject("bookingPolicy").put("slotIntervalMinutes",30).put("bookingWindowDays",30).put("cancellationCutoffHours",12);
        if(category.equals("LESSON")) c.putObject("lessonPolicy").put("lowBalanceThreshold",2).put("expiryAlertDays",7).put("restoreOnTimelyCancellation",true);
        else c.putObject("beautyPolicy").put("depositEnabled",true).put("noShowEnabled",true);
        return c;
    }
    ResultActions complete(Fixture f,ObjectNode c) throws Exception {
        return perform(f.user,post(f.base()+"/onboarding/complete").content(c.toString()));
    }
    ResultActions update(Fixture f,UUID actor,ObjectNode c) throws Exception {
        return perform(actor,put(f.base()+"/configuration").content(c.toString()));
    }
    void member(Fixture f,UUID user,String role) {
        jdbc.update("insert into studio_memberships(id,studio_id,user_id,role,status,created_at) values (?,?,?,?,'ACTIVE',now())",UUID.randomUUID(),f.studio,user,role);
    }
    @Test void bothCategoriesCompletePersistReloadAndRejectRepeat() throws Exception {
        for(String category:List.of("LESSON","BEAUTY")) {
            var f=fixture();var c=body(category);
            perform(f.user,get(f.base()+"/onboarding")).andExpect(jsonPath("$.configuration").isEmpty()).andExpect(jsonPath("$.status").value("PRE_ONBOARDING"));
            complete(f,c).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ACTIVE")).andExpect(jsonPath("$.version").value(1));
            perform(f.user,get(f.base()+"/configuration")).andExpect(jsonPath("$.businessCategory").value(category))
                .andExpect(jsonPath("$.configuration.businessHours.length()").value(7));
            complete(f,c).andExpect(status().isConflict());
            assertThat(jdbc.queryForObject("select count(*) from business_hours where studio_id=?",Integer.class,f.studio)).isEqualTo(7);
        }
    }
    @Test void directApiRejectsCrossCategorySubtypesCapabilitiesAndPolicies() throws Exception {
        for(String category:List.of("LESSON","BEAUTY")) {
            var f=fixture();var c=body(category);
            c.put("businessType",category.equals("LESSON")?"NAIL":"PILATES");complete(f,c).andExpect(status().isBadRequest());
            c=body(category);((ObjectNode)c.get("capabilities")).put(category.equals("LESSON")?"DEPOSIT":"ATTENDANCE",false);
            complete(f,c).andExpect(status().isBadRequest());
            c=body(category);c.set(category.equals("LESSON")?"beautyPolicy":"lessonPolicy",body(category.equals("LESSON")?"BEAUTY":"LESSON").get(category.equals("LESSON")?"beautyPolicy":"lessonPolicy"));
            complete(f,c).andExpect(status().isBadRequest());
            c=body(category);c.put("businessCategory","UNKNOWN");complete(f,c).andExpect(status().isBadRequest()).andExpect(jsonPath("$.traceId").isNotEmpty());
            c=body(category);((ObjectNode)c.get("capabilities")).put("ARBITRARY",true);complete(f,c).andExpect(status().isBadRequest());
        }
    }
    @Test void invalidHoursBookingPolicyAndIncompleteSubmissionLeaveStudioUnconfigured() throws Exception {
        var f=fixture();var c=body("LESSON");
        c.remove("lessonPolicy");complete(f,c).andExpect(status().isBadRequest());
        c=body("LESSON");((ObjectNode)c.get("businessHours").get(0)).put("closeTime","08:00");complete(f,c).andExpect(status().isBadRequest());
        c=body("LESSON");((ObjectNode)c.get("businessHours").get(1)).put("weekday",1);complete(f,c).andExpect(status().isBadRequest());
        c=body("LESSON");((ObjectNode)c.get("bookingPolicy")).put("slotIntervalMinutes",0);complete(f,c).andExpect(status().isBadRequest());
        assertThat(jdbc.queryForObject("select status from studios where id=?",String.class,f.studio)).isEqualTo("PRE_ONBOARDING");
        assertThat(jdbc.queryForObject("select count(*) from studio_capabilities where studio_id=?",Integer.class,f.studio)).isZero();
    }
    @Test void lessonDependenciesApplyToCompletionAndFullUpdates() throws Exception {
        var f=fixture();var c=body("LESSON");
        ((ObjectNode)c.get("capabilities")).put("PASS_MANAGEMENT",false);complete(f,c).andExpect(status().isBadRequest());
        c=body("LESSON");((ObjectNode)c.get("capabilities")).put("PRIVATE_LESSON",false).put("GROUP_CLASS",false).put("ATTENDANCE",false);
        complete(f,c).andExpect(status().isBadRequest());
        c=body("LESSON");((ObjectNode)c.get("capabilities")).put("GROUP_CLASS",false);complete(f,c).andExpect(status().isBadRequest());
        c=body("LESSON");complete(f,c).andExpect(status().isOk());c.put("version",1);
        ((ObjectNode)c.get("capabilities")).put("GROUP_CLASS",false);update(f,f.user,c).andExpect(status().isBadRequest());
        ((ObjectNode)c.get("capabilities")).put("ATTENDANCE",false).put("CUSTOMER_BOOKING",false);
        update(f,f.user,c).andExpect(status().isOk());
        c.put("version",2);((ObjectNode)c.get("capabilities")).put("PASS_MANAGEMENT",false);update(f,f.user,c).andExpect(status().isBadRequest());
    }
    @Test void ownerSubtypeChangesPreserveRowsAndRejectCategoryChangesAndStaleWrites() throws Exception {
        for(String category:List.of("LESSON","BEAUTY")) {
            var f=fixture();var c=body(category);complete(f,c).andExpect(status().isOk());
            var ids=jdbc.queryForList("select id from studio_capabilities where studio_id=? order by capability",UUID.class,f.studio);
            c.put("version",1).put("businessType",category.equals("LESSON")?"PILATES":"EYELASH");
            ((ObjectNode)c.get("capabilities")).put("CUSTOMER_BOOKING",false);
            update(f,f.user,c).andExpect(status().isOk()).andExpect(jsonPath("$.version").value(2));
            assertThat(jdbc.queryForList("select id from studio_capabilities where studio_id=? order by capability",UUID.class,f.studio)).isEqualTo(ids);
            update(f,f.user,c).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CONFIGURATION_CONFLICT"));
            var opposite=body(category.equals("LESSON")?"BEAUTY":"LESSON");opposite.put("version",2);
            update(f,f.user,opposite).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("CATEGORY_IMMUTABLE"));
        }
    }
    @Test void managerAndStaffCannotCompleteOrChangeStructuralConfiguration() throws Exception {
        for(String category:List.of("LESSON","BEAUTY")) {
            var f=fixture();UUID manager=user(),staff=user();member(f,manager,"MANAGER");member(f,staff,"STAFF");var c=body(category);
            perform(manager,post(f.base()+"/onboarding/complete").content(c.toString())).andExpect(status().isForbidden());
            perform(staff,post(f.base()+"/onboarding/complete").content(c.toString())).andExpect(status().isForbidden());
            complete(f,c).andExpect(status().isOk());c.put("version",1);
            perform(manager,get(f.base()+"/configuration")).andExpect(status().isOk()).andExpect(jsonPath("$.permissions.editCapabilities").value(false)).andExpect(jsonPath("$.permissions.editPolicies").value(true));
            perform(staff,get(f.base()+"/configuration")).andExpect(status().isOk()).andExpect(jsonPath("$.permissions.editPolicies").value(false));
            update(f,staff,c).andExpect(status().isForbidden());
            c.put("businessType",category.equals("LESSON")?"YOGA":"HAIR");update(f,manager,c).andExpect(status().isForbidden());
            c=body(category);c.put("version",1);((ObjectNode)c.get("capabilities")).put("CUSTOMER_BOOKING",false);update(f,manager,c).andExpect(status().isForbidden());
            c=body(category);c.put("version",1);((ObjectNode)c.get("bookingPolicy")).put("bookingWindowDays",45);
            ((ObjectNode)c.get("businessHours").get(0)).put("closed",true);
            if(category.equals("LESSON")) ((ObjectNode)c.get("lessonPolicy")).put("lowBalanceThreshold",3).put("expiryAlertDays",14).put("restoreOnTimelyCancellation",false);
            else ((ObjectNode)c.get("beautyPolicy")).put("noShowEnabled",false);
            update(f,manager,c).andExpect(status().isOk());
        }
    }
    @Test void depositMustMatchAndOnlyOwnerCanToggleIt() throws Exception {
        var f=fixture();var c=body("BEAUTY");UUID manager=user();member(f,manager,"MANAGER");
        ((ObjectNode)c.get("beautyPolicy")).put("depositEnabled",false);complete(f,c).andExpect(status().isBadRequest());
        c=body("BEAUTY");complete(f,c).andExpect(status().isOk());c.put("version",1);
        ((ObjectNode)c.get("beautyPolicy")).put("depositEnabled",false);
        ((ObjectNode)c.get("capabilities")).put("DEPOSIT",false);
        update(f,manager,c).andExpect(status().isForbidden());update(f,f.user,c).andExpect(status().isOk());
    }
    @Test void crossTenantReadWriteAndDirectCategoryRoutesAreProtected() throws Exception {
        var a=fixture();var b=fixture();var c=body("LESSON");
        for(String path:List.of("/configuration","/onboarding","/configuration/lesson-policy","/configuration/beauty-policy"))
            perform(a.user,get(b.base()+path)).andExpect(status().isForbidden());
        perform(a.user,post(b.base()+"/onboarding/complete").content(c.toString())).andExpect(status().isForbidden());
        update(b,a.user,c).andExpect(status().isForbidden());
        perform(b.user,get(b.base()+"/configuration/lesson-policy")).andExpect(status().isConflict());
        complete(a,c).andExpect(status().isOk());complete(b,body("BEAUTY")).andExpect(status().isOk());
        perform(a.user,get(a.base()+"/configuration/lesson-policy")).andExpect(status().isOk());
        perform(a.user,get(a.base()+"/configuration/beauty-policy")).andExpect(status().isForbidden());
        perform(b.user,get(b.base()+"/configuration/lesson-policy")).andExpect(status().isForbidden());
        perform(b.user,get(b.base()+"/configuration/beauty-policy")).andExpect(status().isOk());
        update(b,a.user,body("BEAUTY").put("version",1)).andExpect(status().isForbidden());
        mvc.perform(put(a.base()+"/configuration").contentType("application/json").content(c.toString())).andExpect(status().isForbidden());
        jdbc.update("update studio_memberships set status='INACTIVE' where studio_id=?",a.studio);
        perform(a.user,get(a.base()+"/configuration")).andExpect(status().isForbidden());
    }
    @Test void databaseFailureRollsBackAllConfigurationAndActivation() throws Exception {
        var f=fixture();
        jdbc.execute("create function phase3_fail() returns trigger language plpgsql as $$ begin raise exception 'test rollback'; end $$");
        jdbc.execute("create trigger phase3_fail before insert on lesson_policies for each row execute function phase3_fail()");
        try {
            complete(f,body("LESSON")).andExpect(status().is5xxServerError());
            assertThat(jdbc.queryForObject("select status from studios where id=?",String.class,f.studio)).isEqualTo("PRE_ONBOARDING");
            assertThat(jdbc.queryForObject("select configuration_version from studios where id=?",Long.class,f.studio)).isZero();
            for(String table:List.of("studio_capabilities","business_hours","booking_policies","lesson_policies","beauty_policies"))
                assertThat(jdbc.queryForObject("select count(*) from "+table+" where studio_id=?",Integer.class,f.studio)).isZero();
        } finally {jdbc.execute("drop trigger phase3_fail on lesson_policies");jdbc.execute("drop function phase3_fail()");}
        complete(f,body("LESSON")).andExpect(status().isOk());
    }
    @Test void concurrentCompletionAndUpdatesHaveOneWinner() throws Exception {
        var f=fixture();var c=body("LESSON");
        try(var pool=Executors.newFixedThreadPool(2)) {
            var start=new CountDownLatch(1);
            Callable<Integer> task=()->{start.await();return complete(f,c).andReturn().getResponse().getStatus();};
            var one=pool.submit(task);var two=pool.submit(task);start.countDown();
            assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(200,409);
            c.put("version",1);
            var go=new CountDownLatch(1);
            Callable<Integer> write=()->{go.await();return update(f,f.user,c).andReturn().getResponse().getStatus();};
            one=pool.submit(write);two=pool.submit(write);go.countDown();
            assertThat(List.of(one.get(),two.get())).containsExactlyInAnyOrder(200,409);
        }
    }
    @Test void databaseEnforcesCategoryStateTenantForeignKeysAndUniqueness() throws Exception {
        var f=fixture();complete(f,body("LESSON")).andExpect(status().isOk());
        assertThatThrownBy(()->jdbc.update("update studios set business_type='NAIL' where id=?",f.studio)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("insert into studio_capabilities(id,studio_id,capability,enabled) values (?,?,'ATTENDANCE',true)",UUID.randomUUID(),UUID.randomUUID())).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("insert into business_hours(id,studio_id,weekday,closed) values (?,?,1,true)",UUID.randomUUID(),f.studio)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThatThrownBy(()->jdbc.update("update booking_policies set booking_window_days=0 where studio_id=?",f.studio)).isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
