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
import ee.ut.esi.quickbite.menu.exception.MixedCurrencyException;
import ee.ut.esi.quickbite.menu.exception.RestaurantNotFoundForMenuException;
import ee.ut.esi.quickbite.menu.repository.MenuItemRepository;
import ee.ut.esi.quickbite.menu.security.AuthenticatedUser;
import ee.ut.esi.quickbite.menu.security.CurrentUser;
import ee.ut.esi.quickbite.menu.security.RestaurantOwnershipClient;
import ee.ut.esi.quickbite.menu.security.SecurityRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class MenuService {

    private static final Logger log = LoggerFactory.getLogger(MenuService.class);
    private static final Set<String> KNOWN_CATEGORIES =
        Set.of("Appetizer", "Main", "Dessert", "Drink");

    private final MenuItemRepository menuItems;
    private final CurrentUser currentUser;
    private final RestaurantOwnershipClient restaurantOwnership;
    private final MenuEventPublisher menuEventPublisher;
    private final Clock clock;

    @Autowired
    public MenuService(MenuItemRepository menuItems,
                       CurrentUser currentUser,
                       RestaurantOwnershipClient restaurantOwnership,
                       MenuEventPublisher menuEventPublisher) {
        this(menuItems, currentUser, restaurantOwnership, menuEventPublisher,
            Clock.system(ZoneOffset.UTC));
    }

    MenuService(MenuItemRepository menuItems,
                CurrentUser currentUser,
                RestaurantOwnershipClient restaurantOwnership,
                MenuEventPublisher menuEventPublisher,
                Clock clock) {
        this.menuItems = menuItems;
        this.currentUser = currentUser;
        this.restaurantOwnership = restaurantOwnership;
        this.menuEventPublisher = menuEventPublisher;
        this.clock = clock;
    }

    @Transactional
    public MenuItemResponse create(UUID restaurantId, CreateMenuItemRequest req) {
        UUID ownerId = restaurantOwnership.findOwnerId(restaurantId)
            .orElseThrow(() -> new RestaurantNotFoundForMenuException(restaurantId));
        requireOwnerOrAdmin(restaurantId, ownerId,
            "POST /restaurants/" + restaurantId + "/menu-items");
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
        requireOwnerOrAdmin(m.getRestaurantId(), "PUT /menu-items/" + id);
        validatePrice(req.priceAmount());
        warnIfUnknownCategory(req.category());
        boolean previousAvailability = m.isAvailable();
        Price price = new Price(req.priceAmount(), req.priceCurrency());
        m.updateDetails(req.name(), req.description(), price, req.category(), req.isAvailable());
        if (previousAvailability != m.isAvailable()) {
            publishAvailabilityChanged(m, previousAvailability);
        }
        return MenuItemResponse.from(m);
    }

    private void publishAvailabilityChanged(MenuItem item, boolean previousAvailability) {
        log.info("availability transition menuItemId={} restaurantId={} {} -> {}",
            item.getMenuItemId(), item.getRestaurantId(),
            previousAvailability, item.isAvailable());
        AvailabilityChangedEvent event = new AvailabilityChangedEvent(
            item.getMenuItemId(), item.getRestaurantId(),
            item.isAvailable(), previousAvailability,
            clock.instant()
        );
        try {
            menuEventPublisher.publishAvailabilityChanged(event);
        } catch (RuntimeException ex) {
            log.warn("menu event publish failed menuItemId={} error={}",
                item.getMenuItemId(), ex.getMessage());
        }
    }

    @Transactional
    public void delete(UUID id) {
        MenuItem m = requireMenuItem(id);
        requireOwnerOrAdmin(m.getRestaurantId(), "DELETE /menu-items/" + id);
        menuItems.delete(m);
    }

    private void requireOwnerOrAdmin(UUID restaurantId, String endpoint) {
        AuthenticatedUser actor = currentUser.require();
        if (SecurityRoles.ADMIN.equals(actor.role())) {
            return;
        }
        UUID ownerId = restaurantOwnership.findOwnerId(restaurantId)
            .orElseThrow(() -> new RestaurantNotFoundForMenuException(restaurantId));
        denyIfNotOwner(actor, restaurantId, ownerId, endpoint);
    }

    private void requireOwnerOrAdmin(UUID restaurantId, UUID knownOwnerId, String endpoint) {
        AuthenticatedUser actor = currentUser.require();
        if (SecurityRoles.ADMIN.equals(actor.role())) {
            return;
        }
        denyIfNotOwner(actor, restaurantId, knownOwnerId, endpoint);
    }

    private void denyIfNotOwner(AuthenticatedUser actor, UUID restaurantId, UUID ownerId, String endpoint) {
        if (ownerId.equals(actor.userId())) {
            return;
        }
        log.warn("ownership denial actor={} role={} endpoint={} restaurantId={} ownerId={}",
            actor.userId(), actor.role(), endpoint, restaurantId, ownerId);
        throw new AccessDeniedException(
            "User " + actor.userId() + " does not own restaurant " + restaurantId);
    }

    @Transactional(readOnly = true)
    public ValidateMenuItemsResponse validate(ValidateMenuItemsRequest req) {
        Set<UUID> ids = req.items().stream()
            .map(ValidateMenuItemsRequest.Line::menuItemId)
            .collect(Collectors.toSet());

        Map<UUID, MenuItem> byId = menuItems.findAllByMenuItemIdIn(ids).stream()
            .collect(Collectors.toMap(MenuItem::getMenuItemId, m -> m, (a, b) -> a, LinkedHashMap::new));

        Set<String> distinctCurrencies = byId.values().stream()
            .map(m -> m.getPrice().getCurrency())
            .filter(Objects::nonNull)
            .collect(Collectors.toCollection(LinkedHashSet::new));
        if (distinctCurrencies.size() > 1) {
            throw new MixedCurrencyException(distinctCurrencies);
        }

        List<ValidateMenuItemsResponse.Line> lines = req.items().stream().map(line -> {
            MenuItem m = byId.get(line.menuItemId());
            if (m == null) {
                return new ValidateMenuItemsResponse.Line(
                    line.menuItemId(), line.quantity(),
                    false, false, null, null, null,
                    ValidateMenuItemsResponse.ERROR_NOT_FOUND
                );
            }
            if (!m.isAvailable()) {
                return new ValidateMenuItemsResponse.Line(
                    m.getMenuItemId(), line.quantity(),
                    true, false,
                    m.getPrice().getAmount(), m.getPrice().getCurrency(),
                    null,
                    ValidateMenuItemsResponse.ERROR_NOT_AVAILABLE
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

        boolean allValid = lines.stream().allMatch(l -> l.exists() && l.isAvailable());
        BigDecimal totalAmount = lines.stream()
            .map(ValidateMenuItemsResponse.Line::lineTotal)
            .filter(Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        String currency = lines.stream()
            .map(ValidateMenuItemsResponse.Line::unitPriceCurrency)
            .filter(Objects::nonNull)
            .findFirst()
            .orElse("EUR");
        return new ValidateMenuItemsResponse(allValid, lines, totalAmount, currency);
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
