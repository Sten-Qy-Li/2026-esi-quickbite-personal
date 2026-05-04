package ee.ut.esi.quickbite.restaurant.exception;

import java.util.UUID;

public class DuplicateRestaurantException extends RuntimeException {
    public DuplicateRestaurantException(UUID ownerId, String name) {
        super("Restaurant '" + name + "' already exists for owner " + ownerId);
    }
}
