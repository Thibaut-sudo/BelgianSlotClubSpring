/**
 * Enregistrement partiel des temps rallye.
 * - Ne soumet / n’envoie que les cases modifiées (évite d’écraser les autres groupes).
 * - Autosave au blur (au fur et à mesure), y compris les champs mobile (data-mirror).
 */
(function () {
  'use strict';

  function normalize(v) {
    return String(v == null ? '' : v).trim();
  }

  function isDirty(input) {
    if (!input || input.readOnly || input.disabled) {
      return false;
    }
    var initial = input.getAttribute('data-initial');
    if (initial == null) {
      initial = '';
    }
    return normalize(input.value) !== normalize(initial);
  }

  function markClean(input) {
    input.setAttribute('data-initial', normalize(input.value));
    input.classList.remove('is-dirty', 'is-save-error');
    input.classList.add('is-saved');
    window.setTimeout(function () {
      input.classList.remove('is-saved');
    }, 1200);
  }

  function namedInput(form, el) {
    if (!el) {
      return null;
    }
    if (el.name && el.name.indexOf('time_') === 0) {
      return el;
    }
    var key = el.getAttribute && el.getAttribute('data-mirror');
    if (!key) {
      return null;
    }
    return form.querySelector('input[name="' + key + '"]');
  }

  function syncPair(form, source) {
    var named = namedInput(form, source);
    if (!named) {
      return null;
    }
    if (source !== named) {
      named.value = source.value;
    }
    form.querySelectorAll('[data-mirror="' + named.name + '"]').forEach(function (mirror) {
      if (mirror !== source && mirror.value !== named.value) {
        mirror.value = named.value;
      }
    });
    return named;
  }

  function collectDirty(root) {
    var map = {};
    var nodes = (root || document).querySelectorAll('input[name^="time_"]');
    nodes.forEach(function (inp) {
      if (!isDirty(inp)) return;
      map[inp.name] = inp.value;
    });
    return map;
  }

  function setStatus(el, msg, kind) {
    if (!el) return;
    el.textContent = msg || '';
    el.classList.remove('is-error', 'is-ok', 'is-busy');
    if (kind) el.classList.add(kind);
  }

  function initInput(input) {
    if (input.getAttribute('data-initial') == null) {
      input.setAttribute('data-initial', normalize(input.value));
    }
    input.addEventListener('input', function () {
      if (isDirty(input)) {
        input.classList.add('is-dirty');
        input.classList.remove('is-saved', 'is-save-error');
      } else {
        input.classList.remove('is-dirty');
      }
    });
  }

  function patchTimes(rallyeId, boucle, timesMap) {
    return fetch('/rallye/' + rallyeId + '/api/times', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
      body: JSON.stringify({ boucle: Number(boucle), times: timesMap })
    }).then(async function (res) {
      var data = null;
      try {
        data = await res.json();
      } catch (e) {
        data = null;
      }
      if (!res.ok) {
        var msg = (data && (data.message || data.error)) || ('Erreur ' + res.status);
        throw new Error(msg);
      }
      return data || { ok: true, saved: 0 };
    });
  }

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

  function refreshDetailStandings(form) {
    var table = document.getElementById('rallyeStandingsTable');
    if (!table || !form) {
      return;
    }
    var rallyeId = form.getAttribute('data-rallye-id');
    if (!rallyeId) {
      return;
    }
    var meta = window.__rallyeExportMeta || {};
    var params = new URLSearchParams();
    if (meta.after) {
      params.set('after', String(meta.after));
    }
    var cat = document.getElementById('catFilter');
    if (cat && cat.value) {
      params.set('category', cat.value);
    }
    var query = params.toString();
    fetch('/rallye/' + encodeURIComponent(rallyeId) + '/api/standings' + (query ? '?' + query : ''), {
      cache: 'no-store'
    }).then(function (res) {
      if (!res.ok) {
        throw new Error('standings');
      }
      return res.json();
    }).then(function (data) {
      var rows = data.rows || [];
      var tbody = table.querySelector('tbody');
      if (tbody) {
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
      }
      var cards = document.querySelector('.rallye-standings-mobile');
      if (cards) {
        cards.innerHTML = rows.map(function (row) {
          var car = row.car ? '<span>' + escapeHtml(row.car) + '</span>' : '';
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
      var empty = document.querySelector('#classement .empty-state');
      var wrap = document.querySelector('#classement .rallye-standings-desktop');
      if (empty) {
        empty.style.display = rows.length ? 'none' : '';
      }
      if (wrap) {
        wrap.style.display = rows.length ? '' : 'none';
      }
    }).catch(function () { /* keep last classement */ });
  }

  function bindForm(form) {
    if (!form || form.getAttribute('data-times-patch') === '1') return;
    if (form.getAttribute('data-times-ajax') === '0') {
      return;
    }
    form.setAttribute('data-times-patch', '1');

    var rallyeId = form.getAttribute('data-rallye-id')
      || (form.action.match(/\/rallye\/(\d+)\//) || [])[1];
    var boucleInput = form.querySelector('input[name="boucle"]');
    var statusEl = document.getElementById('timesSaveStatus');
    var saving = false;
    var queue = null;

    form.querySelectorAll('input[name^="time_"]').forEach(initInput);

    function runSave(timesMap, opts) {
      opts = opts || {};
      if (!rallyeId || !boucleInput) {
        return Promise.reject(new Error('Formulaire incomplet'));
      }
      var keys = Object.keys(timesMap);
      if (keys.length === 0) {
        if (!opts.silentEmpty) {
          setStatus(statusEl, 'Rien à enregistrer (aucune case modifiée).', 'is-ok');
        }
        return Promise.resolve({ saved: 0 });
      }

      if (saving) {
        queue = Object.assign({}, queue || {}, timesMap);
        return Promise.resolve({ saved: 0, queued: true });
      }
      saving = true;
      setStatus(statusEl, 'Enregistrement…', 'is-busy');
      keys.forEach(function (name) {
        var inp = form.querySelector('input[name="' + name + '"]');
        if (inp) inp.classList.add('is-saving');
      });

      return patchTimes(rallyeId, boucleInput.value, timesMap)
        .then(function (data) {
          if (data && data.ok === false) {
            throw new Error(data.error || 'Échec de l’enregistrement.');
          }
          keys.forEach(function (name) {
            var inp = form.querySelector('input[name="' + name + '"]');
            if (!inp) return;
            inp.classList.remove('is-saving');
            if (normalize(inp.value) === normalize(timesMap[name])) {
              markClean(inp);
              form.querySelectorAll('[data-mirror="' + name + '"]').forEach(function (mirror) {
                mirror.classList.remove('is-dirty', 'is-save-error');
                mirror.classList.add('is-saved');
                window.setTimeout(function () {
                  mirror.classList.remove('is-saved');
                }, 1200);
              });
            }
          });
          var n = data.saved || keys.length;
          setStatus(
            statusEl,
            n <= 0 ? 'Aucun temps modifié.'
              : (n === 1 ? '1 temps enregistré.' : n + ' temps enregistrés.'),
            'is-ok'
          );
          refreshDetailStandings(form);
          return data;
        })
        .catch(function (err) {
          keys.forEach(function (name) {
            var inp = form.querySelector('input[name="' + name + '"]');
            if (!inp) return;
            inp.classList.remove('is-saving');
            inp.classList.add('is-save-error');
          });
          setStatus(statusEl, err.message || 'Échec de l’enregistrement.', 'is-error');
          throw err;
        })
        .finally(function () {
          saving = false;
          if (queue) {
            var next = queue;
            queue = null;
            runSave(next, { silentEmpty: true });
          }
        });
    }

    var pendingReturnTo = 'grilles';
    form.addEventListener('click', function (e) {
      var btn = e.target.closest('button[type="submit"][name="returnTo"]');
      if (btn) {
        pendingReturnTo = btn.value || 'grilles';
        var hidden = form.querySelector('#grilleReturnTo, input[name="returnTo"]');
        if (hidden) hidden.value = pendingReturnTo;
      }
    });

    form.addEventListener('input', function (e) {
      var named = syncPair(form, e.target);
      if (named && named !== e.target) {
        named.dispatchEvent(new Event('input', { bubbles: false }));
      }
    });

    function saveFromField(el) {
      var named = syncPair(form, el);
      if (!named || named.readOnly) {
        return;
      }
      if (!isDirty(named)) {
        return;
      }
      var one = {};
      one[named.name] = named.value;
      runSave(one, { silentEmpty: true });
    }

    form.addEventListener('focusout', function (e) {
      saveFromField(e.target);
    });

    form.addEventListener('keydown', function (e) {
      if (e.key !== 'Enter') return;
      var inp = e.target;
      if (!namedInput(form, inp)) return;
      e.preventDefault();
      inp.blur();
    });

    form.addEventListener('submit', function (e) {
      form.querySelectorAll('[data-mirror]').forEach(function (mirror) {
        syncPair(form, mirror);
      });
      var dirty = collectDirty(form);
      var dirtyCount = Object.keys(dirty).length;

      form.querySelectorAll('input[name^="time_"]').forEach(function (inp) {
        inp.disabled = !isDirty(inp);
      });

      if (form.getAttribute('data-times-ajax') === '1') {
        e.preventDefault();
        form.querySelectorAll('input[name^="time_"]').forEach(function (inp) {
          inp.disabled = false;
        });
        runSave(dirty).then(function () {
          if (pendingReturnTo === 'saisie') {
            window.location.href = '/rallye/' + rallyeId + '?boucle=' + boucleInput.value + '#saisie';
          }
        }).catch(function () { /* status déjà affiché */ });
        return;
      }

      if (dirtyCount === 0) {
        e.preventDefault();
        form.querySelectorAll('input[name^="time_"]').forEach(function (inp) {
          inp.disabled = false;
        });
        setStatus(statusEl, 'Rien à enregistrer (aucune case modifiée).', 'is-ok');
      }
    });
  }

  function boot() {
    document.querySelectorAll('form.js-times-patch, #grilleTimesForm, form.rallye-times-form, #scanTimesForm')
      .forEach(bindForm);
  }

  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', boot);
  } else {
    boot();
  }

  window.RallyeTimesSave = {
    initInput: initInput,
    markDirtyFromFill: function (input) {
      if (!input) return;
      if (input.getAttribute('data-initial') == null) {
        input.setAttribute('data-initial', '');
      }
      input.classList.add('is-dirty');
    },
    refreshInitials: function (root) {
      (root || document).querySelectorAll('input[name^="time_"]').forEach(function (inp) {
        inp.setAttribute('data-initial', normalize(inp.value));
        inp.classList.remove('is-dirty');
      });
    }
  };
})();
