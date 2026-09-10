package com.studioos.security;

import com.studioos.common.ApiException;
import com.studioos.studio.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class OperationalAccess {
    private final StudioAuthorization authorization;
    private final StudioRepository studios;
    public OperationalAccess(StudioAuthorization authorization,StudioRepository studios) {this.authorization=authorization;this.studios=studios;}
    public AuthorizedStudioContext member(UUID id) {
        var context=authorization.authorize(id);
        if(!studios.findById(context.authorizedStudioId()).orElseThrow().status.equals("ACTIVE"))
            throw new ApiException(409,"ONBOARDING_REQUIRED","사업장 설정을 먼저 완료해 주세요.");
        return context;
    }
    public AuthorizedStudioContext manager(UUID id) {
        var context=member(id);
        if(context.membershipRole()==StudioMembership.Role.STAFF)
            throw new ApiException(403,"OPERATION_FORBIDDEN","이 작업에 접근할 권한이 없습니다.");
        return context;
    }
}
