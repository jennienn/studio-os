package com.studioos.booking;
import com.studioos.customer.CustomerArchiveGuard;
import com.studioos.common.ApiException;
import org.springframework.stereotype.Component;
import java.util.*;
@Component
public class BookingCustomerArchiveGuard implements CustomerArchiveGuard {
    private final BookingRepository bookings;
    public BookingCustomerArchiveGuard(BookingRepository bookings){this.bookings=bookings;}
    public void validateArchive(UUID studio,UUID customer){if(bookings.existsByStudioIdAndCustomerIdAndStatusIn(studio,customer,List.of("PENDING","CONFIRMED")))throw new ApiException(409,"CUSTOMER_HAS_BOOKINGS","미해결 예약을 먼저 처리해 주세요.");}
}
