package ee.ut.esi.quickbite.menu.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class JwtProperties {

    private final String secret;
    private final String issuer;
    private final Duration ttl;

    public JwtProperties(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.issuer}") String issuer,
        @Value("${jwt.ttl:PT1H}") Duration ttl
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.ttl = ttl;
    }

    public String secret() {
        return secret;
    }

    public String issuer() {
        return issuer;
    }

    public Duration ttl() {
        return ttl;
    }
}
