package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.entities.MarketplaceMessage;
import org.example.belgianslotclubspring.entities.MarketplaceThread;
import org.example.belgianslotclubspring.repo.MarketplaceThreadRepo;
import org.example.belgianslotclubspring.utils.SellerPasswords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class MarketplaceChatService {

    public static final String ROLE_BUYER = "buyer";
    public static final String ROLE_SELLER = "seller";

    private static final int NAME_MAX = 40;
    private static final int MESSAGE_MAX = 1000;
    private static final int PREVIEW_MAX = 120;
    private static final int MAX_MESSAGES = 200;
    private static final String PASSWORD_LABEL = "Le mot de passe du chat";

    private final MarketplaceThreadRepo threadRepo;
    private final MarketplaceService marketplaceService;

    public MarketplaceChatService(MarketplaceThreadRepo threadRepo, MarketplaceService marketplaceService) {
        this.threadRepo = threadRepo;
        this.marketplaceService = marketplaceService;
    }

    @Transactional(readOnly = true)
    public MarketplaceThread requireByPublicId(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            throw new IllegalArgumentException("Conversation introuvable.");
        }
        return threadRepo.findDetailedByPublicId(publicId.trim())
                .orElseThrow(() -> new IllegalArgumentException("Conversation introuvable."));
    }

    @Transactional(readOnly = true)
    public List<ThreadCard> listForListing(Long listingId) {
        return threadRepo.findByListingIdOrderByUpdatedAtDesc(listingId).stream()
                .map(ThreadCard::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ThreadCard> listForBuyer(Long buyerAccountId) {
        if (buyerAccountId == null) {
            return List.of();
        }
        return threadRepo.findByBuyerAccountIdOrderByUpdatedAtDesc(buyerAccountId).stream()
                .map(ThreadCard::from)
                .toList();
    }

    @Transactional
    public MarketplaceThread start(Long listingId,
                                   String buyerName,
                                   String message,
                                   String password,
                                   String passwordConfirm,
                                   Long buyerAccountId) {
        MarketplaceListing listing = marketplaceService.require(listingId);
        if (listing.isSold()) {
            throw new IllegalArgumentException("Cette annonce est vendue.");
        }
        if (buyerAccountId != null && buyerAccountId.equals(listing.getOwnerAccountId())) {
            throw new IllegalArgumentException("Vous ne pouvez pas ouvrir un chat sur votre propre annonce.");
        }
        boolean hasPassword = password != null && !password.isBlank();
        if (buyerAccountId == null || hasPassword) {
            SellerPasswords.requireMatch(password, passwordConfirm, PASSWORD_LABEL);
        }
        MarketplaceThread thread = new MarketplaceThread();
        thread.setListing(listing);
        thread.setPublicId(UUID.randomUUID().toString());
        thread.setBuyerName(ForumService.cleanLine(buyerName, NAME_MAX, "votre prénom"));
        thread.setBuyerAccountId(buyerAccountId);
        if (buyerAccountId == null || hasPassword) {
            thread.setBuyerPasswordHash(SellerPasswords.hash(password, PASSWORD_LABEL));
        }
        thread.setCreatedAt(LocalDateTime.now());
        addMessage(thread, false, message);
        return threadRepo.save(thread);
    }

    @Transactional
    public MarketplaceThread reply(String publicId, boolean fromSeller, String message) {
        MarketplaceThread thread = requireByPublicId(publicId);
        if (thread.getMessages() != null && thread.getMessages().size() >= MAX_MESSAGES) {
            throw new IllegalArgumentException("Cette conversation est trop longue. Contactez le vendeur autrement.");
        }
        addMessage(thread, fromSeller, message);
        return threadRepo.save(thread);
    }

    public String resolveRole(MarketplaceThread thread, String password) {
        return resolveRole(thread, password, null);
    }

    public String resolveRole(MarketplaceThread thread, String password, Long accountId) {
        return resolveRole(thread, password, accountId, false);
    }

    public String resolveRole(MarketplaceThread thread, String password, Long accountId, boolean admin) {
        if (thread == null) {
            return null;
        }
        if (accountId != null) {
            if (marketplaceService.ownedBy(thread.getListing(), accountId)) {
                return ROLE_SELLER;
            }
            if (accountId.equals(thread.getBuyerAccountId())) {
                return ROLE_BUYER;
            }
        }
        if (admin) {
            return ROLE_SELLER;
        }
        if (marketplaceService.canManage(thread.getListing(), password, accountId)) {
            return ROLE_SELLER;
        }
        if (SellerPasswords.matches(thread.getBuyerPasswordHash(), password)) {
            return ROLE_BUYER;
        }
        return null;
    }

    private static void addMessage(MarketplaceThread thread, boolean fromSeller, String message) {
        String body = ForumService.cleanBody(message, MESSAGE_MAX, "un message");
        MarketplaceMessage entry = new MarketplaceMessage();
        entry.setFromSeller(fromSeller);
        entry.setBody(body);
        entry.setCreatedAt(LocalDateTime.now());
        thread.addMessage(entry);
        thread.setLastPreview(preview(body));
        thread.setUpdatedAt(entry.getCreatedAt());
    }

    private static String preview(String body) {
        String compact = body.replace('\n', ' ').replaceAll("\\s+", " ").trim();
        if (compact.length() <= PREVIEW_MAX) {
            return compact;
        }
        return compact.substring(0, PREVIEW_MAX - 1) + "…";
    }

    public record ThreadCard(
            String publicId,
            String buyerName,
            String lastPreview,
            LocalDateTime updatedAt,
            Long listingId,
            String listingTitle
    ) {
        static ThreadCard from(MarketplaceThread thread) {
            MarketplaceListing listing = thread.getListing();
            return new ThreadCard(
                    thread.getPublicId(),
                    thread.getBuyerName(),
                    thread.getLastPreview() == null ? "" : thread.getLastPreview(),
                    thread.getUpdatedAt(),
                    listing == null ? null : listing.getId(),
                    listing == null || listing.getTitle() == null ? "" : listing.getTitle()
            );
        }
    }
}
