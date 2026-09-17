package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.belgianslotclubspring.services.AccountCookies;
import org.example.belgianslotclubspring.services.AccountService;
import org.example.belgianslotclubspring.services.AccountService.AccountView;
import org.example.belgianslotclubspring.services.MarketplaceChatService;
import org.example.belgianslotclubspring.services.MarketplaceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AccountController {

    private final AccountService accountService;
    private final MarketplaceService marketplaceService;
    private final MarketplaceChatService chatService;

    public AccountController(AccountService accountService,
                             MarketplaceService marketplaceService,
                             MarketplaceChatService chatService) {
        this.accountService = accountService;
        this.marketplaceService = marketplaceService;
        this.chatService = chatService;
    }

    @GetMapping("/compte")
    public String page(
            @RequestParam(required = false) String next,
            HttpSession session,
            Model model
    ) {
        AccountView account = AccountService.current(session);
        model.addAttribute("next", safeNext(next));
        if (account != null) {
            model.addAttribute("listings", marketplaceService.listOwned(account.id()));
            model.addAttribute("chats", chatService.listForBuyer(account.id()));
        }
        return "pages/compte";
    }

    @PostMapping("/compte/inscription")
    public String register(
            @RequestParam String name,
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam String passwordConfirm,
            @RequestParam(required = false) String next,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String back = "/compte" + nextQuery(next);
        try {
            AccountView view = accountService.register(name, email, password, passwordConfirm);
            persistLogin(request, response, session, view);
            redirectAttributes.addFlashAttribute(
                    "success",
                    view.admin()
                            ? "Compte administrateur créé. Les actions organisateur n’ont plus besoin de mot de passe."
                            : "Compte créé. Vos annonces et chats sont liés à ce compte, sans mot de passe supplémentaire.");
            return "redirect:" + safeNext(next);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + back;
        }
    }

    @PostMapping("/compte/connexion")
    public String login(
            @RequestParam String email,
            @RequestParam String password,
            @RequestParam(required = false) String next,
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String back = "/compte" + nextQuery(next);
        try {
            AccountView view = accountService.authenticate(email, password);
            persistLogin(request, response, session, view);
            redirectAttributes.addFlashAttribute(
                    "success",
                    view.admin()
                            ? "Connecté en tant qu’administrateur."
                            : "Connecté. Bienvenue " + view.name() + ".");
            return "redirect:" + safeNext(next);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + back;
        }
    }

    @GetMapping("/compte/deconnexion")
    public String logoutGet(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return logout(request, response, session, redirectAttributes);
    }

    @PostMapping("/compte/deconnexion")
    public String logout(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        AccountView current = AccountService.current(session);
        if (current != null) {
            accountService.forget(current.id());
        }
        AccountService.logout(session);
        session.invalidate();
        AccountCookies.clear(request, response);
        redirectAttributes.addFlashAttribute("success", "Vous êtes déconnecté.");
        return "redirect:/compte";
    }

    private void persistLogin(
            HttpServletRequest request,
            HttpServletResponse response,
            HttpSession session,
            AccountView view
    ) {
        session.invalidate();
        HttpSession fresh = request.getSession(true);
        AccountService.login(fresh, view);
        AccountCookies.write(request, response, accountService.issueRememberToken(view));
    }

    static String safeNext(String next) {
        if (next == null || next.isBlank()) {
            return "/compte";
        }
        String value = next.trim();
        if (!value.startsWith("/") || value.startsWith("//") || value.contains("://") || value.indexOf('\r') >= 0
                || value.indexOf('\n') >= 0) {
            return "/compte";
        }
        return value;
    }

    private static String nextQuery(String next) {
        String value = safeNext(next);
        if ("/compte".equals(value)) {
            return "";
        }
        return "?next=" + value;
    }
}
