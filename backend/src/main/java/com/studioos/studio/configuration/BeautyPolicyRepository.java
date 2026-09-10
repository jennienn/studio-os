package com.studioos.studio.configuration;

import java.util.*;
import org.springframework.data.repository.Repository;
public interface BeautyPolicyRepository extends Repository<BeautyPolicy,UUID> {
    Optional<BeautyPolicy> findByStudioId(UUID studioId);
    BeautyPolicy save(BeautyPolicy entity);
}
