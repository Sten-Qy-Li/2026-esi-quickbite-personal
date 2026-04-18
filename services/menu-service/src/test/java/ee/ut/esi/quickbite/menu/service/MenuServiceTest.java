package ee.ut.esi.quickbite.menu.service;

import ee.ut.esi.quickbite.menu.domain.MenuItem;
import ee.ut.esi.quickbite.menu.domain.Price;
import ee.ut.esi.quickbite.menu.dto.CreateMenuItemRequest;
import ee.ut.esi.quickbite.menu.dto.MenuItemResponse;
import ee.ut.esi.quickbite.menu.dto.UpdateMenuItemRequest;
import ee.ut.esi.quickbite.menu.dto.ValidateMenuItemsRequest;
import ee.ut.esi.quickbite.menu.dto.ValidateMenuItemsResponse;
import ee.ut.esi.quickbite.menu.exception.InvalidPriceException;
import ee.ut.esi.quickbite.menu.exception.MenuItemNotFoundException;
import ee.ut.esi.quickbite.menu.repository.MenuItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MenuServiceTest {

    private static final UUID RESTAURANT_ID =
        UUID.fromString("d0000001-0000-0000-0000-000000000001");

    @Mock
    private MenuItemRepository menuItems;

    @InjectMocks
    private MenuService service;

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
            "New Name", "New desc", new BigDecimal("9.90"), "EUR", "Main", false
        ));

        assertThat(updated.name()).isEqualTo("New Name");
        assertThat(updated.priceAmount()).isEqualByComparingTo("9.90");
        assertThat(updated.isAvailable()).isFalse();
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
