package org.example.belgianslotclubspring.models;

import org.example.belgianslotclubspring.entities.ForumQuestion;

public record ForumQuestionCard(
        ForumQuestion question,
        long replyCount,
        long attachmentCount
) {
}
