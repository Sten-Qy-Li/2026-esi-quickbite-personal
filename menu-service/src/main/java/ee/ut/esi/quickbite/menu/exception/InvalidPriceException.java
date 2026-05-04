package ee.ut.esi.quickbite.menu.exception;

import java.math.BigDecimal;

public class InvalidPriceException extends RuntimeException {
    public InvalidPriceException(BigDecimal amount, String reason) {
        super("Invalid price " + amount + ": " + reason);
    }
}
