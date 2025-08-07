/**
 * Navigation améliorée pour Belgian Slot Club
 * Gère le menu hamburger, les dropdowns et l'accessibilité
 */

document.addEventListener('DOMContentLoaded', function() {
    // Variables pour la navigation
    const hamburger = document.getElementById('hamburger');
    const navLinks = document.getElementById('navLinks');
    const navOverlay = document.getElementById('navOverlay');
    const navbar = document.getElementById('navbar');
    const dropdownToggles = document.querySelectorAll('.dropdown-toggle');
    
    // Vérifier si les éléments existent avant d'ajouter les listeners
    if (!hamburger || !navLinks || !navOverlay || !navbar) {
        console.warn('🏁 Certains éléments de navigation sont manquants');
        return;
    }
    
    // Toggle du menu mobile
    function toggleMobileMenu() {
        const isActive = navLinks.classList.contains('active');
        
        // Toggle des classes
        hamburger.classList.toggle('active');
        navLinks.classList.toggle('active');
        navOverlay.classList.toggle('active');
        
        // Mise à jour de l'accessibilité
        hamburger.setAttribute('aria-expanded', !isActive);
        
        // Empêcher le scroll du body quand le menu est ouvert
        document.body.style.overflow = isActive ? 'auto' : 'hidden';
    }
    
    // Fermer le menu mobile
    function closeMobileMenu() {
        hamburger.classList.remove('active');
        navLinks.classList.remove('active');
        navOverlay.classList.remove('active');
        hamburger.setAttribute('aria-expanded', 'false');
        document.body.style.overflow = 'auto';
    }
    
    // Event listeners pour le menu mobile
    hamburger.addEventListener('click', toggleMobileMenu);
    navOverlay.addEventListener('click', closeMobileMenu);
    
    // Fermer le menu mobile lors du redimensionnement
    window.addEventListener('resize', () => {
        if (window.innerWidth > 768) {
            closeMobileMenu();
        }
    });
    
    // Gestion du scroll pour l'effet de navbar
    let lastScrollY = window.scrollY;
    
    window.addEventListener('scroll', () => {
        const currentScrollY = window.scrollY;
        
        // Ajouter classe scrolled après 50px
        if (currentScrollY > 50) {
            navbar.classList.add('scrolled');
        } else {
            navbar.classList.remove('scrolled');
        }
        
        lastScrollY = currentScrollY;
    });
    
    // Gestion des dropdowns avec accessibilité
    dropdownToggles.forEach(toggle => {
        const dropdown = toggle.closest('.nav-dropdown');
        const menu = dropdown.querySelector('.dropdown-menu');
        
        if (!menu) return;
        
        // Toggle dropdown au clic (pour mobile)
        toggle.addEventListener('click', (e) => {
            if (window.innerWidth <= 768) {
                e.preventDefault();
                const isExpanded = toggle.getAttribute('aria-expanded') === 'true';
                
                // Fermer tous les autres dropdowns
                dropdownToggles.forEach(otherToggle => {
                    if (otherToggle !== toggle) {
                        otherToggle.setAttribute('aria-expanded', 'false');
                        const otherMenu = otherToggle.closest('.nav-dropdown').querySelector('.dropdown-menu');
                        if (otherMenu) {
                            otherMenu.style.display = 'none';
                        }
                    }
                });
                
                // Toggle le dropdown actuel
                toggle.setAttribute('aria-expanded', !isExpanded);
                menu.style.display = isExpanded ? 'none' : 'block';
            }
        });
        
        // Navigation clavier
        toggle.addEventListener('keydown', (e) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                if (window.innerWidth <= 768) {
                    toggle.click();
                }
            }
            
            if (e.key === 'Escape') {
                toggle.setAttribute('aria-expanded', 'false');
                menu.style.display = 'none';
                toggle.focus();
            }
        });
    });
    
    // Gestion du focus trap dans le menu mobile
    function trapFocus(element) {
        const focusableElements = element.querySelectorAll(
            'a[href], button, textarea, input[type="text"], input[type="radio"], input[type="checkbox"], select'
        );
        const firstFocusableElement = focusableElements[0];
        const lastFocusableElement = focusableElements[focusableElements.length - 1];
        
        element.addEventListener('keydown', (e) => {
            if (e.key === 'Tab') {
                if (e.shiftKey) {
                    if (document.activeElement === firstFocusableElement) {
                        lastFocusableElement.focus();
                        e.preventDefault();
                    }
                } else {
                    if (document.activeElement === lastFocusableElement) {
                        firstFocusableElement.focus();
                        e.preventDefault();
                    }
                }
            }
            
            if (e.key === 'Escape') {
                closeMobileMenu();
                hamburger.focus();
            }
        });
    }
    
    // Appliquer le focus trap au menu mobile
    trapFocus(navLinks);
    
    // Effet de survol amélioré pour les liens (desktop)
    document.querySelectorAll('.nav-links a:not(.dropdown-toggle)').forEach(link => {
        link.addEventListener('mouseenter', function() {
            if (window.innerWidth > 768) {
                this.style.transform = 'translateY(-2px)';
            }
        });
        
        link.addEventListener('mouseleave', function() {
            if (window.innerWidth > 768) {
                this.style.transform = 'translateY(0)';
            }
        });
    });
    
    // Amélioration de la performance avec IntersectionObserver
    const observerOptions = {
        rootMargin: '0px',
        threshold: 0.1
    };
    
    // Observation des éléments pour les animations
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add('animate-in');
            }
        });
    }, observerOptions);
    
    // Observer les éléments de navigation
    document.querySelectorAll('.nav-links li').forEach(item => {
        observer.observe(item);
    });
    
    // Observer les cartes et sections
    document.querySelectorAll('.card, .section, .race-card').forEach(item => {
        observer.observe(item);
    });
    
    console.log('🏁 Navigation améliorée initialisée avec succès !');
});

// Fonction utilitaire pour mettre à jour l'état actif des liens de navigation
function updateActiveNavigation(currentPath) {
    document.querySelectorAll('.nav-links a').forEach(link => {
        link.classList.remove('active');
        link.removeAttribute('aria-current');
        
        // Vérifier si le lien correspond à la page actuelle
        const href = link.getAttribute('href');
        if (href && (href === currentPath || (href !== '/' && currentPath.includes(href)))) {
            link.classList.add('active');
            link.setAttribute('aria-current', 'page');
        }
    });
}

// Mettre à jour automatiquement l'état actif basé sur l'URL
updateActiveNavigation(window.location.pathname);