package net.marcelomartins.maisumtodo.infra;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import net.marcelomartins.maisumtodo.domain.SystemLogin;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@ApplicationScoped
public class AuthTokenService {

    public static final String COOKIE_NAME = "auth_token";

    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "maisumtodo.auth.issuer")
    String issuer;

    @ConfigProperty(name = "maisumtodo.auth.secret")
    String secret;

    @ConfigProperty(name = "maisumtodo.auth.expires-seconds")
    long expiresSeconds;

    @ConfigProperty(name = "maisumtodo.auth.cookie-secure")
    boolean cookieSecure;

    public String createToken(SystemLogin login) {
        try {
            long now = Instant.now().getEpochSecond();
            long expiresAt = now + expiresSeconds;

            Map<String, Object> header = new LinkedHashMap<>();
            header.put("alg", "HS256");
            header.put("typ", "JWT");

            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("iss", issuer);
            payload.put("sub", login.uuid);
            payload.put("email", login.email);
            payload.put("ver", login.currentAuthTokenVersion());
            payload.put("iat", now);
            payload.put("exp", expiresAt);

            String headerPart = encodeJson(header);
            String payloadPart = encodeJson(payload);
            String signedContent = headerPart + "." + payloadPart;
            return signedContent + "." + sign(signedContent);
        } catch (Exception e) {
            throw new IllegalStateException("Could not create auth token", e);
        }
    }

    public TokenClaims verify(String token) {
        try {
            if (token == null || token.isBlank()) {
                throw unauthorized();
            }

            String[] parts = token.split("\\.", -1);
            if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
                throw unauthorized();
            }

            String signedContent = parts[0] + "." + parts[1];
            String expectedSignature = sign(signedContent);
            if (!MessageDigest.isEqual(
                    expectedSignature.getBytes(StandardCharsets.US_ASCII),
                    parts[2].getBytes(StandardCharsets.US_ASCII))) {
                throw unauthorized();
            }

            Map<String, Object> header = decodeJson(parts[0]);
            if (!"HS256".equals(asString(header.get("alg")))) {
                throw unauthorized();
            }

            Map<String, Object> payload = decodeJson(parts[1]);
            if (!issuer.equals(asString(payload.get("iss")))) {
                throw unauthorized();
            }

            String userUuid = asString(payload.get("sub"));
            Long tokenVersion = asLong(payload.get("ver"));
            Long expiresAt = asLong(payload.get("exp"));
            if (userUuid == null || userUuid.isBlank() || tokenVersion == null || expiresAt == null) {
                throw unauthorized();
            }
            if (expiresAt <= Instant.now().getEpochSecond()) {
                throw unauthorized();
            }

            return new TokenClaims(userUuid, tokenVersion);
        } catch (WebApplicationException e) {
            throw e;
        } catch (Exception e) {
            throw unauthorized();
        }
    }

    public NewCookie createCookie(String token) {
        int maxAge = expiresSeconds > Integer.MAX_VALUE ? Integer.MAX_VALUE : Math.max(0, (int) expiresSeconds);
        return new NewCookie.Builder(COOKIE_NAME)
                .value(token)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(NewCookie.SameSite.STRICT)
                .path("/")
                .maxAge(maxAge)
                .build();
    }

    public NewCookie clearCookie() {
        return new NewCookie.Builder(COOKIE_NAME)
                .value("")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite(NewCookie.SameSite.STRICT)
                .path("/")
                .maxAge(0)
                .build();
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private Map<String, Object> decodeJson(String value) throws Exception {
        return objectMapper.readValue(BASE64_URL_DECODER.decode(value), MAP_TYPE);
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private Long asLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String asString(Object value) {
        return value == null ? null : value.toString();
    }

    private WebApplicationException unauthorized() {
        return new WebApplicationException(Response.Status.UNAUTHORIZED);
    }

    public record TokenClaims(String userUuid, long authTokenVersion) {
    }
}
