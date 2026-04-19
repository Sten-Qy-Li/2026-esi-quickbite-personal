package ee.ut.esi.quickbite.restaurant.service;

import ee.ut.esi.quickbite.restaurant.domain.Location;
import ee.ut.esi.quickbite.restaurant.domain.Restaurant;
import ee.ut.esi.quickbite.restaurant.dto.CreateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.dto.RestaurantResponse;
import ee.ut.esi.quickbite.restaurant.dto.UpdateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.exception.DuplicateRestaurantException;
import ee.ut.esi.quickbite.restaurant.exception.RestaurantNotFoundException;
import ee.ut.esi.quickbite.restaurant.repository.RestaurantRepository;
import ee.ut.esi.quickbite.restaurant.security.AuthenticatedUser;
import ee.ut.esi.quickbite.restaurant.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestaurantServiceTest {

    private static final UUID OWNER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Mock
    private RestaurantRepository restaurants;

    @Mock
    private CurrentUser currentUser;

    private RestaurantService service;

    private AuthenticatedUser ownerPrincipal;

    @BeforeEach
    void setUp() {
        ownerPrincipal = new AuthenticatedUser(OWNER_ID, "RestaurantOwner", "USER", null);
        Clock fixedClock = Clock.fixed(Instant.parse("2026-05-05T12:34:56Z"), ZoneOffset.UTC);
        service = new RestaurantService(restaurants, currentUser, fixedClock);
    }

    @Test
    void create_persistsRestaurantUsingCurrentUserAsOwner() {
        when(currentUser.require()).thenReturn(ownerPrincipal);
        when(restaurants.existsByOwnerIdAndNameIgnoreCase(OWNER_ID, "Pizza Antonio")).thenReturn(false);
        when(restaurants.save(any(Restaurant.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RestaurantResponse response = service.create(new CreateRestaurantRequest(
            "Pizza Antonio", "Ruutli 12", "Tartu", 58.3776, 26.7290, "11:00-22:00"
        ));

        assertThat(response.name()).isEqualTo("Pizza Antonio");
        assertThat(response.ownerId()).isEqualTo(OWNER_ID);
        assertThat(response.isOpen()).isFalse();
    }

    @Test
    void create_rejectsDuplicateNameForSameOwner() {
        when(currentUser.require()).thenReturn(ownerPrincipal);
        when(restaurants.existsByOwnerIdAndNameIgnoreCase(OWNER_ID, "Pizza Antonio")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new CreateRestaurantRequest(
            "Pizza Antonio", "Ruutli 12", "Tartu", 58.3776, 26.7290, "11:00-22:00"
        ))).isInstanceOf(DuplicateRestaurantException.class);
    }

    @Test
    void findById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(restaurants.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
            .isInstanceOf(RestaurantNotFoundException.class);
    }

    @Test
    void update_appliesDetailsAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        Restaurant existing = new Restaurant(OWNER_ID, "Old Name",
            new Location("Addr", "Tartu", 58.0, 26.0), "10:00-20:00");
        when(restaurants.findById(eq(id))).thenReturn(Optional.of(existing));
        when(currentUser.require()).thenReturn(ownerPrincipal);

        RestaurantResponse updated = service.update(id, new UpdateRestaurantRequest(
            "New Name", "Addr 2", "Tartu", 58.1, 26.1, "11:00-23:00"
        ));

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.city()).isEqualTo("Tartu");
        assertThat(updated.operatingHours()).isEqualTo("11:00-23:00");
    }

    @Test
    void update_deniedWhenCallerIsNotOwnerOrAdmin() {
        UUID id = UUID.randomUUID();
        Restaurant existing = new Restaurant(OWNER_ID, "Old Name",
            new Location("Addr", "Tartu", 58.0, 26.0), "10:00-20:00");
        when(restaurants.findById(eq(id))).thenReturn(Optional.of(existing));
        AuthenticatedUser otherOwner = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "RestaurantOwner", "USER", null);
        when(currentUser.require()).thenReturn(otherOwner);

        assertThatThrownBy(() -> service.update(id, new UpdateRestaurantRequest(
            "New Name", "Addr 2", "Tartu", 58.1, 26.1, "11:00-23:00"
        ))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void update_adminPassesOwnershipCheck() {
        UUID id = UUID.randomUUID();
        Restaurant existing = new Restaurant(OWNER_ID, "Old Name",
            new Location("Addr", "Tartu", 58.0, 26.0), "10:00-20:00");
        when(restaurants.findById(eq(id))).thenReturn(Optional.of(existing));
        AuthenticatedUser admin = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-0000000000a1"),
            "Admin", "USER", null);
        when(currentUser.require()).thenReturn(admin);

        RestaurantResponse updated = service.update(id, new UpdateRestaurantRequest(
            "Admin Rename", "Addr", "Tartu", 58.0, 26.0, "11:00-22:00"
        ));

        assertThat(updated.name()).isEqualTo("Admin Rename");
    }

    @Test
    void setStatus_deniedWhenCallerIsNotOwnerOrAdmin() {
        UUID id = UUID.randomUUID();
        Restaurant existing = new Restaurant(OWNER_ID, "Old Name",
            new Location("Addr", "Tartu", 58.0, 26.0), "10:00-20:00");
        when(restaurants.findById(eq(id))).thenReturn(Optional.of(existing));
        AuthenticatedUser otherOwner = new AuthenticatedUser(
            UUID.fromString("00000000-0000-0000-0000-000000000002"),
            "RestaurantOwner", "USER", null);
        when(currentUser.require()).thenReturn(otherOwner);

        assertThatThrownBy(() -> service.setStatus(id, true))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void availability_returnsOperatingHoursAndOpenFlag() {
        UUID id = UUID.randomUUID();
        Restaurant existing = new Restaurant(OWNER_ID, "Pizza Antonio",
            new Location("Addr", "Tartu", 58.0, 26.0), "11:00-22:00");
        existing.setStatus(true);
        when(restaurants.findById(id)).thenReturn(Optional.of(existing));

        var availability = service.availability(id);

        assertThat(availability.isOpen()).isTrue();
        assertThat(availability.operatingHours()).isEqualTo("11:00-22:00");
    }

    @Test
    void availability_malformedOperatingHours_doesNotAcceptOrders() {
        UUID id = UUID.randomUUID();
        Restaurant existing = new Restaurant(OWNER_ID, "Bad Data",
            new Location("Addr", "Tartu", 58.0, 26.0), "99:99-99:99");
        existing.setStatus(true);
        when(restaurants.findById(id)).thenReturn(Optional.of(existing));

        var availability = service.availability(id);

        assertThat(availability.isOpen()).isTrue();
        assertThat(availability.acceptsOrders()).isFalse();
    }

    @Test
    void availability_unparseableOperatingHours_doesNotAcceptOrders() {
        UUID id = UUID.randomUUID();
        Restaurant existing = new Restaurant(OWNER_ID, "Bad Data",
            new Location("Addr", "Tartu", 58.0, 26.0), "not-a-time");
        existing.setStatus(true);
        when(restaurants.findById(id)).thenReturn(Optional.of(existing));

        var availability = service.availability(id);

        assertThat(availability.isOpen()).isTrue();
        assertThat(availability.acceptsOrders()).isFalse();
    }
}
