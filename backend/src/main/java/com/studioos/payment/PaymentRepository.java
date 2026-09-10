package com.studioos.payment;
import org.springframework.data.repository.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.*;
import java.util.*;
public interface PaymentRepository extends Repository<Payment,UUID> {
    Payment save(Payment payment);
    Optional<Payment> findByStudioIdAndId(UUID studioId,UUID id);
    @Query("select p from Payment p where p.studioId=:studioId and (:status='ALL' or p.status=:status) and (:customerId is null or p.customerId=:customerId)")
    Page<Payment> search(UUID studioId,String status,UUID customerId,Pageable pageable);
}
