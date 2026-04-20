package ee.ut.esi.quickbite.menu.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import ee.ut.esi.quickbite.menu.exception.ErrorResponse;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtProperties jwt;
    private final Key signingKey;
    private final ObjectMapper objectMapper;

    public JwtAuthFilter(JwtProperties jwt, ObjectMapper objectMapper) {
        this.jwt = jwt;
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwt.secret()));
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
        throws ServletException, IOException {

        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith(BEARER_PREFIX)) {
            chain.doFilter(request, response);
            return;
        }

        String token = header.substring(BEARER_PREFIX.length()).trim();
        try {
            Claims claims = Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .requireIssuer(jwt.issuer())
                .build()
                .parseClaimsJws(token)
                .getBody();

            UUID userId = UUID.fromString(requireClaim(claims, "userId"));
            String role = requireClaim(claims, "role");
            String tokenType = claims.get("tokenType", String.class);
            if (tokenType == null) {
                tokenType = "USER";
            }
            String serviceName = claims.get("serviceName", String.class);

            AuthenticatedUser principal = new AuthenticatedUser(userId, role, tokenType, serviceName);
            var authentication = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + role))
            );
            SecurityContextHolder.getContext().setAuthentication(authentication);

            chain.doFilter(request, response);
        } catch (JwtException | IllegalArgumentException ex) {
            SecurityContextHolder.clearContext();
            writeUnauthorized(request, response, "Invalid or expired token");
        }
    }

    private static String requireClaim(Claims claims, String name) {
        String value = claims.get(name, String.class);
        if (value == null || value.isBlank()) {
            throw new JwtException("missing '" + name + "' claim");
        }
        return value;
    }

    private void writeUnauthorized(HttpServletRequest request, HttpServletResponse response, String message)
        throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ErrorResponse body = new ErrorResponse(
            OffsetDateTime.now(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.UNAUTHORIZED.getReasonPhrase(),
            message,
            request.getRequestURI(),
            null
        );
        objectMapper.writeValue(response.getWriter(), body);
    }
}
