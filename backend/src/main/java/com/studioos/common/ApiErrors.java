package com.studioos.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.dao.DataIntegrityViolationException;

@RestControllerAdvice
public class ApiErrors {
    public record Error(String code, String message, List<Object> fieldErrors, String traceId) {}
    public static Error body(String code, String message) {
        return new Error(code, message, List.of(), UUID.randomUUID().toString());
    }
    public static void write(HttpServletResponse response, ObjectMapper mapper, int status, String code, String message) throws IOException {
        response.setStatus(status); response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");
        mapper.writeValue(response.getOutputStream(), body(code, message));
    }
    @ExceptionHandler(ApiException.class)
    ResponseEntity<Error> known(ApiException e) { return ResponseEntity.status(e.status).body(body(e.code,e.getMessage())); }
    @ExceptionHandler({MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
        org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class})
    ResponseEntity<Error> invalid(Exception e) {
        return ResponseEntity.badRequest().body(body("INVALID_REQUEST","입력 내용을 확인해 주세요."));
    }
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Error> conflict(Exception e) {
        return ResponseEntity.status(409).body(body("CONFLICT","이미 사용 중이거나 유효하지 않은 정보입니다."));
    }
    @ExceptionHandler(Exception.class)
    ResponseEntity<Error> unexpected(Exception e) {
        return ResponseEntity.status(500).body(body("INTERNAL_ERROR","요청을 처리하지 못했습니다."));
    }
}
