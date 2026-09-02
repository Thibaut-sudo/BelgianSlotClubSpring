package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.models.GlobalCalendarEvent;
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
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
            "/calendrier.ics",
            "/calendrier/feed.ics",
            "/calendrier/all.ics",
            "/calendrier/all/ics",
            "/calendrier/all/v3.ics"
    })
    public ResponseEntity<byte[]> icsAll(HttpServletRequest request) {
        Map<String, List<GlobalCalendarEvent>> events = clubCalendarService.allEventsByDate();
        String etagValue = ClubIcsCalendar.contentEtagAll(events);
        if (etagMatches(request.getHeader(HttpHeaders.IF_NONE_MATCH), etagValue)) {
            return notModified(etagValue);
        }
        byte[] body = ClubIcsCalendar.buildAll(events).getBytes(StandardCharsets.UTF_8);
        return icsOk(etagValue, body, ClubIcsCalendar.fileNameAll());
    }

    @GetMapping({"/calendrier/google", "/calendrier/all/google"})
    public String googleAll(HttpServletRequest request, Model model) {
        String icsUrl = publicBase(request) + ClubIcsCalendar.publicFeedPathAll();
        String webcal = icsUrl.replaceFirst("^https://", "webcal://").replaceFirst("^http://", "webcal://");
        String googleUrl = "https://calendar.google.com/calendar/r?cid="
                + URLEncoder.encode(webcal, StandardCharsets.UTF_8);
        model.addAttribute("club", null);
        model.addAttribute("combined", true);
        model.addAttribute("clubDisplayName", "Belgian Slot Club");
        model.addAttribute("icsUrl", icsUrl);
        model.addAttribute("googleUrl", googleUrl);
        return "pages/ajouterGoogleAgenda";
    }

    @GetMapping({"/calendrier/apple", "/calendrier/all/apple"})
    public String appleAll(HttpServletRequest request) {
        String httpsUrl = publicBase(request) + ClubIcsCalendar.publicFeedPathAll();
        String webcal = httpsUrl.replaceFirst("^https://", "webcal://").replaceFirst("^http://", "webcal://");
        return "redirect:" + webcal;
    }

    @GetMapping({
            "/calendrier/{club}/ics",
            "/calendrier/{club}.ics",
            "/calendrier/{club}/feed",
            "/calendrier/{club}/feed.ics",
            "/calendrier/{club}/v2.ics",
            "/calendrier/{club}/v3.ics"
    })
    public ResponseEntity<byte[]> ics(@PathVariable String club, HttpServletRequest request) {
        Optional<Club> parsed = Club.fromCode(stripIcsSuffix(club));
        if (parsed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Club clubEnum = parsed.get();
        var events = clubCalendarService.eventsFor(clubEnum);
        String etagValue = ClubIcsCalendar.contentEtag(clubEnum, events);
        if (etagMatches(request.getHeader(HttpHeaders.IF_NONE_MATCH), etagValue)) {
            return notModified(etagValue);
        }
        byte[] body = ClubIcsCalendar.build(clubEnum, events).getBytes(StandardCharsets.UTF_8);
        return icsOk(etagValue, body, ClubIcsCalendar.fileName(clubEnum));
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
        model.addAttribute("combined", false);
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

    private ResponseEntity<byte[]> icsOk(String etagValue, byte[] body, String fileName) {
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .eTag(etagValue)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, max-age=0, must-revalidate")
                .header("Pragma", "no-cache")
                .header("CDN-Cache-Control", "no-store")
                .header("Cloudflare-CDN-Cache-Control", "no-store")
                .body(body);
    }

    private static ResponseEntity<byte[]> notModified(String etagValue) {
        return ResponseEntity.status(304)
                .eTag(etagValue)
                .header(HttpHeaders.CACHE_CONTROL, "no-store, no-cache, max-age=0, must-revalidate")
                .header("CDN-Cache-Control", "no-store")
                .header("Cloudflare-CDN-Cache-Control", "no-store")
                .build();
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

    private static boolean etagMatches(String ifNoneMatch, String etagValue) {
        if (ifNoneMatch == null || ifNoneMatch.isBlank() || etagValue == null) {
            return false;
        }
        String quoted = "\"" + etagValue + "\"";
        for (String part : ifNoneMatch.split(",")) {
            String token = part.trim();
            if (token.equals(etagValue) || token.equals(quoted) || token.equals("W/" + quoted)) {
                return true;
            }
        }
        return false;
    }
}
