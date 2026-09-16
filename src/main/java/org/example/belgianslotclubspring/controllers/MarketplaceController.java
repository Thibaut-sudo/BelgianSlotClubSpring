package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpSession;
import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.MarketplacePhotoStorage;
import org.example.belgianslotclubspring.services.MarketplaceService;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.MalformedURLException;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/marketplace")
public class MarketplaceController {

    private static final String MANAGE_SESSION = "marketplace.manage.";

    private final MarketplaceService marketplaceService;
    private final MarketplacePhotoStorage photoStorage;

    public MarketplaceController(MarketplaceService marketplaceService,
                                 MarketplacePhotoStorage photoStorage) {
        this.marketplaceService = marketplaceService;
        this.photoStorage = photoStorage;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String club,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String tri,
            Model model
    ) {
        addClub(model, club);
        String sort = MarketplaceService.normalizeSort(tri);
        model.addAttribute("categories", MarketplaceService.CATEGORIES);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("selectedSort", sort);
        model.addAttribute("listings", marketplaceService.list(category, sort));
        model.addAttribute("clubs", Club.values());
        return "pages/marketplace";
    }

    @PostMapping
    public String publish(
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam(required = false) String price,
            @RequestParam String sellerName,
            @RequestParam String contact,
            @RequestParam(required = false) String sellerClub,
            @RequestParam(required = false) String club,
            @RequestParam String sellerPassword,
            @RequestParam String sellerPasswordConfirm,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            RedirectAttributes redirectAttributes
    ) {
        String backClub = queryClub(club);
        try {
            MarketplaceListing listing = marketplaceService.publish(
                    title, description, category, price, sellerName, contact, sellerClub,
                    sellerPassword, sellerPasswordConfirm, photos);
            redirectAttributes.addFlashAttribute(
                    "success",
                    "Annonce publiée. Conservez le mot de passe de l’annonce pour la modifier ou la marquer vendue.");
            return "redirect:/marketplace/" + listing.getId() + backClub;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketplace" + backClub;
        }
    }

    @GetMapping("/photo/{filename}")
    public ResponseEntity<Resource> photo(@PathVariable String filename) {
        try {
            Resource resource = new UrlResource(photoStorage.resolvePublic(filename).toUri());
            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(photoStorage.mediaType(filename))
                    .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                    .body(resource);
        } catch (IllegalArgumentException | MalformedURLException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}")
    public String detail(
            @PathVariable Long id,
            @RequestParam(required = false) String club,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            MarketplaceListing listing = marketplaceService.require(id);
            addClub(model, club);
            model.addAttribute("listing", listing);
            model.addAttribute("sellerClubName", Club.fromCode(listing.getClubName())
                    .map(Club::getDisplayName)
                    .orElse(""));
            return "pages/marketplaceDetail";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketplace" + queryClub(club);
        }
    }

    @GetMapping("/{id}/gerer")
    public String manage(
            @PathVariable Long id,
            @RequestParam(required = false) String club,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        try {
            MarketplaceListing listing = marketplaceService.require(id);
            addClub(model, club);
            model.addAttribute("listing", listing);
            model.addAttribute("categories", MarketplaceService.CATEGORIES);
            model.addAttribute("clubs", Club.values());
            model.addAttribute("unlocked", isUnlocked(session, id));
            model.addAttribute("hasSellerLock", marketplaceService.hasSellerLock(listing));
            model.addAttribute("photoSlots",
                    Math.max(0, MarketplacePhotoStorage.MAX_PHOTOS
                            - (listing.getPhotos() == null ? 0 : listing.getPhotos().size())));
            return "pages/marketplaceGerer";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketplace" + queryClub(club);
        }
    }

    @PostMapping("/{id}/gerer")
    public String unlock(
            @PathVariable Long id,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String back = "/marketplace/" + id + "/gerer" + queryClub(club);
        try {
            MarketplaceListing listing = marketplaceService.require(id);
            if (!marketplaceService.canManage(listing, password)) {
                redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
                return "redirect:" + back;
            }
            unlock(session, id);
            redirectAttributes.addFlashAttribute("success", "Annonce déverrouillée. Vous pouvez la modifier.");
            return "redirect:" + back;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/marketplace" + queryClub(club);
        }
    }

    @PostMapping("/{id}/update")
    public String update(
            @PathVariable Long id,
            @RequestParam String title,
            @RequestParam String description,
            @RequestParam String category,
            @RequestParam(required = false) String price,
            @RequestParam String sellerName,
            @RequestParam String contact,
            @RequestParam(required = false) String sellerClub,
            @RequestParam(required = false) String club,
            @RequestParam(required = false) String newSellerPassword,
            @RequestParam(required = false) String newSellerPasswordConfirm,
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String manageUrl = "/marketplace/" + id + "/gerer" + queryClub(club);
        if (!isUnlocked(session, id)) {
            redirectAttributes.addFlashAttribute("error", "Saisissez d’abord le mot de passe de l’annonce.");
            return "redirect:" + manageUrl;
        }
        try {
            marketplaceService.update(
                    id, title, description, category, price, sellerName, contact, sellerClub,
                    newSellerPassword, newSellerPasswordConfirm, photos);
            redirectAttributes.addFlashAttribute("success", "Annonce mise à jour.");
            return "redirect:/marketplace/" + id + queryClub(club);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + manageUrl;
        }
    }

    @PostMapping("/{id}/sold")
    public String markSold(
            @PathVariable Long id,
            @RequestParam(required = false) String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return setSold(id, true, club, session, redirectAttributes);
    }

    @PostMapping("/{id}/reopen")
    public String reopen(
            @PathVariable Long id,
            @RequestParam(required = false) String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        return setSold(id, false, club, session, redirectAttributes);
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @RequestParam(required = false) String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String manageUrl = "/marketplace/" + id + "/gerer" + queryClub(club);
        if (!isUnlocked(session, id)) {
            redirectAttributes.addFlashAttribute("error", "Saisissez d’abord le mot de passe de l’annonce.");
            return "redirect:" + manageUrl;
        }
        String listUrl = "/marketplace" + queryClub(club);
        try {
            marketplaceService.delete(id);
            lock(session, id);
            redirectAttributes.addFlashAttribute("success", "Annonce supprimée.");
            return "redirect:" + listUrl;
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + manageUrl;
        }
    }

    private String setSold(
            Long id,
            boolean sold,
            String club,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        String manageUrl = "/marketplace/" + id + "/gerer" + queryClub(club);
        if (!isUnlocked(session, id)) {
            redirectAttributes.addFlashAttribute("error", "Saisissez d’abord le mot de passe de l’annonce.");
            return "redirect:" + manageUrl;
        }
        try {
            marketplaceService.markSold(id, sold);
            redirectAttributes.addFlashAttribute(
                    "success",
                    sold ? "Annonce marquée comme vendue." : "Annonce remise en vente.");
            return "redirect:/marketplace/" + id + queryClub(club);
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:" + manageUrl;
        }
    }

    private static boolean isUnlocked(HttpSession session, Long id) {
        return Boolean.TRUE.equals(session.getAttribute(MANAGE_SESSION + id));
    }

    private static void unlock(HttpSession session, Long id) {
        session.setAttribute(MANAGE_SESSION + id, Boolean.TRUE);
    }

    private static void lock(HttpSession session, Long id) {
        session.removeAttribute(MANAGE_SESSION + id);
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
