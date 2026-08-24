/**
 * Enregistrement partiel des temps rallye.
 * - Ne soumet / n’envoie que les cases modifiées (évite d’écraser les autres groupes).
 * - Autosave au blur (au fur et à mesure).
 */
(function () {
  'use strict';

  function normalize(v) {
    return String(v == null ? '' : v).trim();
  }

  function isDirty(input) {
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

  function bindForm(form) {
    if (!form || form.getAttribute('data-times-patch') === '1') return;
    if (form.getAttribute('data-times-ajax') === '0' || form.querySelector('input[name^="time_"][readonly]')) {
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
        queue = timesMap;
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
            // ne marquer clean que si la valeur n’a pas rechangé pendant l’appel
            if (normalize(inp.value) === normalize(timesMap[name])) {
              markClean(inp);
            }
          });
          var n = data.saved || keys.length;
          setStatus(
            statusEl,
            n <= 0 ? 'Aucun temps modifié.'
              : (n === 1 ? '1 temps enregistré.' : n + ' temps enregistrés.'),
            'is-ok'
          );
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

    // Autosave d’une case au blur
    form.addEventListener('focusout', function (e) {
      var inp = e.target;
      if (!inp || !inp.name || inp.name.indexOf('time_') !== 0) return;
      if (!isDirty(inp)) return;
      var one = {};
      one[inp.name] = inp.value;
      runSave(one, { silentEmpty: true });
    });

    // Entrée = blur → autosave
    form.addEventListener('keydown', function (e) {
      if (e.key !== 'Enter') return;
      var inp = e.target;
      if (!inp || !inp.name || inp.name.indexOf('time_') !== 0) return;
      e.preventDefault();
      inp.blur();
    });

    // Submit : uniquement les cases dirty (form classique OU ajax)
    form.addEventListener('submit', function (e) {
      var dirty = collectDirty(form);
      var dirtyCount = Object.keys(dirty).length;

      // Désactive les champs non modifiés pour qu’ils ne partent pas dans un POST classique
      form.querySelectorAll('input[name^="time_"]').forEach(function (inp) {
        inp.disabled = !isDirty(inp);
      });

      // Préférer AJAX pour rester sur la page sans perdre le scroll / autres groupes
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
      // sinon POST classique avec seulement les dirty (disabled exclus)
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

  // Expose pour le scan (remplit des cases après OCR → dirty)
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
