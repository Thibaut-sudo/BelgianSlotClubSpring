/**
 * Agrandit les photos marketplace (liste, fiche, aperçu) dans un overlay.
 */
(function () {
    var overlay = document.getElementById('photoLightbox');
    if (!overlay) {
        return;
    }

    var img = overlay.querySelector('[data-lightbox-image]');
    var prevBtn = overlay.querySelector('[data-lightbox-prev]');
    var nextBtn = overlay.querySelector('[data-lightbox-next]');
    var sources = [];
    var index = 0;

    function sourceOf(node) {
        return node.getAttribute('data-lightbox') || '';
    }

    function uniqueSources(start) {
        var group = start.getAttribute('data-lightbox-group');
        var list = [];
        var nodes = group
            ? document.querySelectorAll('[data-lightbox-group="' + group.replace(/"/g, '') + '"]')
            : [start];
        nodes.forEach(function (node) {
            var src = sourceOf(node);
            if (src && list.indexOf(src) === -1) {
                list.push(src);
            }
        });
        return list;
    }

    function show() {
        if (!sources.length) {
            return;
        }
        img.src = sources[index];
        var many = sources.length > 1;
        prevBtn.hidden = !many;
        nextBtn.hidden = !many;
        overlay.hidden = false;
        overlay.classList.add('is-open');
        document.body.classList.add('lightbox-open');
    }

    function close() {
        overlay.hidden = true;
        overlay.classList.remove('is-open');
        document.body.classList.remove('lightbox-open');
        img.removeAttribute('src');
    }

    function step(delta) {
        if (sources.length < 2) {
            return;
        }
        index = (index + delta + sources.length) % sources.length;
        show();
    }

    function openFrom(node) {
        var src = sourceOf(node);
        if (!src) {
            return;
        }
        sources = uniqueSources(node);
        index = Math.max(0, sources.indexOf(src));
        show();
    }

    document.addEventListener('click', function (event) {
        var trigger = event.target.closest('[data-lightbox]');
        if (!trigger || overlay.contains(trigger)) {
            return;
        }
        if (!sourceOf(trigger)) {
            return;
        }
        event.preventDefault();
        event.stopPropagation();
        openFrom(trigger);
    }, true);

    overlay.addEventListener('click', function (event) {
        if (event.target === overlay || event.target.hasAttribute('data-lightbox-close')) {
            close();
        }
    });

    prevBtn.addEventListener('click', function (event) {
        event.stopPropagation();
        step(-1);
    });
    nextBtn.addEventListener('click', function (event) {
        event.stopPropagation();
        step(1);
    });

    document.addEventListener('keydown', function (event) {
        if (overlay.hidden) {
            return;
        }
        if (event.key === 'Escape') {
            close();
        } else if (event.key === 'ArrowLeft') {
            step(-1);
        } else if (event.key === 'ArrowRight') {
            step(1);
        }
    });
})();
