package com.studioos.auth;
import com.studioos.security.SessionManager;
import com.studioos.common.ApiException;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class OAuthLoginHandler implements AuthenticationSuccessHandler {
    private final OAuthAccountService accounts;
    private final SessionManager sessions;
    private final String baseUrl;
    public OAuthLoginHandler(OAuthAccountService accounts,SessionManager sessions,@Value("${app.auth.base-url}") String baseUrl) {
        this.accounts=accounts; this.sessions=sessions; this.baseUrl=baseUrl;
    }
    public void onAuthenticationSuccess(HttpServletRequest request,HttpServletResponse response,Authentication authentication) throws IOException {
        try {
            var oauth=(OAuth2AuthenticationToken)authentication;
            var identity=OAuthIdentityMapper.map(oauth.getAuthorizedClientRegistrationId(),oauth.getPrincipal().getAttributes());
            sessions.authenticate(accounts.resolve(identity),request,response);
            response.sendRedirect(baseUrl+"/app");
        } catch (RuntimeException e) {
            SecurityContextHolder.clearContext();
            if (request.getSession(false)!=null) request.getSession(false).invalidate();
            String code=e instanceof ApiException a && a.code.equals("OAUTH_PROFILE_REQUIRED") ? "profile-required" : "oauth";
            response.sendRedirect(baseUrl+"/login?error="+code);
        }
    }
}
