package ee.ut.esi.quickbite.restaurant.service;

import ee.ut.esi.quickbite.restaurant.domain.Location;
import ee.ut.esi.quickbite.restaurant.domain.Restaurant;
import ee.ut.esi.quickbite.restaurant.dto.AvailabilityResponse;
import ee.ut.esi.quickbite.restaurant.dto.CreateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.dto.RestaurantResponse;
import ee.ut.esi.quickbite.restaurant.dto.UpdateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.exception.DuplicateRestaurantException;
import ee.ut.esi.quickbite.restaurant.exception.RestaurantNotFoundException;
import ee.ut.esi.quickbite.restaurant.repository.RestaurantRepository;
import ee.ut.esi.quickbite.restaurant.security.AuthenticatedUser;
import ee.ut.esi.quickbite.restaurant.security.CurrentUser;
import ee.ut.esi.quickbite.restaurant.security.SecurityRoles;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.UUID;

@Service
public class RestaurantService {

    private static final Logger log = LoggerFactory.getLogger(RestaurantService.class);
    private static final ZoneId OPERATING_HOURS_ZONE = ZoneId.of("Europe/Tallinn");

    private final RestaurantRepository restaurants;
    private final CurrentUser currentUser;
    private final Clock clock;

    @Autowired
    public RestaurantService(RestaurantRepository restaurants, CurrentUser currentUser) {
        this(restaurants, currentUser, Clock.system(OPERATING_HOURS_ZONE));
    }

    RestaurantService(RestaurantRepository restaurants, CurrentUser currentUser, Clock clock) {
        this.restaurants = restaurants;
        this.currentUser = currentUser;
        this.clock = clock;
    }

    @Transactional
    public RestaurantResponse create(CreateRestaurantRequest req) {
        UUID ownerId = currentUser.require().userId();
        if (restaurants.existsByOwnerIdAndNameIgnoreCase(ownerId, req.name())) {
            throw new DuplicateRestaurantException(ownerId, req.name());
        }
        Location location = new Location(req.address(), req.city(), req.latitude(), req.longitude());
        Restaurant saved = restaurants.save(
            new Restaurant(ownerId, req.name(), location, req.operatingHours())
        );
        return RestaurantResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public RestaurantResponse findById(UUID id) {
        return RestaurantResponse.from(requireRestaurant(id));
    }

    @Transactional(readOnly = true)
    public Page<RestaurantResponse> search(String city, Boolean isOpen, Pageable pageable) {
        return restaurants.search(city, isOpen, pageable).map(RestaurantResponse::from);
    }

    @Transactional
    public RestaurantResponse update(UUID id, UpdateRestaurantRequest req) {
        Restaurant r = requireRestaurant(id);
        requireOwnerOrAdmin(r, "PUT /restaurants/" + id);
        Location location = new Location(req.address(), req.city(), req.latitude(), req.longitude());
        r.updateDetails(req.name(), location, req.operatingHours());
        return RestaurantResponse.from(r);
    }

    @Transactional
    public RestaurantResponse setStatus(UUID id, boolean isOpen) {
        Restaurant r = requireRestaurant(id);
        requireOwnerOrAdmin(r, "PATCH /restaurants/" + id + "/status");
        r.setStatus(isOpen);
        return RestaurantResponse.from(r);
    }

    private void requireOwnerOrAdmin(Restaurant r, String endpoint) {
        AuthenticatedUser actor = currentUser.require();
        if (SecurityRoles.ADMIN.equals(actor.role())) {
            return;
        }
        if (actor.userId().equals(r.getOwnerId())) {
            return;
        }
        log.warn("ownership denial actor={} role={} endpoint={} restaurantId={} ownerId={}",
            actor.userId(), actor.role(), endpoint, r.getRestaurantId(), r.getOwnerId());
        throw new AccessDeniedException(
            "User " + actor.userId() + " does not own restaurant " + r.getRestaurantId());
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse availability(UUID id) {
        Restaurant r = requireRestaurant(id);
        Instant checkedAt = clock.instant();
        boolean acceptsOrders = r.isOpen() && isWithinOperatingHours(r.getOperatingHours());
        return new AvailabilityResponse(
            r.getRestaurantId(),
            r.isOpen(),
            acceptsOrders,
            r.getOperatingHours(),
            checkedAt
        );
    }

    private boolean isWithinOperatingHours(String operatingHours) {
        if (operatingHours == null) {
            return true;
        }
        String[] parts = operatingHours.split("-", 2);
        if (parts.length != 2) {
            log.warn("malformed operatingHours (no '-' separator): {}", operatingHours);
            return false;
        }
        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(parts[0].trim());
            end = LocalTime.parse(parts[1].trim());
        } catch (Exception e) {
            log.warn("malformed operatingHours (unparseable times): {}", operatingHours);
            return false;
        }
        LocalTime now = LocalTime.now(clock.withZone(OPERATING_HOURS_ZONE));
        if (start.equals(end)) {
            return true;
        }
        if (start.isBefore(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isBefore(start) || !now.isAfter(end);
    }

    private Restaurant requireRestaurant(UUID id) {
        return restaurants.findById(id).orElseThrow(() -> new RestaurantNotFoundException(id));
    }
}
