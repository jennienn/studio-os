package com.studioos.studio.configuration;

import java.util.*;
import org.springframework.data.repository.Repository;
public interface StudioCapabilityRepository extends Repository<StudioCapability,UUID> {
    List<StudioCapability> findByStudioIdOrderByCapabilityAsc(UUID studioId);
    StudioCapability save(StudioCapability entity);
}
