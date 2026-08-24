package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketplaceListingRepo extends JpaRepository<MarketplaceListing, Long> {

    @Query("""
            SELECT DISTINCT l FROM MarketplaceListing l
            LEFT JOIN FETCH l.photos
            """)
    List<MarketplaceListing> findAllWithPhotos();

    @Query("""
            SELECT DISTINCT l FROM MarketplaceListing l
            LEFT JOIN FETCH l.photos
            WHERE l.category = :category
            """)
    List<MarketplaceListing> findByCategoryWithPhotos(@Param("category") String category);

    @Query("""
            SELECT DISTINCT l FROM MarketplaceListing l
            LEFT JOIN FETCH l.photos
            WHERE l.id = :id
            """)
    Optional<MarketplaceListing> findDetailedById(@Param("id") Long id);
}
