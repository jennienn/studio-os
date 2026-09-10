package com.studioos.studio.configuration;

import com.studioos.common.ApiException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import java.util.HashSet;
import static com.studioos.studio.configuration.StudioTaxonomy.*;

/** Validation shared by atomic onboarding and configuration updates. */
@Component
public class ConfigurationValidation {
    private final Validator validator;
    public ConfigurationValidation(Validator validator) { this.validator = validator; }

    public void validate(ConfigurationDto.Command command) {
        if (command == null || !validator.validate(command).isEmpty())
            invalid("필수 설정과 숫자 범위를 확인해 주세요.");
        StudioTaxonomy.validateType(command.businessCategory(), command.businessType());
        var allowed = new HashSet<>(branch(command.businessCategory()).capabilities());
        if (!command.capabilities().keySet().equals(allowed))
            invalid("사업장 유형에 맞는 기능 설정을 모두 보내 주세요.");
        var days = new HashSet<Integer>();
        for (var hours : command.businessHours()) {
            if (!days.add(hours.weekday())) invalid("요일별 운영 시간은 한 번만 설정할 수 있습니다.");
            if (!hours.closed() && (hours.openTime() == null || hours.closeTime() == null
                    || !hours.openTime().isBefore(hours.closeTime())))
                invalid("영업일의 시작 시간은 종료 시간보다 빨라야 합니다.");
        }
        if (command.businessCategory() == Category.LESSON) {
            if (command.lessonPolicy() == null || command.beautyPolicy() != null)
                invalid("레슨 사업장에는 레슨 정책만 설정할 수 있습니다.");
            var flags = command.capabilities();
            if (!flags.get(Capability.PASS_MANAGEMENT)) invalid("레슨 사업장은 이용권 관리가 필수입니다.");
            if (!flags.get(Capability.PRIVATE_LESSON) && !flags.get(Capability.GROUP_CLASS))
                invalid("개인 레슨 또는 그룹 수업을 선택해 주세요.");
            if (flags.get(Capability.ATTENDANCE) && !flags.get(Capability.GROUP_CLASS))
                invalid("출석 관리는 그룹 수업을 사용하는 경우에만 가능합니다.");
        } else {
            if (command.beautyPolicy() == null || command.lessonPolicy() != null)
                invalid("뷰티 사업장에는 뷰티 정책만 설정할 수 있습니다.");
            if (!command.capabilities().get(Capability.DEPOSIT).equals(command.beautyPolicy().depositEnabled()))
                invalid("예약금 기능과 예약금 정책은 같은 값이어야 합니다.");
        }
    }

    private static void invalid(String message) {
        throw new ApiException(400, "INVALID_CONFIGURATION", message);
    }
}
