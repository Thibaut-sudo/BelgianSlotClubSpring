package org.example.belgianslotclubspring.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Plafonne les POST forum / marketplace pour éviter le flood sur le Pi.
 */
@Component
@Profile("prod")
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class WriteRateLimitFilter extends OncePerRequestFilter {

    private final WriteRateLimiter limiter = new WriteRateLimiter();

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!HttpMethod.POST.matches(request.getMethod())
                && !HttpMethod.PUT.matches(request.getMethod())
                && !HttpMethod.DELETE.matches(request.getMethod())) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.startsWith("/forum") || path.startsWith("/marketplace"));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String ip = clientIp(request);
        if (!limiter.tryAcquire(ip)) {
            response.setStatus(429);
            response.setHeader("Retry-After", "60");
            response.setCharacterEncoding(StandardCharsets.UTF_8.name());
            response.setContentType(MediaType.TEXT_HTML_VALUE);
            response.getWriter().write("""
                    <!DOCTYPE html><html lang="fr"><head><meta charset="UTF-8"><title>Trop de messages</title></head>
                    <body><p>Trop de publications en peu de temps. Réessayez dans une minute.</p>
                    <p><a href="/">Retour à l’accueil</a></p></body></html>
                    """);
            return;
        }
        filterChain.doFilter(request, response);
    }

    static String clientIp(HttpServletRequest request) {
        String cf = request.getHeader("CF-Connecting-IP");
        if (cf != null && !cf.isBlank()) {
            return cf.trim();
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr() == null ? "unknown" : request.getRemoteAddr();
    }
}
