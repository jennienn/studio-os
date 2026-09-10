package com.studioos.auth;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import com.studioos.common.ApiException;
@Component @Profile("!local & !test")
public class UnavailableTokenDelivery implements TokenDelivery {
    public boolean available() { return false; }
    public void deliver(String email,TokenStore.Kind kind,String token) {
        throw new ApiException(503,"DELIVERY_UNAVAILABLE","이메일 전송이 설정되지 않았습니다.");
    }
}
