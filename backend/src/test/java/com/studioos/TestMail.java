package com.studioos;
import com.studioos.auth.*;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import java.util.concurrent.ConcurrentHashMap;

@TestConfiguration
public class TestMail {
    public static class Mailbox implements TokenDelivery {
        private final ConcurrentHashMap<String,String> messages=new ConcurrentHashMap<>();
        public void deliver(String email,TokenStore.Kind kind,String token) { messages.put(email+":"+kind,token); }
        public String token(String email,TokenStore.Kind kind) { return messages.get(email+":"+kind); }
    }
    @Bean Mailbox mailbox() { return new Mailbox(); }
}
