package com.studioos.studio.configuration;

import java.util.*;
import org.springframework.data.repository.Repository;
public interface BookingPolicyRepository extends Repository<BookingPolicy,UUID> {
    Optional<BookingPolicy> findByStudioId(UUID studioId);
    BookingPolicy save(BookingPolicy entity);
}
