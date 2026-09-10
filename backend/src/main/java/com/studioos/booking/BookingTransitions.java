package com.studioos.booking;
import com.studioos.common.ApiException;
import java.time.Instant;
import java.util.Set;
public final class BookingTransitions {
 private BookingTransitions(){}
 public static void apply(Booking b,String command){
  String next=switch(command){case "CANCEL"->"CANCELLED";case "COMPLETE"->"COMPLETED";case "NO_SHOW"->"NO_SHOW";case "CONFIRM"->"CONFIRMED";default->throw new ApiException(400,"INVALID_COMMAND","지원하지 않는 예약 명령입니다.");};
  boolean allowed=command.equals("CANCEL")?Set.of("PENDING","CONFIRMED").contains(b.status):command.equals("CONFIRM")?b.status.equals("PENDING"):b.status.equals("CONFIRMED");
  if(!allowed)throw new ApiException(409,"INVALID_BOOKING_TRANSITION","현재 예약 상태에서 처리할 수 없습니다.");
  b.status=next;b.updatedAt=Instant.now();
 }
}
