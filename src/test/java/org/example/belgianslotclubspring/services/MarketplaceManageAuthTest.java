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
        MarketplaceService service = new MarketplaceService(null, null, admin, null);
        MarketplaceListing listing = new MarketplaceListing();
        listing.setSellerPasswordHash(SellerPasswords.hash("vendeur1"));

        assertTrue(service.canManage(listing, "vendeur1"));
        assertTrue(service.canManage(listing, "admin-secret"));
        assertFalse(service.canManage(listing, "wrong"));
        assertFalse(service.canManage(listing, null));
        assertTrue(service.hasSellerLock(listing));
    }

    @Test
    void ownerAccountUnlocksListingWithoutPassword() {
        ImportAuthService admin = new ImportAuthService("admin-secret");
        MarketplaceService service = new MarketplaceService(null, null, admin, null);
        MarketplaceListing listing = new MarketplaceListing();
        listing.setOwnerAccountId(7L);

        assertTrue(service.ownedBy(listing, 7L));
        assertTrue(service.canManage(listing, null, 7L));
        assertTrue(service.hasOwnerAccount(listing));
        assertFalse(service.ownedBy(listing, 8L));
        assertFalse(service.canManage(listing, "wrong", 8L));
    }

    @Test
    void listingWithoutSellerPasswordAcceptsOnlyAdmin() {
        ImportAuthService admin = new ImportAuthService("admin-secret");
        MarketplaceService service = new MarketplaceService(null, null, admin, null);
        MarketplaceListing listing = new MarketplaceListing();

        assertFalse(service.hasSellerLock(listing));
        assertFalse(service.canManage(listing, "vendeur1"));
        assertTrue(service.canManage(listing, "admin-secret"));
    }

    @Test
    void siteAdminAccountUnlocksAnyListing() {
        ImportAuthService importAuth = new ImportAuthService("admin-secret");
        MarketplaceService service = new MarketplaceService(null, null, importAuth, null);
        MarketplaceListing listing = new MarketplaceListing();
        listing.setSellerPasswordHash(SellerPasswords.hash("vendeur1"));

        assertTrue(service.canManage(listing, null, 99L, true));
        assertFalse(service.canManage(listing, null, 99L, false));
    }
}
