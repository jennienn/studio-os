package com.studioos.lesson;
import com.studioos.common.ApiException;
import com.studioos.studio.configuration.*;
import org.springframework.stereotype.Component;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
@Component
public class LessonConfigurationGuard implements ConfigurationChangeGuard {
 private final JdbcTemplate jdbc;public LessonConfigurationGuard(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public void validate(UUID studio,ConfigurationDto.Command command){if(command.businessCategory()!=StudioTaxonomy.Category.LESSON)return;
  for(String capability:new String[]{"PRIVATE_LESSON","GROUP_CLASS","ATTENDANCE"}){
   if(Boolean.TRUE.equals(command.capabilities().get(StudioTaxonomy.Capability.valueOf(capability))))continue;
   String condition=capability.equals("PRIVATE_LESSON")?"d.lesson_mode='PRIVATE'":capability.equals("GROUP_CLASS")?"d.lesson_mode='GROUP'":"c.deduction_trigger_snapshot='ATTENDANCE_PRESENT'";
   if(jdbc.queryForObject("select count(*) from lesson_booking_details d join bookings b on b.studio_id=d.studio_id and b.id=d.booking_id join enrollment_cycles c on c.studio_id=d.studio_id and c.id=d.enrollment_cycle_id where d.studio_id=? and b.status in ('PENDING','CONFIRMED') and "+condition,Long.class,studio)>0)throw new ApiException(409,"LESSON_SETTLEMENT_REQUIRED","미해결 레슨 예약을 처리한 뒤 기능을 꺼 주세요.");
  }
 }
}
