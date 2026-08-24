package org.example.belgianslotclubspring.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "forum_reply")
public class ForumReply {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private ForumQuestion question;

    @Column(nullable = false, length = 40)
    private String author;

    @Column(nullable = false, length = 4000)
    private String body;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
