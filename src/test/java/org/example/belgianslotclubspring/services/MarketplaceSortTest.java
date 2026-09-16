package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MarketplaceSortTest {

    @Test
    void newestPutsLaterDatesFirst() {
        MarketplaceListing first = listing(1L, LocalDateTime.of(2026, 1, 1, 10, 0));
        MarketplaceListing second = listing(2L, LocalDateTime.of(2026, 9, 16, 18, 0));
        List<MarketplaceListing> listings = new ArrayList<>(List.of(first, second));

        MarketplaceService.sortGroup(listings, MarketplaceService.SORT_NEWEST);
        assertEquals(2L, listings.get(0).getId());
        assertEquals(1L, listings.get(1).getId());

        MarketplaceService.sortGroup(listings, MarketplaceService.SORT_OLDEST);
        assertEquals(1L, listings.get(0).getId());
        assertEquals(2L, listings.get(1).getId());
    }

    @Test
    void unknownSortFallsBackToRandomConstant() {
        assertEquals(MarketplaceService.SORT_RANDOM, MarketplaceService.normalizeSort(null));
        assertEquals(MarketplaceService.SORT_NEWEST, MarketplaceService.normalizeSort("recentes"));
        assertEquals(MarketplaceService.SORT_OLDEST, MarketplaceService.normalizeSort("Anciennes"));
    }

    private static MarketplaceListing listing(Long id, LocalDateTime createdAt) {
        MarketplaceListing listing = new MarketplaceListing();
        listing.setId(id);
        listing.setCreatedAt(createdAt);
        return listing;
    }
}
