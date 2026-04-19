package ee.ut.esi.quickbite.menu.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record UpdateMenuItemRequest(

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    String name,

    @Size(max = 2000, message = "description must be at most 2000 characters")
    String description,

    @NotNull(message = "priceAmount is required")
    @Positive(message = "priceAmount must be greater than 0")
    @Digits(integer = 17, fraction = 2,
            message = "priceAmount must have at most 2 decimal places")
    BigDecimal priceAmount,

    @NotBlank(message = "priceCurrency is required")
    @Pattern(regexp = "^[A-Z]{3}$", message = "priceCurrency must be a 3-letter ISO code (e.g. EUR)")
    String priceCurrency,

    @NotBlank(message = "category is required")
    @Size(max = 100, message = "category must be at most 100 characters")
    String category,

    @NotNull(message = "isAvailable is required")
    Boolean isAvailable

) {}
