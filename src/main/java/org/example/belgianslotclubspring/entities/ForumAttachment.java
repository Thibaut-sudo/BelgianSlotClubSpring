package org.example.belgianslotclubspring.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.Locale;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "forum_attachment")
public class ForumAttachment {

    private static final Set<String> IMAGE_EXT = Set.of("jpg", "png", "webp", "gif");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private ForumQuestion question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reply_id")
    private ForumReply reply;

    @Column(nullable = false, length = 80)
    private String storedName;

    @Column(nullable = false, length = 120)
    private String originalName;

    @Column(nullable = false, length = 80)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private int sortOrder;

    public String getUrl() {
        return "/forum/attachment/" + id;
    }

    public boolean isImage() {
        return IMAGE_EXT.contains(extension());
    }

    public boolean isPdf() {
        return "pdf".equals(extension());
    }

    public String getDisplaySize() {
        if (sizeBytes < 1024) {
            return sizeBytes + " o";
        }
        if (sizeBytes < 1024 * 1024) {
            return (sizeBytes / 1024) + " Ko";
        }
        return String.format(Locale.FRANCE, "%.1f Mo", sizeBytes / (1024.0 * 1024.0));
    }

    private String extension() {
        int dot = storedName.lastIndexOf('.');
        if (dot < 0 || dot == storedName.length() - 1) {
            return "";
        }
        return storedName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
