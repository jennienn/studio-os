package com.studioos.security;
import com.studioos.auth.*;
import com.studioos.studio.*;
import com.studioos.common.ApiException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class StudioAuthorization {
    private final MembershipRepository memberships;
    private final UserRepository users;
    public StudioAuthorization(MembershipRepository memberships,UserRepository users) { this.memberships=memberships; this.users=users; }
    public OperatorUser user() {
        var auth=SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof OperatorPrincipal p)) throw unauthorized();
        return users.findById(p.userId()).filter(u -> u.status.equals("ACTIVE") && u.securityVersion==p.securityVersion())
            .orElseThrow(StudioAuthorization::unauthorized);
    }
    public AuthorizedStudioContext authorize(UUID studioId) {
        var user=user();
        var membership=memberships.findByStudioIdAndUserIdAndStatus(studioId,user.id,"ACTIVE")
            .orElseThrow(() -> new ApiException(403,"STUDIO_FORBIDDEN","사업장 접근 권한이 없습니다."));
        return new AuthorizedStudioContext(user.id,studioId,membership.role);
    }
    public List<StudioMembership> memberships() {
        return memberships.findByUserIdAndStatusOrderByCreatedAtAsc(user().id,"ACTIVE");
    }
    private static ApiException unauthorized() { return new ApiException(401,"SESSION_REQUIRED","다시 로그인해 주세요."); }
}
