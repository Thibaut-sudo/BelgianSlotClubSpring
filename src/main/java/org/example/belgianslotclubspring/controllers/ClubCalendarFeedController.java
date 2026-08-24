package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.example.belgianslotclubspring.models.Club;
import org.example.belgianslotclubspring.services.ShareUrlService;
import org.example.belgianslotclubspring.utils.ClubIcsCalendar;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.servlet.view.RedirectView;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Optional;

@Controller
public class ClubCalendarFeedController {

    private static final MediaType TEXT_CALENDAR = new MediaType("text", "calendar", StandardCharsets.UTF_8);

    private final ShareUrlService shareUrlService;

    public ClubCalendarFeedController(ShareUrlService shareUrlService) {
        this.shareUrlService = shareUrlService;
    }

    @GetMapping({"/calendrier/{club}/ics", "/calendrier/{club}.ics"})
    public ResponseEntity<byte[]> ics(@PathVariable String club) {
        Optional<Club> parsed = Club.fromCode(stripIcsSuffix(club));
        if (parsed.isEmpty()) {
            return ResponseEntity.notFound().build();
        }
        Club clubEnum = parsed.get();
        byte[] body = ClubIcsCalendar.build(clubEnum).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .contentType(TEXT_CALENDAR)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + ClubIcsCalendar.fileName(clubEnum) + "\"")
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(body);
    }

    @GetMapping("/calendrier/{club}/google")
    public RedirectView google(@PathVariable String club, HttpServletRequest request) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return new RedirectView("/#clubs");
        }
        String icsUrl = publicBase(request) + "/calendrier/" + parsed.get().getCode() + ".ics";
        String google = "https://calendar.google.com/calendar/render?cid="
                + URLEncoder.encode(icsUrl, StandardCharsets.UTF_8);
        RedirectView view = new RedirectView(google);
        view.setExposeModelAttributes(false);
        return view;
    }

    @GetMapping("/calendrier/{club}/apple")
    public RedirectView apple(@PathVariable String club, HttpServletRequest request) {
        Optional<Club> parsed = Club.fromCode(club);
        if (parsed.isEmpty()) {
            return new RedirectView("/#clubs");
        }
        String httpsUrl = publicBase(request) + "/calendrier/" + parsed.get().getCode() + ".ics";
        String webcal = httpsUrl.replaceFirst("^https://", "webcal://").replaceFirst("^http://", "webcal://");
        RedirectView view = new RedirectView(webcal);
        view.setExposeModelAttributes(false);
        return view;
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
