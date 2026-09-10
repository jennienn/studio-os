package com.studioos.lesson;
import org.springframework.stereotype.Component;
import org.springframework.scheduling.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.LoggerFactory;
import java.util.UUID;
@Component @EnableScheduling
public class LessonJobs {
 private final JdbcTemplate jdbc;private final LessonMaintenance maintenance;
 public LessonJobs(JdbcTemplate jdbc,LessonMaintenance maintenance){this.jdbc=jdbc;this.maintenance=maintenance;}
 @Scheduled(fixedDelayString="${app.lesson.maintenance-delay-ms:3600000}",initialDelayString="${app.lesson.maintenance-delay-ms:3600000}")
 public void run(){
  for(UUID studio:jdbc.queryForList("select id from studios where business_category='LESSON' and status='ACTIVE'",UUID.class)){
   try{maintenance.expire(studio);}catch(RuntimeException e){LoggerFactory.getLogger(getClass()).warn("Lesson expiry requires retry for studio {}",studio);}
   for(UUID schedule:jdbc.queryForList("select id from class_schedules where studio_id=? and active",UUID.class,studio)){
    try{maintenance.generate(studio,schedule);}catch(RuntimeException e){LoggerFactory.getLogger(getClass()).warn("Lesson generation requires schedule review: studio {}, schedule {}",studio,schedule);}
   }
  }
 }
}
