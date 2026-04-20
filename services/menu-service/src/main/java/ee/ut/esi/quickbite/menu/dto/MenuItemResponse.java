package ee.ut.esi.quickbite.menu.dto;

import ee.ut.esi.quickbite.menu.domain.MenuItem;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MenuItemResponse(
    UUID menuItemId,
    UUID restaurantId,
    String name,
    String description,
    BigDecimal priceAmount,
    String priceCurrency,
    String category,
    boolean isAvailable,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static MenuItemResponse from(MenuItem m) {
        return new MenuItemResponse(
            m.getMenuItemId(),
            m.getRestaurantId(),
            m.getName(),
            m.getDescription(),
            m.getPrice() != null ? m.getPrice().getAmount()   : null,
            m.getPrice() != null ? m.getPrice().getCurrency() : null,
            m.getCategory(),
            m.isAvailable(),
            m.getCreatedAt(),
            m.getUpdatedAt()
        );
    }
}
