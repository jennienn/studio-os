package com.studioos.lesson.pass;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;
@Entity @Table(name="pass_products")
public class PassProduct {
 @Id public UUID id;
 public UUID studioId;
 @Column(length=100) public String name;
 @Column(length=16) public String productType;
 public Integer totalCount;
 public Integer validityDays;
 @Column(length=16) public String validityStartRule;
 public long price;
 @Column(length=32) public String deductionTrigger;
 @Column(length=16) public String billingPeriod;
 public boolean active;
 public Instant createdAt;
 public Instant updatedAt;
 public PassProduct(){}
}
