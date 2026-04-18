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
import ee.ut.esi.quickbite.restaurant.security.CurrentUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

@Service
public class RestaurantService {

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
    public List<RestaurantResponse> search(String city, Boolean isOpen) {
        return restaurants.search(city, isOpen).stream()
            .map(RestaurantResponse::from)
            .toList();
    }

    @Transactional
    public RestaurantResponse update(UUID id, UpdateRestaurantRequest req) {
        Restaurant r = requireRestaurant(id);
        Location location = new Location(req.address(), req.city(), req.latitude(), req.longitude());
        r.updateDetails(req.name(), location, req.operatingHours());
        return RestaurantResponse.from(r);
    }

    @Transactional
    public RestaurantResponse setStatus(UUID id, boolean isOpen) {
        Restaurant r = requireRestaurant(id);
        r.setStatus(isOpen);
        return RestaurantResponse.from(r);
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
            return true;
        }
        LocalTime start;
        LocalTime end;
        try {
            start = LocalTime.parse(parts[0].trim());
            end = LocalTime.parse(parts[1].trim());
        } catch (Exception e) {
            return true;
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
