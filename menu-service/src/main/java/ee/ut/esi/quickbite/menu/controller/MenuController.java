package ee.ut.esi.quickbite.menu.controller;

import ee.ut.esi.quickbite.menu.dto.CreateMenuItemRequest;
import ee.ut.esi.quickbite.menu.dto.MenuItemResponse;
import ee.ut.esi.quickbite.menu.dto.UpdateMenuItemRequest;
import ee.ut.esi.quickbite.menu.dto.ValidateMenuItemsRequest;
import ee.ut.esi.quickbite.menu.dto.ValidateMenuItemsResponse;
import ee.ut.esi.quickbite.menu.service.MenuService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Menu items", description = "Menu items and batch validation for order placement")
public class MenuController {

    private final MenuService service;

    public MenuController(MenuService service) {
        this.service = service;
    }

    @PostMapping("/restaurants/{restaurantId}/menu-items")
    @Operation(summary = "Create a menu item for a restaurant")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Menu item created"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    })
    public ResponseEntity<MenuItemResponse> create(
        @PathVariable UUID restaurantId,
        @Valid @RequestBody CreateMenuItemRequest request,
        UriComponentsBuilder uriBuilder
    ) {
        MenuItemResponse created = service.create(restaurantId, request);
        URI location = uriBuilder.path("/menu-items/{id}")
            .buildAndExpand(created.menuItemId())
            .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/restaurants/{restaurantId}/menu-items")
    @Operation(summary = "List menu items for a restaurant; optional category and availability filters")
    @ApiResponse(responseCode = "200", description = "List of menu items")
    public List<MenuItemResponse> listForRestaurant(
        @PathVariable UUID restaurantId,
        @RequestParam(required = false) String category,
        @RequestParam(required = false) Boolean available
    ) {
        return service.listForRestaurant(restaurantId, category, available);
    }

    @GetMapping("/menu-items/{id}")
    @Operation(summary = "Retrieve a menu item by id")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Found"),
        @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public MenuItemResponse get(@PathVariable UUID id) {
        return service.findById(id);
    }

    @PutMapping("/menu-items/{id}")
    @Operation(summary = "Replace a menu item's details")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Updated"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content),
        @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public MenuItemResponse update(
        @PathVariable UUID id,
        @Valid @RequestBody UpdateMenuItemRequest request
    ) {
        return service.update(id, request);
    }

    @DeleteMapping("/menu-items/{id}")
    @Operation(summary = "Delete a menu item")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "Deleted"),
        @ApiResponse(responseCode = "404", description = "Not found", content = @Content)
    })
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/menu-items/validate")
    @Operation(summary = "Batch-validate menu items (existence, availability, pricing) for Order Service")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Per-item validation results"),
        @ApiResponse(responseCode = "400", description = "Validation failed", content = @Content)
    })
    public ValidateMenuItemsResponse validate(@Valid @RequestBody ValidateMenuItemsRequest request) {
        return service.validate(request);
    }
}
