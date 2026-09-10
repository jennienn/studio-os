package com.studioos.lesson.booking;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.UUID;
@RestController @RequestMapping("/api/v1/studios/{studio}/lesson/private-bookings")
public class LessonBookingController {
 private final LessonBookingService service;public LessonBookingController(LessonBookingService service){this.service=service;}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) LessonBookingService.Created create(@PathVariable UUID studio,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody LessonBookingService.PrivateCreate body){return service.create(studio,key,body);}
}
