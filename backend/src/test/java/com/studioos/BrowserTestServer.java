package com.studioos;
import org.springframework.boot.SpringApplication;
import org.testcontainers.containers.*;
import org.testcontainers.utility.DockerImageName;

/** Test-only entry point. Always uses disposable infrastructure, never the local database. */
public final class BrowserTestServer {
    public static void main(String[] args) {
        var postgres=new PostgreSQLContainer<>("postgres:17-alpine");
        var redis=new GenericContainer<>(DockerImageName.parse("redis:7.4-alpine")).withExposedPorts(6379);
        postgres.start(); redis.start();
        System.setProperty("spring.datasource.url",postgres.getJdbcUrl());
        System.setProperty("spring.datasource.username",postgres.getUsername());
        System.setProperty("spring.datasource.password",postgres.getPassword());
        System.setProperty("spring.data.redis.host",redis.getHost());
        System.setProperty("spring.data.redis.port",redis.getMappedPort(6379).toString());
        System.setProperty("spring.data.redis.password","");
        System.setProperty("spring.profiles.active","local");
        System.setProperty("server.port","8080");
        System.setProperty("server.address","127.0.0.1");
        System.setProperty("server.servlet.session.cookie.secure","false");
        System.setProperty("app.auth.base-url","http://127.0.0.1:31741");
        System.setProperty("app.auth.local-mail-directory",".local-mail/e2e");
        System.setProperty("app.auth.rate-limit-per-minute","10000");
        System.setProperty("GOOGLE_CLIENT_ID","");
        System.setProperty("KAKAO_CLIENT_ID","");
        var context=SpringApplication.run(StudioApplication.class,args);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            context.close(); redis.stop(); postgres.stop();
        }));
    }
}
