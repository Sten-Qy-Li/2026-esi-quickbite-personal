package ee.ut.esi.quickbite.restaurant.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;

import java.util.Optional;
import java.util.UUID;

@Configuration
public class AuditingConfig {

    private static final UUID SYSTEM_USER =
        UUID.fromString("00000000-0000-0000-0000-000000000000");

    @Bean
    public AuditorAware<UUID> auditorAware() {
        return () -> Optional.of(SYSTEM_USER);
    }
}
