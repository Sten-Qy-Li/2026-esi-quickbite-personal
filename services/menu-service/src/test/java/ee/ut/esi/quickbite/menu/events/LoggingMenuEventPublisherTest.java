package ee.ut.esi.quickbite.menu.events;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingMenuEventPublisherTest {

    private static final UUID MENU_ITEM_ID =
        UUID.fromString("e0000011-0000-0000-0000-000000000011");
    private static final UUID RESTAURANT_ID =
        UUID.fromString("d0000001-0000-0000-0000-000000000001");
    private static final Instant OCCURRED_AT = Instant.parse("2026-04-19T12:00:00Z");

    private final ObjectMapper objectMapper = new ObjectMapper();
    private Logger eventsLogger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void attachAppender() {
        eventsLogger = (Logger) LoggerFactory.getLogger(LoggingMenuEventPublisher.LOGICAL_TOPIC);
        appender = new ListAppender<>();
        appender.setContext(eventsLogger.getLoggerContext());
        appender.start();
        eventsLogger.addAppender(appender);
        eventsLogger.setLevel(Level.INFO);
    }

    @AfterEach
    void detachAppender() {
        eventsLogger.detachAppender(appender);
    }

    @Test
    void publishesEnvelopeMatchingDecision0032Shape() throws Exception {
        LoggingMenuEventPublisher publisher = new LoggingMenuEventPublisher(objectMapper);
        AvailabilityChangedEvent event = new AvailabilityChangedEvent(
            MENU_ITEM_ID, RESTAURANT_ID, false, true, OCCURRED_AT);

        publisher.publishAvailabilityChanged(event);

        assertThat(appender.list).hasSize(1);
        ILoggingEvent logged = appender.list.get(0);
        assertThat(logged.getLevel()).isEqualTo(Level.INFO);

        String formatted = logged.getFormattedMessage();
        assertThat(formatted)
            .contains("topic=menu-events")
            .contains("key=" + MENU_ITEM_ID);

        int envelopeStart = formatted.indexOf("envelope=") + "envelope=".length();
        String json = formatted.substring(envelopeStart);
        JsonNode envelope = objectMapper.readTree(json);

        assertThat(envelope.get("type").asText()).isEqualTo("menu.item-availability-changed");
        assertThat(envelope.get("occurredAt").asText()).isEqualTo(OCCURRED_AT.toString());
        assertThat(UUID.fromString(envelope.get("id").asText())).isNotNull();

        JsonNode payload = envelope.get("payload");
        assertThat(payload.get("menuItemId").asText()).isEqualTo(MENU_ITEM_ID.toString());
        assertThat(payload.get("restaurantId").asText()).isEqualTo(RESTAURANT_ID.toString());
        assertThat(payload.get("isAvailable").asBoolean()).isFalse();
        assertThat(payload.get("previousIsAvailable").asBoolean()).isTrue();
    }
}
