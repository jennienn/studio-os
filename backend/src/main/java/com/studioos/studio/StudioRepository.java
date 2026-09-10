package com.studioos.studio;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface StudioRepository extends JpaRepository<Studio,UUID> {
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("select s from Studio s where s.id = :id")
    Optional<Studio> lockById(UUID id);
}
