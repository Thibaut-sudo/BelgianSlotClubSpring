package org.example.belgianslotclubspring.configs;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.example.belgianslotclubspring.services.AccountCookies;
import org.example.belgianslotclubspring.services.AccountService;
import org.example.belgianslotclubspring.services.AccountService.AccountView;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Recolle le compte depuis le cookie « rester connecté » si la session HTTP est vide.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 25)
public class RememberAccountFilter extends OncePerRequestFilter {

    private final AccountService accountService;

    public RememberAccountFilter(AccountService accountService) {
        this.accountService = accountService;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/images/")
                || path.startsWith("/docs/")
                || path.startsWith("/marketplace/photo/")
                || path.startsWith("/forum/attachment/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        HttpSession session = request.getSession(false);
        AccountView current = AccountService.current(session);
        if (current != null) {
            if (AccountCookies.read(request) == null) {
                AccountCookies.write(request, response, accountService.issueRememberToken(current));
            }
        } else {
            String token = AccountCookies.read(request);
            if (token != null) {
                AccountView view = accountService.restoreFromRememberToken(token);
                if (view == null) {
                    AccountCookies.clear(request, response);
                } else {
                    AccountService.login(request.getSession(true), view);
                }
            }
        }
        filterChain.doFilter(request, response);
    }
}
