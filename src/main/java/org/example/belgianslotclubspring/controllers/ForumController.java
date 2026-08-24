package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.entities.ForumQuestion;
import org.example.belgianslotclubspring.entities.ForumTheme;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.ForumService;
import org.example.belgianslotclubspring.services.ImportAuthService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/forum")
public class ForumController {

    private final ForumService forumService;
    private final ImportAuthService importAuthService;

    public ForumController(ForumService forumService, ImportAuthService importAuthService) {
        this.forumService = forumService;
        this.importAuthService = importAuthService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String club, Model model) {
        if (club == null || club.isBlank()) {
            return "redirect:/?goto=forum#clubs";
        }
        String clubCode = Club.requireCode(club);
        Club clubEnum = Club.fromCode(clubCode).orElseThrow();
        model.addAttribute("club", clubCode);
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
        model.addAttribute("themes", forumService.listThemes(clubCode));
        return "pages/forum";
    }

    @PostMapping("/themes")
    public String createTheme(
            @RequestParam String club,
            @RequestParam String title,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String password,
            RedirectAttributes redirectAttributes
    ) {
        String clubCode = Club.requireCode(club);
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect. Le thème n'a pas été créé.");
            return "redirect:/forum?club=" + clubCode;
        }
        try {
            forumService.createTheme(clubCode, title, description);
            redirectAttributes.addFlashAttribute("success", "Thème ajouté.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forum?club=" + clubCode;
    }

    @GetMapping("/theme/{id}")
    public String theme(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ForumTheme theme = forumService.requireTheme(id);
            Club clubEnum = Club.fromCode(theme.getClubName()).orElseThrow();
            model.addAttribute("club", theme.getClubName());
            model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
            model.addAttribute("theme", theme);
            model.addAttribute("questions", forumService.listQuestions(id));
            return "pages/forumTheme";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/?goto=forum#clubs";
        }
    }

    @PostMapping("/theme/{id}/questions")
    public String ask(
            @PathVariable Long id,
            @RequestParam String author,
            @RequestParam String title,
            @RequestParam String body,
            RedirectAttributes redirectAttributes
    ) {
        try {
            ForumQuestion question = forumService.ask(id, author, title, body);
            redirectAttributes.addFlashAttribute("success", "Question publiée.");
            return "redirect:/forum/question/" + question.getId();
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/forum/theme/" + id;
        }
    }

    @PostMapping("/theme/{id}/delete")
    public String deleteTheme(
            @PathVariable Long id,
            @RequestParam(required = false) String password,
            RedirectAttributes redirectAttributes
    ) {
        ForumTheme theme = forumService.requireTheme(id);
        String club = theme.getClubName();
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
            return "redirect:/forum/theme/" + id;
        }
        forumService.deleteTheme(id);
        redirectAttributes.addFlashAttribute("success", "Thème supprimé.");
        return "redirect:/forum?club=" + club;
    }

    @GetMapping("/question/{id}")
    public String question(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            ForumQuestion question = forumService.requireQuestion(id);
            ForumTheme theme = question.getTheme();
            Club clubEnum = Club.fromCode(theme.getClubName()).orElseThrow();
            model.addAttribute("club", theme.getClubName());
            model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
            model.addAttribute("theme", theme);
            model.addAttribute("question", question);
            return "pages/forumQuestion";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/?goto=forum#clubs";
        }
    }

    @PostMapping("/question/{id}/replies")
    public String reply(
            @PathVariable Long id,
            @RequestParam String author,
            @RequestParam String body,
            RedirectAttributes redirectAttributes
    ) {
        try {
            forumService.reply(id, author, body);
            redirectAttributes.addFlashAttribute("success", "Réponse publiée.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/forum/question/" + id;
    }

    @PostMapping("/question/{id}/delete")
    public String deleteQuestion(
            @PathVariable Long id,
            @RequestParam(required = false) String password,
            RedirectAttributes redirectAttributes
    ) {
        ForumQuestion question = forumService.requireQuestion(id);
        Long themeId = question.getTheme().getId();
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
            return "redirect:/forum/question/" + id;
        }
        forumService.deleteQuestion(id);
        redirectAttributes.addFlashAttribute("success", "Question supprimée.");
        return "redirect:/forum/theme/" + themeId;
    }

    @PostMapping("/reply/{id}/delete")
    public String deleteReply(
            @PathVariable Long id,
            @RequestParam(required = false) String password,
            RedirectAttributes redirectAttributes
    ) {
        var reply = forumService.requireReply(id);
        Long questionId = reply.getQuestion().getId();
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
            return "redirect:/forum/question/" + questionId;
        }
        forumService.deleteReply(id);
        redirectAttributes.addFlashAttribute("success", "Réponse supprimée.");
        return "redirect:/forum/question/" + questionId;
    }
}
