package ee.ut.esi.quickbite.menu.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ValidateMenuItemsResponse(
    boolean allValid,
    List<Line> results
) {
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record Line(
        UUID menuItemId,
        int quantity,
        boolean exists,
        boolean available,
        BigDecimal unitPriceAmount,
        String unitPriceCurrency,
        BigDecimal lineTotalAmount,
        String reason
    ) {}
}
