package ee.ut.esi.quickbite.restaurant.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record AvailabilityResponse(
    UUID restaurantId,
    boolean isOpen,
    boolean acceptsOrders,
    String operatingHours,
    Instant checkedAt
) {}
