package org.example.belgianslotclubspring.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.example.belgianslotclubspring.services.AccountService;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Pas de cache public sur le HTML : la nav affiche le prénom du compte.
 * Un max-age public servait l’accueil / classements / contact anonymes (« Compte »)
 * aux visiteurs déjà connectés (Cloudflare et cache navigateur).
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
            noStore(response);
            return;
        }
        String path = request.getRequestURI();
        if (path.startsWith("/docs/")) {
            return;
        }
        if (isLive(path) || isPublicPage(path) || isLoggedIn(request)) {
            noStore(response);
        }
    }

    static boolean isLoggedIn(HttpServletRequest request) {
        return AccountService.current(request.getSession(false)) != null;
    }

    private static void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("CDN-Cache-Control", "no-store");
        response.setHeader("Cloudflare-CDN-Cache-Control", "no-store");
    }

    private static boolean isLive(String path) {
        return path.startsWith("/rallye")
                || path.startsWith("/forum")
                || path.startsWith("/marketplace")
                || path.startsWith("/compte")
                || path.startsWith("/upload")
                || path.startsWith("/api/")
                || path.startsWith("/actuator")
                || path.startsWith("/h2-console")
                || path.startsWith("/calendrier")
                || path.startsWith("/prochain-evenement");
    }

    private static boolean isPublicPage(String path) {
        return "/".equals(path)
                || path.startsWith("/selectRace")
                || path.startsWith("/championnat")
                || path.startsWith("/reglement")
                || path.startsWith("/contact")
                || path.startsWith("/statistiques");
    }
}
