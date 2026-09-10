package com.studioos.auth;
/** Transport boundary only, not the future business Notification domain. */
public interface TokenDelivery {
    default boolean available() { return true; }
    void deliver(String email,TokenStore.Kind kind,String token);
}
