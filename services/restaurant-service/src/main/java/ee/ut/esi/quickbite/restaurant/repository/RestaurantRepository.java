package ee.ut.esi.quickbite.restaurant.repository;

import ee.ut.esi.quickbite.restaurant.domain.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, UUID> {

    List<Restaurant> findByLocationCity(String city);

    List<Restaurant> findByOpenTrue();

    boolean existsByOwnerIdAndNameIgnoreCase(UUID ownerId, String name);

    @Query("""
        SELECT r FROM Restaurant r
        WHERE (cast(:city as string) IS NULL
               OR LOWER(r.location.city) = LOWER(cast(:city as string)))
          AND (:open IS NULL OR r.open = :open)
        """)
    List<Restaurant> search(@Param("city") String city, @Param("open") Boolean open);
}
