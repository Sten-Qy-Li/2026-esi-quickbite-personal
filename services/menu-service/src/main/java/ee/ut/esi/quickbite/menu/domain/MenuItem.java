package ee.ut.esi.quickbite.menu.domain;

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
@Table(name = "menu_item")
@EntityListeners(AuditingEntityListener.class)
public class MenuItem {

    @Id
    @Column(name = "menu_item_id")
    private UUID menuItemId;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", length = 2000)
    private String description;

    @Embedded
    private Price price;

    @Column(name = "category", nullable = false, length = 100)
    private String category;

    @Column(name = "is_available", nullable = false)
    private boolean available;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected MenuItem() {
    }

    public MenuItem(UUID restaurantId, String name, String description,
                    Price price, String category, boolean available) {
        this.menuItemId = UUID.randomUUID();
        this.restaurantId = restaurantId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.available = available;
    }

    public void updateDetails(String name, String description, Price price,
                              String category, boolean available) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.category = category;
        this.available = available;
    }

    public UUID getMenuItemId() {
        return menuItemId;
    }

    public UUID getRestaurantId() {
        return restaurantId;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public Price getPrice() {
        return price;
    }

    public String getCategory() {
        return category;
    }

    public boolean isAvailable() {
        return available;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
