package com.studioos.security;

import com.studioos.studio.StudioRepository;
import com.studioos.studio.configuration.StudioTaxonomy.Category;
import com.studioos.common.ApiException;
import org.springframework.stereotype.Service;

@Service
public class StudioCategoryGuard {
    private final StudioAuthorization authorization;
    private final StudioRepository studios;
    public StudioCategoryGuard(StudioAuthorization authorization,StudioRepository studios) {
        this.authorization=authorization; this.studios=studios;
    }
    public AuthorizedStudioContext require(java.util.UUID studioId,Category expected) {
        var context=authorization.authorize(studioId);
        var studio=studios.findById(context.authorizedStudioId()).orElseThrow();
        if (!studio.status.equals("ACTIVE"))
            throw new ApiException(409,"ONBOARDING_REQUIRED","사업장 설정을 먼저 완료해 주세요.");
        if (!expected.name().equals(studio.businessCategory))
            throw new ApiException(403,"CATEGORY_FORBIDDEN","현재 사업장 유형에서 사용할 수 없습니다.");
        return context;
    }
}
