package com.studioos;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class LessonFoundationIntegrationTest extends OperationalTestBase {
 String product(){return "{\"name\":\"7-session\",\"productType\":\"COUNT_BASED\",\"totalCount\":7,\"validityDays\":30,\"validityStartRule\":\"FIRST_USE\",\"price\":\"10000\",\"deductionTrigger\":\"LESSON_COMPLETED\",\"active\":true}";}
 @Test void arbitraryProductsValidationEditAndDeactivate() throws Exception{
  var f=fixture();String path=f.base()+"/lesson/pass-products";
  var id=json(call(f.user(),post(path).content(product())).andExpect(status().isCreated())).get("id").asText();
  call(f.user(),put(path+"/"+id).content(product().replace(":7,",":15,").replace("true","false"))).andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").value(15));
  for(String b:List.of(product().replace(":7,",":0,"),product().replace(":7,",":-1,"),product().replace("10000","0"),product().replace("COUNT_BASED","TIME_BASED")))call(f.user(),post(path).content(b)).andExpect(status().isBadRequest());
  call(f.user(),get(path+"?active=false")).andExpect(jsonPath("$.totalElements").value(1));
 }
 @Test void timeProductRequiresExactlyOnePeriodAndPurchaseStart() throws Exception{
  var f=fixture();String path=f.base()+"/lesson/pass-products";String b="{\"name\":\"Monthly\",\"productType\":\"TIME_BASED\",\"billingPeriod\":\"MONTH\",\"validityStartRule\":\"PURCHASE_DATE\",\"price\":\"10000\",\"active\":true}";
  call(f.user(),post(path).content(b)).andExpect(status().isCreated());
  call(f.user(),post(path).content(b.replace("\"active\"","\"validityDays\":30,\"active\""))).andExpect(status().isBadRequest());
  call(f.user(),post(path).content(b.replace("PURCHASE_DATE","FIRST_USE"))).andExpect(status().isBadRequest());
 }
 @Test void multipleEnrollmentsAndTenantRoleCategoryGuards() throws Exception{
  var a=fixture();var b=fixture();var c=customer(a);String path=a.base()+"/lesson/enrollments";
  String body="{\"customerId\":\""+c+"\",\"kind\":\"PRIVATE\"}";
  for(int i=0;i<2;i++)call(a.user(),post(path).header("Idempotency-Key","enroll"+i).content(body)).andExpect(status().isCreated());
  call(a.user(),get(path)).andExpect(jsonPath("$.totalElements").value(2));
  call(b.user(),get(path)).andExpect(status().isForbidden());
  call(member(a,"STAFF"),post(a.base()+"/lesson/pass-products").content(product())).andExpect(status().isForbidden());
  call(a.user(),post(path).header("Idempotency-Key","foreign").content(body.replace(c.toString(),customer(b).toString()))).andExpect(status().isNotFound());
  jdbc.update("update studios set business_category='BEAUTY',business_type='NAIL' where id=?",b.studio());
  call(b.user(),get(b.base()+"/lesson/pass-products")).andExpect(status().isForbidden());
  call(b.user(),post(b.base()+"/lesson/pass-products").content(product())).andExpect(status().isForbidden());
 }
}
