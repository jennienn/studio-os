package com.studioos.studio;
import java.util.*;
import org.springframework.data.jpa.repository.*;
public interface MembershipRepository extends JpaRepository<StudioMembership,UUID> {
    Optional<StudioMembership> findByStudioIdAndUserIdAndStatus(UUID studioId,UUID userId,String status);
    // Identity-scoped membership discovery is the bootstrap for tenant authorization.
    List<StudioMembership> findByUserIdAndStatusOrderByCreatedAtAsc(UUID userId,String status);
}
