package org.example.belgianslotclubspring.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "marketplace_thread")
public class MarketplaceThread {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "listing_id", nullable = false)
    private MarketplaceListing listing;

    @Column(nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(nullable = false, length = 40)
    private String buyerName;

    /** Hash du mot de passe chat (invité). Null si le chat est lié à un compte. */
    @Column(name = "buyer_password_hash", length = 128)
    private String buyerPasswordHash;

    /** Compte acheteur optionnel : une fois connecté, plus besoin du mot de passe du chat. */
    @Column(name = "buyer_account_id")
    private Long buyerAccountId;

    @Column(length = 120)
    private String lastPreview;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC, id ASC")
    private List<MarketplaceMessage> messages = new ArrayList<>();

    public void addMessage(MarketplaceMessage message) {
        message.setThread(this);
        messages.add(message);
    }
}
