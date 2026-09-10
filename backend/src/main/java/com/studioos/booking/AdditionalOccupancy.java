package com.studioos.booking;
import java.time.Instant;
import java.util.UUID;
/** Additional scheduled work that occupies staff time, called under the booking transaction. */
public interface AdditionalOccupancy {void check(UUID studio,UUID staff,Instant start,Instant end);}
