package com.studioos.booking;
import com.studioos.common.PageResult;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;
import static com.studioos.booking.BookingDto.*;
@RestController @RequestMapping("/api/v1/studios/{studioId}")
public class BookingController {
    private final BookingService bookings;public BookingController(BookingService bookings){this.bookings=bookings;}
    @GetMapping("/bookings") PageResult<View> list(@PathVariable UUID studioId,@RequestParam Instant from,@RequestParam Instant to,@RequestParam(defaultValue="ALL") String status,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return bookings.list(studioId,from,to,status,page,size);}
    @GetMapping("/bookings/{id}") View get(@PathVariable UUID studioId,@PathVariable UUID id){return bookings.get(studioId,id);}
    @PostMapping("/bookings") @ResponseStatus(HttpStatus.CREATED) View create(@PathVariable UUID studioId,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody Create body){return bookings.create(studioId,key,body);}
    @PutMapping("/bookings/{id}") View edit(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody Edit body){return bookings.edit(studioId,id,key,body);}
    @PostMapping("/bookings/{id}/cancel") View cancel(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key){return bookings.transition(studioId,id,key,"CANCEL");}
    @PostMapping("/bookings/{id}/complete") View complete(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key){return bookings.transition(studioId,id,key,"COMPLETE");}
    @PostMapping("/bookings/{id}/no-show") View noShow(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key){return bookings.transition(studioId,id,key,"NO_SHOW");}
    @PostMapping("/bookings/{id}/confirm") View confirm(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key){return bookings.transition(studioId,id,key,"CONFIRM");}
    @GetMapping("/booking-staff") List<StaffView> staff(@PathVariable UUID studioId){return bookings.staff(studioId);}
    @GetMapping("/availability") Availability availability(@PathVariable UUID studioId,@RequestParam UUID staffId,@RequestParam Instant startAt,@RequestParam Instant endAt){return bookings.available(studioId,staffId,startAt,endAt);}
    @GetMapping("/booking-blocks") PageResult<BlockView> blocks(@PathVariable UUID studioId,@RequestParam Instant from,@RequestParam Instant to,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return bookings.blocks(studioId,from,to,page,size);}
    @PostMapping("/booking-blocks") @ResponseStatus(HttpStatus.CREATED) BlockView block(@PathVariable UUID studioId,@RequestHeader(value="Idempotency-Key",required=false) String key,@Valid @RequestBody BlockCreate body){return bookings.block(studioId,key,body);}
    @DeleteMapping("/booking-blocks/{id}") Deleted delete(@PathVariable UUID studioId,@PathVariable UUID id,@RequestHeader(value="Idempotency-Key",required=false) String key){return bookings.deleteBlock(studioId,id,key);}
}
