package org.example.belgianslotclubspring.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Cache court sur les pages stables (classements, règlement) pour Cloudflare / navigateurs.
 * Rallye, forum et marketplace restent sans cache.
 */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class PublicPageCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        filterChain.doFilter(request, response);
        if (response.getHeader("Cache-Control") != null) {
            return;
        }
        if (response.getStatus() != HttpServletResponse.SC_OK) {
            return;
        }
        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.HEAD.matches(request.getMethod())) {
            response.setHeader("Cache-Control", "no-store");
            return;
        }
        String path = request.getRequestURI();
        if (isLive(path)) {
            response.setHeader("Cache-Control", "no-store");
            return;
        }
        if (isPublicPage(path)) {
            response.setHeader("Cache-Control", "public, max-age=45");
            response.setHeader("Vary", "Accept-Encoding");
        }
    }

    private static boolean isLive(String path) {
        return path.startsWith("/rallye")
                || path.startsWith("/forum")
                || path.startsWith("/marketplace")
                || path.startsWith("/upload")
                || path.startsWith("/api/")
                || path.startsWith("/actuator")
                || path.startsWith("/h2-console");
    }

    private static boolean isPublicPage(String path) {
        return "/".equals(path)
                || path.startsWith("/selectRace")
                || path.startsWith("/championnat")
                || path.startsWith("/reglement")
                || path.startsWith("/contact")
                || path.startsWith("/statistiques")
                || path.startsWith("/prochain-evenement")
                || path.startsWith("/calendrier")
                || path.startsWith("/docs/");
    }
}
