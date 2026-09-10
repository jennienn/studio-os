package com.studioos.auth;
import org.springframework.context.annotation.*;
import org.springframework.core.env.Environment;
import org.springframework.security.config.oauth2.client.CommonOAuth2Provider;
import org.springframework.security.oauth2.client.registration.*;
import org.springframework.security.oauth2.core.*;
import java.util.*;

@Configuration
public class OAuthConfiguration {
    @Bean
    ClientRegistrationRepository clientRegistrations(Environment env) {
        Map<String,ClientRegistration> clients=new HashMap<>();
        String base=env.getRequiredProperty("app.auth.base-url");
        String googleId=env.getProperty("GOOGLE_CLIENT_ID","");
        String googleSecret=env.getProperty("GOOGLE_CLIENT_SECRET","");
        if (!googleId.isBlank() && !googleSecret.isBlank()) {
            clients.put("google",CommonOAuth2Provider.GOOGLE.getBuilder("google")
                .clientId(googleId).clientSecret(googleSecret)
                .redirectUri(base+"/login/oauth2/code/google").build());
        }
        String kakaoId=env.getProperty("KAKAO_CLIENT_ID","");
        String kakaoSecret=env.getProperty("KAKAO_CLIENT_SECRET","");
        if (!kakaoId.isBlank() && !kakaoSecret.isBlank()) {
            clients.put("kakao",ClientRegistration.withRegistrationId("kakao")
                .clientName("Kakao").clientId(kakaoId).clientSecret(kakaoSecret)
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_POST)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(base+"/login/oauth2/code/kakao").scope("profile_nickname")
                .authorizationUri("https://kauth.kakao.com/oauth/authorize")
                .tokenUri("https://kauth.kakao.com/oauth/token")
                .userInfoUri("https://kapi.kakao.com/v2/user/me")
                .userNameAttributeName("id").build());
        }
        return clients::get;
    }
}
