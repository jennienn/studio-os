package com.studioos.booking;
import com.studioos.security.AuthorizedStudioContext;
public interface BookingSpecialization {
 boolean supports(Booking booking);
 void authorize(Booking booking,AuthorizedStudioContext actor,String command);
 void validateTransition(Booking booking,AuthorizedStudioContext actor,String command);
 void afterTransition(Booking booking,AuthorizedStudioContext actor,String command);
 void validateEdit(Booking booking,BookingDto.Edit body);
}
