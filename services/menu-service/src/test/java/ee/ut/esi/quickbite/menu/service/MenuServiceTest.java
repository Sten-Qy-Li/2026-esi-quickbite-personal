package ee.ut.esi.quickbite.menu.service;

import ee.ut.esi.quickbite.menu.domain.MenuItem;
import ee.ut.esi.quickbite.menu.domain.Price;
import ee.ut.esi.quickbite.menu.dto.CreateMenuItemRequest;
import ee.ut.esi.quickbite.menu.dto.MenuItemResponse;
import ee.ut.esi.quickbite.menu.dto.UpdateMenuItemRequest;
import ee.ut.esi.quickbite.menu.dto.ValidateMenuItemsRequest;
import ee.ut.esi.quickbite.menu.dto.ValidateMenuItemsResponse;
import ee.ut.esi.quickbite.menu.events.AvailabilityChangedEvent;
import ee.ut.esi.quickbite.menu.events.MenuEventPublisher;
import ee.ut.esi.quickbite.menu.exception.InvalidPriceException;
import ee.ut.esi.quickbite.menu.exception.MenuItemNotFoundException;
import ee.ut.esi.quickbite.menu.exception.RestaurantNotFoundForMenuException;
import ee.ut.esi.quickbite.menu.repository.MenuItemRepository;
import ee.ut.esi.quickbite.menu.security.AuthenticatedUser;
import ee.ut.esi.quickbite.menu.security.CurrentUser;
import ee.ut.esi.quickbite.menu.security.RestaurantOwnershipClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    private static final UUID RESTAURANT_ID =
        UUID.fromString("d0000001-0000-0000-0000-000000000001");
    private static final UUID OWNER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID OTHER_OWNER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID ADMIN_ID =
        UUID.fromString("00000000-0000-0000-0000-0000000000a1");

    private static final Instant FIXED_NOW = Instant.parse("2026-04-19T12:00:00Z");

    @Mock
    private MenuItemRepository menuItems;

    @Mock
    private CurrentUser currentUser;

    @Mock
    private RestaurantOwnershipClient restaurantOwnership;

    @Mock
    private MenuEventPublisher menuEventPublisher;

    private MenuService service;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);
        service = new MenuService(menuItems, currentUser, restaurantOwnership,
            menuEventPublisher, fixedClock);
        lenient().when(currentUser.require()).thenReturn(
            new AuthenticatedUser(OWNER_ID, "RestaurantOwner", "USER", null));
        lenient().when(restaurantOwnership.findOwnerId(RESTAURANT_ID))
            .thenReturn(Optional.of(OWNER_ID));
    }

    @Test
    void create_persistsItemWithDefaultAvailable() {
        when(menuItems.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemResponse response = service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "Margherita", "Classic tomato and mozzarella",
            new BigDecimal("8.50"), "EUR", "Main", null
        ));

        assertThat(response.name()).isEqualTo("Margherita");
        assertThat(response.restaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(response.priceAmount()).isEqualByComparingTo("8.50");
        assertThat(response.priceCurrency()).isEqualTo("EUR");
        assertThat(response.isAvailable()).isTrue();
    }

    @Test
    void create_priceAmountZero_throwsInvalidPriceException() {
        assertThatThrownBy(() -> service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "Free lunch", null, BigDecimal.ZERO, "EUR", "Main", true
        ))).isInstanceOf(InvalidPriceException.class);
    }

    @Test
    void create_priceAmountNegative_throwsInvalidPriceException() {
        assertThatThrownBy(() -> service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "Negative", null, new BigDecimal("-1.00"), "EUR", "Main", true
        ))).isInstanceOf(InvalidPriceException.class);
    }

    @Test
    void create_priceScaleThreeDecimals_throwsInvalidPriceException() {
        assertThatThrownBy(() -> service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "TooPrecise", null, new BigDecimal("1.234"), "EUR", "Main", true
        ))).isInstanceOf(InvalidPriceException.class);
    }

    @Test
    void findById_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(menuItems.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findById(id))
            .isInstanceOf(MenuItemNotFoundException.class);
    }

    @Test
    void update_appliesDetailsAndReturnsResponse() {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem(RESTAURANT_ID, "Old",
            "desc", new Price(new BigDecimal("5.00"), "EUR"), "Main", true);
        when(menuItems.findById(id)).thenReturn(Optional.of(existing));

        MenuItemResponse updated = service.update(id, new UpdateMenuItemRequest(
            "New Name", "New desc", new BigDecimal("9.90"), "EUR", "Main", true
        ));

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.priceAmount()).isEqualByComparingTo("9.90");
        assertThat(updated.isAvailable()).isTrue();
        verifyNoInteractions(menuEventPublisher);
    }

    @Test
    void update_publishesAvailabilityChangedOnTrueToFalse() {
        UUID id = UUID.fromString("e0000011-0000-0000-0000-000000000011");
        MenuItem existing = new MenuItem(RESTAURANT_ID, "Margherita",
            "desc", new Price(new BigDecimal("8.50"), "EUR"), "Main", true);
        when(menuItems.findById(id)).thenReturn(Optional.of(existing));

        service.update(id, new UpdateMenuItemRequest(
            "Margherita", "desc", new BigDecimal("8.50"), "EUR", "Main", false
        ));

        ArgumentCaptor<AvailabilityChangedEvent> captor =
            ArgumentCaptor.forClass(AvailabilityChangedEvent.class);
        verify(menuEventPublisher).publishAvailabilityChanged(captor.capture());
        AvailabilityChangedEvent event = captor.getValue();
        assertThat(event.menuItemId()).isEqualTo(existing.getMenuItemId());
        assertThat(event.restaurantId()).isEqualTo(RESTAURANT_ID);
        assertThat(event.isAvailable()).isFalse();
        assertThat(event.previousIsAvailable()).isTrue();
        assertThat(event.occurredAt()).isEqualTo(FIXED_NOW);
    }

    @Test
    void update_publishesAvailabilityChangedOnFalseToTrue() {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem(RESTAURANT_ID, "Soup of the day",
            "desc", new Price(new BigDecimal("4.00"), "EUR"), "Main", false);
        when(menuItems.findById(id)).thenReturn(Optional.of(existing));

        service.update(id, new UpdateMenuItemRequest(
            "Soup of the day", "desc", new BigDecimal("4.00"), "EUR", "Main", true
        ));

        ArgumentCaptor<AvailabilityChangedEvent> captor =
            ArgumentCaptor.forClass(AvailabilityChangedEvent.class);
        verify(menuEventPublisher).publishAvailabilityChanged(captor.capture());
        AvailabilityChangedEvent event = captor.getValue();
        assertThat(event.isAvailable()).isTrue();
        assertThat(event.previousIsAvailable()).isFalse();
    }

    @Test
    void update_doesNotPublishWhenOnlyNonAvailabilityFieldsChange() {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem(RESTAURANT_ID, "Fries",
            "desc", new Price(new BigDecimal("3.00"), "EUR"), "Main", true);
        when(menuItems.findById(id)).thenReturn(Optional.of(existing));

        service.update(id, new UpdateMenuItemRequest(
            "Curly Fries", "crunchier", new BigDecimal("3.50"), "EUR", "Main", true
        ));

        verifyNoInteractions(menuEventPublisher);
    }

    @Test
    void update_publisherFailureDoesNotFailRequest() {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem(RESTAURANT_ID, "Sold out",
            "desc", new Price(new BigDecimal("6.00"), "EUR"), "Main", false);
        when(menuItems.findById(id)).thenReturn(Optional.of(existing));
        doThrow(new RuntimeException("broker unreachable"))
            .when(menuEventPublisher).publishAvailabilityChanged(any());

        MenuItemResponse response = service.update(id, new UpdateMenuItemRequest(
            "Back in stock", "desc", new BigDecimal("6.00"), "EUR", "Main", true
        ));

        assertThat(response.isAvailable()).isTrue();
        verify(menuEventPublisher).publishAvailabilityChanged(any());
    }

    @Test
    void create_doesNotPublishAvailabilityEvent() {
        when(menuItems.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "Margherita", "Classic", new BigDecimal("8.50"), "EUR", "Main", true));

        verify(menuEventPublisher, never()).publishAvailabilityChanged(any());
    }

    @Test
    void delete_doesNotPublishAvailabilityEvent() {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem(RESTAURANT_ID, "X", null,
            new Price(new BigDecimal("5.00"), "EUR"), "Main", true);
        when(menuItems.findById(id)).thenReturn(Optional.of(existing));

        service.delete(id);

        verify(menuEventPublisher, never()).publishAvailabilityChanged(any());
    }

    @Test
    void update_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(menuItems.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new UpdateMenuItemRequest(
            "X", null, new BigDecimal("1.00"), "EUR", "Main", true
        ))).isInstanceOf(MenuItemNotFoundException.class);
    }

    @Test
    void delete_throwsWhenMissing() {
        UUID id = UUID.randomUUID();
        when(menuItems.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(id))
            .isInstanceOf(MenuItemNotFoundException.class);
    }

    @Test
    void create_deniedWhenCallerIsNotOwnerOrAdmin() {
        when(currentUser.require()).thenReturn(
            new AuthenticatedUser(OTHER_OWNER_ID, "RestaurantOwner", "USER", null));
        when(restaurantOwnership.findOwnerId(RESTAURANT_ID)).thenReturn(Optional.of(OWNER_ID));

        assertThatThrownBy(() -> service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "Margherita", null, new BigDecimal("8.50"), "EUR", "Main", true
        ))).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void create_failsWhenOwningRestaurantUnknown() {
        when(restaurantOwnership.findOwnerId(RESTAURANT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "Margherita", null, new BigDecimal("8.50"), "EUR", "Main", true
        ))).isInstanceOf(RestaurantNotFoundForMenuException.class);
    }

    @Test
    void create_adminBypassesOwnershipCheck() {
        when(currentUser.require()).thenReturn(
            new AuthenticatedUser(ADMIN_ID, "Admin", "USER", null));
        when(menuItems.save(any(MenuItem.class))).thenAnswer(inv -> inv.getArgument(0));

        MenuItemResponse response = service.create(RESTAURANT_ID, new CreateMenuItemRequest(
            "Admin Added", null, new BigDecimal("5.00"), "EUR", "Main", true
        ));

        assertThat(response.name()).isEqualTo("Admin Added");
    }

    @Test
    void delete_deniedWhenCallerIsNotOwnerOrAdmin() {
        UUID id = UUID.randomUUID();
        MenuItem existing = new MenuItem(RESTAURANT_ID, "X", null,
            new Price(new BigDecimal("5.00"), "EUR"), "Main", true);
        when(menuItems.findById(id)).thenReturn(Optional.of(existing));
        when(currentUser.require()).thenReturn(
            new AuthenticatedUser(OTHER_OWNER_ID, "RestaurantOwner", "USER", null));
        when(restaurantOwnership.findOwnerId(RESTAURANT_ID)).thenReturn(Optional.of(OWNER_ID));

        assertThatThrownBy(() -> service.delete(id))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void validate_mixesFoundMissingAndUnavailable() {
        MenuItem available = new MenuItem(RESTAURANT_ID, "Margherita", null,
            new Price(new BigDecimal("8.50"), "EUR"), "Main", true);
        MenuItem unavailable = new MenuItem(RESTAURANT_ID, "Sold out", null,
            new Price(new BigDecimal("4.00"), "EUR"), "Dessert", false);
        UUID missingId = UUID.randomUUID();

        when(menuItems.findAllByMenuItemIdIn(anySet()))
            .thenReturn(List.of(available, unavailable));

        ValidateMenuItemsResponse response = service.validate(new ValidateMenuItemsRequest(List.of(
            new ValidateMenuItemsRequest.Line(available.getMenuItemId(), 2),
            new ValidateMenuItemsRequest.Line(unavailable.getMenuItemId(), 1),
            new ValidateMenuItemsRequest.Line(missingId, 3)
        )));

        assertThat(response.allValid()).isFalse();
        assertThat(response.items()).hasSize(3);
        assertThat(response.totalAmount()).isEqualByComparingTo("17.00");
        assertThat(response.currency()).isEqualTo("EUR");

        ValidateMenuItemsResponse.Line firstLine = response.items().get(0);
        assertThat(firstLine.exists()).isTrue();
        assertThat(firstLine.isAvailable()).isTrue();
        assertThat(firstLine.lineTotal()).isEqualByComparingTo("17.00");
        assertThat(firstLine.error()).isNull();

        ValidateMenuItemsResponse.Line secondLine = response.items().get(1);
        assertThat(secondLine.exists()).isTrue();
        assertThat(secondLine.isAvailable()).isFalse();
        assertThat(secondLine.error()).isEqualTo("MENU_ITEM_NOT_AVAILABLE");

        ValidateMenuItemsResponse.Line thirdLine = response.items().get(2);
        assertThat(thirdLine.exists()).isFalse();
        assertThat(thirdLine.error()).isEqualTo("MENU_ITEM_NOT_FOUND");
    }

    @Test
    void validate_allAvailable_reportsAllValid() {
        MenuItem a = new MenuItem(RESTAURANT_ID, "A", null,
            new Price(new BigDecimal("3.00"), "EUR"), "Main", true);
        MenuItem b = new MenuItem(RESTAURANT_ID, "B", null,
            new Price(new BigDecimal("2.50"), "EUR"), "Drink", true);
        when(menuItems.findAllByMenuItemIdIn(anySet()))
            .thenReturn(List.of(a, b));

        ValidateMenuItemsResponse response = service.validate(new ValidateMenuItemsRequest(List.of(
            new ValidateMenuItemsRequest.Line(a.getMenuItemId(), 1),
            new ValidateMenuItemsRequest.Line(b.getMenuItemId(), 4)
        )));

        assertThat(response.allValid()).isTrue();
        assertThat(response.items()).allMatch(l -> l.exists() && l.isAvailable());
        assertThat(response.totalAmount()).isEqualByComparingTo("13.00");
        assertThat(response.currency()).isEqualTo("EUR");
    }
}
