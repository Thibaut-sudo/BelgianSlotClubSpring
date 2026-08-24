package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.example.belgianslotclubspring.services.ShareUrlService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@RestController
public class ShareUrlController {

    private final ShareUrlService shareUrlService;

    public ShareUrlController(ShareUrlService shareUrlService) {
        this.shareUrlService = shareUrlService;
    }

    @GetMapping("/api/share-base")
    public Map<String, String> shareBase(HttpServletRequest request) {
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

        ShareUrlService.ShareBase base = shareUrlService.resolve(scheme, host, port);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("baseUrl", base.baseUrl());
        body.put("source", base.source());
        return body;
    }

    private static String firstForwarded(String header, String fallback) {
        if (header == null || header.isBlank()) {
            return fallback;
        }
        String first = header.split(",")[0].trim();
        return first.isEmpty() ? fallback : first.toLowerCase(Locale.ROOT).contains("://")
                ? fallback
                : first;
    }
}
