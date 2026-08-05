/**
 * Navigation — Belgian Slot Club
 * Menu mobile, overlay, état scroll, micro-interactions
 */

document.addEventListener('DOMContentLoaded', function () {
    const hamburger = document.getElementById('hamburger');
    const navLinks = document.getElementById('navLinks');
    const navOverlay = document.getElementById('navOverlay');
    const navbar = document.getElementById('navbar');
    const navBackBtn = document.getElementById('navBackBtn');
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (navBackBtn) {
        navBackBtn.addEventListener('click', function () {
            if (window.history.length > 1) {
                window.history.back();
            } else {
                window.location.href = '/';
            }
        });
    }

    function updateScrollState() {
        if (!navbar) return;
        navbar.classList.toggle('scrolled', window.scrollY > 24);
    }

    updateScrollState();
    window.addEventListener('scroll', updateScrollState, { passive: true });

    if (!hamburger || !navLinks || !navOverlay || !navbar) {
        return;
    }

    function openMobileMenu() {
        document.body.classList.add('nav-open');
        hamburger.classList.add('active');
        hamburger.setAttribute('aria-expanded', 'true');
    }

    function closeMobileMenu() {
        document.body.classList.remove('nav-open');
        hamburger.classList.remove('active');
        hamburger.setAttribute('aria-expanded', 'false');
    }

    function toggleMobileMenu() {
        if (document.body.classList.contains('nav-open')) {
            closeMobileMenu();
        } else {
            openMobileMenu();
        }
    }

    hamburger.addEventListener('click', toggleMobileMenu);
    navOverlay.addEventListener('click', closeMobileMenu);

    navLinks.querySelectorAll('a').forEach(function (link) {
        link.addEventListener('click', function () {
            if (window.innerWidth < 768) {
                if (reduceMotion) {
                    closeMobileMenu();
                } else {
                    // Laisse l’animation de clic se sentir avant la fermeture
                    window.setTimeout(closeMobileMenu, 120);
                }
            }
        });
    });

    window.addEventListener('resize', function () {
        if (window.innerWidth >= 768) {
            closeMobileMenu();
        }
    });

    window.addEventListener('keydown', function (e) {
        if (e.key === 'Escape' && document.body.classList.contains('nav-open')) {
            closeMobileMenu();
            hamburger.focus();
        }
    });
});
