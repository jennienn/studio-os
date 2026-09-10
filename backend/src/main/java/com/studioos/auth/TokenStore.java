package com.studioos.auth;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;

@Repository
public class TokenStore {
    public enum Kind {
        VERIFY("email_verification_tokens"), RESET("password_reset_tokens");
        final String table;
        Kind(String table) { this.table=table; }
    }
    private final JdbcTemplate jdbc;
    private final SecureRandom random=new SecureRandom();
    public TokenStore(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    public static String hash(String value) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8))); }
        catch (NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }
    public String issue(Kind kind,UUID userId,Duration lifetime) {
        byte[] bytes=new byte[32]; random.nextBytes(bytes);
        String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        jdbc.update("insert into "+kind.table+" (id,user_id,token_hash,expires_at,created_at) values (?,?,?, ?, current_timestamp)",
            UUID.randomUUID(),userId,hash(token),java.sql.Timestamp.from(Instant.now().plus(lifetime)));
        return token;
    }
    public Optional<UUID> owner(Kind kind,String token) {
        if (token==null || !token.matches("[A-Za-z0-9_-]{43}")) return Optional.empty();
        return jdbc.queryForList("select user_id from "+kind.table+" where token_hash=? and consumed_at is null and expires_at>current_timestamp",
            UUID.class,hash(token)).stream().findFirst();
    }
    public boolean consume(Kind kind,String token) {
        return jdbc.update("update "+kind.table+" set consumed_at=current_timestamp where token_hash=? and consumed_at is null and expires_at>current_timestamp",
            hash(token))==1;
    }
    public void invalidateAll(Kind kind,UUID userId) {
        jdbc.update("update "+kind.table+" set consumed_at=current_timestamp where user_id=? and consumed_at is null",userId);
    }
}
