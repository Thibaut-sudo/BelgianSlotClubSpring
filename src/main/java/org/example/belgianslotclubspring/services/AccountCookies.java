package org.example.belgianslotclubspring.services;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Cookie durable pour rester connecté après fermeture du navigateur ou redémarrage du serveur.
 */
public final class AccountCookies {

    private AccountCookies() {
    }

    public static String read(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (AccountService.REMEMBER_COOKIE.equals(cookie.getName())) {
                String value = cookie.getValue();
                return value == null || value.isBlank() ? null : value;
            }
        }
        return null;
    }

    public static void write(HttpServletRequest request, HttpServletResponse response, String value) {
        if (value == null || value.isBlank()) {
            clear(request, response);
            return;
        }
        response.addCookie(build(request, value, AccountService.REMEMBER_MAX_AGE));
    }

    public static void clear(HttpServletRequest request, HttpServletResponse response) {
        response.addCookie(build(request, "", 0));
    }

    private static Cookie build(HttpServletRequest request, String value, int maxAge) {
        Cookie cookie = new Cookie(AccountService.REMEMBER_COOKIE, value);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setSecure(isHttps(request));
        cookie.setAttribute("SameSite", "Lax");
        return cookie;
    }

    static boolean isHttps(HttpServletRequest request) {
        if (request.isSecure()) {
            return true;
        }
        String forwarded = request.getHeader("X-Forwarded-Proto");
        return forwarded != null && forwarded.toLowerCase().contains("https");
    }
}
