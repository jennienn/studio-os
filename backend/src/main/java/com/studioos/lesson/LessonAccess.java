package com.studioos.lesson;

import com.studioos.common.ApiException;
import com.studioos.security.*;
import com.studioos.studio.*;
import com.studioos.studio.configuration.*;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class LessonAccess {
    private final OperationalAccess access;
    private final StudioRepository studios;
    private final StudioCapabilityRepository capabilities;
    public LessonAccess(OperationalAccess access,StudioRepository studios,StudioCapabilityRepository capabilities){this.access=access;this.studios=studios;this.capabilities=capabilities;}
    public AuthorizedStudioContext authorizedMember(UUID studio){return access.member(studio);}
    public AuthorizedStudioContext authorizedManager(UUID studio){return access.manager(studio);}
    public AuthorizedStudioContext authorizedLock(UUID studio){var actor=authorizedManager(studio);studios.lockById(studio).orElseThrow();return actor;}
    public void lesson(UUID studio){
        if(!studios.findById(studio).orElseThrow().businessCategory.equals("LESSON"))
            throw new ApiException(403,"LESSON_REQUIRED","레슨 사업장에서만 사용할 수 있습니다.");
    }
    public AuthorizedStudioContext member(UUID studio){
        var actor=authorizedMember(studio);
        lesson(actor.authorizedStudioId());
        return actor;
    }
    public AuthorizedStudioContext manager(UUID studio){var actor=authorizedManager(studio);lesson(actor.authorizedStudioId());return actor;}
    public AuthorizedStudioContext lock(UUID studio){var actor=manager(studio);studios.lockById(studio).orElseThrow();return actor;}
    public void capability(UUID studio,String value){if(capabilities.findByStudioIdOrderByCapabilityAsc(studio).stream().noneMatch(c->c.capability.name().equals(value)&&c.enabled))throw new ApiException(409,"LESSON_CAPABILITY_DISABLED","필요한 레슨 기능이 비활성화되어 있습니다.");}
    public void mode(UUID studio,String kind,String trigger){
        capability(studio,kind.equals("PRIVATE")?"PRIVATE_LESSON":"GROUP_CLASS");
        if((kind.equals("PRIVATE")&&"ATTENDANCE_PRESENT".equals(trigger))||(kind.equals("GROUP")&&"LESSON_COMPLETED".equals(trigger)))throw new ApiException(400,"INCOMPATIBLE_TRIGGER","수강 형태와 차감 시점이 맞지 않습니다.");
        if("ATTENDANCE_PRESENT".equals(trigger))capability(studio,"ATTENDANCE");
    }
}
