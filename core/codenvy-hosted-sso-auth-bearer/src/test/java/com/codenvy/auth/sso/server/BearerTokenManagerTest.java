package com.codenvy.auth.sso.server;

import com.codenvy.api.dao.authentication.TokenGenerator;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import org.eclipse.che.commons.lang.Pair;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Map;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

/**
 * Test for #BearerTokenManager
 */
public class BearerTokenManagerTest {
    TokenGenerator tokenGenerator = new SecureRandomTokenGenerator();

    BearerTokenManager tokenManager;

    @BeforeMethod
    public void setUp() {
        tokenManager = new BearerTokenManager(100L, tokenGenerator);
    }

    @Test
    public void shouldBeAbleToGenerateNewToken() throws InvalidBearerTokenException {
        //given
        Map<String, String> payload = ImmutableMap.of("k1", "v1", "k2", "v2");
        //when
        String token = tokenManager.generateBearerToken(payload);
        //then
        assertTrue(tokenManager.getTokenMap().containsKey(token));
        Pair<Long, Map<String, String>> pair = tokenManager.getTokenMap().get(token);
        assertNotNull(pair);
        Map<String, String> actual = pair.second;
        assertTrue(pair.first < System.currentTimeMillis() + 1000);
        assertTrue(Maps.difference(payload, actual).areEqual());
    }

    @Test
    public void shouldBeAbleToTestInvalidToken() throws InvalidBearerTokenException {
        //given
        //when
        assertTrue(tokenManager.getTokenMap().isEmpty());
        assertFalse(tokenManager.isValid("invalidToken"));
        //then
    }

    @Test
    public void shouldBeAbleToTest3ValidTokens() throws InvalidBearerTokenException {
        //given
        String token = addToken(ImmutableMap.of("k1", "v1", "k2", "v2"));
        String token2 = addToken(ImmutableMap.of("k1", "v1", "k2", "v2"));
        String token3 = addToken(ImmutableMap.of("k1", "v1", "k2", "v2"));
        //when
        //then
        assertTrue(tokenManager.isValid(token));
        assertTrue(tokenManager.isValid(token2));
        assertTrue(tokenManager.isValid(token3));
    }

    @Test
    public void shouldBeAbleToReturnPayloadForValidToken() throws InvalidBearerTokenException {
        //given
        Map<String, String> payload = ImmutableMap.of("k1", "v1", "k2", "v2");
        String token = addToken(payload);
        //when
        Map<String, String> actual = tokenManager.getPayload(token);
        //then
        assertNotNull(actual);
        assertTrue(Maps.difference(payload, actual).areEqual());
    }

    @Test(expectedExceptions = InvalidBearerTokenException.class, expectedExceptionsMessageRegExp = "Provided token .* not found or expired")
    public void shouldThrowExceptionForGetPayloadIfTokenIsInvalid() throws InvalidBearerTokenException {
        //given
        //when
        tokenManager.getPayload("invalidToken");
        //then
    }


    @Test
    public void shouldRemoveTokenAndReturnPayloadOnCheckValid() throws InvalidBearerTokenException {
        //given
        Map<String, String> payload = ImmutableMap.of("k1", "v1", "k2", "v2");
        String token = addToken(payload);
        //when
        Map<String, String> actual = tokenManager.checkValid(token);
        //then
        assertFalse(tokenManager.getTokenMap().containsKey(token));
        assertTrue(Maps.difference(payload, actual).areEqual());
    }

    @Test(expectedExceptions = InvalidBearerTokenException.class, expectedExceptionsMessageRegExp = "Provided token .* not found or expired")
    public void shouldThrowExceptionIfNoToken() throws InvalidBearerTokenException {
        //given
        //when
        tokenManager.checkValid("invalidToken");
        //then
    }

    @Test(expectedExceptions = InvalidBearerTokenException.class, expectedExceptionsMessageRegExp = "Provided token .* not found or expired")
    public void shouldThrowExceptionIfExistedTokenIsExpired() throws InvalidBearerTokenException {
        //given
        Map<String, String> payload = ImmutableMap.of("k1", "v1", "k2", "v2");
        String token = addToken(System.currentTimeMillis() - 1000*1000, payload);
        //when
        tokenManager.checkValid(token);
        //then
    }

    @Test
    public void shouldReturnFailToCheckExpiredToken() throws InvalidBearerTokenException {
        //given
        Map<String, String> payload = ImmutableMap.of("k1", "v1", "k2", "v2");
        String token = addToken(System.currentTimeMillis() - 1000*1000, payload);
        //when

        assertFalse(tokenManager.isValid(token));
        //then
    }

    @Test
    public void shouldRemoveInvalidTokens() throws InvalidBearerTokenException {
        //given
        String invalid1 = addToken(System.currentTimeMillis() - 1000*1000, ImmutableMap.of("k1", "v1", "k2", "v2"));
        String invalid2 = addToken(System.currentTimeMillis() - 1000*1000, ImmutableMap.of("k1", "v1", "k2", "v2"));
        String invalid3 = addToken(System.currentTimeMillis() - 1000*1000, ImmutableMap.of("k1", "v1", "k2", "v2"));
        String token = addToken(ImmutableMap.of("k1", "v1", "k2", "v2"));
        String token2 = addToken(ImmutableMap.of("k1", "v1", "k2", "v2"));
        String token3 = addToken(ImmutableMap.of("k1", "v1", "k2", "v2"));
        //when
        tokenManager.removeInvalidTokens();
        //then
        Map<String, Pair<Long, Map<String, String>>> tokenMap = tokenManager.getTokenMap();
        assertEquals(tokenMap.size(), 3);
        assertFalse(tokenMap.containsKey(invalid1));
        assertFalse(tokenMap.containsKey(invalid2));
        assertFalse(tokenMap.containsKey(invalid3));
        assertTrue(tokenMap.containsKey(token2));
        assertTrue(tokenMap.containsKey(token3));
        assertTrue(tokenMap.containsKey(token));
        assertTrue(tokenMap.containsKey(token2));
        assertTrue(tokenMap.containsKey(token3));
    }


    private void addToken(String token, Long time, Map<String, String> payload) {
        tokenManager.getTokenMap().put(token, new Pair<>(time, ImmutableMap.copyOf(payload)));
    }

    private void addToken(String token, Map<String, String> payload) {
        addToken(token, System.currentTimeMillis(), payload);
    }

    private String addToken(Map<String, String> payload) {
        String token = tokenGenerator.generate();
        addToken(token, payload);
        return token;
    }

    private String addToken(Long time, Map<String, String> payload) {
        String token = tokenGenerator.generate();
        addToken(token, time, payload);
        return token;
    }
}
