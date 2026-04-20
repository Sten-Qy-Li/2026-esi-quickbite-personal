package ee.ut.esi.quickbite.menu.events;

import java.time.Instant;
import java.util.UUID;

public record AvailabilityChangedEvent(
    UUID menuItemId,
    UUID restaurantId,
    boolean isAvailable,
    boolean previousIsAvailable,
    Instant occurredAt
) { }
