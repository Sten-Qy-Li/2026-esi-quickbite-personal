package ee.ut.esi.quickbite.restaurant.security;

import java.util.UUID;

public record AuthenticatedUser(UUID userId, String role, String tokenType, String serviceName) {

    public boolean isService() {
        return "SERVICE".equals(tokenType);
    }
}
