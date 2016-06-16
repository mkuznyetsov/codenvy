/*
 *  [2012] - [2016] Codenvy, S.A.
 *  All Rights Reserved.
 *
 * NOTICE:  All information contained herein is, and remains
 * the property of Codenvy S.A. and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Codenvy S.A.
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Codenvy S.A..
 */
package com.codenvy.auth.sso.server;

import com.codenvy.api.dao.authentication.TokenGenerator;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;

import org.eclipse.che.commons.lang.Pair;
import org.eclipse.che.commons.schedule.ScheduleRate;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manager of time based tokens.
 * Tokens can be associated with some additional information via Map<string, string>.
 * @author Sergii Kabashniuk
 */
@Singleton
public class BearerTokenManager {
    private final Map<String, Pair<Long, Map<String, String>>> tokenMap;
    /** Period of time when bearer ticked keep valid */

    private final Long                                         ticketLifeTimeSeconds;

    private final TokenGenerator tokenGenerator;


    @Inject
    public BearerTokenManager(@Named("auth.sso.bearer_ticket_lifetime_seconds")
                              Long ticketLifeTimeSeconds,
                              TokenGenerator tokenGenerator) {
        this.tokenMap = new ConcurrentHashMap<>();
        this.ticketLifeTimeSeconds = ticketLifeTimeSeconds;
        this.tokenGenerator = tokenGenerator;
    }

    @ScheduleRate(initialDelay = 5, period = 60, unit = TimeUnit.SECONDS)
    public void removeInvalidTokens() {
        tokenMap.entrySet().removeIf(entry -> !isValid(entry.getValue()));
    }

    /**
     * Generate new token and associate some payload with it.
     *
     * @return - token for one time authentication.
     */
    public String generateBearerToken(Map<String, String> payload) {
        String token = tokenGenerator.generate();
        tokenMap.put(token, new Pair<>(System.currentTimeMillis(), ImmutableMap.copyOf(payload)));
        return token;
    }

    /**
     * Get token payload.
     *
     * @param token
     *         - bearer token that associated with payload.
     * @return - map with payload
     */
    public Map<String, String> getPayload(String token) throws InvalidBearerTokenException {
        Pair<Long, Map<String, String>> payload = tokenMap.get(token);
        if (isValid(payload)) {
            return payload.second;
        }
        throw new InvalidBearerTokenException(String.format("Provided token %s not found or expired", token), token);

    }


    /**
     * Check is bearer token still valid.
     *
     * @param token
     *         - bearer token
     * @return - true if it is valid, false otherwise
     */
    public boolean isValid(String token) {
        return isValid(tokenMap.get(token));
    }


    /**
     * Check is bearer token still valid.
     * If it's invalidate it and return associated payload.
     *
     * @param token
     *         - bearer token
     * @return - associated payload
     */
    public Map<String, String> checkValid(String token) throws InvalidBearerTokenException {
        Pair<Long, Map<String, String>> payload = tokenMap.remove(token);
        if (isValid(payload)) {
            return payload.second;
        }
        throw new InvalidBearerTokenException(String.format("Provided token %s not found or expired", token), token);
    }


    private boolean isValid(Pair<Long, Map<String, String>> payload) {
        if (payload != null) {
            long creationTime = payload.first;
            long currentTime = System.currentTimeMillis();

            return (creationTime + ticketLifeTimeSeconds * 1000) > currentTime;
        }
        return false;
    }

    @VisibleForTesting
    Map<String, Pair<Long, Map<String, String>>> getTokenMap() {
        return tokenMap;
    }
}
