package ee.ut.esi.quickbite.restaurant.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateRestaurantStatusRequest(

    @NotNull(message = "isOpen is required")
    Boolean isOpen

) {}
