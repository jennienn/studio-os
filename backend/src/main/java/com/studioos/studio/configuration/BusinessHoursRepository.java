package com.studioos.studio.configuration;

import java.util.*;
import org.springframework.data.repository.Repository;
public interface BusinessHoursRepository extends Repository<BusinessHours,UUID> {
    List<BusinessHours> findByStudioIdOrderByWeekdayAsc(UUID studioId);
    BusinessHours save(BusinessHours entity);
}
