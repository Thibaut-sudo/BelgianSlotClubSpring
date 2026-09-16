package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpSession;
import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.entities.MarketplaceThread;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.AccountService;
import org.example.belgianslotclubspring.services.MarketplaceChatService;
import org.example.belgianslotclubspring.services.MarketplaceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/marketplace")
public class MarketplaceChatController {

    private static final String MANAGE_SESSION = "marketplace.manage.";
    private static final String CHAT_ROLE = "marketplace.chat.role.";

    private final MarketplaceChatService chatService;

    public MarketplaceChatController(MarketplaceChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/{id}/chat")
    public String start(
            @PathVariable Long id,
            @RequestParam(required = false) String buyerName,
            @RequestParam String message,
            @RequestParam(required = false) String chatPassword,
            @RequestParam(required = false) String chatPasswordConfirm,
            @RequestParam(required = false) String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String listingUrl = "/marketplace/" + id + queryClub(club);
        try {
            AccountService.AccountView account = AccountService.current(session);
            String name = buyerName;
            if ((name == null || name.isBlank()) && account != null) {
                name = account.name();
            }
            MarketplaceThread thread = chatService.start(
                    id, name, message, chatPassword, chatPasswordConfirm, AccountService.currentId(session));
            session.setAttribute(CHAT_ROLE + thread.getPublicId(), MarketplaceChatService.ROLE_BUYER);
            if (account != null) {
                redirectAttributes.addFlashAttribute(
                        "success",
                        "Conversation privée créée. Elle est liée à votre compte : vous la retrouverez dans Compte.");
            } else {
                redirectAttributes.addFlashAttribute(
                        "success",
                        "Conversation privée créée. Conservez ce lien et le mot de passe du chat pour y revenir.");
            }
            return "redirect:/marketplace/chat/" + thread.getPublicId() + queryClub(club);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + listingUrl;
        }
    }

    @GetMapping("/chat/{publicId}")
    public String view(
            @PathVariable String publicId,
            @RequestParam(required = false) String club,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            MarketplaceThread thread = chatService.requireByPublicId(publicId);
            MarketplaceListing listing = thread.getListing();
            String role = currentRole(session, thread);
            addClub(model, club);
            model.addAttribute("thread", thread);
            model.addAttribute("listing", listing);
            model.addAttribute("role", role);
            model.addAttribute("unlocked", role != null);
            model.addAttribute("sellerClubName", Club.fromCode(listing.getClubName())
                    .map(Club::getDisplayName)
                    .orElse(""));
            return "pages/marketplaceChat";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketplace" + queryClub(club);
        }
    }

    @PostMapping("/chat/{publicId}")
    public String unlock(
            @PathVariable String publicId,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String back = "/marketplace/chat/" + publicId + queryClub(club);
        try {
            MarketplaceThread thread = chatService.requireByPublicId(publicId);
            String role = chatService.resolveRole(
                    thread, password, AccountService.currentId(session), AccountService.isAdmin(session));
            if (role == null) {
                redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
                return "redirect:" + back;
            }
            if (MarketplaceChatService.ROLE_SELLER.equals(role)) {
                session.setAttribute(MANAGE_SESSION + thread.getListing().getId(), Boolean.TRUE);
            }
            session.setAttribute(CHAT_ROLE + publicId, role);
            return "redirect:" + back;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketplace" + queryClub(club);
        }
    }

    @PostMapping("/chat/{publicId}/message")
    public String reply(
            @PathVariable String publicId,
            @RequestParam String message,
            @RequestParam(required = false) String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String back = "/marketplace/chat/" + publicId + queryClub(club);
        try {
            MarketplaceThread thread = chatService.requireByPublicId(publicId);
            String role = currentRole(session, thread);
            if (role == null) {
                redirectAttributes.addFlashAttribute("error", "Connectez-vous ou saisissez le mot de passe du chat.");
                return "redirect:" + back;
            }
            chatService.reply(publicId, MarketplaceChatService.ROLE_SELLER.equals(role), message);
            return "redirect:" + back;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + back;
        }
    }

    private String currentRole(HttpSession session, MarketplaceThread thread) {
        String fromAccount = chatService.resolveRole(
                thread, null, AccountService.currentId(session), AccountService.isAdmin(session));
        if (fromAccount != null) {
            return fromAccount;
        }
        if (Boolean.TRUE.equals(session.getAttribute(MANAGE_SESSION + thread.getListing().getId()))) {
            return MarketplaceChatService.ROLE_SELLER;
        }
        Object role = session.getAttribute(CHAT_ROLE + thread.getPublicId());
        if (MarketplaceChatService.ROLE_BUYER.equals(role) || MarketplaceChatService.ROLE_SELLER.equals(role)) {
            return (String) role;
        }
        return null;
    }

    private static void addClub(Model model, String club) {
        Club resolved = Club.fromCode(club).orElse(null);
        if (resolved != null) {
            model.addAttribute("club", resolved.getCode());
            model.addAttribute("clubDisplayName", resolved.getDisplayName());
        } else {
            model.addAttribute("club", null);
            model.addAttribute("clubDisplayName", "");
        }
    }

    private static String queryClub(String club) {
        return Club.fromCode(club).map(c -> "?club=" + c.getCode()).orElse("");
    }
}
