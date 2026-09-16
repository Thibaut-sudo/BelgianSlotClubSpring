package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.entities.MarketplacePhoto;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.repo.MarketplaceListingRepo;
import org.example.belgianslotclubspring.utils.SellerPasswords;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class MarketplaceService {

    public static final List<String> CATEGORIES = List.of(
            "Voitures",
            "Châssis / carrosseries",
            "Moteurs / transmissions",
            "Pneus / jantes",
            "Électronique",
            "Divers"
    );

    public static final String SORT_RANDOM = "aleatoire";
    public static final String SORT_NEWEST = "recentes";
    public static final String SORT_OLDEST = "anciennes";

    private static final int TITLE_MAX = 120;
    private static final int BODY_MAX = 4000;
    private static final int PRICE_MAX = 40;
    private static final int NAME_MAX = 40;
    private static final int CONTACT_MAX = 120;

    private final MarketplaceListingRepo listingRepo;
    private final MarketplacePhotoStorage photoStorage;
    private final ImportAuthService importAuthService;

    public MarketplaceService(MarketplaceListingRepo listingRepo,
                              MarketplacePhotoStorage photoStorage,
                              ImportAuthService importAuthService) {
        this.listingRepo = listingRepo;
        this.photoStorage = photoStorage;
        this.importAuthService = importAuthService;
    }

    @Transactional(readOnly = true)
    public List<ListingCard> list(String category) {
        return list(category, SORT_RANDOM);
    }

    @Transactional(readOnly = true)
    public List<ListingCard> list(String category, String sort) {
        String order = normalizeSort(sort);
        List<MarketplaceListing> listings;
        if (category == null || category.isBlank() || "tous".equalsIgnoreCase(category) || !CATEGORIES.contains(category)) {
            listings = listingRepo.findAllWithPhotos();
        } else {
            listings = listingRepo.findByCategoryWithPhotos(category);
        }
        List<MarketplaceListing> available = new ArrayList<>();
        List<MarketplaceListing> sold = new ArrayList<>();
        for (MarketplaceListing listing : unique(listings)) {
            if (listing.isSold()) {
                sold.add(listing);
            } else {
                available.add(listing);
            }
        }
        sortGroup(available, order);
        sortGroup(sold, order);
        available.addAll(sold);
        return available.stream().map(ListingCard::from).toList();
    }

    public static String normalizeSort(String sort) {
        if (sort == null) {
            return SORT_RANDOM;
        }
        return switch (sort.trim().toLowerCase(Locale.ROOT)) {
            case "recentes", "recente", "recent" -> SORT_NEWEST;
            case "anciennes", "ancienne", "ancien" -> SORT_OLDEST;
            default -> SORT_RANDOM;
        };
    }

    static void sortGroup(List<MarketplaceListing> listings, String sort) {
        if (listings == null || listings.size() < 2) {
            return;
        }
        if (SORT_NEWEST.equals(sort)) {
            listings.sort(byCreatedAt(false));
        } else if (SORT_OLDEST.equals(sort)) {
            listings.sort(byCreatedAt(true));
        } else {
            Collections.shuffle(listings);
        }
    }

    private static Comparator<MarketplaceListing> byCreatedAt(boolean oldestFirst) {
        Comparator<MarketplaceListing> byDate = Comparator.comparing(
                MarketplaceListing::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
        Comparator<MarketplaceListing> byId = Comparator.comparing(
                MarketplaceListing::getId,
                Comparator.nullsLast(Comparator.naturalOrder())
        );
        Comparator<MarketplaceListing> order = byDate.thenComparing(byId);
        return oldestFirst ? order : order.reversed();
    }

    public record ListingCard(MarketplaceListing listing, String sellerClubLabel, String coverUrl, int photoCount) {
        static ListingCard from(MarketplaceListing listing) {
            String label = Club.fromCode(listing.getClubName()).map(Club::getDisplayName).orElse("");
            int count = listing.getPhotos() == null ? 0 : listing.getPhotos().size();
            return new ListingCard(listing, label, listing.getCoverUrl(), count);
        }
    }

    public boolean canManage(MarketplaceListing listing, String password) {
        if (listing == null) {
            return false;
        }
        if (importAuthService.matches(password)) {
            return true;
        }
        return SellerPasswords.matches(listing.getSellerPasswordHash(), password);
    }

    public boolean hasSellerLock(MarketplaceListing listing) {
        return listing != null
                && listing.getSellerPasswordHash() != null
                && !listing.getSellerPasswordHash().isBlank();
    }

    @Transactional(readOnly = true)
    public MarketplaceListing require(Long id) {
        return listingRepo.findDetailedById(id)
                .orElseThrow(() -> new IllegalArgumentException("Annonce introuvable."));
    }

    @Transactional
    public MarketplaceListing publish(String title,
                                      String description,
                                      String category,
                                      String price,
                                      String sellerName,
                                      String contact,
                                      String club,
                                      String sellerPassword,
                                      String sellerPasswordConfirm,
                                      List<MultipartFile> photos) {
        if (category == null || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Choisissez une catégorie.");
        }
        SellerPasswords.requireMatch(sellerPassword, sellerPasswordConfirm);
        List<String> stored = photoStorage.saveAll(photos);
        try {
            MarketplaceListing listing = new MarketplaceListing();
            listing.setTitle(ForumService.cleanLine(title, TITLE_MAX, "un titre"));
            listing.setDescription(ForumService.cleanBody(description, BODY_MAX, "une description"));
            listing.setCategory(category);
            listing.setPrice(ForumService.cleanOptionalLine(price, PRICE_MAX));
            listing.setSellerName(ForumService.cleanLine(sellerName, NAME_MAX, "votre nom"));
            listing.setContact(ForumService.cleanLine(contact, CONTACT_MAX, "un moyen de contact"));
            listing.setClubName(optionalClub(club));
            listing.setSold(false);
            listing.setSellerPasswordHash(SellerPasswords.hash(sellerPassword));
            listing.setCreatedAt(LocalDateTime.now());
            int order = 0;
            for (String storedName : stored) {
                MarketplacePhoto photo = new MarketplacePhoto();
                photo.setStoredName(storedName);
                photo.setSortOrder(order++);
                listing.addPhoto(photo);
            }
            return listingRepo.save(listing);
        } catch (RuntimeException e) {
            stored.forEach(photoStorage::deleteQuietly);
            throw e;
        }
    }

    @Transactional
    public MarketplaceListing update(Long id,
                                     String title,
                                     String description,
                                     String category,
                                     String price,
                                     String sellerName,
                                     String contact,
                                     String club,
                                     String newSellerPassword,
                                     String newSellerPasswordConfirm,
                                     List<MultipartFile> photos) {
        if (category == null || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Choisissez une catégorie.");
        }
        MarketplaceListing listing = require(id);
        listing.setTitle(ForumService.cleanLine(title, TITLE_MAX, "un titre"));
        listing.setDescription(ForumService.cleanBody(description, BODY_MAX, "une description"));
        listing.setCategory(category);
        listing.setPrice(ForumService.cleanOptionalLine(price, PRICE_MAX));
        listing.setSellerName(ForumService.cleanLine(sellerName, NAME_MAX, "votre nom"));
        listing.setContact(ForumService.cleanLine(contact, CONTACT_MAX, "un moyen de contact"));
        listing.setClubName(optionalClub(club));
        if (newSellerPassword != null && !newSellerPassword.isBlank()) {
            SellerPasswords.requireMatch(newSellerPassword, newSellerPasswordConfirm);
            listing.setSellerPasswordHash(SellerPasswords.hash(newSellerPassword));
        }
        int existing = listing.getPhotos() == null ? 0 : listing.getPhotos().size();
        int remaining = MarketplacePhotoStorage.MAX_PHOTOS - existing;
        List<String> stored = photoStorage.saveAll(photos, remaining);
        try {
            int order = existing;
            for (String storedName : stored) {
                MarketplacePhoto photo = new MarketplacePhoto();
                photo.setStoredName(storedName);
                photo.setSortOrder(order++);
                listing.addPhoto(photo);
            }
            return listingRepo.save(listing);
        } catch (RuntimeException e) {
            stored.forEach(photoStorage::deleteQuietly);
            throw e;
        }
    }

    @Transactional
    public void markSold(Long id, boolean sold) {
        MarketplaceListing listing = require(id);
        listing.setSold(sold);
        listingRepo.save(listing);
    }

    @Transactional
    public void delete(Long id) {
        MarketplaceListing listing = require(id);
        List<String> storedNames = new ArrayList<>();
        if (listing.getPhotos() != null) {
            listing.getPhotos().forEach(photo -> storedNames.add(photo.getStoredName()));
        }
        listingRepo.delete(listing);
        storedNames.forEach(photoStorage::deleteQuietly);
    }

    private static List<MarketplaceListing> unique(List<MarketplaceListing> listings) {
        Map<Long, MarketplaceListing> byId = new LinkedHashMap<>();
        for (MarketplaceListing listing : listings) {
            byId.putIfAbsent(listing.getId(), listing);
        }
        return List.copyOf(byId.values());
    }

    private static String optionalClub(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        return Club.fromCode(raw).map(Club::getCode).orElse("");
    }
}
