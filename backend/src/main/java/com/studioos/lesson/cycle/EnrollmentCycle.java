package com.studioos.lesson.cycle;
import jakarta.persistence.*;
import java.time.*;
import java.util.UUID;
@Entity @Table(name="enrollment_cycles")
public class EnrollmentCycle {
 @Id public UUID id;
 public UUID studioId;
 public UUID enrollmentId;
 public UUID passProductId;
 @Column(length=100) public String productNameSnapshot;
 @Column(length=16) public String productTypeSnapshot;
 public Integer purchasedCount;
 public Integer validityDaysSnapshot;
 @Column(length=16) public String billingPeriodSnapshot;
 public long purchasePriceSnapshot;
 @Column(length=32) public String deductionTriggerSnapshot;
 @Column(length=16) public String validityStartRuleSnapshot;
 public LocalDate startDate;
 public LocalDate validEndDate;
 public LocalDate paymentDate;
 public LocalDate nextDueDate;
 @Column(length=16) public String status;
 public Instant createdAt;
 public EnrollmentCycle(){}
}
