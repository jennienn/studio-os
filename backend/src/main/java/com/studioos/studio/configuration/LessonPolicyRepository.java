package com.studioos.studio.configuration;

import java.util.*;
import org.springframework.data.repository.Repository;
public interface LessonPolicyRepository extends Repository<LessonPolicy,UUID> {
    Optional<LessonPolicy> findByStudioId(UUID studioId);
    LessonPolicy save(LessonPolicy entity);
}
