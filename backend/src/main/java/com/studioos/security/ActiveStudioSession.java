package com.studioos.security;

import jakarta.servlet.http.HttpSession;
import java.util.UUID;
import org.springframework.stereotype.Service;

/** The sole reader/writer of the session's tenant selection. Selection is never authority. */
@Service
public class ActiveStudioSession {
    private static final String ATTRIBUTE = "activeStudioId";
    private final StudioAuthorization authorization;

    public ActiveStudioSession(StudioAuthorization authorization) { this.authorization = authorization; }

    public UUID current(HttpSession session) {
        var memberships = authorization.memberships();
        UUID selected = memberships.size() == 1 ? memberships.getFirst().studioId
            : session.getAttribute(ATTRIBUTE) instanceof UUID id ? id : null;
        if (selected != null) {
            UUID candidate = selected;
            if (memberships.stream().noneMatch(m -> m.studioId.equals(candidate))) selected = null;
        }
        if (selected == null) clear(session);
        else session.setAttribute(ATTRIBUTE, selected);
        return selected;
    }

    public void select(UUID studioId, HttpSession session) {
        authorization.authorize(studioId);
        session.setAttribute(ATTRIBUTE, studioId);
    }

    public static void clear(HttpSession session) { session.removeAttribute(ATTRIBUTE); }
}
