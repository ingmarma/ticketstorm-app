package com.ticketstorm.catalog.domain.port;

import com.ticketstorm.catalog.domain.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface EventRepository extends JpaRepository<Event, UUID> {

    @Query("SELECT e FROM Event e LEFT JOIN FETCH e.ticketSections WHERE e.id = :id")
    Optional<Event> findByIdWithSections(@Param("id") UUID id);

    List<Event> findByCategory(String category);

    List<Event> findByCity(String city);

    List<Event> findByEventDateBetween(Instant start, Instant end);

    @Query(value = """
            SELECT * FROM events e
            WHERE plainto_tsquery('english', :query) @@ e.search_vector
            ORDER BY ts_rank(e.search_vector, plainto_tsquery('english', :query)) DESC
            """, nativeQuery = true)
    List<Event> searchByFullText(@Param("query") String query);

    List<Event> findByCategoryAndCity(String category, String city);
}
