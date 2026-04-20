package ee.ut.esi.quickbite.menu.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

public record ValidateMenuItemsRequest(

    @NotEmpty(message = "items must contain at least one entry")
    @Size(max = 100, message = "items must not contain more than 100 entries")
    @Valid
    List<Line> items

) {
    public record Line(
        @NotNull(message = "menuItemId is required")
        UUID menuItemId,

        @NotNull(message = "quantity is required")
        @Min(value = 1, message = "quantity must be at least 1")
        @Max(value = 100, message = "quantity must not exceed 100")
        Integer quantity
    ) {}
}
