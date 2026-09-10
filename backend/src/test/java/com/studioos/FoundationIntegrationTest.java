package com.studioos;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.session.Session;
import org.springframework.session.SessionRepository;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Testcontainers
@org.springframework.context.annotation.Import(TestMail.class)
@ActiveProfiles("test")
@SpringBootTest
@AutoConfigureMockMvc
class FoundationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17-alpine");
    @Container
    static final GenericContainer<?> redis = new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", postgres::getJdbcUrl);
        properties.add("spring.datasource.username", postgres::getUsername);
        properties.add("spring.datasource.password", postgres::getPassword);
        properties.add("spring.data.redis.host", redis::getHost);
        properties.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        properties.add("spring.data.redis.password", () -> "");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired Flyway flyway;
    @Autowired RedisConnectionFactory redisConnections;
    @Autowired SessionRepository<?> sessions;
    @Autowired MockMvc mvc;

    @Test void contextConnectsToPostgresAndValidatesEmptyMigrationHistory() {
        assertThat(jdbc.queryForObject("select 1", Integer.class)).isEqualTo(1);
        flyway.validate();
        assertThat(flyway.info().pending()).isEmpty();
        assertThat(jdbc.queryForList("select tablename from pg_tables where schemaname = 'public'", String.class))
                .containsExactlyInAnyOrder("flyway_schema_history", "users", "auth_accounts", "studios",
                    "studio_memberships", "staff", "email_verification_tokens", "password_reset_tokens",
                    "studio_capabilities", "business_hours", "booking_policies", "lesson_policies", "beauty_policies");
    }

    @Test void redisAndServerSessionRoundTrip() {
        try (var connection = redisConnections.getConnection()) {
            assertThat(connection.ping()).isEqualTo("PONG");
        }
        verifySessionRoundTrip(sessions);
    }

    private <S extends Session> void verifySessionRoundTrip(SessionRepository<S> repository) {
        S session = repository.createSession();
        session.setAttribute("foundationSmoke", "ok");
        repository.save(session);
        try {
            Session restored = repository.findById(session.getId());
            assertThat(restored).isNotNull();
            assertThat(restored.<String>getAttribute("foundationSmoke")).isEqualTo("ok");
        } finally {
            repository.deleteById(session.getId());
        }
        assertThat(repository.findById(session.getId())).isNull();
    }

    @Test void healthIsPublicAndDoesNotExposeInfrastructureDetails() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
    }

    @Test void nonHealthRequestsAreDeniedAndCsrfRemainsEnabled() throws Exception {
        mvc.perform(get("/api/v1/customers")).andExpect(status().isUnauthorized());
        mvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/v1/customers")).andExpect(status().isForbidden());
    }
}
