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
 * Pas de cache public sur le HTML : la nav affiche le prénom du compte.
 * Les en-têtes doivent être posés avant la chaîne : la compression gzip commit
 * la réponse et rend tout Cache-Control ultérieur inopérant.
 */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class PublicPageCacheFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (shouldNoStore(request)) {
            noStore(response);
        }
        filterChain.doFilter(request, response);
    }

    static boolean shouldNoStore(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (isStaticAsset(path)) {
            return false;
        }
        if (!HttpMethod.GET.matches(request.getMethod()) && !HttpMethod.HEAD.matches(request.getMethod())) {
            return true;
        }
        return isLive(path) || isPublicPage(path);
    }

    static void noStore(HttpServletResponse response) {
        response.setHeader("Cache-Control", "private, no-store");
        response.setHeader("CDN-Cache-Control", "no-store");
        response.setHeader("Cloudflare-CDN-Cache-Control", "no-store");
        response.setHeader("Pragma", "no-cache");
        response.setDateHeader("Expires", 0);
    }

    private static boolean isStaticAsset(String path) {
        return path.startsWith("/docs/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/marketplace/photo/")
                || path.startsWith("/forum/attachment/");
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
