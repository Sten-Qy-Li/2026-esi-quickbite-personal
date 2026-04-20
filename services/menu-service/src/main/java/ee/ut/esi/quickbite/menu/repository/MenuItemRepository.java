package ee.ut.esi.quickbite.menu.repository;

import ee.ut.esi.quickbite.menu.domain.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface MenuItemRepository extends JpaRepository<MenuItem, UUID> {

    List<MenuItem> findByRestaurantId(UUID restaurantId);

    List<MenuItem> findByRestaurantIdAndAvailableTrue(UUID restaurantId);

    List<MenuItem> findAllByMenuItemIdIn(Collection<UUID> menuItemIds);

    @Query("""
        SELECT m FROM MenuItem m
        WHERE m.restaurantId = :restaurantId
          AND (cast(:category as string) IS NULL
               OR LOWER(m.category) = LOWER(cast(:category as string)))
          AND (:available IS NULL OR m.available = :available)
        """)
    List<MenuItem> searchForRestaurant(
        @Param("restaurantId") UUID restaurantId,
        @Param("category") String category,
        @Param("available") Boolean available
    );
}
