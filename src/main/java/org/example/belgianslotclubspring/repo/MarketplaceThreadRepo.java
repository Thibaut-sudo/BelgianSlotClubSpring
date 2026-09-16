package org.example.belgianslotclubspring.repo;

import org.example.belgianslotclubspring.entities.MarketplaceThread;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface MarketplaceThreadRepo extends JpaRepository<MarketplaceThread, Long> {

    @Query("""
            SELECT DISTINCT t FROM MarketplaceThread t
            JOIN FETCH t.listing
            LEFT JOIN FETCH t.messages
            WHERE t.publicId = :publicId
            """)
    Optional<MarketplaceThread> findDetailedByPublicId(@Param("publicId") String publicId);

    @Query("""
            SELECT t FROM MarketplaceThread t
            WHERE t.listing.id = :listingId
            ORDER BY t.updatedAt DESC
            """)
    List<MarketplaceThread> findByListingIdOrderByUpdatedAtDesc(@Param("listingId") Long listingId);

    @Query("""
            SELECT t FROM MarketplaceThread t
            JOIN FETCH t.listing
            WHERE t.buyerAccountId = :buyerAccountId
            ORDER BY t.updatedAt DESC
            """)
    List<MarketplaceThread> findByBuyerAccountIdOrderByUpdatedAtDesc(@Param("buyerAccountId") Long buyerAccountId);
}
