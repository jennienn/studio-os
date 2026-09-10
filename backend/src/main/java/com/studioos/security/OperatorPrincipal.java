package com.studioos.security;
import java.io.Serializable;
import java.security.Principal;
import java.util.UUID;
public record OperatorPrincipal(UUID userId, long securityVersion) implements Principal, Serializable {
    @Override public String getName() { return userId.toString(); }
}
