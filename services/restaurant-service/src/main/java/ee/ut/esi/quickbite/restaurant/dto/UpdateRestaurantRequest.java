package ee.ut.esi.quickbite.restaurant.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateRestaurantRequest(

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be at most 255 characters")
    String name,

    @Size(max = 255, message = "address must be at most 255 characters")
    String address,

    @NotBlank(message = "city is required")
    @Size(max = 120, message = "city must be at most 120 characters")
    String city,

    @NotNull(message = "latitude is required")
    @DecimalMin(value = "-90.0",  message = "latitude must be >= -90")
    @DecimalMax(value = "90.0",   message = "latitude must be <= 90")
    Double latitude,

    @NotNull(message = "longitude is required")
    @DecimalMin(value = "-180.0", message = "longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "longitude must be <= 180")
    Double longitude,

    @Pattern(regexp = "^\\d{2}:\\d{2}-\\d{2}:\\d{2}$",
             message = "operatingHours must match HH:MM-HH:MM (e.g. 09:00-22:00)")
    String operatingHours

) {}
