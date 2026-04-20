package ee.ut.esi.quickbite.menu.exception;

import java.util.Set;

public class MixedCurrencyException extends RuntimeException {
    public MixedCurrencyException(Set<String> currencies) {
        super("Mixed currencies in validate request: " + currencies);
    }
}
