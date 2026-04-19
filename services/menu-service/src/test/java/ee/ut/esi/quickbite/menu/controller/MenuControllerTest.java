package ee.ut.esi.quickbite.menu.controller;

import ee.ut.esi.quickbite.menu.dto.MenuItemResponse;
import ee.ut.esi.quickbite.menu.dto.ValidateMenuItemsResponse;
import ee.ut.esi.quickbite.menu.exception.MenuItemNotFoundException;
import ee.ut.esi.quickbite.menu.exception.RestaurantNotFoundForMenuException;
import ee.ut.esi.quickbite.menu.security.JwtDevMint;
import ee.ut.esi.quickbite.menu.security.JwtProperties;
import ee.ut.esi.quickbite.menu.security.RestaurantOwnershipClient;
import ee.ut.esi.quickbite.menu.service.MenuService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MenuControllerTest {

    private static final UUID RESTAURANT_ID =
        UUID.fromString("d0000001-0000-0000-0000-000000000001");
    private static final UUID MENU_ITEM_ID =
        UUID.fromString("e0000011-0000-0000-0000-000000000011");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtProperties jwt;

    @MockBean
    private MenuService service;

    @MockBean
    private RestaurantOwnershipClient restaurantOwnership;

    private String customerToken;
    private String ownerToken;
    private String adminToken;

    @BeforeEach
    void setUp() {
        customerToken = JwtDevMint.mint(jwt.secret(), jwt.issuer(), jwt.ttl(),
            JwtDevMint.DEFAULT_CUSTOMER_USER_ID, "dev-customer", "Customer");
        ownerToken = JwtDevMint.mint(jwt.secret(), jwt.issuer(), jwt.ttl(),
            JwtDevMint.DEFAULT_OWNER_USER_ID, "dev-owner", "RestaurantOwner");
        adminToken = JwtDevMint.mint(jwt.secret(), jwt.issuer(), jwt.ttl(),
            JwtDevMint.DEFAULT_ADMIN_USER_ID, "dev-admin", "Admin");
    }

    @Test
    void listForRestaurant_isPublic() throws Exception {
        when(service.listForRestaurant(eq(RESTAURANT_ID), any(), any()))
            .thenReturn(List.of());
        mvc.perform(get("/restaurants/{rid}/menu-items", RESTAURANT_ID))
            .andExpect(status().isOk());
    }

    @Test
    void getMenuItemById_isPublic() throws Exception {
        when(service.findById(MENU_ITEM_ID)).thenReturn(sampleResponse());
        mvc.perform(get("/menu-items/{id}", MENU_ITEM_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Margherita"));
    }

    @Test
    void getMenuItemById_returns404WhenMissing() throws Exception {
        when(service.findById(MENU_ITEM_ID)).thenThrow(new MenuItemNotFoundException(MENU_ITEM_ID));
        mvc.perform(get("/menu-items/{id}", MENU_ITEM_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void createMenuItem_unauthenticatedReturns401() throws Exception {
        mvc.perform(post("/restaurants/{rid}/menu-items", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateBody()))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void createMenuItem_customerTokenReturns403() throws Exception {
        mvc.perform(post("/restaurants/{rid}/menu-items", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(validCreateBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createMenuItem_ownerTokenReturns201() throws Exception {
        when(service.create(eq(RESTAURANT_ID), any())).thenReturn(sampleResponse());
        mvc.perform(post("/restaurants/{rid}/menu-items", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(validCreateBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Margherita"));
    }

    @Test
    void createMenuItem_adminTokenAlsoAccepted() throws Exception {
        when(service.create(eq(RESTAURANT_ID), any())).thenReturn(sampleResponse());
        mvc.perform(post("/restaurants/{rid}/menu-items", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(validCreateBody()))
            .andExpect(status().isCreated());
    }

    @Test
    void createMenuItem_adminUnknownRestaurantReturns404() throws Exception {
        UUID unknownRestaurantId = UUID.fromString("ffffffff-ffff-ffff-ffff-ffffffffffff");
        when(service.create(eq(unknownRestaurantId), any()))
            .thenThrow(new RestaurantNotFoundForMenuException(unknownRestaurantId));
        mvc.perform(post("/restaurants/{rid}/menu-items", unknownRestaurantId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(validCreateBody()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void createMenuItem_zeroPriceReturns400() throws Exception {
        String zeroPriceBody = """
            { "name": "Free lunch",
              "priceAmount": 0,
              "priceCurrency": "EUR",
              "category": "Main" }
            """;
        mvc.perform(post("/restaurants/{rid}/menu-items", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(zeroPriceBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.validationErrors[?(@.field == 'priceAmount')]").exists());
    }

    @Test
    void createMenuItem_missingNameReturns400() throws Exception {
        String invalidBody = """
            { "priceAmount": 5.00,
              "priceCurrency": "EUR",
              "category": "Main" }
            """;
        mvc.perform(post("/restaurants/{rid}/menu-items", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void validate_requiresToken() throws Exception {
        mvc.perform(post("/menu-items/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validValidateBody()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void validate_succeedsWithCustomerToken() throws Exception {
        when(service.validate(any()))
            .thenReturn(new ValidateMenuItemsResponse(true, List.of(), BigDecimal.ZERO, "EUR"));
        mvc.perform(post("/menu-items/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(validValidateBody()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.allValid").value(true))
            .andExpect(jsonPath("$.items").isArray())
            .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void validate_emptyItemsReturns400() throws Exception {
        mvc.perform(post("/menu-items/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content("{\"items\": []}"))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors[?(@.field == 'items')]").exists());
    }

    @Test
    void deleteMenuItem_unauthenticatedReturns401() throws Exception {
        mvc.perform(delete("/menu-items/{id}", MENU_ITEM_ID))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void deleteMenuItem_customerForbidden() throws Exception {
        mvc.perform(delete("/menu-items/{id}", MENU_ITEM_ID)
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isForbidden());
    }

    @Test
    void deleteMenuItem_ownerReturns204() throws Exception {
        mvc.perform(delete("/menu-items/{id}", MENU_ITEM_ID)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNoContent());
    }

    @Test
    void deleteMenuItem_foreignOwnerForbidden() throws Exception {
        doThrow(new AccessDeniedException("User does not own restaurant"))
            .when(service).delete(MENU_ITEM_ID);
        mvc.perform(delete("/menu-items/{id}", MENU_ITEM_ID)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void putMenuItem_foreignOwnerForbidden() throws Exception {
        doThrow(new AccessDeniedException("User does not own restaurant"))
            .when(service).update(eq(MENU_ITEM_ID), any());
        mvc.perform(put("/menu-items/{id}", MENU_ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(validCreateBody()))
            .andExpect(status().isForbidden());
    }

    @Test
    void putMenuItem_adminBypassesOwnership() throws Exception {
        when(service.update(eq(MENU_ITEM_ID), any())).thenReturn(sampleResponse());
        mvc.perform(put("/menu-items/{id}", MENU_ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(validCreateBody()))
            .andExpect(status().isOk());
    }

    @Test
    void deleteMenuItem_notFoundReturns404() throws Exception {
        doThrow(new MenuItemNotFoundException(MENU_ITEM_ID)).when(service).delete(MENU_ITEM_ID);
        mvc.perform(delete("/menu-items/{id}", MENU_ITEM_ID)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isNotFound());
    }

    @Test
    void patchMenuItem_unsupportedMethodReturns405() throws Exception {
        mvc.perform(patch("/menu-items/{id}", MENU_ITEM_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(validCreateBody()))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        mvc.perform(post("/menu-items/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer this.is.not-a-valid-token")
                .content(validValidateBody()))
            .andExpect(status().isUnauthorized());
    }

    private static String validCreateBody() {
        return """
            { "name": "Margherita",
              "description": "Classic tomato and mozzarella",
              "priceAmount": 8.50,
              "priceCurrency": "EUR",
              "category": "Main",
              "isAvailable": true }
            """;
    }

    private static String validValidateBody() {
        return """
            { "items": [
                { "menuItemId": "e0000011-0000-0000-0000-000000000011", "quantity": 2 }
            ] }
            """;
    }

    private static MenuItemResponse sampleResponse() {
        return new MenuItemResponse(
            MENU_ITEM_ID,
            RESTAURANT_ID,
            "Margherita",
            "Classic tomato and mozzarella",
            new BigDecimal("8.50"),
            "EUR",
            "Main",
            true,
            LocalDateTime.parse("2026-04-20T10:00:00"),
            LocalDateTime.parse("2026-04-20T10:00:00")
        );
    }
}
