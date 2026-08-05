/**
 * Scan feuille groupe : QR (identité) + OCR conservateur (ancres p{id}e{es}).
 * Règle d’or : en cas de doute → case vide (moins d’erreurs de classement).
 */
(function () {
  'use strict';

  var MIN_WORD_CONF = 72;
  var MIN_ANCHOR_CONF = 65;
  var MAX_TIME_SECONDS = 45 * 60;

  var body = document.body;
  var rallyeId = body.getAttribute('data-rallye-id');
  var defaultBoucle = body.getAttribute('data-boucle') || '1';

  var video = document.getElementById('scanVideo');
  var preview = document.getElementById('scanPreview');
  var placeholder = document.getElementById('scanPlaceholder');
  var statusEl = document.getElementById('scanStatus');
  var btnCamera = document.getElementById('btnCamera');
  var btnSnap = document.getElementById('btnSnap');
  var btnAnalyze = document.getElementById('btnAnalyze');
  var fileInput = document.getElementById('scanFile');
  var reviewSection = document.getElementById('scanReviewSection');
  var reviewGrid = document.getElementById('scanReviewGrid');
  var reviewLead = document.getElementById('scanReviewLead');
  var badges = document.getElementById('scanBadges');
  var boucleInput = document.getElementById('scanBoucle');

  var stream = null;
  var imageBitmap = null;
  var workerPromise = null;

  function setStatus(msg, kind) {
    statusEl.textContent = msg || '';
    statusEl.classList.remove('is-error', 'is-ok');
    if (kind) statusEl.classList.add(kind);
  }

  function showPreviewFromCanvas() {
    placeholder.hidden = true;
    video.hidden = true;
    preview.hidden = false;
    btnAnalyze.disabled = false;
  }

  function drawImageToPreview(source) {
    var w = source.videoWidth || source.naturalWidth || source.width;
    var h = source.videoHeight || source.naturalHeight || source.height;
    if (!w || !h) return;
    var maxW = 1600;
    var scale = Math.min(1, maxW / w);
    preview.width = Math.round(w * scale);
    preview.height = Math.round(h * scale);
    var ctx = preview.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, preview.width, preview.height);
    ctx.drawImage(source, 0, 0, preview.width, preview.height);
    showPreviewFromCanvas();
  }

  async function stopCamera() {
    if (stream) {
      stream.getTracks().forEach(function (t) { t.stop(); });
      stream = null;
    }
    video.srcObject = null;
    btnSnap.hidden = true;
  }

  btnCamera.addEventListener('click', async function () {
    try {
      await stopCamera();
      stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' } },
        audio: false
      });
      video.srcObject = stream;
      video.hidden = false;
      preview.hidden = true;
      placeholder.hidden = true;
      btnSnap.hidden = false;
      btnAnalyze.disabled = true;
      await video.play();
      setStatus('Caméra prête — cadre le QR et toute la feuille.');
    } catch (err) {
      setStatus('Caméra indisponible : utilise « Choisir une photo ».', 'is-error');
    }
  });

  btnSnap.addEventListener('click', function () {
    drawImageToPreview(video);
    stopCamera();
    setStatus('Photo prise — lance l’analyse.');
  });

  fileInput.addEventListener('change', function () {
    var file = fileInput.files && fileInput.files[0];
    if (!file) return;
    stopCamera();
    var url = URL.createObjectURL(file);
    var img = new Image();
    img.onload = function () {
      drawImageToPreview(img);
      URL.revokeObjectURL(url);
      setStatus('Photo chargée — lance l’analyse.');
    };
    img.onerror = function () {
      URL.revokeObjectURL(url);
      setStatus('Impossible de lire cette image.', 'is-error');
    };
    img.src = url;
  });

  function preprocessForOcr(sourceCanvas) {
    var c = document.createElement('canvas');
    c.width = sourceCanvas.width;
    c.height = sourceCanvas.height;
    var ctx = c.getContext('2d');
    ctx.drawImage(sourceCanvas, 0, 0);
    var img = ctx.getImageData(0, 0, c.width, c.height);
    var d = img.data;
    for (var i = 0; i < d.length; i += 4) {
      var g = 0.299 * d[i] + 0.587 * d[i + 1] + 0.114 * d[i + 2];
      // contraste fort
      g = g < 140 ? Math.max(0, g * 0.75) : Math.min(255, 255 - (255 - g) * 0.55);
      var v = g > 165 ? 255 : (g < 110 ? 0 : g);
      d[i] = d[i + 1] = d[i + 2] = v;
    }
    ctx.putImageData(img, 0, 0);
    return c;
  }

  function decodeQrFromCanvas(canvas) {
    if (typeof jsQR === 'undefined') return null;
    var ctx = canvas.getContext('2d');
    var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    var code = jsQR(imageData.data, imageData.width, imageData.height, {
      inversionAttempts: 'attemptBoth'
    });
    return code && code.data ? code.data.trim() : null;
  }

  function parseQrPayload(raw) {
    var parts = (raw || '').split('|');
    if (parts.length !== 6 || parts[0] !== 'BSC1') {
      throw new Error('QR non reconnu. Réimprime les feuilles (QR BSC1).');
    }
    var id = parts[1];
    var boucle = parseInt(parts[2], 10);
    var group = parseInt(parts[3], 10);
    var pilots = parts[4].split(',').filter(Boolean).map(Number);
    var stages = parts[5].split(',').filter(Boolean).map(Number);
    if (String(id) !== String(rallyeId)) {
      throw new Error('Cette feuille appartient à un autre rallye.');
    }
    if (!boucle || !group || !pilots.length || !stages.length) {
      throw new Error('QR incomplet.');
    }
    return { rallyeId: id, boucle: boucle, group: group, pilots: pilots, stages: stages, raw: raw };
  }

  function parseTimeCandidate(raw) {
    if (!raw) return null;
    var value = String(raw).trim().replace(',', '.').replace(/\s+/g, '');
    value = value.replace(/[Oo]/g, '0').replace(/[Il|]/g, '1').replace(/S/gi, '5');
    value = value.replace(/[^\d:.]/g, '');
    if (!value || value === '-' || value === '.') return null;

    var seconds = null;
    var hh = value.match(/^(\d+):([0-5]?\d):([0-5]?\d)(?:\.(\d{1,3}))?$/);
    var mm = value.match(/^(\d+):([0-5]?\d)(?:\.(\d{1,3}))?$/);
    var ss = value.match(/^(\d+)(?:\.(\d{1,3}))?$/);
    function frac(digits) {
      if (!digits) return 0;
      return parseInt((digits + '000').slice(0, 3), 10) / 1000;
    }
    try {
      if (hh) {
        seconds = (+hh[1]) * 3600 + (+hh[2]) * 60 + (+hh[3]) + frac(hh[4]);
      } else if (mm) {
        seconds = (+mm[1]) * 60 + (+mm[2]) + frac(mm[3]);
      } else if (ss) {
        seconds = (+ss[1]) + frac(ss[2]);
      } else {
        return null;
      }
    } catch (e) {
      return null;
    }
    if (!isFinite(seconds) || seconds <= 0 || seconds > MAX_TIME_SECONDS) return null;
    // Reformate proprement
    return formatSeconds(seconds);
  }

  function formatSeconds(seconds) {
    var totalMillis = Math.round(seconds * 1000);
    var abs = Math.abs(totalMillis);
    var h = Math.floor(abs / 3600000);
    var m = Math.floor((abs % 3600000) / 60000);
    var s = Math.floor((abs % 60000) / 1000);
    var ms = abs % 1000;
    function pad(n, w) {
      return String(n).padStart(w, '0');
    }
    if (h > 0) return h + ':' + pad(m, 2) + ':' + pad(s, 2) + '.' + pad(ms, 3);
    if (m > 0) return m + ':' + pad(s, 2) + '.' + pad(ms, 3);
    return s + '.' + pad(ms, 3);
  }

  function getWorker() {
    if (!workerPromise) {
      workerPromise = Tesseract.createWorker('eng', 1, {
        logger: function () {}
      }).then(async function (worker) {
        await worker.setParameters({
          tessedit_char_whitelist: '0123456789pPeE.:',
          preserve_interword_spaces: '1'
        });
        return worker;
      });
    }
    return workerPromise;
  }

  function wordCenter(word) {
    var b = word.bbox;
    return { x: (b.x0 + b.x1) / 2, y: (b.y0 + b.y1) / 2, bbox: b };
  }

  function findAnchors(words) {
    var anchors = [];
    var re = /^p(\d+)e(\d+)$/i;
    words.forEach(function (w) {
      var text = (w.text || '').replace(/\s+/g, '');
      var m = text.match(re);
      if (!m) return;
      if ((w.confidence || 0) < MIN_ANCHOR_CONF) return;
      anchors.push({
        pilotId: Number(m[1]),
        es: Number(m[2]),
        confidence: w.confidence || 0,
        center: wordCenter(w)
      });
    });
    return anchors;
  }

  function findTimeWords(words) {
    var times = [];
    words.forEach(function (w) {
      if ((w.confidence || 0) < MIN_WORD_CONF) return;
      var formatted = parseTimeCandidate(w.text);
      if (!formatted) return;
      times.push({
        value: formatted,
        confidence: w.confidence || 0,
        center: wordCenter(w)
      });
    });
    // Aussi tenter paires de mots adjacents type "1:23" + "456"
    for (var i = 0; i < words.length - 1; i++) {
      var a = words[i];
      var b = words[i + 1];
      if ((a.confidence || 0) < MIN_WORD_CONF || (b.confidence || 0) < MIN_WORD_CONF) continue;
      var merged = parseTimeCandidate((a.text || '') + (b.text || ''));
      if (!merged) continue;
      var ca = wordCenter(a);
      var cb = wordCenter(b);
      times.push({
        value: merged,
        confidence: Math.min(a.confidence || 0, b.confidence || 0),
        center: { x: (ca.x + cb.x) / 2, y: (ca.y + cb.y) / 2 }
      });
    }
    return times;
  }

  function matchTimesToAnchors(anchors, times) {
    var used = new Set();
    var map = {}; // key pilotId_es -> {value, confidence, uncertain}

    anchors.forEach(function (anchor) {
      var key = anchor.pilotId + '_' + anchor.es;
      var best = null;
      var bestScore = Infinity;
      times.forEach(function (t, idx) {
        if (used.has(idx)) return;
        var dx = t.center.x - anchor.center.x;
        var dy = t.center.y - anchor.center.y;
        // le temps est typiquement AU-DESSUS de l'ancre
        if (dy > 40) return;
        if (Math.abs(dx) > 120) return;
        var dist = Math.abs(dx) * 1.2 + Math.abs(dy);
        // favorise au-dessus
        if (dy < -5) dist *= 0.75;
        if (dist < bestScore) {
          bestScore = dist;
          best = { idx: idx, time: t, dist: dist };
        }
      });
      if (!best || best.dist > 140) {
        map[key] = null;
        return;
      }
      used.add(best.idx);
      var uncertain = best.time.confidence < 85 || best.dist > 90;
      map[key] = {
        value: best.time.value,
        confidence: best.time.confidence,
        uncertain: uncertain
      };
    });
    return map;
  }

  function renderReview(sheet, detections) {
    reviewGrid.innerHTML = '';
    badges.innerHTML = '';
    var filled = 0;
    var uncertain = 0;
    var empty = 0;

    sheet.stages.forEach(function (stage) {
      var article = document.createElement('article');
      article.className = 'scan-stage';
      var header = document.createElement('header');
      header.className = 'scan-stage-header';
      header.innerHTML = '<span>ES ' + stage.esNumber + '</span>';
      article.appendChild(header);

      var table = document.createElement('table');
      var tbody = document.createElement('tbody');
      stage.pilots.forEach(function (p) {
        var key = p.id + '_' + stage.esNumber;
        var det = detections[key];
        var tr = document.createElement('tr');
        var tdPilot = document.createElement('td');
        tdPilot.innerHTML = '<span class="pilot-name"></span><span class="pilot-meta"></span>';
        tdPilot.querySelector('.pilot-name').textContent = p.name;
        tdPilot.querySelector('.pilot-meta').textContent = (p.car || '') + ' · ' + (p.anchor || '');

        var tdTime = document.createElement('td');
        var wrap = document.createElement('div');
        wrap.className = 'scan-time-wrap';
        var input = document.createElement('input');
        input.type = 'text';
        input.inputMode = 'decimal';
        input.autocomplete = 'off';
        input.name = 'time_' + p.id + '_' + stage.esNumber;
        input.placeholder = '—';

        var existing = (p.time || '').trim();
        var conf = document.createElement('span');
        conf.className = 'scan-conf';

        if (det && det.value) {
          input.value = det.value;
          if (det.uncertain) {
            input.classList.add('is-uncertain');
            conf.textContent = 'À vérifier (' + Math.round(det.confidence) + '%)';
            uncertain++;
          } else {
            input.classList.add('is-filled');
            conf.textContent = 'OK · ' + Math.round(det.confidence) + '%';
            filled++;
          }
        } else if (existing) {
          input.value = existing;
          conf.textContent = 'Déjà en base';
          filled++;
        } else {
          conf.textContent = 'Vide (non lu)';
          empty++;
        }

        wrap.appendChild(input);
        wrap.appendChild(conf);
        tdTime.appendChild(wrap);
        tr.appendChild(tdPilot);
        tr.appendChild(tdTime);
        tbody.appendChild(tr);
      });
      table.appendChild(tbody);
      article.appendChild(table);
      reviewGrid.appendChild(article);
    });

    function badge(text, cls) {
      var b = document.createElement('span');
      b.className = 'scan-badge ' + (cls || '');
      b.textContent = text;
      badges.appendChild(b);
    }
    badge('Groupe ' + sheet.groupNumber);
    badge('Boucle ' + sheet.boucle);
    badge(filled + ' lus', 'scan-badge--ok');
    badge(uncertain + ' à vérifier', 'scan-badge--warn');
    badge(empty + ' vides', 'scan-badge--empty');

    reviewLead.textContent = 'Groupe ' + sheet.groupNumber + ' · Boucle ' + sheet.boucle +
      ' — corrige les cases orange, laisse vide si illisible.';
    reviewSection.hidden = false;
  }

  btnAnalyze.addEventListener('click', async function () {
    if (preview.hidden || !preview.width) {
      setStatus('Prends ou charge une photo d’abord.', 'is-error');
      return;
    }
    btnAnalyze.disabled = true;
    setStatus('Lecture du QR…');

    try {
      var qrRaw = decodeQrFromCanvas(preview);
      if (!qrRaw) {
        // essai sur version prétraitée
        var preQr = preprocessForOcr(preview);
        qrRaw = decodeQrFromCanvas(preQr);
      }
      if (!qrRaw) {
        throw new Error('QR introuvable. Recadre pour que le QR soit net et entier.');
      }
      var qr = parseQrPayload(qrRaw);
      boucleInput.value = String(qr.boucle);
      setStatus('QR OK — Groupe ' + qr.group + ' · chargement de la feuille…');

      var res = await fetch('/rallye/' + rallyeId + '/api/group-sheet?boucle=' + qr.boucle + '&group=' + qr.group);
      if (!res.ok) throw new Error('Impossible de charger la feuille groupe.');
      var sheet = await res.json();

      setStatus('OCR en cours (peut prendre 10–30 s)…');
      var pre = preprocessForOcr(preview);
      var worker = await getWorker();
      var result = await worker.recognize(pre);
      var words = (result && result.data && result.data.words) || [];
      var anchors = findAnchors(words);
      var times = findTimeWords(words);
      var detections = matchTimesToAnchors(anchors, times);

      // Si peu d’ancres lues, on n’invente rien : review vide sauf existants
      if (anchors.length < Math.max(1, Math.floor(qr.pilots.length * qr.stages.length * 0.35))) {
        setStatus(
          'Peu d’ancres lues (' + anchors.length + '). Remplissage partiel seulement — vérifie tout.',
          'is-error'
        );
      } else {
        setStatus(
          'Analyse terminée : ' + anchors.length + ' ancres, ' + times.length + ' temps candidats.',
          'is-ok'
        );
      }

      renderReview(sheet, detections);
      reviewSection.scrollIntoView({ behavior: 'smooth', block: 'start' });
    } catch (err) {
      console.error(err);
      setStatus(err.message || 'Analyse échouée.', 'is-error');
    } finally {
      btnAnalyze.disabled = false;
    }
  });
})();
