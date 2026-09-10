package com.studioos;

import com.fasterxml.jackson.databind.*;
import com.studioos.auth.*;
import com.studioos.security.*;
import com.studioos.studio.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.session.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.*;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.*;
import org.testcontainers.utility.DockerImageName;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;

@Testcontainers @ActiveProfiles("test") @Import(TestMail.class)
@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT,
 properties={"app.auth.rate-limit-per-minute=10000","GOOGLE_CLIENT_ID=test-google","GOOGLE_CLIENT_SECRET=test-secret",
 "KAKAO_CLIENT_ID=test-kakao","KAKAO_CLIENT_SECRET=test-secret"})
class AuthTenantIntegrationTest {
    @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17-alpine");
    @Container static final GenericContainer<?> redis=new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
    @DynamicPropertySource static void infrastructure(DynamicPropertyRegistry p) {
        p.add("spring.datasource.url",postgres::getJdbcUrl); p.add("spring.datasource.username",postgres::getUsername);
        p.add("spring.datasource.password",postgres::getPassword); p.add("spring.data.redis.host",redis::getHost);
        p.add("spring.data.redis.port",() -> redis.getMappedPort(6379)); p.add("spring.data.redis.password",() -> "");
    }
    @LocalServerPort int port;
    @Autowired ObjectMapper mapper;
    @Autowired JdbcTemplate jdbc;
    @Autowired TestMail.Mailbox mail;
    @Autowired PasswordEncoder encoder;
    @Autowired OAuthAccountService oauth;
    @Autowired FindByIndexNameSessionRepository<?> sessions;
    @Autowired StringRedisTemplate strings;
    static final String PASSWORD="A safe password 2026!";
    class Browser {
        final CookieManager cookies=new CookieManager(null,CookiePolicy.ACCEPT_ALL);
        final HttpClient client=HttpClient.newBuilder().cookieHandler(cookies).followRedirects(HttpClient.Redirect.NEVER).build();
        HttpResponse<String> get(String path) throws Exception { return send("GET",path,null,null); }
        HttpResponse<String> post(String path,Object body) throws Exception {
            var csrf=json(get("/api/v1/auth/csrf")); return send("POST",path,body,csrf.get("token").asText());
        }
        HttpResponse<String> send(String method,String path,Object body,String csrf) throws Exception {
            var builder=HttpRequest.newBuilder(URI.create("http://localhost:"+port+path)).timeout(Duration.ofSeconds(20));
            if (csrf!=null) builder.header("X-CSRF-TOKEN",csrf);
            builder.header("Content-Type","application/json");
            return client.send(builder.method(method,body==null ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build(),HttpResponse.BodyHandlers.ofString());
        }
        String cookie() { return cookies.getCookieStore().getCookies().stream().filter(c -> c.getName().equals("SESSION")).map(HttpCookie::getValue).findFirst().orElse(""); }
    }
    JsonNode json(HttpResponse<String> response) throws Exception { return mapper.readTree(response.body()); }
    String email() { return "owner-"+UUID.randomUUID()+"@example.test"; }
    void signup(Browser b,String email) throws Exception {
        assertThat(b.post("/api/v1/auth/signup",Map.of("email",email,"name","테스트 운영자","password",PASSWORD)).statusCode()).isEqualTo(202);
    }
    Browser verified(String email) throws Exception {
        var b=new Browser(); signup(b,email);
        assertThat(b.post("/api/v1/auth/email-verification/confirm",Map.of("token",mail.token(email,TokenStore.Kind.VERIFY))).statusCode()).isEqualTo(200);
        return b;
    }
    Browser loggedIn(String email) throws Exception {
        var b=verified(email);
        assertThat(b.post("/api/v1/auth/login",Map.of("email",email,"password",PASSWORD)).statusCode()).isEqualTo(200);
        return b;
    }
    @Test void signupHashesNormalizesAndDoesNotOverwriteDuplicate() throws Exception {
        var b=new Browser(); String email=email(); signup(b,email.toUpperCase(Locale.ROOT));
        String hash=jdbc.queryForObject("select password_hash from auth_accounts where provider='EMAIL' and provider_user_id=?",String.class,email);
        assertThat(hash).isNotEqualTo(PASSWORD); assertThat(encoder.matches(PASSWORD,hash)).isTrue();
        signup(b,email);
        assertThat(jdbc.queryForObject("select count(*) from auth_accounts where provider_user_id=?",Long.class,email)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select token_hash from email_verification_tokens where user_id=(select user_id from auth_accounts where provider_user_id=?)",String.class,email))
            .isEqualTo(TokenStore.hash(mail.token(email,TokenStore.Kind.VERIFY)));
        assertThat(b.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(b.post("/api/v1/auth/signup",Map.of("email",email(),"name","Test","password","short")).statusCode()).isEqualTo(400);
    }
    @Test void verificationGatesLoginAndRejectsReplayAndExpiry() throws Exception {
        String email=email(); var b=new Browser(); signup(b,email);
        var login=b.post("/api/v1/auth/login",Map.of("email",email,"password",PASSWORD));
        assertThat(login.statusCode()).isEqualTo(403);
        assertThat(json(login).get("code").asText()).isEqualTo("EMAIL_VERIFICATION_REQUIRED");
        String token=mail.token(email,TokenStore.Kind.VERIFY);
        jdbc.update("update email_verification_tokens set expires_at=current_timestamp - interval '1 second' where token_hash=?",TokenStore.hash(token));
        assertThat(b.post("/api/v1/auth/email-verification/confirm",Map.of("token",token)).statusCode()).isEqualTo(400);
        assertThat(b.post("/api/v1/auth/email-verification/request",Map.of("email",email)).statusCode()).isEqualTo(202);
        token=mail.token(email,TokenStore.Kind.VERIFY);
        assertThat(b.post("/api/v1/auth/email-verification/confirm",Map.of("token",token)).statusCode()).isEqualTo(200);
        assertThat(b.post("/api/v1/auth/email-verification/confirm",Map.of("token",token)).statusCode()).isEqualTo(400);
    }
    @Test void loginRotatesSessionLogoutInvalidatesAndCsrfIsRequired() throws Exception {
        String email=email(); var b=verified(email);
        String before=b.cookie();
        assertThat(b.post("/api/v1/auth/login",Map.of("email",email,"password","incorrect")).statusCode()).isEqualTo(401);
        assertThat(b.post("/api/v1/auth/login",Map.of("email",email,"password",PASSWORD)).statusCode()).isEqualTo(200);
        assertThat(b.cookie()).isNotEqualTo(before);
        assertThat(b.get("/api/v1/auth/me").statusCode()).isEqualTo(200);
        var denied=b.send("POST","/api/v1/studios",Map.of("name","Denied","slug","denied","timezone","Asia/Seoul"),null);
        assertThat(denied.statusCode()).isEqualTo(403);
        assertThat(json(denied).hasNonNull("traceId")).isTrue();
        assertThat(b.send("POST","/api/v1/auth/logout",null,null).statusCode()).isEqualTo(403);
        assertThat(b.post("/api/v1/auth/logout",Map.of()).statusCode()).isEqualTo(204);
        assertThat(b.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(new Browser().get("/api/v1/studios").statusCode()).isEqualTo(401);
    }
    @Test void resetIsGenericOneUseAndRevokesAllSessions() throws Exception {
        String email=email(); var first=loggedIn(email); var second=new Browser();
        second.post("/api/v1/auth/login",Map.of("email",email,"password",PASSWORD));
        var known=first.post("/api/v1/auth/password-reset/request",Map.of("email",email));
        var unknown=new Browser().post("/api/v1/auth/password-reset/request",Map.of("email",email()));
        assertThat(known.statusCode()).isEqualTo(202); assertThat(unknown.statusCode()).isEqualTo(202);
        assertThat(known.body()).isEqualTo(unknown.body());
        String token=mail.token(email,TokenStore.Kind.RESET);
        String replacement="A replacement password!";
        assertThat(new Browser().post("/api/v1/auth/password-reset/confirm",Map.of("token",token,"password",replacement)).statusCode()).isEqualTo(200);
        assertThat(new Browser().post("/api/v1/auth/password-reset/confirm",Map.of("token",token,"password",PASSWORD)).statusCode()).isEqualTo(400);
        assertThat(first.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(second.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        assertThat(first.post("/api/v1/auth/login",Map.of("email",email,"password",PASSWORD)).statusCode()).isEqualTo(401);
        assertThat(first.post("/api/v1/auth/login",Map.of("email",email,"password",replacement)).statusCode()).isEqualTo(200);
    }
    @Test void expiredResetCannotChangePassword() throws Exception {
        String email=email(); var b=loggedIn(email);
        b.post("/api/v1/auth/password-reset/request",Map.of("email",email));
        String token=mail.token(email,TokenStore.Kind.RESET);
        jdbc.update("update password_reset_tokens set expires_at=current_timestamp - interval '1 second' where token_hash=?",TokenStore.hash(token));
        assertThat(b.post("/api/v1/auth/password-reset/confirm",Map.of("token",token,"password","Another good password!")).statusCode()).isEqualTo(400);
        assertThat(b.post("/api/v1/auth/login",Map.of("email",email,"password",PASSWORD)).statusCode()).isEqualTo(200);
    }
    @Test void concurrentResetConsumesTokenExactlyOnce() throws Exception {
        String email=email(); var browser=loggedIn(email);
        browser.post("/api/v1/auth/password-reset/request",Map.of("email",email));
        String token=mail.token(email,TokenStore.Kind.RESET);
        try (var pool=Executors.newFixedThreadPool(2)) {
            Callable<Integer> reset=() -> new Browser().post("/api/v1/auth/password-reset/confirm",
                Map.of("token",token,"password","Concurrent new password!")).statusCode();
            var results=pool.invokeAll(List.of(reset,reset));
            assertThat(List.of(results.get(0).get(),results.get(1).get())).containsExactlyInAnyOrder(200,400);
        }
        assertThat(jdbc.queryForObject("select security_version from users where id=(select user_id from auth_accounts where provider='EMAIL' and provider_user_id=?)",Long.class,email)).isEqualTo(1);
    }
    @Test void studioCreationOwnsStaffAndRejectsOtherTenantAndGlobalSlugCollision() throws Exception {
        var owner=loggedIn(email()); var other=loggedIn(email()); String slug="studio-"+UUID.randomUUID();
        var created=owner.post("/api/v1/studios",Map.of("name","새 사업장","slug",slug.toUpperCase(Locale.ROOT),"timezone","Asia/Seoul"));
        assertThat(created.statusCode()).isEqualTo(201); var studio=json(created); String id=studio.get("id").asText();
        assertThat(studio.get("businessCategory").isNull()).isTrue();
        assertThat(studio.get("status").asText()).isEqualTo("PRE_ONBOARDING");
        UUID userId=UUID.fromString(json(owner.get("/api/v1/auth/me")).get("id").asText());
        assertThat(jdbc.queryForObject("select user_id from staff where studio_id=?",UUID.class,UUID.fromString(id))).isEqualTo(userId);
        assertThat(jdbc.queryForObject("select role from studio_memberships where studio_id=? and user_id=?",String.class,UUID.fromString(id),userId)).isEqualTo("OWNER");
        assertThat(owner.get("/api/v1/studios/"+id).statusCode()).isEqualTo(200);
        assertThat(other.get("/api/v1/studios/"+id).statusCode()).isEqualTo(403);
        assertThat(other.post("/api/v1/studios/"+id+"/activate",Map.of()).statusCode()).isEqualTo(403);
        assertThat(json(other.get("/api/v1/studios")).size()).isZero();
        assertThat(other.post("/api/v1/studios",Map.of("name","Collision","slug",slug,"timezone","Asia/Seoul")).statusCode()).isEqualTo(409);
        assertThat(owner.post("/api/v1/studios",Map.of("name","Invalid","slug","../bad","timezone","Asia/Seoul")).statusCode()).isEqualTo(400);
        assertThat(json(owner.get("/api/v1/auth/me")).get("activeStudioId").asText()).isEqualTo(id);
    }
    @Test void multipleStudiosSelectAndRecheckRevokedMembership() throws Exception {
        var b=loggedIn(email());
        String first=json(b.post("/api/v1/studios",Map.of("name","One","slug","one-"+UUID.randomUUID(),"timezone","Asia/Seoul"))).get("id").asText();
        String second=json(b.post("/api/v1/studios",Map.of("name","Two","slug","two-"+UUID.randomUUID(),"timezone","Asia/Seoul"))).get("id").asText();
        assertThat(json(b.get("/api/v1/auth/me")).get("activeStudioId").asText()).isEqualTo(second);
        assertThat(b.post("/api/v1/studios/"+first+"/activate",Map.of()).statusCode()).isEqualTo(200);
        assertThat(json(b.get("/api/v1/auth/me")).get("activeStudioId").asText()).isEqualTo(first);
        jdbc.update("update studio_memberships set status='INACTIVE' where studio_id=?",UUID.fromString(first));
        assertThat(b.get("/api/v1/studios/"+first).statusCode()).isEqualTo(403);
        assertThat(json(b.get("/api/v1/auth/me")).get("activeStudioId").asText()).isEqualTo(second);
    }
    @Test void staffFailureRollsBackStudioAndMembership() throws Exception {
        var b=loggedIn(email()); String slug="rollback-"+UUID.randomUUID();
        jdbc.execute("create function phase2_reject_staff() returns trigger language plpgsql as $$ begin raise exception 'test rollback'; end $$");
        jdbc.execute("create trigger phase2_reject_staff before insert on staff for each row execute function phase2_reject_staff()");
        try {
            assertThat(b.post("/api/v1/studios",Map.of("name","Rollback","slug",slug,"timezone","Asia/Seoul")).statusCode()).isEqualTo(500);
            assertThat(jdbc.queryForObject("select count(*) from studios where slug=?",Long.class,slug)).isZero();
            assertThat(json(b.get("/api/v1/studios")).size()).isZero();
        } finally { jdbc.execute("drop trigger phase2_reject_staff on staff"); jdbc.execute("drop function phase2_reject_staff()"); }
    }
    @Test void oauthIdentitiesNeverLinkOnEmailAndMissingKakaoEmailIsSupported() throws Exception {
        String email=email(); loggedIn(email);
        var google=OAuthIdentityMapper.map("google",Map.of("sub","google-"+UUID.randomUUID(),"name","Google Owner","email",email,"email_verified",true));
        var first=oauth.resolve(google);
        assertThat(oauth.resolve(google).userId()).isEqualTo(first.userId());
        assertThat(jdbc.queryForObject("select user_id from auth_accounts where provider='EMAIL' and provider_user_id=?",UUID.class,email)).isNotEqualTo(first.userId());
        var kakao=OAuthIdentityMapper.map("kakao",Map.of("id",System.nanoTime(),"kakao_account",Map.of("profile",Map.of("nickname","카카오 운영자"))));
        var second=oauth.resolve(kakao);
        assertThat(jdbc.queryForObject("select email from users where id=?",String.class,second.userId())).isNull();
        assertThat(second.userId()).isNotEqualTo(first.userId());
        assertThatThrownBy(() -> oauth.resolve(OAuthIdentityMapper.map("kakao",Map.of("id",System.nanoTime()))))
            .isInstanceOf(com.studioos.common.ApiException.class).hasMessageContaining("이름");
    }
    @Test void concurrentOauthIdentityCreatesOnlyOneUser() throws Exception {
        var identity=new OAuthIdentityMapper.Identity("GOOGLE","race-"+UUID.randomUUID(),email(),"Concurrent");
        try (var pool=Executors.newFixedThreadPool(2)) {
            var results=pool.invokeAll(List.of(() -> oauth.resolve(identity),() -> oauth.resolve(identity)));
            assertThat(results.get(0).get()).isEqualTo(results.get(1).get());
        }
        assertThat(jdbc.queryForObject("select count(*) from users where email=?",Long.class,identity.email())).isEqualTo(1);
    }
    @Test void oauthUsesStateAndRejectsCallbacksWithoutIt() throws Exception {
        var b=new Browser();
        for (String provider:List.of("google","kakao")) {
            var redirect=b.get("/oauth2/authorization/"+provider);
            assertThat(redirect.statusCode()).isEqualTo(302);
            assertThat(redirect.headers().firstValue("location").orElseThrow()).contains("state=").contains("client_id=");
            var invalid=b.get("/login/oauth2/code/"+provider+"?code=invalid&state=wrong");
            assertThat(invalid.statusCode()).isEqualTo(302);
            assertThat(invalid.headers().firstValue("location").orElseThrow()).endsWith("/login?error=oauth");
            assertThat(b.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        }
    }
    @Test void sessionExpiryAndUserDisableAreEnforced() throws Exception {
        String email=email(); var b=loggedIn(email);
        UUID id=UUID.fromString(json(b.get("/api/v1/auth/me")).get("id").asText());
        assertThat(sessions.findByPrincipalName(id.toString())).hasSize(1);
        assertThat(sessions.findByPrincipalName(id.toString()).values().iterator().next().getMaxInactiveInterval())
            .isEqualTo(Duration.ofHours(12));
        expire(sessions,id);
        Thread.sleep(1200);
        assertThat(b.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
        b.post("/api/v1/auth/login",Map.of("email",email,"password",PASSWORD));
        jdbc.update("update users set status='DISABLED' where id=?",id);
        assertThat(b.get("/api/v1/auth/me").statusCode()).isEqualTo(401);
    }
    private <S extends Session> void expire(FindByIndexNameSessionRepository<S> repository,UUID id) {
        repository.findByPrincipalName(id.toString()).values().forEach(s -> { s.setMaxInactiveInterval(Duration.ofSeconds(1)); repository.save(s); });
    }
    @Test void sensitiveRequestObjectsCannotPrintCredentials() {
        assertThat(new AuthController.Credentials("owner@example.test",PASSWORD).toString()).doesNotContain(PASSWORD);
        assertThat(new AuthController.Reset("raw-secret-token",PASSWORD).toString())
            .doesNotContain(PASSWORD,"raw-secret-token");
        assertThat(new AuthController.TokenRequest("raw-secret-token").toString()).doesNotContain("raw-secret-token");
    }
    @Test void rateLimitFailsClosedAndRoleGuardRejectsNonOwner() throws Exception {
        var filter=new AuthRateLimitFilter(strings,mapper,1);
        var request=new MockHttpServletRequest("POST","/api/v1/auth/login");
        request.setRemoteAddr("test-"+UUID.randomUUID());
        var first=new MockHttpServletResponse(); filter.doFilter(request,first,(a,b) -> {});
        var second=new MockHttpServletResponse(); filter.doFilter(request,second,(a,b) -> { throw new AssertionError("limited request reached controller"); });
        assertThat(second.getStatus()).isEqualTo(429);
        var unavailable=org.mockito.Mockito.mock(StringRedisTemplate.class);
        org.mockito.Mockito.doThrow(new IllegalStateException("unavailable")).when(unavailable)
            .execute(org.mockito.ArgumentMatchers.<org.springframework.data.redis.core.script.RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.anyList(),org.mockito.ArgumentMatchers.<Object[]>any());
        var outage=new MockHttpServletResponse();
        new AuthRateLimitFilter(unavailable,mapper,30).doFilter(request,outage,
            (a,b) -> { throw new AssertionError("Redis outage reached controller"); });
        assertThat(outage.getStatus()).isEqualTo(503);
        assertThatThrownBy(() -> new AuthorizedStudioContext(UUID.randomUUID(),UUID.randomUUID(),StudioMembership.Role.STAFF).requireOwner())
            .isInstanceOf(com.studioos.common.ApiException.class);
    }
}
