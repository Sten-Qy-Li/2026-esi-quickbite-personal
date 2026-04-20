package ee.ut.esi.quickbite.menu.exception;

public class OwningRestaurantLookupException extends RuntimeException {

    public OwningRestaurantLookupException(String message) {
        super(message);
    }

    public OwningRestaurantLookupException(String message, Throwable cause) {
        super(message, cause);
    }
}
