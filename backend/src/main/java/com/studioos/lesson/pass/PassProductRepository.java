package com.studioos.lesson.pass;
import org.springframework.data.repository.Repository;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.Query;
import java.util.*;
public interface PassProductRepository extends Repository<PassProduct,UUID>{
 PassProduct save(PassProduct p);
 Optional<PassProduct> findByStudioIdAndId(UUID studio,UUID id);
 @Query("select p from PassProduct p where p.studioId=:studio and (:active is null or p.active=:active)")
 Page<PassProduct> list(UUID studio,Boolean active,Pageable page);
}
