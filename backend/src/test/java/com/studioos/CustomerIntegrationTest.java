package com.studioos;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CustomerIntegrationTest extends OperationalTestBase {
    @Test void createEditDetailArchiveAndPreserveDuplicateBoundary() throws Exception {
        var f=fixture();var id=customer(f);String path=f.base()+"/customers/"+id;
        call(f.user(),get(path)).andExpect(status().isOk()).andExpect(jsonPath("$.normalizedPhone").value("01012345678"));
        call(f.user(),put(path).content(customerBody("Edited","010-1234-5678"))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Edited"));
        call(f.user(),post(path+"/archive")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ARCHIVED")).andExpect(jsonPath("$.archivedAt").isNotEmpty());
        call(f.user(),post(path+"/archive")).andExpect(status().isOk());
        call(f.user(),get(f.base()+"/customers")).andExpect(jsonPath("$.totalElements").value(0));
        call(f.user(),get(f.base()+"/customers?status=ARCHIVED")).andExpect(jsonPath("$.items[0].memo").value("Internal only"));
        call(f.user(),post(f.base()+"/customers").content(customerBody("Edited","010 1234 5678"))).andExpect(status().isConflict());
    }
    @Test void searchesNamesAndFormattedPhoneWithPaginationAndLiteralWildcards() throws Exception {
        var f=fixture();customer(f);
        call(f.user(),post(f.base()+"/customers").content(customerBody("Second","01099998888"))).andExpect(status().isCreated());
        call(f.user(),get(f.base()+"/customers").param("search","Customer")).andExpect(jsonPath("$.totalElements").value(1));
        call(f.user(),get(f.base()+"/customers").param("search","010-1234-5678")).andExpect(jsonPath("$.totalElements").value(1));
        call(f.user(),get(f.base()+"/customers").param("search","%_")).andExpect(jsonPath("$.totalElements").value(0));
        call(f.user(),get(f.base()+"/customers?size=1&page=1")).andExpect(jsonPath("$.items.length()").value(1)).andExpect(jsonPath("$.totalPages").value(2));
        call(f.user(),get(f.base()+"/customers?size=0")).andExpect(status().isBadRequest());
    }
    @Test void malformedInputsAndForeignResourcesAreRejected() throws Exception {
        var a=fixture();var b=fixture();var id=customer(b);String path=b.base()+"/customers/"+id;
        call(a.user(),get(b.base()+"/customers")).andExpect(status().isForbidden());
        call(a.user(),get(path)).andExpect(status().isForbidden());
        call(a.user(),put(path).content(customerBody("Wrong","01011112222"))).andExpect(status().isForbidden());
        call(a.user(),post(path+"/archive")).andExpect(status().isForbidden());
        call(a.user(),get(a.base()+"/customers/"+id)).andExpect(status().isNotFound());
        call(a.user(),post(a.base()+"/customers").content(customerBody("Customer","bad"))).andExpect(status().isBadRequest());
        call(a.user(),post(a.base()+"/customers").content(customerBody(" ","01012345678"))).andExpect(status().isBadRequest());
        call(a.user(),post(a.base()+"/customers").content("{\"name\":\"X\",\"phone\":\"01012345678\",\"memo\":\""+"x".repeat(2001)+"\"}")).andExpect(status().isBadRequest());
        customer(a); // Same name/phone allowed in another tenant.
    }
    @Test void managerCanManageButStaffCannotReadMemoOrMutate() throws Exception {
        var f=fixture();UUID manager=member(f,"MANAGER"),staff=member(f,"STAFF");var id=customer(f);
        call(manager,get(f.base()+"/customers")).andExpect(status().isOk());
        call(manager,put(f.base()+"/customers/"+id).content(customerBody("Manager edit","01012345678"))).andExpect(status().isOk());
        call(staff,get(f.base()+"/customers")).andExpect(status().isForbidden());
        call(staff,get(f.base()+"/customers/"+id)).andExpect(status().isForbidden());
        call(staff,post(f.base()+"/customers").content(customerBody("Denied","01012345678"))).andExpect(status().isForbidden());
        call(staff,post(f.base()+"/customers/"+id+"/archive")).andExpect(status().isForbidden());
    }
}
