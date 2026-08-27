package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.ClubCalendarService;
import org.example.belgianslotclubspring.services.ShareUrlService;
import org.example.belgianslotclubspring.utils.ClubIcsCalendar;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Controller
public class ClubCalendarFeedController {

    private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final ShareUrlService shareUrlService;
    private final ClubCalendarService clubCalendarService;

    public ClubCalendarFeedController(ShareUrlService shareUrlService,
                                      ClubCalendarService clubCalendarService) {
        this.shareUrlService = shareUrlService;
        this.clubCalendarService = clubCalendarService;
    }

    @GetMapping({
            "/calendrier/{club}/ics",
            "/calendrier/{club}.ics",
            "/calendrier/{club}/feed",
            "/calendrier/{club}/feed.ics",
            "/calendrier/{club}/v2.ics"
    })
    public ResponseEntity<byte[]> ics(@PathVariable String club) {
        Optional<Club> parsed = Club.fromCode(stripIcsSuffix(club));
        if (parsed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Club clubEnum = parsed.get();
        byte[] body = ClubIcsCalendar.build(clubEnum, clubCalendarService.eventsFor(clubEnum))
                .getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + ClubIcsCalendar.fileName(clubEnum) + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, max-age=0, must-revalidate")
                .header("Pragma", "no-cache")
                .header("CDN-Cache-Control", "no-store")
                .header("Cloudflare-CDN-Cache-Control", "no-store")
                .body(body);
    }

    @GetMapping("/calendrier/{club}/google")
    public String google(@PathVariable String club, HttpServletRequest request, Model model) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return "redirect:/#clubs";
        }
        Club clubEnum = parsed.get();
        String icsUrl = publicBase(request) + ClubIcsCalendar.publicFeedPath(clubEnum);
        String webcal = icsUrl.replaceFirst("^https://", "webcal://").replaceFirst("^http://", "webcal://");
        // Google refuse souvent cid=https://… (« Vérifiez l'URL ») ; webcal:// est accepté.
        String googleUrl = "https://calendar.google.com/calendar/r?cid="
                + URLEncoder.encode(webcal, StandardCharsets.UTF_8);
        model.addAttribute("club", clubEnum.getCode());
        model.addAttribute("clubDisplayName", clubEnum.getDisplayName());
        model.addAttribute("icsUrl", icsUrl);
        model.addAttribute("googleUrl", googleUrl);
        return "pages/ajouterGoogleAgenda";
    }

    @GetMapping("/calendrier/{club}/apple")
    public String apple(@PathVariable String club, HttpServletRequest request) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return "redirect:/#clubs";
        }
        String httpsUrl = publicBase(request) + ClubIcsCalendar.publicFeedPath(parsed.get());
        String webcal = httpsUrl.replaceFirst("^https://", "webcal://").replaceFirst("^http://", "webcal://");
        return "redirect:" + webcal;
    }

    private String publicBase(HttpServletRequest request) {
        String scheme = firstForwarded(request.getHeader("X-Forwarded-Proto"), request.getScheme());
        String hostHeader = firstForwarded(request.getHeader("X-Forwarded-Host"), request.getHeader("Host"));
        if (hostHeader == null || hostHeader.isBlank()) {
            hostHeader = request.getServerName();
        }
        String host = hostHeader;
        int port = request.getServerPort();
        int colon = hostHeader.indexOf(':');
        if (colon > 0) {
            host = hostHeader.substring(0, colon);
            try {
                port = Integer.parseInt(hostHeader.substring(colon + 1));
            } catch (NumberFormatException ignored) {
                // keep server port
            }
        } else if ("https".equalsIgnoreCase(scheme)) {
            port = 443;
        } else if ("http".equalsIgnoreCase(scheme) && request.getHeader("X-Forwarded-Proto") != null) {
            port = 80;
        }
        return shareUrlService.resolve(scheme, host, port).baseUrl();
    }

    private static String stripIcsSuffix(String club) {
        if (club != null && club.toLowerCase(Locale.ROOT).endsWith(".ics")) {
            return club.substring(0, club.length() - 4);
        }
        return club;
    }

    private static String firstForwarded(String header, String fallback) {
        if (header == null || header.isBlank()) {
            return fallback;
        }
        String first = header.split(",")[0].trim();
        return first.isEmpty() ? fallback : first;
    }
}
