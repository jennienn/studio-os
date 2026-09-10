package com.studioos.security;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.studioos.common.ApiErrors;
import com.studioos.auth.TokenStore;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.List;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.web.filter.OncePerRequestFilter;

public class AuthRateLimitFilter extends OncePerRequestFilter {
    private final StringRedisTemplate redis;
    private final ObjectMapper mapper;
    private final int limit;
    private static final DefaultRedisScript<Long> COUNT=new DefaultRedisScript<>(
        "local n=redis.call('INCR',KEYS[1]); if n==1 then redis.call('EXPIRE',KEYS[1],60) end; return n",Long.class);
    public AuthRateLimitFilter(StringRedisTemplate redis,ObjectMapper mapper,int limit) { this.redis=redis; this.mapper=mapper; this.limit=limit; }
    protected boolean shouldNotFilter(HttpServletRequest r) {
        return !(r.getMethod().equals("POST") && r.getRequestURI().startsWith("/api/v1/auth/"))
            && !r.getRequestURI().startsWith("/oauth2/authorization/");
    }
    protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain) throws ServletException,IOException {
        try {
            // Never trust client-supplied forwarding headers. Proxy deployments share
            // an aggregate limit unless a trusted ingress is configured separately.
            Long count=redis.execute(COUNT,List.of("auth:rate:"+TokenStore.hash(request.getRemoteAddr())));
            if (count==null) throw new IllegalStateException();
            if (count>limit) {
                response.setHeader("Retry-After","60");
                ApiErrors.write(response,mapper,429,"RATE_LIMITED","잠시 후 다시 시도해 주세요."); return;
            }
        } catch (RuntimeException e) {
            ApiErrors.write(response,mapper,503,"AUTH_UNAVAILABLE","잠시 후 다시 시도해 주세요."); return;
        }
        chain.doFilter(request,response);
    }
}
