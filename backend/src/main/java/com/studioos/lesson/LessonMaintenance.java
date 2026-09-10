package com.studioos.lesson;
import com.studioos.lesson.classes.ClassService;
import com.studioos.lesson.cycle.CycleService;
import com.studioos.studio.StudioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.jdbc.core.JdbcTemplate;
import java.util.UUID;
@Service
public class LessonMaintenance {
 private final StudioRepository studios;private final JdbcTemplate jdbc;private final CycleService cycles;private final ClassService classes;
 public LessonMaintenance(StudioRepository studios,JdbcTemplate jdbc,CycleService cycles,ClassService classes){this.studios=studios;this.jdbc=jdbc;this.cycles=cycles;this.classes=classes;}
 @Transactional public void expire(UUID studio){var s=studios.lockById(studio).orElseThrow();if(!"LESSON".equals(s.businessCategory)||!s.status.equals("ACTIVE"))return;
  jdbc.queryForList("select distinct enrollment_id from enrollment_cycles where studio_id=? and status='ACTIVE'",UUID.class,studio).forEach(e->cycles.settleEnrollment(studio,e,cycles.today(studio)));
 }
 @Transactional public void generate(UUID studio,UUID schedule){var s=studios.lockById(studio).orElseThrow();if(!"LESSON".equals(s.businessCategory)||!s.status.equals("ACTIVE"))return;
  if(jdbc.queryForObject("select count(*) from studio_capabilities where studio_id=? and capability='GROUP_CLASS' and enabled",Integer.class,studio)==1)classes.generate(studio,schedule);
 }
}
