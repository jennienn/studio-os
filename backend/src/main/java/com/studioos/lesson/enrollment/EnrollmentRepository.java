package com.studioos.lesson.enrollment;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import java.util.*;
public interface EnrollmentRepository extends Repository<Enrollment,UUID>{
 Enrollment save(Enrollment e);
 Optional<Enrollment> findByStudioIdAndId(UUID studio,UUID id);
 @Query("select e from Enrollment e where e.studioId=:studio and (:customer is null or e.customerId=:customer)")
 Page<Enrollment> list(UUID studio,UUID customer,Pageable page);
}
