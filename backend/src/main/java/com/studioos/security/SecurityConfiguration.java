package com.studioos.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.auth.*;
import com.studioos.common.ApiErrors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.*;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestRedirectFilter;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;

@Configuration
public class SecurityConfiguration {
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(12); }
    @Bean SecurityFilterChain security(HttpSecurity http,ObjectMapper mapper,UserRepository users,
        StringRedisTemplate redis,ClientRegistrationRepository clients,OAuthLoginHandler oauth,
        @Value("${app.auth.base-url}") String baseUrl,
        @Value("${app.auth.rate-limit-per-minute:30}") int rateLimit) throws Exception {
        http.csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
            .requestCache(cache -> cache.requestCache(new NullRequestCache()))
            .authorizeHttpRequests(r -> r
                .requestMatchers(HttpMethod.GET,"/actuator/health","/actuator/health/**","/api/v1/auth/csrf","/api/v1/auth/providers").permitAll()
                .requestMatchers(HttpMethod.POST,"/api/v1/auth/signup","/api/v1/auth/login",
                    "/api/v1/auth/email-verification/request","/api/v1/auth/email-verification/confirm",
                    "/api/v1/auth/password-reset/request","/api/v1/auth/password-reset/confirm").permitAll()
                .requestMatchers("/oauth2/authorization/**","/login/oauth2/code/**").permitAll()
                .requestMatchers("/api/v1/auth/me","/api/v1/studios","/api/v1/studios/**").authenticated()
                .anyRequest().denyAll())
            .exceptionHandling(e -> e
                .authenticationEntryPoint((req,res,ex) -> ApiErrors.write(res,mapper,401,"SESSION_REQUIRED","다시 로그인해 주세요."))
                .accessDeniedHandler((req,res,ex) -> ApiErrors.write(res,mapper,403,"ACCESS_DENIED","요청 권한 또는 보안 토큰을 확인해 주세요.")))
            .sessionManagement(s -> s.sessionFixation(f -> f.changeSessionId()))
            .logout(l -> l.logoutUrl("/api/v1/auth/logout")
                .logoutSuccessHandler((req,res,auth) -> res.setStatus(204))
                .deleteCookies("SESSION"))
            .addFilterAfter(new SessionVersionFilter(users),SecurityContextHolderFilter.class)
            .addFilterBefore(new AuthRateLimitFilter(redis,mapper,rateLimit),OAuth2AuthorizationRequestRedirectFilter.class);
        http.oauth2Login(o -> o.clientRegistrationRepository(clients)
            .authorizedClientRepository(new TransientOAuthClientRepository())
            .loginPage(baseUrl+"/login").successHandler(oauth)
            .failureHandler((req,res,ex) -> {
                org.springframework.security.core.context.SecurityContextHolder.clearContext();
                if (req.getSession(false)!=null) req.getSession(false).invalidate();
                res.sendRedirect(baseUrl+"/login?error=oauth");
            }));
        return http.build();
    }
}
