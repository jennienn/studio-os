package com.studioos.customer;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;

public interface CustomerRepository extends Repository<Customer,UUID> {
    Customer save(Customer customer);
    Optional<Customer> findByStudioIdAndId(UUID studioId,UUID id);
    @Query("select c from Customer c where c.studioId=:studioId and (:status='ALL' or c.status=:status) and " +
        "(:search='' or lower(c.name) like :namePattern escape '!' or c.normalizedPhone like :phonePattern escape '!')")
    Page<Customer> search(UUID studioId,String status,String search,String namePattern,String phonePattern,Pageable pageable);
}
