package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.entities.MarketplaceThread;
import org.example.belgianslotclubspring.utils.SellerPasswords;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class MarketplaceChatAuthTest {

    @Test
    void buyerAndSellerPasswordsOpenDifferentRoles() {
        ImportAuthService admin = new ImportAuthService("admin-secret");
        MarketplaceService listings = new MarketplaceService(null, null, admin, null);
        MarketplaceChatService chats = new MarketplaceChatService(null, listings);

        MarketplaceListing listing = new MarketplaceListing();
        listing.setSellerPasswordHash(SellerPasswords.hash("vendeur1"));
        MarketplaceThread thread = new MarketplaceThread();
        thread.setListing(listing);
        thread.setBuyerPasswordHash(SellerPasswords.hash("achat1", "Le mot de passe du chat"));

        assertEquals(MarketplaceChatService.ROLE_SELLER, chats.resolveRole(thread, "vendeur1"));
        assertEquals(MarketplaceChatService.ROLE_SELLER, chats.resolveRole(thread, "admin-secret"));
        assertEquals(MarketplaceChatService.ROLE_BUYER, chats.resolveRole(thread, "achat1"));
        assertNull(chats.resolveRole(thread, "wrong"));
        assertNull(chats.resolveRole(thread, null));
    }

    @Test
    void accountIdsOpenRolesWithoutPasswords() {
        ImportAuthService admin = new ImportAuthService("admin-secret");
        MarketplaceService listings = new MarketplaceService(null, null, admin, null);
        MarketplaceChatService chats = new MarketplaceChatService(null, listings);

        MarketplaceListing listing = new MarketplaceListing();
        listing.setOwnerAccountId(2L);
        MarketplaceThread thread = new MarketplaceThread();
        thread.setListing(listing);
        thread.setBuyerAccountId(5L);

        assertEquals(MarketplaceChatService.ROLE_SELLER, chats.resolveRole(thread, null, 2L));
        assertEquals(MarketplaceChatService.ROLE_BUYER, chats.resolveRole(thread, null, 5L));
        assertNull(chats.resolveRole(thread, null, 9L));
        assertNull(chats.resolveRole(thread, "achat1", 9L));
        assertEquals(MarketplaceChatService.ROLE_SELLER, chats.resolveRole(thread, null, 9L, true));
        assertEquals(MarketplaceChatService.ROLE_BUYER, chats.resolveRole(thread, null, 5L, true));
    }
}
