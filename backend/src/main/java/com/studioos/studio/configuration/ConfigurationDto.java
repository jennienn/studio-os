package com.studioos.studio.configuration;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalTime;
import java.util.*;
import com.studioos.studio.configuration.StudioTaxonomy.*;

public final class ConfigurationDto {
    private ConfigurationDto() {}
    public record Hours(@NotNull @Min(1) @Max(7) Integer weekday,
                        LocalTime openTime, LocalTime closeTime, @NotNull Boolean closed) {}
    public record Booking(@NotNull @Min(1) @Max(1440) Integer slotIntervalMinutes,
                          @NotNull @Min(1) Integer bookingWindowDays,
                          @NotNull @Min(0) Integer cancellationCutoffHours) {}
    public record Lesson(@NotNull @Min(0) Integer lowBalanceThreshold,
                         @NotNull @Min(0) Integer expiryAlertDays,
                         @NotNull Boolean restoreOnTimelyCancellation) {}
    public record Beauty(@NotNull Boolean depositEnabled,@NotNull Boolean noShowEnabled) {}
    public record Command(
        @NotNull @Min(0) Long version,
        @NotNull Category businessCategory,
        @NotBlank @Size(max=32) String businessType,
        @NotNull Map<@NotNull Capability,@NotNull Boolean> capabilities,
        @NotNull @Size(min=7,max=7) List<@NotNull @Valid Hours> businessHours,
        @NotNull @Valid Booking bookingPolicy,
        @Valid Lesson lessonPolicy,
        @Valid Beauty beautyPolicy
    ) {}
    public record Settings(Map<Capability,Boolean> capabilities,List<Hours> businessHours,
                           Booking bookingPolicy,Lesson lessonPolicy,Beauty beautyPolicy) {}
    public record Permissions(boolean completeOnboarding,boolean editIdentity,boolean editCapabilities,
                              boolean editPolicies,boolean editDeposit) {}
    public record View(UUID studioId,String status,long version,Category businessCategory,String businessType,
                       Map<Category,Branch> catalog,Booking bookingDefaults,Settings configuration,
                       Permissions permissions) {}
    public static Booking bookingDefaults() { return new Booking(30,30,12); }
}
