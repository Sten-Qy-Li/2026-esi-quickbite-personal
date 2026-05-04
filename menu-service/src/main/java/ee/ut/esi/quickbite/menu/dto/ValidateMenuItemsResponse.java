package ee.ut.esi.quickbite.menu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ValidateMenuItemsResponse(
    boolean allValid,
    List<Line> items,
    BigDecimal totalAmount,
    String currency
) {

    public static final String ERROR_NOT_FOUND = "MENU_ITEM_NOT_FOUND";
    public static final String ERROR_NOT_AVAILABLE = "MENU_ITEM_NOT_AVAILABLE";

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Line(
        UUID menuItemId,
        int quantity,
        boolean exists,
        @JsonProperty("isAvailable") boolean isAvailable,
        BigDecimal unitPriceAmount,
        String unitPriceCurrency,
        BigDecimal lineTotal,
        String error
    ) {}
}
