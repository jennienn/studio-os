package com.studioos;

import com.studioos.common.ApiException;
import com.studioos.studio.configuration.ConfigurationDto.*;
import com.studioos.studio.configuration.ConfigurationValidation;
import com.studioos.studio.configuration.StudioTaxonomy;
import jakarta.validation.Validation;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.IntStream;
import static com.studioos.studio.configuration.StudioTaxonomy.*;
import static org.assertj.core.api.Assertions.*;

class ConfigurationValidationTest {
    private void validate(Command command) {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            new ConfigurationValidation(factory.getValidator()).validate(command);
        }
    }
    private Command valid(Category category, String type) {
        var capabilities = new EnumMap<Capability, Boolean>(Capability.class);
        branch(category).capabilities().forEach(c -> capabilities.put(c, true));
        return new Command(0L, category, type, capabilities,
            IntStream.rangeClosed(1, 7).mapToObj(day -> new Hours(day, null, null, true)).toList(),
            new Booking(30, 30, 12), category == Category.LESSON ? new Lesson(2, 7, true) : null,
            category == Category.BEAUTY ? new Beauty(true, false) : null);
    }
    @Test void everyDocumentedSubtypeIsValidOnlyInItsOwnCategory() {
        for (var category : Category.values()) {
            for (var type : branch(category).businessTypes()) {
                assertThatCode(() -> validate(valid(category, type))).doesNotThrowAnyException();
                var other = category == Category.LESSON ? Category.BEAUTY : Category.LESSON;
                assertThatThrownBy(() -> StudioTaxonomy.validateType(other, type)).isInstanceOf(ApiException.class);
            }
        }
    }
    @Test void evenDisabledForeignCapabilitiesAreRejected() {
        var command = valid(Category.BEAUTY, "NAIL");
        command.capabilities().put(Capability.ATTENDANCE, false);
        assertThatThrownBy(() -> validate(command)).isInstanceOf(ApiException.class);
    }
    @Test void depositRepresentationsMustAgree() {
        var command = valid(Category.BEAUTY, "NAIL");
        command.capabilities().put(Capability.DEPOSIT, false);
        assertThatThrownBy(() -> validate(command)).isInstanceOf(ApiException.class);
    }
    @Test void rejectsOppositePolicyAndMissingCategoryPolicy() {
        var c = valid(Category.LESSON, "DANCE");
        assertThatThrownBy(() -> validate(new Command(c.version(), c.businessCategory(), c.businessType(),
            c.capabilities(), c.businessHours(), c.bookingPolicy(), c.lessonPolicy(), new Beauty(false, false))))
            .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new Command(c.version(), c.businessCategory(), c.businessType(),
            c.capabilities(), c.businessHours(), c.bookingPolicy(), null, null))).isInstanceOf(ApiException.class);
    }
    @Test void rejectsDuplicateDaysAndInvalidOpenInterval() {
        var c = valid(Category.LESSON, "DANCE");
        var hours = new ArrayList<>(c.businessHours());
        hours.set(1, hours.getFirst());
        assertThatThrownBy(() -> validate(new Command(c.version(), c.businessCategory(), c.businessType(),
            c.capabilities(), hours, c.bookingPolicy(), c.lessonPolicy(), null))).isInstanceOf(ApiException.class);
        hours.set(1, new Hours(2, LocalTime.NOON, LocalTime.NOON, false));
        assertThatThrownBy(() -> validate(new Command(c.version(), c.businessCategory(), c.businessType(),
            c.capabilities(), hours, c.bookingPolicy(), c.lessonPolicy(), null))).isInstanceOf(ApiException.class);
    }
    @Test void rejectsInvalidPolicyNumbersAndIncompleteConfiguration() {
        var c = valid(Category.LESSON, "DANCE");
        assertThatThrownBy(() -> validate(new Command(c.version(), c.businessCategory(), c.businessType(),
            c.capabilities(), c.businessHours(), new Booking(0, 0, -1), c.lessonPolicy(), null)))
            .isInstanceOf(ApiException.class);
        assertThatThrownBy(() -> validate(new Command(0L, null, "", null, null, null, null, null)))
            .isInstanceOf(ApiException.class);
    }
}
