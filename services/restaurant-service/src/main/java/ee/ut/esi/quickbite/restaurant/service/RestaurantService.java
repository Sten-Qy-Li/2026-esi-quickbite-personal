package ee.ut.esi.quickbite.restaurant.service;

import ee.ut.esi.quickbite.restaurant.domain.Location;
import ee.ut.esi.quickbite.restaurant.domain.Restaurant;
import ee.ut.esi.quickbite.restaurant.dto.AvailabilityResponse;
import ee.ut.esi.quickbite.restaurant.dto.CreateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.dto.RestaurantResponse;
import ee.ut.esi.quickbite.restaurant.dto.UpdateRestaurantRequest;
import ee.ut.esi.quickbite.restaurant.exception.RestaurantNotFoundException;
import ee.ut.esi.quickbite.restaurant.repository.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class RestaurantService {

    // Phase 3 placeholder — replaced by JWT-derived ownerId in Phase 7.
    private static final UUID PLACEHOLDER_OWNER_ID =
        UUID.fromString("00000000-0000-0000-0000-000000000001");

    private final RestaurantRepository restaurants;

    public RestaurantService(RestaurantRepository restaurants) {
        this.restaurants = restaurants;
    }

    @Transactional
    public RestaurantResponse create(CreateRestaurantRequest req) {
        Location location = new Location(req.address(), req.city(), req.latitude(), req.longitude());
        Restaurant saved = restaurants.save(
            new Restaurant(PLACEHOLDER_OWNER_ID, req.name(), location, req.operatingHours())
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
        return new AvailabilityResponse(r.getRestaurantId(), r.isOpen(), r.getOperatingHours());
    }

    private Restaurant requireRestaurant(UUID id) {
        return restaurants.findById(id).orElseThrow(() -> new RestaurantNotFoundException(id));
    }
}
