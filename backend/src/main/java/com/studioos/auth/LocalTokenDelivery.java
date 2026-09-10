package com.studioos.auth;
import org.springframework.context.annotation.Profile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import com.studioos.common.ApiException;

@Component @Profile("local")
public class LocalTokenDelivery implements TokenDelivery {
    private final Path directory;
    private final String baseUrl;
    private final ObjectMapper mapper;
    public LocalTokenDelivery(@Value("${app.auth.local-mail-directory:.local-mail}") String directory,
        @Value("${app.auth.base-url}") String baseUrl,ObjectMapper mapper) {
        this.directory=Path.of(directory); this.baseUrl=baseUrl; this.mapper=mapper;
    }
    public void deliver(String email,TokenStore.Kind kind,String token) {
        try {
            Files.createDirectories(directory,PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rwx------")));
            Files.setPosixFilePermissions(directory,PosixFilePermissions.fromString("rwx------"));
            Path file=Files.createTempFile(directory,"message-",".json",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")));
            String route=kind==TokenStore.Kind.VERIFY ? "/verify-email" : "/reset-password";
            mapper.writeValue(file.toFile(),Map.of("to",email,"kind",kind.name(),"url",baseUrl+route+"#token="+token));
        } catch (IOException e) { throw new ApiException(503,"DELIVERY_UNAVAILABLE","인증 메일을 준비하지 못했습니다."); }
    }
}
