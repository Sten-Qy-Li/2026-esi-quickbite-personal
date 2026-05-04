package ee.ut.esi.quickbite.menu.security;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import ee.ut.esi.quickbite.menu.exception.OwningRestaurantLookupException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;
import java.util.UUID;

@Component
public class RestaurantOwnershipClient {

    private static final Logger log = LoggerFactory.getLogger(RestaurantOwnershipClient.class);

    private final RestClient http;

    public RestaurantOwnershipClient(
        RestClient.Builder builder,
        @Value("${restaurant-service.base-url:http://restaurant-service:8081}") String baseUrl
    ) {
        this.http = builder.baseUrl(baseUrl).build();
        log.info("RestaurantOwnershipClient base-url={}", baseUrl);
    }

    public Optional<UUID> findOwnerId(UUID restaurantId) {
        try {
            RestaurantSummary body = http.get()
                .uri("/restaurants/{id}", restaurantId)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                    if (res.getStatusCode().value() == 404) {
                        throw new RestaurantNotFound(restaurantId);
                    }
                    throw new OwningRestaurantLookupException(
                        "Restaurant Service returned " + res.getStatusCode() + " for " + restaurantId);
                })
                .body(RestaurantSummary.class);
            if (body == null || body.ownerId() == null) {
                throw new OwningRestaurantLookupException(
                    "Restaurant Service returned empty ownerId for " + restaurantId);
            }
            return Optional.of(body.ownerId());
        } catch (RestaurantNotFound ex) {
            return Optional.empty();
        } catch (RestClientException ex) {
            throw new OwningRestaurantLookupException(
                "Failed to resolve ownership for restaurant " + restaurantId + ": " + ex.getMessage(), ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RestaurantSummary(UUID restaurantId, UUID ownerId) { }

    private static final class RestaurantNotFound extends RuntimeException {
        RestaurantNotFound(UUID id) {
            super("restaurant " + id + " not found");
        }
    }
}
