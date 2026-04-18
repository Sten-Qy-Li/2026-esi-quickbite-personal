package ee.ut.esi.quickbite.restaurant.dto;

import java.util.UUID;

public record AvailabilityResponse(
    UUID restaurantId,
    boolean isOpen,
    String operatingHours
) {}
