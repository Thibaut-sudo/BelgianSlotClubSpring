(function () {
    const root = document.getElementById('liveClassement');
    if (!root) {
        return;
    }

    const rallyeId = root.dataset.rallyeId;
    let after = Number(root.dataset.after) || 0;
    let category = root.dataset.category || '';
    let fingerprint = root.dataset.fingerprint || '';

    const tbody = document.getElementById('liveStandingsBody');
    const tableWrap = document.getElementById('liveTableWrap');
    const cardsWrap = document.getElementById('liveCardsWrap');
    const empty = document.getElementById('liveEmpty');
    const badge = document.getElementById('liveBadge');
    const catFilter = document.getElementById('liveCatFilter');
    const checkpoints = document.getElementById('liveCheckpoints');

    function escapeHtml(value) {
        return String(value == null ? '' : value)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function rowClass(row) {
        if (row.position === 1) {
            return ' is-leader';
        }
        if (row.totalSeconds == null) {
            return ' is-dnf';
        }
        return '';
    }

    function posLabel(row) {
        return row.totalSeconds == null ? '—' : String(row.position);
    }

    function render(data) {
        const rows = data.rows || [];
        const hasRows = rows.length > 0;

        tableWrap.classList.toggle('is-empty', !hasRows);
        cardsWrap.classList.toggle('is-empty', !hasRows);
        empty.classList.toggle('is-empty', hasRows);

        tbody.innerHTML = rows.map(function (row) {
            return '<tr class="' + rowClass(row).trim() + '">'
                + '<td>' + escapeHtml(posLabel(row)) + '</td>'
                + '<td>' + escapeHtml(row.name) + '</td>'
                + '<td>' + escapeHtml(row.car) + '</td>'
                + '<td>' + escapeHtml(row.category) + '</td>'
                + '<td class="mono">' + escapeHtml(row.totalFormatted) + '</td>'
                + '<td class="mono">' + escapeHtml(row.gapToPreviousFormatted) + '</td>'
                + '<td class="mono">' + escapeHtml(row.gapFormatted) + '</td>'
                + '<td>' + escapeHtml(row.stagesCompleted + '/' + row.stagesExpected) + '</td>'
                + '</tr>';
        }).join('');

        cardsWrap.innerHTML = rows.map(function (row) {
            const car = row.car ? '<span>' + escapeHtml(row.car) + '</span>' : '';
            return '<article class="rallye-standing-card' + rowClass(row) + '">'
                + '<div class="rallye-standing-card__pos">' + escapeHtml(posLabel(row)) + '</div>'
                + '<div class="rallye-standing-card__info"><strong>' + escapeHtml(row.name) + '</strong>' + car + '</div>'
                + '<div class="rallye-standing-card__times">'
                + '<span class="mono">' + escapeHtml(row.totalFormatted) + '</span>'
                + '<span class="rallye-standing-card__gap mono">'
                + '<span>préc. <span>' + escapeHtml(row.gapToPreviousFormatted) + '</span></span>'
                + '<span class="rallye-standing-card__gap-sep">·</span>'
                + '<span>1er <span>' + escapeHtml(row.gapFormatted) + '</span></span>'
                + '</span>'
                + '<span class="rallye-standing-card__es">' + escapeHtml(row.stagesCompleted + '/' + row.stagesExpected) + '</span>'
                + '</div></article>';
        }).join('');
    }

    function flashUpdated() {
        if (!badge) {
            return;
        }
        badge.classList.add('is-updated');
        window.setTimeout(function () {
            badge.classList.remove('is-updated');
        }, 1200);
    }

    function apiUrl() {
        const params = new URLSearchParams();
        if (after) {
            params.set('after', String(after));
        }
        if (category) {
            params.set('category', category);
        }
        const query = params.toString();
        return '/rallye/' + encodeURIComponent(rallyeId) + '/api/standings' + (query ? '?' + query : '');
    }

    async function poll() {
        try {
            const response = await fetch(apiUrl(), { cache: 'no-store' });
            if (!response.ok) {
                return;
            }
            const data = await response.json();
            const nextFp = String(data.fingerprint);
            if (nextFp !== fingerprint) {
                fingerprint = nextFp;
                render(data);
                flashUpdated();
            }
        } catch (err) {
            /* keep last ranking if the network drops */
        }
    }

    if (checkpoints) {
        checkpoints.addEventListener('click', function (event) {
            const btn = event.target.closest('[data-after]');
            if (!btn) {
                return;
            }
            after = Number(btn.getAttribute('data-after')) || after;
            checkpoints.querySelectorAll('.rallye-tab').forEach(function (tab) {
                tab.classList.toggle('is-active', tab === btn);
            });
            fingerprint = '';
            poll();
        });
    }

    if (catFilter) {
        catFilter.addEventListener('change', function () {
            category = catFilter.value || '';
            fingerprint = '';
            poll();
        });
    }

    window.setInterval(poll, 2500);
    document.addEventListener('visibilitychange', function () {
        if (!document.hidden) {
            poll();
        }
    });
})();
