package ee.ut.esi.quickbite.restaurant.controller;

import ee.ut.esi.quickbite.restaurant.dto.AvailabilityResponse;
import ee.ut.esi.quickbite.restaurant.dto.RestaurantResponse;
import ee.ut.esi.quickbite.restaurant.exception.DuplicateRestaurantException;
import ee.ut.esi.quickbite.restaurant.exception.RestaurantNotFoundException;
import ee.ut.esi.quickbite.restaurant.security.JwtDevMint;
import ee.ut.esi.quickbite.restaurant.security.JwtProperties;
import ee.ut.esi.quickbite.restaurant.service.RestaurantService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verifyNoInteractions;
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
class RestaurantControllerTest {

    private static final UUID RESTAURANT_ID =
        UUID.fromString("d0000001-0000-0000-0000-000000000001");

    @Autowired
    private MockMvc mvc;

    @Autowired
    private JwtProperties jwt;

    @MockBean
    private RestaurantService service;

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
    void listRestaurants_isPublic() throws Exception {
        Page<RestaurantResponse> empty = new PageImpl<>(List.of(), Pageable.ofSize(20), 0);
        when(service.search(eq(null), eq(null), any(Pageable.class))).thenReturn(empty);
        mvc.perform(get("/restaurants"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.pageable").exists());
    }

    @Test
    void getRestaurantById_isPublic() throws Exception {
        when(service.findById(RESTAURANT_ID)).thenReturn(sampleResponse());
        mvc.perform(get("/restaurants/{id}", RESTAURANT_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Pizza Antonio"));
    }

    @Test
    void getRestaurantById_returns404WhenMissing() throws Exception {
        when(service.findById(RESTAURANT_ID)).thenThrow(new RestaurantNotFoundException(RESTAURANT_ID));
        mvc.perform(get("/restaurants/{id}", RESTAURANT_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    void availability_requiresToken() throws Exception {
        mvc.perform(get("/restaurants/{id}/availability", RESTAURANT_ID))
            .andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void availability_succeedsWithCustomerToken() throws Exception {
        when(service.availability(RESTAURANT_ID))
            .thenReturn(new AvailabilityResponse(
                RESTAURANT_ID, true, true, "11:00-22:00",
                Instant.parse("2026-05-05T12:34:56Z")
            ));
        mvc.perform(get("/restaurants/{id}/availability", RESTAURANT_ID)
                .header("Authorization", "Bearer " + customerToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isOpen").value(true))
            .andExpect(jsonPath("$.acceptsOrders").value(true))
            .andExpect(jsonPath("$.operatingHours").value("11:00-22:00"))
            .andExpect(jsonPath("$.checkedAt").exists());
    }

    @Test
    void createRestaurant_unauthenticatedReturns401() throws Exception {
        mvc.perform(post("/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .content(validCreateBody()))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void createRestaurant_customerTokenReturns403() throws Exception {
        mvc.perform(post("/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content(validCreateBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void createRestaurant_ownerTokenReturns201() throws Exception {
        when(service.create(any())).thenReturn(sampleResponse());
        mvc.perform(post("/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(validCreateBody()))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.name").value("Pizza Antonio"));
    }

    @Test
    void createRestaurant_adminTokenAlsoAccepted() throws Exception {
        when(service.create(any())).thenReturn(sampleResponse());
        mvc.perform(post("/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content(validCreateBody()))
            .andExpect(status().isCreated());
    }

    @Test
    void createRestaurant_missingNameReturns400() throws Exception {
        String invalidBody = """
            { "address": "Ruutli 12", "city": "Tartu",
              "latitude": 58.37, "longitude": 26.72,
              "operatingHours": "11:00-22:00" }
            """;
        mvc.perform(post("/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(invalidBody))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors[?(@.field == 'name')]").exists());
    }

    @Test
    void createRestaurant_impossibleOperatingHoursReturns400() throws Exception {
        mvc.perform(post("/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(bodyWithHours("99:99-99:99")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors[?(@.field == 'operatingHours')]").exists());
        verifyNoInteractions(service);
    }

    @Test
    void createRestaurant_outOfRangeOperatingHoursReturns400() throws Exception {
        mvc.perform(post("/restaurants")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(bodyWithHours("24:00-24:00")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors[?(@.field == 'operatingHours')]").exists());
        verifyNoInteractions(service);
    }

    @Test
    void invalidJwtReturns401() throws Exception {
        mvc.perform(get("/restaurants/{id}/availability", RESTAURANT_ID)
                .header("Authorization", "Bearer this.is.not-a-valid-token"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void patchStatus_customerForbidden() throws Exception {
        mvc.perform(patch("/restaurants/{id}/status", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + customerToken)
                .content("{\"isOpen\": true}"))
            .andExpect(status().isForbidden());
    }

    @Test
    void putRestaurant_foreignOwnerForbidden() throws Exception {
        doThrow(new org.springframework.security.access.AccessDeniedException(
            "User does not own restaurant " + RESTAURANT_ID))
            .when(service).update(eq(RESTAURANT_ID), any());
        mvc.perform(put("/restaurants/{id}", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(validCreateBody()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    void putRestaurant_duplicateNameForSameOwnerReturns409() throws Exception {
        doThrow(new DuplicateRestaurantException(JwtDevMint.DEFAULT_OWNER_USER_ID, "Pizza Antonio"))
            .when(service).update(eq(RESTAURANT_ID), any());
        mvc.perform(put("/restaurants/{id}", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(validCreateBody()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.error").value("Conflict"));
    }

    @Test
    void deleteRestaurant_unsupportedMethodReturns405() throws Exception {
        mvc.perform(delete("/restaurants/{id}", RESTAURANT_ID)
                .header("Authorization", "Bearer " + ownerToken))
            .andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void putRestaurant_outOfRangeOperatingHoursReturns400() throws Exception {
        mvc.perform(put("/restaurants/{id}", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + ownerToken)
                .content(bodyWithHours("29:59-29:59")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.validationErrors[?(@.field == 'operatingHours')]").exists());
        verifyNoInteractions(service);
    }

    @Test
    void patchStatus_adminBypassesOwnership() throws Exception {
        when(service.setStatus(eq(RESTAURANT_ID), eq(true))).thenReturn(sampleResponse());
        mvc.perform(patch("/restaurants/{id}/status", RESTAURANT_ID)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + adminToken)
                .content("{\"isOpen\": true}"))
            .andExpect(status().isOk());
    }

    private static String validCreateBody() {
        return bodyWithHours("11:00-22:00");
    }

    private static String bodyWithHours(String operatingHours) {
        return """
            { "name": "Pizza Antonio",
              "address": "Ruutli 12",
              "city": "Tartu",
              "latitude": 58.3776,
              "longitude": 26.7290,
              "operatingHours": "%s" }
            """.formatted(operatingHours);
    }

    private static RestaurantResponse sampleResponse() {
        return new RestaurantResponse(
            RESTAURANT_ID,
            JwtDevMint.DEFAULT_OWNER_USER_ID,
            "Pizza Antonio",
            "Ruutli 12",
            "Tartu",
            58.3776,
            26.7290,
            "11:00-22:00",
            false,
            LocalDateTime.parse("2026-04-20T10:00:00"),
            LocalDateTime.parse("2026-04-20T10:00:00")
        );
    }
}
