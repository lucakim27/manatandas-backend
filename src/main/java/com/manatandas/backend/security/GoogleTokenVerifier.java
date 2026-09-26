package com.manatandas.backend.security;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Verifies Google Identity Services ID tokens by calling Google's public
 * tokeninfo endpoint. This is Google's officially supported verification
 * method and needs no extra dependency, but note the trade-off: it costs a
 * network round-trip per login and is rate-limited by Google, so it's fine
 * for a project at this scale. A high-traffic production app should instead
 * verify the token's signature locally against Google's public keys (e.g.
 * via the google-api-client library's GoogleIdTokenVerifier) to avoid that
 * per-login network call.
 */
@Service
public class GoogleTokenVerifier {

    private static final String TOKENINFO_URL = "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private final String expectedClientId;
    private final HttpClient client = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GoogleTokenVerifier(@Value("${google.oauth.client-id}") String expectedClientId) {
        this.expectedClientId = expectedClientId;
    }

    public record GoogleUser(String googleId, String email, String displayName, String pictureUrl) {}

    /**
     * Throws IllegalArgumentException if the token is missing, expired, not
     * meant for this app, or otherwise doesn't check out.
     */
    public GoogleUser verify(String idToken) {
        JsonNode claims = fetchClaims(idToken);

        String audience = claims.path("aud").asText("");
        if (!expectedClientId.equals(audience)) {
            throw new IllegalArgumentException("Token was not issued for this app");
        }

        String issuer = claims.path("iss").asText("");
        if (!"accounts.google.com".equals(issuer) && !"https://accounts.google.com".equals(issuer)) {
            throw new IllegalArgumentException("Unexpected token issuer: " + issuer);
        }

        if (!claims.path("email_verified").asBoolean(false)) {
            throw new IllegalArgumentException("Google account email is not verified");
        }

        String googleId = claims.path("sub").asText(null);
        String email = claims.path("email").asText(null);
        if (googleId == null || email == null) {
            throw new IllegalArgumentException("Token is missing required claims");
        }

        String name = claims.path("name").asText(null);
        String picture = claims.path("picture").asText(null);

        return new GoogleUser(googleId, email, name, picture);
    }

    private JsonNode fetchClaims(String idToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(TOKENINFO_URL + idToken))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            // Google returns 400 for any invalid/expired/malformed token.
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException("Invalid or expired Google token");
            }

            return objectMapper.readTree(response.body());
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not verify Google token", e);
        }
    }
}
