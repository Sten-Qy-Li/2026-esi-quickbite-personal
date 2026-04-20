package ee.ut.esi.quickbite.menu.events;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Log-only transport for Phase 16's optional menu-events stretch.
 * Envelope shape is locked by decision 0032 §6 so a future
 * Kafka-backed publisher can replace this class without touching
 * the call site.
 */
@Component
public class LoggingMenuEventPublisher implements MenuEventPublisher {

    static final String EVENT_TYPE = "menu.item-availability-changed";
    static final String LOGICAL_TOPIC = "menu-events";
    private static final Logger log = LoggerFactory.getLogger(LOGICAL_TOPIC);

    private final ObjectMapper objectMapper;

    public LoggingMenuEventPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void publishAvailabilityChanged(AvailabilityChangedEvent event) {
        UUID envelopeId = UUID.randomUUID();
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("menuItemId", event.menuItemId().toString());
        payload.put("restaurantId", event.restaurantId().toString());
        payload.put("isAvailable", event.isAvailable());
        payload.put("previousIsAvailable", event.previousIsAvailable());

        Map<String, Object> envelope = new LinkedHashMap<>();
        envelope.put("id", envelopeId.toString());
        envelope.put("type", EVENT_TYPE);
        envelope.put("occurredAt", event.occurredAt().toString());
        envelope.put("payload", payload);

        try {
            String json = objectMapper.writeValueAsString(envelope);
            log.info("topic={} key={} envelope={}", LOGICAL_TOPIC, event.menuItemId(), json);
        } catch (JsonProcessingException ex) {
            log.warn("failed to serialise menu-events envelope id={} menuItemId={} error={}",
                envelopeId, event.menuItemId(), ex.getMessage());
        }
    }
}
