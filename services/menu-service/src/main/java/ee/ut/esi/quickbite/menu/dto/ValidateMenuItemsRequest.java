package ee.ut.esi.quickbite.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.util.List;
import java.util.UUID;

public record ValidateMenuItemsRequest(

    @NotEmpty(message = "items must contain at least one entry")
    @Valid
    List<Line> items

) {
    public record Line(
        @NotNull(message = "menuItemId is required")
        UUID menuItemId,

        @NotNull(message = "quantity is required")
        @Positive(message = "quantity must be greater than 0")
        Integer quantity
    ) {}
}
