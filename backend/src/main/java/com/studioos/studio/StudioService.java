package com.studioos.studio;
import com.studioos.staff.*;
import com.studioos.security.*;
import com.studioos.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.ZoneId;
import java.util.*;

@Service
public class StudioService {
    private final StudioRepository studios;
    private final MembershipRepository memberships;
    private final StaffRepository staff;
    private final StudioAuthorization authorization;
    public StudioService(StudioRepository studios,MembershipRepository memberships,StaffRepository staff,StudioAuthorization authorization) {
        this.studios=studios; this.memberships=memberships; this.staff=staff; this.authorization=authorization;
    }
    public record View(UUID id,String name,String slug,String timezone,String status,
                       String businessCategory,String businessType,StudioMembership.Role role) {}
    private View view(Studio s,StudioMembership.Role role) {
        return new View(s.id,s.name,s.slug,s.timezone,s.status,s.businessCategory,s.businessType,role);
    }
    @Transactional
    public View create(String name,String requestedSlug,String timezone) {
        var owner=authorization.user();
        String slug=requestedSlug.trim().toLowerCase(Locale.ROOT);
        if (!slug.matches("[a-z0-9]+(-[a-z0-9]+)*") || slug.length()<3 || slug.length()>63)
            throw new ApiException(400,"INVALID_SLUG","주소는 영문 소문자·숫자·하이픈 3~63자로 입력해 주세요.");
        if (!ZoneId.getAvailableZoneIds().contains(timezone))
            throw new ApiException(400,"INVALID_TIMEZONE","유효한 시간대를 선택해 주세요.");
        var studio=studios.saveAndFlush(new Studio(name.trim(),slug,timezone));
        memberships.saveAndFlush(new StudioMembership(studio.id,owner.id,StudioMembership.Role.OWNER));
        staff.save(new Staff(studio.id,owner.id,owner.name));
        return view(studio,StudioMembership.Role.OWNER);
    }
    @Transactional(readOnly=true)
    public List<View> list() {
        return authorization.memberships().stream().map(m -> view(studios.findById(m.studioId).orElseThrow(),m.role)).toList();
    }
    @Transactional(readOnly=true)
    public View get(UUID id) {
        var context=authorization.authorize(id);
        return view(studios.findById(context.authorizedStudioId()).orElseThrow(),context.membershipRole());
    }
}
