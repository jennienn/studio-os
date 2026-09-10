package com.studioos.security;
import com.studioos.auth.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.security.core.context.SecurityContextHolder;

public class SessionVersionFilter extends OncePerRequestFilter {
    private final UserRepository users;
    public SessionVersionFilter(UserRepository users) { this.users=users; }
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth!=null && auth.getPrincipal() instanceof OperatorPrincipal p) {
            boolean valid=users.findById(p.userId()).filter(u -> u.status.equals("ACTIVE") && u.securityVersion==p.securityVersion()).isPresent();
            if (!valid) {
                if (request.getSession(false)!=null) request.getSession(false).invalidate();
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request,response);
    }
}
