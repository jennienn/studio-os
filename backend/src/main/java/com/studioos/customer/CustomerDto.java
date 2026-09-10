package com.studioos.customer;

import jakarta.validation.constraints.*;
import java.time.Instant;
import java.util.UUID;

public final class CustomerDto {
    private CustomerDto() {}
    public record Edit(@NotBlank @Size(max=100) String name,
                       @NotBlank @Size(max=32) String phone,
                       @Size(max=2000) String memo) {}
    public record View(UUID id,UUID studioId,String name,String phone,String normalizedPhone,
                       String memo,String status,Instant createdAt,Instant updatedAt,Instant archivedAt) {}
}
