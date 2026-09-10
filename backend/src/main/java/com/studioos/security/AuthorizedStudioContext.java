package com.studioos.security;
import java.util.UUID;
import com.studioos.studio.StudioMembership.Role;
import com.studioos.common.ApiException;
public record AuthorizedStudioContext(UUID authenticatedUserId, UUID authorizedStudioId, Role membershipRole) {
    public void requireOwner() {
        if (membershipRole != Role.OWNER) throw new ApiException(403,"FORBIDDEN","이 작업을 수행할 권한이 없습니다.");
    }
}
