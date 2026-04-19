package ee.ut.esi.quickbite.menu.exception;

import java.util.UUID;

public class RestaurantNotFoundForMenuException extends RuntimeException {

    private final UUID restaurantId;

    public RestaurantNotFoundForMenuException(UUID restaurantId) {
        super("Restaurant " + restaurantId + " not found");
        this.restaurantId = restaurantId;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }
}
