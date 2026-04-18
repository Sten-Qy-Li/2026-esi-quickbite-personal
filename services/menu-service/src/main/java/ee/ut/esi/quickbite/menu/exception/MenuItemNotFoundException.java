package ee.ut.esi.quickbite.menu.exception;

import java.util.UUID;

public class MenuItemNotFoundException extends RuntimeException {
    public MenuItemNotFoundException(UUID id) {
        super("Menu item not found: " + id);
    }
}
