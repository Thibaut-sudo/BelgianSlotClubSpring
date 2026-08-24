package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.entities.MarketplacePhoto;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.repo.MarketplaceListingRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
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

    private static final int TITLE_MAX = 120;
    private static final int BODY_MAX = 4000;
    private static final int PRICE_MAX = 40;
    private static final int NAME_MAX = 40;
    private static final int CONTACT_MAX = 120;

    private final MarketplaceListingRepo listingRepo;
    private final MarketplacePhotoStorage photoStorage;

    public MarketplaceService(MarketplaceListingRepo listingRepo, MarketplacePhotoStorage photoStorage) {
        this.listingRepo = listingRepo;
        this.photoStorage = photoStorage;
    }

    @Transactional(readOnly = true)
    public List<ListingCard> list(String category) {
        List<MarketplaceListing> listings;
        if (category == null || category.isBlank() || "tous".equalsIgnoreCase(category) || !CATEGORIES.contains(category)) {
            listings = listingRepo.findAllWithPhotos();
        } else {
            listings = listingRepo.findByCategoryWithPhotos(category);
        }
        return unique(listings).stream()
                .sorted(Comparator.comparing(MarketplaceListing::isSold)
                        .thenComparing(MarketplaceListing::getCreatedAt, Comparator.reverseOrder()))
                .map(ListingCard::from)
                .toList();
    }

    public record ListingCard(MarketplaceListing listing, String sellerClubLabel, String coverUrl, int photoCount) {
        static ListingCard from(MarketplaceListing listing) {
            String label = Club.fromCode(listing.getClubName()).map(Club::getDisplayName).orElse("");
            int count = listing.getPhotos() == null ? 0 : listing.getPhotos().size();
            return new ListingCard(listing, label, listing.getCoverUrl(), count);
        }
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
                                      List<MultipartFile> photos) {
        if (category == null || !CATEGORIES.contains(category)) {
            throw new IllegalArgumentException("Choisissez une catégorie.");
        }
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
    public void markSold(Long id) {
        MarketplaceListing listing = require(id);
        listing.setSold(true);
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
