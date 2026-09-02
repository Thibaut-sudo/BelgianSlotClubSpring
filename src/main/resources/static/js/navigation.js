/**
 * Navigation — Belgian Slot Club
 * Menu mobile, overlay, état scroll, micro-interactions
 *
 * Le bouton retour suit la hiérarchie des pages (parent), pas l’historique du navigateur.
 */
(function () {
    function currentClub() {
        const navbar = document.getElementById('navbar');
        const fromNav = navbar && navbar.getAttribute('data-club');
        if (fromNav) {
            return fromNav;
        }
        return new URLSearchParams(window.location.search).get('club') || '';
    }

    function clubQuery(club) {
        return club ? '?club=' + encodeURIComponent(club) : '';
    }

    function linearParentHref() {
        const heroBack = document.querySelector('a.page-hero-back[href]');
        if (heroBack) {
            const href = heroBack.getAttribute('href');
            if (href && href !== '#') {
                return href;
            }
        }

        const path = (window.location.pathname || '/').replace(/\/+$/, '') || '/';
        const params = new URLSearchParams(window.location.search);
        const club = currentClub();

        const rallyLeaf = path.match(/^\/rallye\/(\d+)\/(classement|grilles|scan)$/);
        if (rallyLeaf) {
            if (rallyLeaf[2] === 'scan') {
                const boucle = params.get('boucle');
                return '/rallye/' + rallyLeaf[1] + '/grilles'
                    + (boucle ? '?boucle=' + encodeURIComponent(boucle) : '');
            }
            return '/rallye/' + rallyLeaf[1];
        }
        if (/^\/rallye\/\d+$/.test(path)) {
            return '/rallye' + clubQuery(club);
        }

        if (path.indexOf('/forum/question/') === 0) {
            return '/forum' + clubQuery(club);
        }
        if (path.indexOf('/forum/theme/') === 0) {
            return '/forum' + clubQuery(club);
        }

        if (/^\/marketplace\/\d+$/.test(path)) {
            return '/marketplace' + clubQuery(club);
        }

        if (path === '/processRaceDate') {
            return club ? '/selectRace/' + encodeURIComponent(club) : '/';
        }

        const stats = path.match(/^\/statistiques(?:\/([^/]+))?$/);
        if (stats) {
            const statsClub = stats[1] || club;
            return statsClub ? '/selectRace/' + encodeURIComponent(statsClub) : '/';
        }

        const googleAll = path === '/calendrier/google' || path === '/calendrier/all/google';
        if (googleAll) {
            return '/calendrier';
        }
        const googleCal = path.match(/^\/calendrier\/([^/]+)\/google$/);
        if (googleCal) {
            return '/prochain-evenement?club=' + encodeURIComponent(googleCal[1]);
        }

        if (path === '/championnat' && params.get('categorie')) {
            params.delete('categorie');
            const query = params.toString();
            return '/championnat' + (query ? '?' + query : '');
        }

        return '/';
    }

    document.addEventListener('DOMContentLoaded', function () {
        const hamburger = document.getElementById('hamburger');
        const navLinks = document.getElementById('navLinks');
        const navOverlay = document.getElementById('navOverlay');
        const navbar = document.getElementById('navbar');
        const navBackBtn = document.getElementById('navBackBtn');
        const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;

        if (navBackBtn) {
            navBackBtn.setAttribute('href', linearParentHref());
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

        let ignoreOverlayUntil = 0;

        function openMobileMenu() {
            document.body.classList.add('nav-open');
            hamburger.classList.add('active');
            hamburger.setAttribute('aria-expanded', 'true');
            ignoreOverlayUntil = Date.now() + 400;
        }

        function closeMobileMenu() {
            document.body.classList.remove('nav-open');
            hamburger.classList.remove('active');
            hamburger.setAttribute('aria-expanded', 'false');
        }

        function toggleMobileMenu(e) {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            if (document.body.classList.contains('nav-open')) {
                closeMobileMenu();
            } else {
                openMobileMenu();
            }
        }

        hamburger.addEventListener('click', toggleMobileMenu);
        navOverlay.addEventListener('click', function () {
            if (Date.now() < ignoreOverlayUntil) {
                return;
            }
            closeMobileMenu();
        });

        navLinks.querySelectorAll('a').forEach(function (link) {
            link.addEventListener('click', function () {
                if (window.innerWidth < 768) {
                    if (reduceMotion) {
                        closeMobileMenu();
                    } else {
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
})();
