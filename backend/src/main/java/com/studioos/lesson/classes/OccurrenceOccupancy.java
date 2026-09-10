package com.studioos.lesson.classes;
import com.studioos.booking.AdditionalOccupancy;
import com.studioos.common.ApiException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.time.Instant;
import java.sql.Timestamp;
import java.util.UUID;
@Component
public class OccurrenceOccupancy implements AdditionalOccupancy {
 private final JdbcTemplate jdbc;public OccurrenceOccupancy(JdbcTemplate jdbc){this.jdbc=jdbc;}
 public void check(UUID studio,UUID staff,Instant start,Instant end){
  if(jdbc.queryForObject("select count(*) from class_occurrences where studio_id=? and instructor_staff_id=? and status='SCHEDULED' and start_at<? and end_at>?",Long.class,studio,staff,Timestamp.from(end),Timestamp.from(start))>0)
   throw new ApiException(409,"INSTRUCTOR_CONFLICT","담당자의 그룹 수업과 시간이 겹칩니다.");
 }
}
