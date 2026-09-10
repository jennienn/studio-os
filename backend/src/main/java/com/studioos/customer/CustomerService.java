package com.studioos.customer;

import com.studioos.common.*;
import com.studioos.security.OperationalAccess;
import com.studioos.studio.StudioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.*;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.*;
import static com.studioos.customer.CustomerDto.*;

@Service
public class CustomerService {
    private final CustomerRepository customers;
    private final OperationalAccess access;
    private final StudioRepository studios;
    private final Validator validator;
    private final List<CustomerArchiveGuard> archiveGuards;
    public CustomerService(CustomerRepository customers,OperationalAccess access,StudioRepository studios,
        Validator validator,List<CustomerArchiveGuard> archiveGuards) {
        this.customers=customers;this.access=access;this.studios=studios;this.validator=validator;this.archiveGuards=archiveGuards;
    }
    @Transactional(readOnly=true)
    public PageResult<View> list(UUID studioId,String search,String status,int page,int size) {
        var context=access.manager(studioId);PageResult.validate(page,size);
        if(!Set.of("ACTIVE","ARCHIVED","ALL").contains(status) || search.length()>100)
            throw new ApiException(400,"INVALID_FILTER","검색 조건을 확인해 주세요.");
        String query=search.trim();
        String phone=query.replaceAll("[\\s-]","");
        String phonePattern=phone.matches("[0-9]+") ? "%"+phone+"%" : "!%";
        var result=customers.search(context.authorizedStudioId(),status,query,
            "%"+query.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%",phonePattern,
            PageRequest.of(page,size,Sort.by("name").ascending().and(Sort.by("id"))));
        return PageResult.from(result.map(CustomerService::view));
    }
    @Transactional(readOnly=true)
    public View get(UUID studioId,UUID id) {return view(find(access.manager(studioId).authorizedStudioId(),id));}
    @Transactional
    public View create(UUID studioId,Edit edit) {
        var context=access.manager(studioId);validate(edit);
        studios.lockById(context.authorizedStudioId()).orElseThrow();
        var customer=customers.save(new Customer(context.authorizedStudioId(),edit.name().trim(),edit.phone().trim(),
            KoreanPhone.normalize(edit.phone()),edit.memo()==null?"":edit.memo()));
        studios.flush();return view(customer);
    }
    @Transactional
    public View edit(UUID studioId,UUID id,Edit edit) {
        var context=access.manager(studioId);validate(edit);
        studios.lockById(context.authorizedStudioId()).orElseThrow();
        var customer=find(context.authorizedStudioId(),id);
        customer.name=edit.name().trim();customer.phone=edit.phone().trim();
        customer.normalizedPhone=KoreanPhone.normalize(edit.phone());customer.memo=edit.memo()==null?"":edit.memo();
        customer.updatedAt=Instant.now();studios.flush();return view(customer);
    }
    @Transactional
    public View archive(UUID studioId,UUID id) {
        var context=access.manager(studioId);studios.lockById(context.authorizedStudioId()).orElseThrow();
        var customer=find(context.authorizedStudioId(),id);
        if(customer.status.equals("ACTIVE")) {
            archiveGuards.forEach(guard->guard.validateArchive(context.authorizedStudioId(),id));
            customer.status="ARCHIVED";customer.archivedAt=customer.updatedAt=Instant.now();
        }
        return view(customer);
    }
    private void validate(Edit edit) {
        if(edit==null || !validator.validate(edit).isEmpty()) throw new ApiException(400,"INVALID_CUSTOMER","이름·전화번호·메모 길이를 확인해 주세요.");
    }
    private Customer find(UUID studioId,UUID id) {return customers.findByStudioIdAndId(studioId,id)
        .orElseThrow(()->new ApiException(404,"CUSTOMER_NOT_FOUND","고객 정보를 찾을 수 없습니다."));}
    static View view(Customer c) {return new View(c.id,c.studioId,c.name,c.phone,c.normalizedPhone,c.memo,c.status,c.createdAt,c.updatedAt,c.archivedAt);}
}
