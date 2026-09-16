package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "marketplace_listing")
public class MarketplaceListing {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 4000)
    private String description;

    @Column(nullable = false, length = 40)
    private String category;

    @Column(length = 40)
    private String price;

    @Column(nullable = false, length = 40)
    private String sellerName;

    @Column(nullable = false, length = 120)
    private String contact;

    /** Club du vendeur (optionnel, pour l'affichage). L'annonce reste visible par tous. */
    @Column(name = "club_name", length = 32)
    private String clubName;

    @Column(nullable = false)
    private boolean sold = false;

    /** Hash sel:sha256 du mot de passe vendeur. Vide sur les anciennes annonces. */
    @Column(name = "seller_password_hash", length = 128)
    private String sellerPasswordHash;

    /** Compte vendeur optionnel : une fois connecté, plus besoin du mot de passe d’annonce. */
    @Column(name = "owner_account_id")
    private Long ownerAccountId;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "listing", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    private List<MarketplacePhoto> photos = new ArrayList<>();

    public void addPhoto(MarketplacePhoto photo) {
        photo.setListing(this);
        photos.add(photo);
    }

    public String getCoverUrl() {
        if (photos == null || photos.isEmpty()) {
            return "";
        }
        return photos.get(0).getUrl();
    }
}
