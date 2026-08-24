package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.example.belgianslotclubspring.models.Club;

@Getter
@Setter
@Entity
@Table(
        name = "forum_theme",
        uniqueConstraints = @UniqueConstraint(columnNames = {"club_name", "code"})
)
public class ForumTheme {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_name", nullable = false, length = 32)
    private String clubName;

    @Column(nullable = false, length = 40)
    private String code;

    @Column(nullable = false, length = 80)
    private String title;

    @Column(length = 240)
    private String description;

    @Column(nullable = false)
    private int sortOrder;

    public ForumTheme() {
    }

    public ForumTheme(String club, String code, String title, String description, int sortOrder) {
        this.clubName = Club.requireCode(club);
        this.code = code;
        this.title = title;
        this.description = description;
        this.sortOrder = sortOrder;
    }
}
