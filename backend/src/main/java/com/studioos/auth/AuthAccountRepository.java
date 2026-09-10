package com.studioos.auth;
import java.util.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuthAccountRepository extends JpaRepository<AuthAccount,UUID> {
    Optional<AuthAccount> findByProviderAndProviderUserId(String provider,String providerUserId);
    Optional<AuthAccount> findByUserIdAndProvider(UUID userId,String provider);
}
