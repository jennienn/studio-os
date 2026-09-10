package com.studioos.lesson.classes;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.time.Instant;
import java.util.*;
@RestController @RequestMapping("/api/v1/studios/{studio}/lesson")
public class ClassController {
 private final ClassService service;public ClassController(ClassService service){this.service=service;}
 @GetMapping("/classes") List<ClassService.ClassView> list(@PathVariable UUID studio,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.list(studio,page,size);}
 @PostMapping("/classes") @ResponseStatus(HttpStatus.CREATED) ClassService.ClassView create(@PathVariable UUID studio,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ClassService.Edit body){return service.save(studio,null,key,body);}
 @PutMapping("/classes/{id}") ClassService.ClassView edit(@PathVariable UUID studio,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ClassService.Edit body){return service.save(studio,id,key,body);}
 @GetMapping("/classes/{id}/schedules") List<ClassService.Schedule> schedules(@PathVariable UUID studio,@PathVariable UUID id){return service.schedules(studio,id);}
 @PostMapping("/classes/{id}/schedules") @ResponseStatus(HttpStatus.CREATED) ClassService.Schedule schedule(@PathVariable UUID studio,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ClassService.ScheduleEdit body){return service.schedule(studio,id,null,key,body);}
 @PutMapping("/classes/{id}/schedules/{schedule}") ClassService.Schedule editSchedule(@PathVariable UUID studio,@PathVariable UUID id,@PathVariable UUID schedule,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody ClassService.ScheduleEdit body){return service.schedule(studio,id,schedule,key,body);}
 @GetMapping("/occurrences") List<ClassService.Occurrence> occurrences(@PathVariable UUID studio,@RequestParam Instant from,@RequestParam Instant to,@RequestParam(required=false) UUID classId,@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size){return service.occurrences(studio,from,to,classId,page,size);}
 @PostMapping("/occurrences/{id}/complete") ClassService.Occurrence complete(@PathVariable UUID studio,@PathVariable UUID id,@RequestHeader("Idempotency-Key") String key){return service.complete(studio,id,key);}
}
