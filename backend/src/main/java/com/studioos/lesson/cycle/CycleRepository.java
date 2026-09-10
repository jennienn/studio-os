package com.studioos.lesson.cycle;
import org.springframework.data.repository.Repository;
import java.util.*;
public interface CycleRepository extends Repository<EnrollmentCycle,UUID>{
 EnrollmentCycle save(EnrollmentCycle c);
 Optional<EnrollmentCycle> findByStudioIdAndId(UUID studio,UUID id);
 List<EnrollmentCycle> findByStudioIdAndEnrollmentIdOrderByCreatedAtDesc(UUID studio,UUID enrollment);
 Optional<EnrollmentCycle> findByStudioIdAndEnrollmentIdAndStatus(UUID studio,UUID enrollment,String status);
}
