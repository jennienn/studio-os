package com.studioos.auth;

import com.studioos.common.ApiException;
import com.studioos.security.OperatorPrincipal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;

@Service
public class AuthService {
    private final UserRepository users;
    private final AuthAccountRepository accounts;
    private final PasswordEncoder passwords;
    private final TokenStore tokens;
    private final TokenDelivery delivery;
    private final Duration verificationTtl,resetTtl;
    private final String dummyHash;
    public AuthService(UserRepository users,AuthAccountRepository accounts,PasswordEncoder passwords,
        TokenStore tokens,TokenDelivery delivery,
        @Value("${app.auth.verification-ttl:24h}") Duration verificationTtl,
        @Value("${app.auth.reset-ttl:30m}") Duration resetTtl) {
        this.users=users; this.accounts=accounts; this.passwords=passwords; this.tokens=tokens;
        this.delivery=delivery; this.verificationTtl=verificationTtl; this.resetTtl=resetTtl;
        dummyHash=passwords.encode(UUID.randomUUID().toString());
    }
    public static String normalizeEmail(String email) { return email.trim().toLowerCase(Locale.ROOT); }
    public static void validatePassword(String password) {
        if (password==null || password.codePointCount(0,password.length())<12 ||
            password.getBytes(StandardCharsets.UTF_8).length>72)
            throw new ApiException(400,"INVALID_PASSWORD","비밀번호는 12자 이상, UTF-8 72바이트 이하로 입력해 주세요.");
    }
    @Transactional
    public void signup(String email,String password,String name) {
        validatePassword(password);
        if (!delivery.available()) throw new ApiException(503,"DELIVERY_UNAVAILABLE","이메일 전송이 설정되지 않았습니다.");
        String normalized=normalizeEmail(email);
        // Same public response for an existing email account. Never overwrite it.
        String hash=passwords.encode(password);
        if (accounts.findByProviderAndProviderUserId("EMAIL",normalized).isPresent()) return;
        var user=users.saveAndFlush(new OperatorUser(normalized,name.trim(),"PENDING_VERIFICATION"));
        accounts.saveAndFlush(new AuthAccount(user.id,"EMAIL",normalized,hash));
        delivery.deliver(normalized,TokenStore.Kind.VERIFY,tokens.issue(TokenStore.Kind.VERIFY,user.id,verificationTtl));
    }
    @Transactional(readOnly=true)
    public OperatorPrincipal login(String email,String password) {
        var account=accounts.findByProviderAndProviderUserId("EMAIL",normalizeEmail(email));
        boolean supported=password.getBytes(StandardCharsets.UTF_8).length<=72;
        boolean matches=passwords.matches(supported ? password : "",account.map(a -> a.passwordHash).orElse(dummyHash)) && supported;
        if (!matches || account.isEmpty()) throw new ApiException(401,"INVALID_CREDENTIALS","이메일 또는 비밀번호를 확인해 주세요.");
        var user=users.findById(account.get().userId).orElseThrow();
        if (user.status.equals("PENDING_VERIFICATION"))
            throw new ApiException(403,"EMAIL_VERIFICATION_REQUIRED","이메일 인증을 완료해 주세요.");
        if (!user.status.equals("ACTIVE")) throw new ApiException(401,"INVALID_CREDENTIALS","로그인할 수 없습니다.");
        return new OperatorPrincipal(user.id,user.securityVersion);
    }
    @Transactional
    public void requestToken(String email,TokenStore.Kind kind) {
        var account=accounts.findByProviderAndProviderUserId("EMAIL",normalizeEmail(email));
        if (account.isEmpty()) return;
        var user=users.lockById(account.get().userId).orElseThrow();
        if (user.status.equals("DISABLED")) return;
        if (kind==TokenStore.Kind.VERIFY && !user.status.equals("PENDING_VERIFICATION")) return;
        if (kind==TokenStore.Kind.RESET && !user.status.equals("ACTIVE")) return;
        delivery.deliver(account.get().providerUserId,kind,
            tokens.issue(kind,user.id,kind==TokenStore.Kind.VERIFY ? verificationTtl : resetTtl));
    }
    @Transactional
    public void verify(String token) {
        var user=lockTokenUser(TokenStore.Kind.VERIFY,token);
        if (!user.status.equals("PENDING_VERIFICATION") || !tokens.consume(TokenStore.Kind.VERIFY,token)) throw invalidToken();
        var account=accounts.findByUserIdAndProvider(user.id,"EMAIL").orElseThrow(AuthService::invalidToken);
        account.verifiedAt=Instant.now(); user.status="ACTIVE"; user.updatedAt=Instant.now();
        tokens.invalidateAll(TokenStore.Kind.VERIFY,user.id);
    }
    @Transactional
    public UUID reset(String token,String password) {
        validatePassword(password);
        var user=lockTokenUser(TokenStore.Kind.RESET,token);
        if (!user.status.equals("ACTIVE") || !tokens.consume(TokenStore.Kind.RESET,token)) throw invalidToken();
        var account=accounts.findByUserIdAndProvider(user.id,"EMAIL").orElseThrow(AuthService::invalidToken);
        account.passwordHash=passwords.encode(password);
        user.securityVersion++; user.updatedAt=Instant.now();
        tokens.invalidateAll(TokenStore.Kind.RESET,user.id);
        return user.id;
    }
    private OperatorUser lockTokenUser(TokenStore.Kind kind,String token) {
        UUID id=tokens.owner(kind,token).orElseThrow(AuthService::invalidToken);
        return users.lockById(id).orElseThrow(AuthService::invalidToken);
    }
    private static ApiException invalidToken() { return new ApiException(400,"INVALID_TOKEN","링크가 만료되었거나 이미 사용되었습니다."); }
}
