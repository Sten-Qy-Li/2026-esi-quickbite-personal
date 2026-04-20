package ee.ut.esi.quickbite.menu.config;

import ee.ut.esi.quickbite.menu.security.AuthenticatedUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class AuditingConfig {

    static final UUID SYSTEM_USER =
        UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !auth.isAuthenticated()) {
                return Optional.of(SYSTEM_USER);
            }
            Object principal = auth.getPrincipal();
            if (principal instanceof AuthenticatedUser user) {
                return Optional.of(user.userId());
            }
            return Optional.of(SYSTEM_USER);
        };
    }
}
