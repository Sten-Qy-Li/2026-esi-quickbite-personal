package ee.ut.esi.quickbite.menu.events;

public interface MenuEventPublisher {

    void publishAvailabilityChanged(AvailabilityChangedEvent event);
}
