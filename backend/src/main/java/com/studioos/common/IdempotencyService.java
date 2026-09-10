package com.studioos.common;

import com.fasterxml.jackson.databind.*;
import com.studioos.security.AuthorizedStudioContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Supplier;

/** Caller must reauthorize and lock the Studio before entering this same transaction. */
@Service
public class IdempotencyService {
    private final JdbcTemplate jdbc;private final ObjectMapper mapper;
    public IdempotencyService(JdbcTemplate jdbc,ObjectMapper mapper){this.jdbc=jdbc;this.mapper=mapper;}
    @Transactional(propagation=Propagation.MANDATORY)
    public <T> T execute(AuthorizedStudioContext actor,String operation,String key,Object request,int status,Class<T> type,Supplier<T> action){
        if(key==null || !key.matches("[A-Za-z0-9._:-]{1,128}"))
            throw new ApiException(400,"IDEMPOTENCY_KEY_REQUIRED","유효한 Idempotency-Key가 필요합니다.");
        try {
            String serialized=mapper.writer().with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS).writeValueAsString(request);
            String hash=HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(serialized.getBytes(StandardCharsets.UTF_8)));
            var rows=jdbc.queryForList("select request_hash,response_body from idempotency_records where studio_id=? and actor_type='OPERATOR_USER' and actor_id=? and operation=? and idempotency_key=?",
                actor.authorizedStudioId(),actor.authenticatedUserId().toString(),operation,key);
            if(!rows.isEmpty()) {
                var row=rows.getFirst();if(!hash.equals(row.get("request_hash")))throw new ApiException(409,"IDEMPOTENCY_MISMATCH","동일 요청 키에 다른 내용을 사용할 수 없습니다.");
                return mapper.readValue((String)row.get("response_body"),type);
            }
            T result=action.get();
            jdbc.update("insert into idempotency_records(id,studio_id,actor_type,actor_id,operation,idempotency_key,request_hash,response_status,response_body,created_at) values (?,?,'OPERATOR_USER',?,?,?,?,?,?,now())",
                UUID.randomUUID(),actor.authorizedStudioId(),actor.authenticatedUserId().toString(),operation,key,hash,status,mapper.writeValueAsString(result));
            return result;
        }catch(ApiException e){throw e;}catch(RuntimeException e){throw e;}catch(Exception e){throw new IllegalStateException("Idempotency serialization failed",e);}
    }
}
