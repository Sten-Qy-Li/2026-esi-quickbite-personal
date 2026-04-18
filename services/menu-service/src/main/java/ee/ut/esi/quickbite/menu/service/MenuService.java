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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);
    private static final Set<String> KNOWN_CATEGORIES =
        Set.of("Appetizer", "Main", "Dessert", "Drink");

    private final MenuItemRepository menuItems;

    public MenuService(MenuItemRepository menuItems) {
        this.menuItems = menuItems;
    }

    @Transactional
    public MenuItemResponse create(UUID restaurantId, CreateMenuItemRequest req) {
        validatePrice(req.priceAmount());
        warnIfUnknownCategory(req.category());
        boolean available = req.isAvailable() == null ? true : req.isAvailable();
        Price price = new Price(req.priceAmount(), req.priceCurrency());
        MenuItem saved = menuItems.save(new MenuItem(
            restaurantId, req.name(), req.description(), price, req.category(), available
        ));
        return MenuItemResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<MenuItemResponse> listForRestaurant(UUID restaurantId, String category, Boolean available) {
        return menuItems.searchForRestaurant(restaurantId, category, available).stream()
            .map(MenuItemResponse::from)
            .toList();
    }

    @Transactional(readOnly = true)
    public MenuItemResponse findById(UUID id) {
        return MenuItemResponse.from(requireMenuItem(id));
    }

    @Transactional
    public MenuItemResponse update(UUID id, UpdateMenuItemRequest req) {
        MenuItem m = requireMenuItem(id);
        validatePrice(req.priceAmount());
        warnIfUnknownCategory(req.category());
        Price price = new Price(req.priceAmount(), req.priceCurrency());
        m.updateDetails(req.name(), req.description(), price, req.category(), req.isAvailable());
        return MenuItemResponse.from(m);
    }

    @Transactional
    public void delete(UUID id) {
        MenuItem m = requireMenuItem(id);
        menuItems.delete(m);
    }

    @Transactional(readOnly = true)
    public ValidateMenuItemsResponse validate(ValidateMenuItemsRequest req) {
        Set<UUID> ids = req.items().stream()
            .map(ValidateMenuItemsRequest.Line::menuItemId)
            .collect(Collectors.toSet());

        Map<UUID, MenuItem> byId = menuItems.findAllByMenuItemIdIn(ids).stream()
            .collect(Collectors.toMap(MenuItem::getMenuItemId, m -> m, (a, b) -> a, LinkedHashMap::new));

        List<ValidateMenuItemsResponse.Line> lines = req.items().stream().map(line -> {
            MenuItem m = byId.get(line.menuItemId());
            if (m == null) {
                return new ValidateMenuItemsResponse.Line(
                    line.menuItemId(), line.quantity(),
                    false, false, null, null, null, "not_found"
                );
            }
            if (!m.isAvailable()) {
                return new ValidateMenuItemsResponse.Line(
                    m.getMenuItemId(), line.quantity(),
                    true, false,
                    m.getPrice().getAmount(), m.getPrice().getCurrency(),
                    null, "not_available"
                );
            }
            BigDecimal lineTotal = m.getPrice().getAmount().multiply(BigDecimal.valueOf(line.quantity()));
            return new ValidateMenuItemsResponse.Line(
                m.getMenuItemId(), line.quantity(),
                true, true,
                m.getPrice().getAmount(), m.getPrice().getCurrency(),
                lineTotal, null
            );
        }).toList();

        boolean allValid = lines.stream().allMatch(l -> l.exists() && l.available());
        return new ValidateMenuItemsResponse(allValid, lines);
    }

    private static void validatePrice(BigDecimal amount) {
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidPriceException(amount, "must be greater than 0");
        }
        if (amount.stripTrailingZeros().scale() > 2) {
            throw new InvalidPriceException(amount, "must have at most 2 decimal places");
        }
    }

    private static void warnIfUnknownCategory(String category) {
        if (category != null && !KNOWN_CATEGORIES.contains(category) && log.isDebugEnabled()) {
            log.debug("Menu item created with unknown category '{}' (allowed: {})", category, KNOWN_CATEGORIES);
        }
    }

    private MenuItem requireMenuItem(UUID id) {
        return menuItems.findById(id).orElseThrow(() -> new MenuItemNotFoundException(id));
    }
}
