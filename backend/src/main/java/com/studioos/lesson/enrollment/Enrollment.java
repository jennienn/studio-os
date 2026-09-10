package com.studioos.lesson.enrollment;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="enrollments")
public class Enrollment {
 @Id public UUID id;
 public UUID studioId;
 public UUID customerId;
 @Column(length=16) public String kind;
 public UUID classId;
 @Column(length=16) public String status;
 public Instant createdAt;
 public Instant endedAt;
 public Enrollment(){}
}
