package com.studioos;

import com.studioos.customer.KoreanPhone;
import com.studioos.common.ApiException;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class KoreanPhoneTest {
    @Test void formattingVariantsShareOneMatchingValue() {
        for(String phone:new String[]{"010-1234-5678","010 1234 5678","01012345678"})
            assertThat(KoreanPhone.normalize(phone)).isEqualTo("01012345678");
        assertThat(KoreanPhone.normalize("02-123-4567")).isEqualTo("021234567");
        assertThat(KoreanPhone.normalize("031-123-4567")).isEqualTo("0311234567");
    }
    @Test void malformedOrInternationalInputsAreRejectedWithoutEchoingPhone() {
        for(String phone:new String[]{"","123","010-ABCD-1234","+82 10 1234 5678","00012345678","010123456789"})
            assertThatThrownBy(()->KoreanPhone.normalize(phone)).isInstanceOf(ApiException.class);
        assertThatThrownBy(()->KoreanPhone.normalize(null)).isInstanceOf(ApiException.class);
    }
}
