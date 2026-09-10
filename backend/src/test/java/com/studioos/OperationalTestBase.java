package com.studioos;
import com.fasterxml.jackson.databind.*;
import com.studioos.security.OperatorPrincipal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.context.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import java.util.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers @Import(TestMail.class) @ActiveProfiles("test")
@SpringBootTest @AutoConfigureMockMvc @DirtiesContext(classMode=DirtiesContext.ClassMode.AFTER_CLASS)
abstract class OperationalTestBase {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @Container static final GenericContainer<?> redis=new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    @DynamicPropertySource static void infrastructure(DynamicPropertyRegistry p){
        p.add("spring.datasource.url",postgres::getJdbcUrl);p.add("spring.datasource.username",postgres::getUsername);p.add("spring.datasource.password",postgres::getPassword);
        p.add("spring.data.redis.host",redis::getHost);p.add("spring.data.redis.port",()->redis.getMappedPort(6379));p.add("spring.data.redis.password",()->"");
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper mapper; @Autowired JdbcTemplate jdbc;
    record Fixture(UUID user,UUID studio,UUID staff){String base(){return "/api/v1/studios/"+studio;}}
    UUID user(){UUID id=UUID.randomUUID();jdbc.update("insert into users(id,name,status,created_at,updated_at) values (?,'Operator','ACTIVE',now(),now())",id);return id;}
    ResultActions call(UUID user,MockHttpServletRequestBuilder request) throws Exception {
        return mvc.perform(request.contentType("application/json").with(csrf()).with(authentication(UsernamePasswordAuthenticationToken.authenticated(new OperatorPrincipal(user,0),null,List.of()))));
    }
    JsonNode json(ResultActions result) throws Exception {return mapper.readTree(result.andReturn().getResponse().getContentAsString());}
    Fixture fixture() throws Exception {
        UUID user=user();var created=json(call(user,post("/api/v1/studios").content(mapper.writeValueAsString(Map.of("name","Studio","slug","ops-"+UUID.randomUUID(),"timezone","Asia/Seoul")))).andExpect(status().isCreated()));
        UUID studio=UUID.fromString(created.get("id").asText());
        var body=mapper.createObjectNode().put("version",0).put("businessCategory","LESSON").put("businessType","DANCE");
        body.putObject("capabilities").put("PRIVATE_LESSON",true).put("GROUP_CLASS",false).put("ATTENDANCE",false).put("PASS_MANAGEMENT",true).put("CUSTOMER_BOOKING",false);
        var days=body.putArray("businessHours");for(int i=1;i<=7;i++)days.addObject().put("weekday",i).put("closed",false).put("openTime","08:00").put("closeTime","22:00");
        body.putObject("bookingPolicy").put("slotIntervalMinutes",30).put("bookingWindowDays",30).put("cancellationCutoffHours",12);
        body.putObject("lessonPolicy").put("lowBalanceThreshold",2).put("expiryAlertDays",7).put("restoreOnTimelyCancellation",true);
        call(user,post("/api/v1/studios/"+studio+"/onboarding/complete").content(body.toString())).andExpect(status().isOk());
        return new Fixture(user,studio,jdbc.queryForObject("select id from staff where studio_id=?",UUID.class,studio));
    }
    UUID member(Fixture f,String role){UUID id=user();jdbc.update("insert into studio_memberships(id,studio_id,user_id,role,status,created_at) values (?,?,?,?,'ACTIVE',now())",UUID.randomUUID(),f.studio,id,role);return id;}
    UUID customer(Fixture f) throws Exception {return UUID.fromString(json(call(f.user,post(f.base()+"/customers").content(customerBody("Customer","01012345678")))).get("id").asText());}
    String customerBody(String name,String phone) throws Exception {return mapper.writeValueAsString(Map.of("name",name,"phone",phone,"memo","Internal only"));}
}
