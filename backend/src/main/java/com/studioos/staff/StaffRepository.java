package com.studioos.staff;
import java.util.*;
import org.springframework.data.repository.Repository;
public interface StaffRepository extends Repository<Staff,UUID> {
    Staff save(Staff staff);
    Optional<Staff> findByStudioIdAndUserId(UUID studioId,UUID userId);
}
