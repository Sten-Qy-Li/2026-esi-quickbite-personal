package ee.ut.esi.quickbite.restaurant.controller;

import ee.ut.esi.quickbite.restaurant.dto.AvailabilityResponse;
import ee.ut.esi.quickbite.restaurant.dto.CreateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.dto.RestaurantResponse;
import ee.ut.esi.quickbite.restaurant.dto.UpdateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.dto.UpdateRestaurantStatusRequest;
import ee.ut.esi.quickbite.restaurant.service.RestaurantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/restaurants")
@Tag(name = "Restaurants", description = "Restaurant profiles and availability")
public class RestaurantController {

    private final RestaurantService service;

    public RestaurantController(RestaurantService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Create a restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Restaurant created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public ResponseEntity<RestaurantResponse> create(
        @Valid @RequestBody CreateRestaurantRequest request,
        UriComponentsBuilder uriBuilder
    ) {
        RestaurantResponse created = service.create(request);
        URI location = uriBuilder.path("/restaurants/{id}")
            .buildAndExpand(created.restaurantId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Retrieve a restaurant by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public RestaurantResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @GetMapping
    @Operation(summary = "List restaurants; optional filters by city and open status")
    @ApiResponse(responseCode = "200", description = "List of restaurants")
    public List<RestaurantResponse> list(
        @RequestParam(required = false) String city,
        @RequestParam(required = false) Boolean isOpen
    ) {
        return service.search(city, isOpen);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace an existing restaurant's details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @io.swagger.v3.oas.annotations.media.Content),
        @ApiResponse(responseCode = "404", description = "Not found", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public RestaurantResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateRestaurantRequest request
    ) {
        return service.update(id, request);
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Toggle a restaurant's open/closed state")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "404", description = "Not found", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public RestaurantResponse setStatus(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateRestaurantStatusRequest request
    ) {
        return service.setStatus(id, request.isOpen());
    }

    @GetMapping("/{id}/availability")
    @Operation(summary = "Lightweight open/closed check (for Order Service)")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Availability"),
        @ApiResponse(responseCode = "404", description = "Not found", content = @io.swagger.v3.oas.annotations.media.Content)
    })
    public AvailabilityResponse availability(@PathVariable UUID id) {
        return service.availability(id);
    }
}
