package com.studioos.auth;
import java.util.*;
import org.springframework.data.jpa.repository.*;
import jakarta.persistence.LockModeType;
public interface UserRepository extends JpaRepository<OperatorUser,UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from OperatorUser u where u.id = :id")
    Optional<OperatorUser> lockById(UUID id);
}
