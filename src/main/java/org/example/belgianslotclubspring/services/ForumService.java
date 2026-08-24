package org.example.belgianslotclubspring.services;

import org.example.belgianslotclubspring.entities.ForumQuestion;
import org.example.belgianslotclubspring.entities.ForumReply;
import org.example.belgianslotclubspring.entities.ForumTheme;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.ForumQuestionCard;
import org.example.belgianslotclubspring.models.ForumThemeCard;
import org.example.belgianslotclubspring.repo.ForumQuestionRepo;
import org.example.belgianslotclubspring.repo.ForumReplyRepo;
import org.example.belgianslotclubspring.repo.ForumThemeRepo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class ForumService {

    private static final int AUTHOR_MAX = 40;
    private static final int TITLE_MAX = 120;
    private static final int BODY_MAX = 4000;
    private static final int THEME_TITLE_MAX = 80;
    private static final int THEME_DESC_MAX = 240;

    private static final List<DefaultTheme> DEFAULT_THEMES = List.of(
            new DefaultTheme("reglement", "Règlement", "Questions sur le règlement sportif et technique.", 10),
            new DefaultTheme("technique", "Technique", "Moteurs, châssis, pneus, montage et réglages.", 20),
            new DefaultTheme("organisation", "Organisation", "Horaires, inscriptions, courses et calendrier.", 30),
            new DefaultTheme("divers", "Divers", "Tout ce qui ne rentre pas dans les autres thèmes.", 40)
    );

    private final ForumThemeRepo themeRepo;
    private final ForumQuestionRepo questionRepo;
    private final ForumReplyRepo replyRepo;

    public ForumService(ForumThemeRepo themeRepo,
                        ForumQuestionRepo questionRepo,
                        ForumReplyRepo replyRepo) {
        this.themeRepo = themeRepo;
        this.questionRepo = questionRepo;
        this.replyRepo = replyRepo;
    }

    @Transactional
    public List<ForumThemeCard> listThemes(String club) {
        String clubCode = Club.requireCode(club);
        ensureDefaultThemes(clubCode);
        List<ForumThemeCard> cards = new ArrayList<>();
        for (ForumTheme theme : themeRepo.findByClubNameOrderBySortOrderAscTitleAsc(clubCode)) {
            long count = questionRepo.countByThemeId(theme.getId());
            LocalDateTime last = questionRepo.findFirstByThemeIdOrderByCreatedAtDesc(theme.getId())
                    .map(ForumQuestion::getCreatedAt)
                    .orElse(null);
            cards.add(new ForumThemeCard(theme, count, last));
        }
        return cards;
    }

    @Transactional
    public ForumTheme requireTheme(Long id) {
        return themeRepo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Thème introuvable."));
    }

    @Transactional
    public List<ForumQuestionCard> listQuestions(Long themeId) {
        requireTheme(themeId);
        List<ForumQuestionCard> cards = new ArrayList<>();
        for (ForumQuestion question : questionRepo.findByThemeIdOrderByCreatedAtDesc(themeId)) {
            cards.add(new ForumQuestionCard(question, replyRepo.countByQuestionId(question.getId())));
        }
        return cards;
    }

    @Transactional
    public ForumQuestion requireQuestion(Long id) {
        return questionRepo.findDetailedById(id)
                .or(() -> questionRepo.findById(id))
                .orElseThrow(() -> new IllegalArgumentException("Question introuvable."));
    }

    @Transactional
    public ForumQuestion ask(Long themeId, String author, String title, String body) {
        ForumTheme theme = requireTheme(themeId);
        ForumQuestion question = new ForumQuestion();
        question.setTheme(theme);
        question.setAuthor(cleanLine(author, AUTHOR_MAX, "un nom"));
        question.setTitle(cleanLine(title, TITLE_MAX, "un titre"));
        question.setBody(cleanBody(body, BODY_MAX, "une question"));
        question.setCreatedAt(LocalDateTime.now());
        return questionRepo.save(question);
    }

    @Transactional
    public ForumReply reply(Long questionId, String author, String body) {
        ForumQuestion question = requireQuestion(questionId);
        ForumReply reply = new ForumReply();
        reply.setQuestion(question);
        reply.setAuthor(cleanLine(author, AUTHOR_MAX, "un nom"));
        reply.setBody(cleanBody(body, BODY_MAX, "une réponse"));
        reply.setCreatedAt(LocalDateTime.now());
        question.getReplies().add(reply);
        return replyRepo.save(reply);
    }

    @Transactional
    public ForumTheme createTheme(String club, String title, String description) {
        String clubCode = Club.requireCode(club);
        String cleanTitle = cleanLine(title, THEME_TITLE_MAX, "un nom de thème");
        String code = slug(cleanTitle);
        if (themeRepo.findByClubNameAndCode(clubCode, code).isPresent()) {
            throw new IllegalArgumentException("Un thème avec ce nom existe déjà.");
        }
        int nextOrder = themeRepo.findByClubNameOrderBySortOrderAscTitleAsc(clubCode).stream()
                .mapToInt(ForumTheme::getSortOrder)
                .max()
                .orElse(0) + 10;
        ForumTheme theme = new ForumTheme(
                clubCode,
                code,
                cleanTitle,
                cleanOptionalLine(description, THEME_DESC_MAX),
                nextOrder
        );
        return themeRepo.save(theme);
    }

    @Transactional
    public String deleteTheme(Long themeId) {
        ForumTheme theme = requireTheme(themeId);
        String club = theme.getClubName();
        for (ForumQuestion question : questionRepo.findByThemeIdOrderByCreatedAtDesc(themeId)) {
            questionRepo.delete(question);
        }
        themeRepo.delete(theme);
        return club;
    }

    @Transactional
    public ForumTheme deleteQuestion(Long questionId) {
        ForumQuestion question = requireQuestion(questionId);
        ForumTheme theme = question.getTheme();
        questionRepo.delete(question);
        return theme;
    }

    @Transactional
    public ForumReply requireReply(Long id) {
        return replyRepo.findByIdWithQuestion(id)
                .orElseThrow(() -> new IllegalArgumentException("Réponse introuvable."));
    }

    @Transactional
    public ForumQuestion deleteReply(Long replyId) {
        ForumReply reply = requireReply(replyId);
        ForumQuestion question = reply.getQuestion();
        question.getReplies().remove(reply);
        replyRepo.delete(reply);
        return question;
    }

    private void ensureDefaultThemes(String clubCode) {
        for (DefaultTheme def : DEFAULT_THEMES) {
            if (themeRepo.findByClubNameAndCode(clubCode, def.code()).isEmpty()) {
                themeRepo.save(new ForumTheme(clubCode, def.code(), def.title(), def.description(), def.sortOrder()));
            }
        }
    }

    static String cleanLine(String raw, int max, String label) {
        String value = cleanOptionalLine(raw, max);
        if (value.isBlank()) {
            throw new IllegalArgumentException("Indiquez " + label + ".");
        }
        return value;
    }

    static String cleanBody(String raw, int max, String label) {
        if (raw == null) {
            throw new IllegalArgumentException("Indiquez " + label + ".");
        }
        String value = raw.replace('\u0000', ' ').replace("\r\n", "\n").replace('\r', '\n').trim();
        if (value.isBlank()) {
            throw new IllegalArgumentException("Indiquez " + label + ".");
        }
        if (value.length() > max) {
            return value.substring(0, max);
        }
        return value;
    }

    static String cleanOptionalLine(String raw, int max) {
        if (raw == null) {
            return "";
        }
        String value = raw.replace('\u0000', ' ').trim().replaceAll("\\s+", " ");
        if (value.length() > max) {
            return value.substring(0, max);
        }
        return value;
    }

    static String slug(String title) {
        String slug = title.toLowerCase(Locale.ROOT)
                .replaceAll("[àáâä]", "a")
                .replaceAll("[éèêë]", "e")
                .replaceAll("[îï]", "i")
                .replaceAll("[ôö]", "o")
                .replaceAll("[ùûü]", "u")
                .replaceAll("[ç]", "c")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
        if (slug.isBlank()) {
            slug = "theme";
        }
        return slug.length() > 40 ? slug.substring(0, 40) : slug;
    }

    private record DefaultTheme(String code, String title, String description, int sortOrder) {
    }
}
