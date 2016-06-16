package com.codenvy.auth.sso.server;

import com.codenvy.api.dao.authentication.AuthenticationHandler;

import org.eclipse.che.api.auth.AuthenticationException;
import org.eclipse.che.api.core.ApiException;
import org.eclipse.che.api.user.server.dao.User;
import org.eclipse.che.api.user.server.dao.UserDao;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

/**
 * Provide ability to authenticate users with one time token.
 */
public class BearerAuthenticationHandler implements AuthenticationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(BearerAuthenticationHandler.class);

    @Inject
    BearerTokenManager tokenManager;

    @Inject
    UserDao userDao;

    public Subject authenticate(final String login, final String password)
            throws AuthenticationException {
        if (login == null || login.isEmpty() || password == null || password.isEmpty()) {
            throw new AuthenticationException(401, "Authentication failed. Please check username and password.");
        }
        try {
            Map<String, String> payload = tokenManager.checkValid(password);
            if (!login.equals(payload.get("userName"))) {
                throw new AuthenticationException(401, "Authentication failed. Please check username and password.");
            }
            User user = userDao.getByName(login);
            return new SubjectImpl(user.getName(), user.getId(), null, false);
        } catch (ApiException | InvalidBearerTokenException e) {
            LOG.debug(e.getLocalizedMessage(), e);
            throw new AuthenticationException(401, "Authentication failed. Please check username and password.");
        }
    }


    @Override
    public String getType() {
        return "bearer";
    }
}
