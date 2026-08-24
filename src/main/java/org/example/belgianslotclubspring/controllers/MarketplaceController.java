package org.example.belgianslotclubspring.controllers;

import org.example.belgianslotclubspring.entities.MarketplaceListing;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.ImportAuthService;
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

    private final MarketplaceService marketplaceService;
    private final MarketplacePhotoStorage photoStorage;
    private final ImportAuthService importAuthService;

    public MarketplaceController(MarketplaceService marketplaceService,
                                 MarketplacePhotoStorage photoStorage,
                                 ImportAuthService importAuthService) {
        this.marketplaceService = marketplaceService;
        this.photoStorage = photoStorage;
        this.importAuthService = importAuthService;
    }

    @GetMapping
    public String index(
            @RequestParam(required = false) String club,
            @RequestParam(required = false) String category,
            Model model
    ) {
        addClub(model, club);
        model.addAttribute("categories", MarketplaceService.CATEGORIES);
        model.addAttribute("selectedCategory", category == null ? "" : category);
        model.addAttribute("listings", marketplaceService.list(category));
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
            @RequestParam(value = "photos", required = false) List<MultipartFile> photos,
            RedirectAttributes redirectAttributes
    ) {
        String backClub = queryClub(club);
        try {
            MarketplaceListing listing = marketplaceService.publish(
                    title, description, category, price, sellerName, contact, sellerClub, photos);
            redirectAttributes.addFlashAttribute("success", "Annonce publiée.");
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

    @PostMapping("/{id}/sold")
    public String markSold(
            @PathVariable Long id,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String club,
            RedirectAttributes redirectAttributes
    ) {
        String back = "/marketplace/" + id + queryClub(club);
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
            return "redirect:" + back;
        }
        try {
            marketplaceService.markSold(id);
            redirectAttributes.addFlashAttribute("success", "Annonce marquée comme vendue.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + back;
    }

    @PostMapping("/{id}/delete")
    public String delete(
            @PathVariable Long id,
            @RequestParam(required = false) String password,
            @RequestParam(required = false) String club,
            RedirectAttributes redirectAttributes
    ) {
        String listUrl = "/marketplace" + queryClub(club);
        if (!importAuthService.matches(password)) {
            redirectAttributes.addFlashAttribute("error", "Mot de passe incorrect.");
            return "redirect:/marketplace/" + id + queryClub(club);
        }
        try {
            marketplaceService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Annonce supprimée.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:" + listUrl;
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
