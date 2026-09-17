package org.example.belgianslotclubspring.services;

import jakarta.servlet.http.HttpSession;
import org.example.belgianslotclubspring.entities.MemberAccount;
import org.example.belgianslotclubspring.repo.MemberAccountRepo;
import org.example.belgianslotclubspring.utils.SellerPasswords;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class AccountService {

    public static final String SESSION_KEY = "account.view";
    public static final String REMEMBER_COOKIE = "bsc_remember";
    public static final int PASSWORD_MIN = 6;
    public static final int REMEMBER_DAYS = 60;
    public static final int REMEMBER_MAX_AGE = REMEMBER_DAYS * 24 * 60 * 60;
    public static final String DEFAULT_ADMIN_EMAIL = "thibaut.lenertz@gmail.com";

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final String PASSWORD_LABEL = "Le mot de passe du compte";
    private static final String TOKEN_LABEL = "Le jeton de connexion";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final HexFormat HEX = HexFormat.of();

    private final MemberAccountRepo accountRepo;
    private final Set<String> adminEmails;

    public AccountService(MemberAccountRepo accountRepo,
                          @Value("${app.admin.emails:thibaut.lenertz@gmail.com}") String adminEmails) {
        this.accountRepo = accountRepo;
        this.adminEmails = parseAdminEmails(adminEmails);
    }

    public static AccountView current(HttpSession session) {
        if (session == null) {
            return null;
        }
        Object value = session.getAttribute(SESSION_KEY);
        return value instanceof AccountView view ? view : null;
    }

    public static Long currentId(HttpSession session) {
        AccountView view = current(session);
        return view == null ? null : view.id();
    }

    public static void login(HttpSession session, AccountView view) {
        session.setAttribute(SESSION_KEY, view);
        session.setMaxInactiveInterval(REMEMBER_MAX_AGE);
    }

    public static void logout(HttpSession session) {
        if (session != null) {
            session.removeAttribute(SESSION_KEY);
        }
    }

    public static boolean isAdmin(HttpSession session) {
        AccountView view = current(session);
        return view != null && view.admin();
    }

    @Transactional
    public String issueRememberToken(AccountView view) {
        if (view == null || view.id() == null) {
            return null;
        }
        MemberAccount account = accountRepo.findById(view.id()).orElse(null);
        if (account == null) {
            return null;
        }
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String token = HEX.formatHex(bytes);
        account.setRememberTokenHash(SellerPasswords.hash(token, TOKEN_LABEL));
        account.setRememberUntil(LocalDateTime.now().plusDays(REMEMBER_DAYS));
        accountRepo.save(account);
        return view.id() + "." + token;
    }

    @Transactional
    public AccountView restoreFromRememberToken(String cookieValue) {
        RememberCookie parsed = parseRememberCookie(cookieValue);
        if (parsed == null) {
            return null;
        }
        MemberAccount account = accountRepo.findById(parsed.accountId()).orElse(null);
        if (account == null || account.getRememberTokenHash() == null || account.getRememberUntil() == null) {
            return null;
        }
        if (account.getRememberUntil().isBefore(LocalDateTime.now())
                || !SellerPasswords.matches(account.getRememberTokenHash(), parsed.token())) {
            forget(account.getId());
            return null;
        }
        return toView(account);
    }

    @Transactional
    public void forget(Long accountId) {
        if (accountId == null) {
            return;
        }
        accountRepo.findById(accountId).ifPresent(account -> {
            account.setRememberTokenHash(null);
            account.setRememberUntil(null);
            accountRepo.save(account);
        });
    }

    static RememberCookie parseRememberCookie(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        int sep = raw.indexOf('.');
        if (sep <= 0 || sep == raw.length() - 1) {
            return null;
        }
        try {
            long id = Long.parseLong(raw.substring(0, sep));
            String token = raw.substring(sep + 1);
            if (id <= 0 || token.length() < 16 || token.length() > 80) {
                return null;
            }
            return new RememberCookie(id, token);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public boolean isAdminEmail(String email) {
        return email != null && adminEmails.contains(email.trim().toLowerCase(Locale.ROOT));
    }

    @Transactional
    public AccountView register(String name, String email, String password, String passwordConfirm) {
        String cleanEmail = normalizeEmail(email);
        if (accountRepo.findByEmailIgnoreCase(cleanEmail).isPresent()) {
            throw new IllegalArgumentException("Un compte existe déjà avec cet e-mail.");
        }
        requireAccountPassword(password, passwordConfirm);
        MemberAccount account = new MemberAccount();
        account.setName(ForumService.cleanLine(name, 40, "votre nom"));
        account.setEmail(cleanEmail);
        account.setPasswordHash(SellerPasswords.hash(password, PASSWORD_LABEL));
        account.setCreatedAt(LocalDateTime.now());
        MemberAccount saved = accountRepo.save(account);
        return toView(saved);
    }

    @Transactional(readOnly = true)
    public AccountView authenticate(String email, String password) {
        String cleanEmail = normalizeEmail(email);
        MemberAccount account = accountRepo.findByEmailIgnoreCase(cleanEmail)
                .orElseThrow(() -> new IllegalArgumentException("E-mail ou mot de passe incorrect."));
        if (!SellerPasswords.matches(account.getPasswordHash(), password)) {
            throw new IllegalArgumentException("E-mail ou mot de passe incorrect.");
        }
        return toView(account);
    }

    AccountView toView(MemberAccount account) {
        return new AccountView(
                account.getId(),
                account.getName(),
                account.getEmail(),
                isAdminEmail(account.getEmail()));
    }

    static Set<String> parseAdminEmails(String raw) {
        Set<String> emails = new LinkedHashSet<>();
        if (raw != null) {
            for (String part : raw.split(",")) {
                String email = part.trim().toLowerCase(Locale.ROOT);
                if (!email.isEmpty()) {
                    emails.add(email);
                }
            }
        }
        if (emails.isEmpty()) {
            emails.add(DEFAULT_ADMIN_EMAIL);
        }
        return Set.copyOf(emails);
    }

    static String normalizeEmail(String raw) {
        String value = raw == null ? "" : raw.trim().toLowerCase(Locale.ROOT);
        if (value.isBlank() || !EMAIL.matcher(value).matches() || value.length() > 120) {
            throw new IllegalArgumentException("Indiquez une adresse e-mail valide.");
        }
        return value;
    }

    static void requireAccountPassword(String password, String confirm) {
        if (password == null || password.length() < PASSWORD_MIN) {
            throw new IllegalArgumentException(
                    "Le mot de passe du compte doit faire au moins " + PASSWORD_MIN + " caractères.");
        }
        SellerPasswords.requireMatch(password, confirm, PASSWORD_LABEL);
    }

    public record AccountView(Long id, String name, String email, boolean admin) {
    }

    record RememberCookie(long accountId, String token) {
    }
}
