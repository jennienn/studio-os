package com.studioos.auth;
import com.studioos.common.ApiException;
import java.util.*;

public final class OAuthIdentityMapper {
    private OAuthIdentityMapper() {}
    public record Identity(String provider,String providerUserId,String email,String name) {}
    public static Identity map(String registration,Map<String,Object> attributes) {
        String id,name,email;
        if (registration.equals("google")) {
            id=text(attributes.get("sub")); name=text(attributes.get("name"));
            email=Boolean.TRUE.equals(attributes.get("email_verified")) ? text(attributes.get("email")) : null;
        } else if (registration.equals("kakao")) {
            Object raw=attributes.get("id"); id=raw instanceof Number n ? Long.toString(n.longValue()) : text(raw);
            Map<?,?> account=attributes.get("kakao_account") instanceof Map<?,?> m ? m : Map.of();
            Map<?,?> profile=account.get("profile") instanceof Map<?,?> m ? m : Map.of();
            name=text(profile.get("nickname"));
            email=Boolean.TRUE.equals(account.get("is_email_valid")) && Boolean.TRUE.equals(account.get("is_email_verified"))
                ? text(account.get("email")) : null;
        } else throw invalid();
        if (id==null || id.length()>254) throw invalid();
        if (email!=null && (email.length()>254 || !email.contains("@"))) email=null;
        return new Identity(registration.toUpperCase(Locale.ROOT),id,email==null ? null : AuthService.normalizeEmail(email),name);
    }
    public static void requireName(Identity identity) {
        if (identity.name()==null || identity.name().isBlank() || identity.name().length()>100)
            throw new ApiException(400,"OAUTH_PROFILE_REQUIRED","로그인 제공자의 이름 정보 제공에 동의한 후 다시 시도해 주세요.");
    }
    private static String text(Object value) { return value instanceof String s && !s.isBlank() ? s.trim() : null; }
    private static ApiException invalid() { return new ApiException(400,"OAUTH_IDENTITY_INVALID","로그인 제공자의 계정 정보를 확인할 수 없습니다."); }
}
