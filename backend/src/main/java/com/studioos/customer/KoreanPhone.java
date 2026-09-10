package com.studioos.customer;

import com.studioos.common.ApiException;

/** Domestic matching format; no international conversion or ownership verification. */
public final class KoreanPhone {
    private KoreanPhone() {}
    public static String normalize(String input) {
        if (input==null || input.length()>32 || !input.matches("[0-9\\s-]+")) throw invalid();
        String digits=input.replaceAll("[\\s-]","");
        if (!digits.matches("0[1-9][0-9]{7,9}")) throw invalid();
        return digits;
    }
    private static ApiException invalid() {
        return new ApiException(400,"INVALID_PHONE","국내 전화번호를 숫자, 공백, 하이픈으로 입력해 주세요.");
    }
}
