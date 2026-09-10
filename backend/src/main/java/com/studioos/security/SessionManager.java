package com.studioos.security;
import jakarta.servlet.http.*;
import org.springframework.stereotype.Component;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.session.FindByIndexNameSessionRepository;
import java.util.*;

@Component
public class SessionManager {
    private final FindByIndexNameSessionRepository<?> sessions;
    public SessionManager(FindByIndexNameSessionRepository<?> sessions) { this.sessions=sessions; }
    public void authenticate(OperatorPrincipal principal,HttpServletRequest request,HttpServletResponse response) {
        request.getSession();
        request.changeSessionId();
        ActiveStudioSession.clear(request.getSession());
        var context=SecurityContextHolder.createEmptyContext();
        context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(principal,null,List.of()));
        SecurityContextHolder.setContext(context);
        new HttpSessionSecurityContextRepository().saveContext(context,request,response);
        new HttpSessionCsrfTokenRepository().saveToken(null,request,response);
    }
    public void invalidateUser(UUID userId) {
        // PostgreSQL securityVersion is also checked on every request, covering races
        // with in-flight login/session saves and Redis index eventual cleanup.
        sessions.findByPrincipalName(userId.toString()).keySet().forEach(sessions::deleteById);
    }
}
