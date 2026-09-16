package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.utils.SellerPasswords;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MarketplaceManageAuthTest {

    @Test
    void sellerPasswordOrAdminPasswordUnlocksListing() {
        ImportAuthService admin = new ImportAuthService("admin-secret");
        MarketplaceService service = new MarketplaceService(null, null, admin);
        MarketplaceListing listing = new MarketplaceListing();
        listing.setSellerPasswordHash(SellerPasswords.hash("vendeur1"));

        assertTrue(service.canManage(listing, "vendeur1"));
        assertTrue(service.canManage(listing, "admin-secret"));
        assertFalse(service.canManage(listing, "wrong"));
        assertFalse(service.canManage(listing, null));
        assertTrue(service.hasSellerLock(listing));
    }

    @Test
    void listingWithoutSellerPasswordAcceptsOnlyAdmin() {
        ImportAuthService admin = new ImportAuthService("admin-secret");
        MarketplaceService service = new MarketplaceService(null, null, admin);
        MarketplaceListing listing = new MarketplaceListing();

        assertFalse(service.hasSellerLock(listing));
        assertFalse(service.canManage(listing, "vendeur1"));
        assertTrue(service.canManage(listing, "admin-secret"));
    }
}
