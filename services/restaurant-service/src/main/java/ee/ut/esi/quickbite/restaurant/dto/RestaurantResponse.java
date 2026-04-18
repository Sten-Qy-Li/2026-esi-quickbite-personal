package ee.ut.esi.quickbite.restaurant.dto;

import ee.ut.esi.quickbite.restaurant.domain.Restaurant;

import java.time.LocalDateTime;
import java.util.UUID;

public record RestaurantResponse(
    UUID restaurantId,
    UUID ownerId,
    String name,
    String address,
    String city,
    Double latitude,
    Double longitude,
    String operatingHours,
    boolean isOpen,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static RestaurantResponse from(Restaurant r) {
        return new RestaurantResponse(
            r.getRestaurantId(),
            r.getOwnerId(),
            r.getName(),
            r.getLocation() != null ? r.getLocation().getAddress()   : null,
            r.getLocation() != null ? r.getLocation().getCity()      : null,
            r.getLocation() != null ? r.getLocation().getLatitude()  : null,
            r.getLocation() != null ? r.getLocation().getLongitude() : null,
            r.getOperatingHours(),
            r.isOpen(),
            r.getCreatedAt(),
            r.getUpdatedAt()
        );
    }
}
