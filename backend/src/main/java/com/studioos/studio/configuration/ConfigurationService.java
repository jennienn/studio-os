package com.studioos.studio.configuration;

import com.studioos.common.ApiException;
import com.studioos.security.*;
import com.studioos.studio.*;
import com.studioos.studio.StudioMembership.Role;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.time.Instant;
import java.util.*;
import static com.studioos.studio.configuration.ConfigurationDto.*;
import static com.studioos.studio.configuration.StudioTaxonomy.*;

@Service
public class ConfigurationService {
    private final StudioAuthorization authorization;
    private final StudioRepository studios;
    private final ConfigurationStore store;
    private final ConfigurationValidation validation;
    private final StudioCategoryGuard categoryGuard;

    public ConfigurationService(StudioAuthorization authorization, StudioRepository studios,
        ConfigurationStore store, ConfigurationValidation validation, StudioCategoryGuard categoryGuard) {
        this.authorization=authorization; this.studios=studios; this.store=store;
        this.validation=validation; this.categoryGuard=categoryGuard;
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public View get(UUID id) {
        var context=authorization.authorize(id);
        return view(find(context.authorizedStudioId()),context.membershipRole());
    }

    @Transactional
    public View complete(UUID id, Command command) {
        var context=authorization.authorize(id);
        context.requireOwner();
        var studio=studios.lockById(context.authorizedStudioId()).orElseThrow(ConfigurationService::missing);
        if (!studio.status.equals("PRE_ONBOARDING"))
            throw new ApiException(409,"ONBOARDING_ALREADY_COMPLETE","이미 온보딩이 완료되었습니다.");
        validation.validate(command);
        checkVersion(studio,command);
        store.write(studio.id,command);
        studio.businessCategory=command.businessCategory().name(); studio.businessType=command.businessType();
        studio.status="ACTIVE"; studio.configurationVersion++; studio.updatedAt=Instant.now();
        studios.flush();
        return view(studio,context.membershipRole());
    }

    @Transactional
    public View update(UUID id, Command command) {
        var context=authorization.authorize(id);
        if (context.membershipRole()==Role.STAFF) forbidden();
        var studio=studios.lockById(context.authorizedStudioId()).orElseThrow(ConfigurationService::missing);
        if (!studio.status.equals("ACTIVE"))
            throw new ApiException(409,"ONBOARDING_REQUIRED","먼저 온보딩을 완료해 주세요.");
        validation.validate(command);
        if (!studio.businessCategory.equals(command.businessCategory().name()))
            throw new ApiException(409,"CATEGORY_IMMUTABLE","완료된 사업장의 유형은 변경할 수 없습니다.");
        checkVersion(studio,command);
        if (context.membershipRole()!=Role.OWNER) {
            var before=store.read(studio.id);
            if (!studio.businessType.equals(command.businessType())
                || !before.capabilities().equals(command.capabilities())
                || (before.beautyPolicy()!=null && !before.beautyPolicy().depositEnabled().equals(command.beautyPolicy().depositEnabled())))
                forbidden();
        }
        store.write(studio.id,command);
        studio.businessType=command.businessType();
        studio.configurationVersion++; studio.updatedAt=Instant.now();
        studios.flush();
        return view(studio,context.membershipRole());
    }

    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Lesson lessonPolicy(UUID id) {
        var context=categoryGuard.require(id,Category.LESSON);
        return store.read(context.authorizedStudioId()).lessonPolicy();
    }
    @Transactional(readOnly=true, isolation=Isolation.REPEATABLE_READ)
    public Beauty beautyPolicy(UUID id) {
        var context=categoryGuard.require(id,Category.BEAUTY);
        return store.read(context.authorizedStudioId()).beautyPolicy();
    }
    private View view(Studio s,Role role) {
        var category=s.businessCategory==null ? null : Category.valueOf(s.businessCategory);
        boolean owner=role==Role.OWNER, operational=owner || role==Role.MANAGER;
        return new View(s.id,s.status,s.configurationVersion,category,s.businessType,
            category==null ? catalog() : Map.of(category,branch(category)),bookingDefaults(),
            category==null ? null : store.read(s.id),
            new Permissions(owner,owner,owner,operational,owner));
    }
    private Studio find(UUID id) { return studios.findById(id).orElseThrow(ConfigurationService::missing); }
    private void checkVersion(Studio studio,Command command) {
        if (studio.configurationVersion!=command.version())
            throw new ApiException(409,"CONFIGURATION_CONFLICT","다른 변경이 저장되었습니다. 최신 설정을 다시 불러와 주세요.");
    }
    private static ApiException missing() { return new ApiException(404,"STUDIO_NOT_FOUND","사업장을 찾을 수 없습니다."); }
    private static void forbidden() { throw new ApiException(403,"CONFIGURATION_FORBIDDEN","이 설정을 변경할 권한이 없습니다."); }
}
