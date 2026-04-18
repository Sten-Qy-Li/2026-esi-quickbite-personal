package ee.ut.esi.quickbite.menu.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

public final class JwtDevMint {

    public static final UUID DEFAULT_CUSTOMER_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-0000000000c1");
    public static final UUID DEFAULT_OWNER_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");
    public static final UUID DEFAULT_ADMIN_USER_ID =
        UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private JwtDevMint() {
    }

    public static String mint(String base64Secret, String issuer, Duration ttl,
                              UUID userId, String subject, String role) {
        return mint(base64Secret, issuer, ttl, userId, subject, role, "USER", null);
    }

    public static String mint(String base64Secret, String issuer, Duration ttl,
                              UUID userId, String subject, String role,
                              String tokenType, String serviceName) {
        Key key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
        Instant now = Instant.now();
        var builder = Jwts.builder()
            .setIssuer(issuer)
            .setSubject(subject)
            .claim("userId", userId.toString())
            .claim("role", role)
            .claim("tokenType", tokenType)
            .setIssuedAt(Date.from(now))
            .setExpiration(Date.from(now.plus(ttl)))
            .signWith(key);
        if (serviceName != null) {
            builder.claim("serviceName", serviceName);
        }
        return builder.compact();
    }
}
