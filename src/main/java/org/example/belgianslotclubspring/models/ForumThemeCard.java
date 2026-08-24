package org.example.belgianslotclubspring.models;

import org.example.belgianslotclubspring.entities.ForumTheme;

import java.time.LocalDateTime;

public record ForumThemeCard(
        ForumTheme theme,
        long questionCount,
        LocalDateTime lastActivity
) {
}
