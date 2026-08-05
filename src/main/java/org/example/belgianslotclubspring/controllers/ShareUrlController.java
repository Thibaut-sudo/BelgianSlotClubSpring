package org.example.belgianslotclubspring.controllers;

import jakarta.servlet.http.HttpServletRequest;
import org.example.belgianslotclubspring.services.ShareUrlService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ShareUrlController {

    private final ShareUrlService shareUrlService;

    public ShareUrlController(ShareUrlService shareUrlService) {
        this.shareUrlService = shareUrlService;
    }

    @GetMapping("/api/share-base")
    public Map<String, String> shareBase(HttpServletRequest request) {
        String scheme = request.getScheme();
        String host = request.getServerName();
        int port = request.getServerPort();
        ShareUrlService.ShareBase base = shareUrlService.resolve(scheme, host, port);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("baseUrl", base.baseUrl());
        body.put("source", base.source());
        return body;
    }
}
