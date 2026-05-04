package ee.ut.esi.quickbite.restaurant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "restaurant")
@EntityListeners(AuditingEntityListener.class)
public class Restaurant {

    @Id
    @Column(name = "restaurant_id")
    private UUID restaurantId;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "name", nullable = false)
    private String name;

    @Embedded
    private Location location;

    @Column(name = "operating_hours", length = 20)
    private String operatingHours;

    @Column(name = "is_open", nullable = false)
    private boolean open;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected Restaurant() {
    }

    public Restaurant(UUID ownerId, String name, Location location, String operatingHours) {
        this.restaurantId = UUID.randomUUID();
        this.ownerId = ownerId;
        this.name = name;
        this.location = location;
        this.operatingHours = operatingHours;
        this.open = false;
    }

    public void updateDetails(String name, Location location, String operatingHours) {
        this.name = name;
        this.location = location;
        this.operatingHours = operatingHours;
    }

    public void setStatus(boolean open) {
        this.open = open;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public Location getLocation() {
        return location;
    }

    public String getOperatingHours() {
        return operatingHours;
    }

    public boolean isOpen() {
        return open;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
