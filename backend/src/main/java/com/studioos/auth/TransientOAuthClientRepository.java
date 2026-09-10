package com.studioos.auth;
import jakarta.servlet.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;

/** Login-only integration: access/refresh tokens are not needed after user-info lookup. */
public class TransientOAuthClientRepository implements OAuth2AuthorizedClientRepository {
    public <T extends OAuth2AuthorizedClient> T loadAuthorizedClient(String id,Authentication principal,HttpServletRequest request) { return null; }
    public void saveAuthorizedClient(OAuth2AuthorizedClient client,Authentication principal,HttpServletRequest request,HttpServletResponse response) {}
    public void removeAuthorizedClient(String id,Authentication principal,HttpServletRequest request,HttpServletResponse response) {}
}
