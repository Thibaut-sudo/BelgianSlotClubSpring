/**
 * Scan feuille groupe : QR puis une photo par ES (chiffres plus gros → OCR plus fiable).
 * 1) Photo du QR → groupe / boucle
 * 2) Photo de près de chaque tableau ES → cases temps
 * 3) Review avec miniature — doute → case orange
 */
(function () {
  'use strict';

  var MAX_TIME_SECONDS = 45 * 60;

  var body = document.body;
  var rallyeId = body.getAttribute('data-rallye-id');

  var video = document.getElementById('scanVideo');
  var preview = document.getElementById('scanPreview');
  var placeholder = document.getElementById('scanPlaceholder');
  var statusEl = document.getElementById('scanStatus');
  var stepTitle = document.getElementById('scanStepTitle');
  var stepLead = document.getElementById('scanStepLead');
  var kickerEl = document.getElementById('scanKicker');
  var viewerTag = document.getElementById('scanViewerTag');
  var filmstripEl = document.getElementById('scanFilmstrip');
  var progressEl = document.getElementById('scanProgress');
  var btnCamera = document.getElementById('btnCamera');
  var btnSnap = document.getElementById('btnSnap');
  var btnAnalyze = document.getElementById('btnAnalyze');
  var btnSkipEs = document.getElementById('btnSkipEs');
  var btnRestart = document.getElementById('btnRestart');
  var fileInput = document.getElementById('scanFile');
  var reviewSection = document.getElementById('scanReviewSection');
  var reviewGrid = document.getElementById('scanReviewGrid');
  var reviewLead = document.getElementById('scanReviewLead');
  var badges = document.getElementById('scanBadges');
  var boucleInput = document.getElementById('scanBoucle');

  var stream = null;
  var workerPromise = null;
  var usedCamera = false;
  var session = newSession();

  function newSession() {
    return {
      phase: 'qr',
      sheet: null,
      qr: null,
      esIndex: 0,
      detections: {},
      shots: [],
      awaitingCapture: false,
      viewingArchive: false
    };
  }

  function setStatus(msg, kind) {
    if (!statusEl) return;
    statusEl.textContent = msg || '';
    statusEl.classList.remove('is-error', 'is-ok');
    if (kind) statusEl.classList.add(kind);
  }

  function showPreviewFromCanvas() {
    placeholder.hidden = true;
    video.hidden = true;
    preview.hidden = false;
    session.viewingArchive = false;
    session.awaitingCapture = false;
    btnAnalyze.disabled = false;
    updateViewerTag();
  }

  function copyToPreview(source) {
    var w = source.videoWidth || source.naturalWidth || source.width;
    var h = source.videoHeight || source.naturalHeight || source.height;
    if (!w || !h) return false;
    var maxSide = 3200;
    var scale = Math.min(1, maxSide / Math.max(w, h));
    preview.width = Math.round(w * scale);
    preview.height = Math.round(h * scale);
    var ctx = preview.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, preview.width, preview.height);
    ctx.drawImage(source, 0, 0, preview.width, preview.height);
    showPreviewFromCanvas();
    return true;
  }

  async function stopCamera() {
    if (stream) {
      stream.getTracks().forEach(function (t) { t.stop(); });
      stream = null;
    }
    video.srcObject = null;
    btnSnap.hidden = true;
  }

  function cameraHint() {
    if (session.phase === 'es' && session.sheet && session.sheet.stages[session.esIndex]) {
      return 'Caméra prête — cadre l’ES ' + session.sheet.stages[session.esIndex].esNumber + ', cases temps bien nettes.';
    }
    return 'Caméra prête — cadre le QR, bien net.';
  }

  async function startCamera() {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      throw new Error('Caméra non supportée sur ce navigateur.');
    }
    await stopCamera();
    setStatus('Ouverture de la caméra…');
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
    if (video.readyState < 2) {
      await new Promise(function (resolve) {
        video.onloadedmetadata = resolve;
        setTimeout(resolve, 1500);
      });
    }
    await video.play();
    usedCamera = true;
    session.viewingArchive = false;
    setStatus(cameraHint());
    updateViewerTag();
  }

  function clearPreview() {
    preview.hidden = true;
    preview.width = 0;
    preview.height = 0;
    placeholder.hidden = false;
    btnAnalyze.disabled = true;
    if (fileInput) fileInput.value = '';
  }

  function snapshotPreview(maxW) {
    if (!preview.width || !preview.height) return '';
    var scale = Math.min(1, maxW / preview.width);
    var c = document.createElement('canvas');
    c.width = Math.max(1, Math.round(preview.width * scale));
    c.height = Math.max(1, Math.round(preview.height * scale));
    c.getContext('2d').drawImage(preview, 0, 0, c.width, c.height);
    return c.toDataURL('image/jpeg', 0.78);
  }

  function saveCurrentShot(key, label) {
    var thumb = snapshotPreview(280);
    var full = snapshotPreview(1400);
    if (!full) return;
    var shots = session.shots.filter(function (s) { return s.key !== key; });
    shots.push({ key: key, label: label, thumb: thumb, full: full });
    session.shots = shots;
    session.activeShotKey = key;
    renderFilmstrip();
  }

  function shotByKey(key) {
    var found = null;
    session.shots.forEach(function (s) {
      if (s.key === key) found = s;
    });
    return found;
  }

  function showArchivedShot(key) {
    var shot = shotByKey(key);
    if (!shot) return;
    var img = new Image();
    img.onload = function () {
      preview.width = img.naturalWidth;
      preview.height = img.naturalHeight;
      var ctx = preview.getContext('2d');
      ctx.fillStyle = '#fff';
      ctx.fillRect(0, 0, preview.width, preview.height);
      ctx.drawImage(img, 0, 0);
      placeholder.hidden = true;
      video.hidden = true;
      preview.hidden = false;
      session.viewingArchive = true;
      session.activeShotKey = key;
      btnAnalyze.disabled = true;
      if (btnSnap) btnSnap.hidden = true;
      updateViewerTag();
      renderFilmstrip();
    };
    img.src = shot.full || shot.thumb;
  }

  function updateViewerTag() {
    if (!viewerTag) return;
    if (video && !video.hidden) {
      viewerTag.textContent = session.phase === 'es' && currentStage()
        ? 'Caméra live · ES ' + currentStage().esNumber
        : 'Caméra live · QR';
      return;
    }
    if (preview.hidden || !preview.width) {
      viewerTag.textContent = 'En attente d’une photo';
      return;
    }
    if (session.viewingArchive) {
      var archived = shotByKey(session.activeShotKey);
      viewerTag.textContent = archived ? 'Photo lue · ' + archived.label : 'Photo précédente';
      return;
    }
    if (session.awaitingCapture) {
      var next = currentStage();
      viewerTag.textContent = next
        ? 'Dernière photo lue — prends l’ES ' + next.esNumber
        : 'Dernière photo lue';
      return;
    }
    if (session.phase === 'es' && currentStage()) {
      viewerTag.textContent = 'Photo à lire · ES ' + currentStage().esNumber;
      return;
    }
    if (session.phase === 'qr') {
      viewerTag.textContent = 'Photo à lire · QR';
      return;
    }
    viewerTag.textContent = 'Photo de travail';
  }

  function renderFilmstrip() {
    if (!filmstripEl) return;
    filmstripEl.innerHTML = '';
    if (!session.shots.length) {
      filmstripEl.hidden = true;
      return;
    }
    filmstripEl.hidden = false;
    session.shots.forEach(function (shot) {
      var btn = document.createElement('button');
      btn.type = 'button';
      btn.className = 'scan-shot';
      if (shot.key === session.activeShotKey) {
        btn.className += session.viewingArchive ? ' is-active' : ' is-working';
      }
      var img = document.createElement('img');
      img.src = shot.thumb;
      img.alt = shot.label;
      var cap = document.createElement('span');
      cap.textContent = shot.label;
      btn.appendChild(img);
      btn.appendChild(cap);
      btn.addEventListener('click', function () { showArchivedShot(shot.key); });
      filmstripEl.appendChild(btn);
    });
  }

  function currentStage() {
    return session.sheet && session.sheet.stages ? session.sheet.stages[session.esIndex] : null;
  }

  function esHasReading(stage) {
    if (!stage) return false;
    return stage.pilots.some(function (p) {
      var det = session.detections[p.id + '_' + stage.esNumber];
      return det && (det.value || det.thumb);
    });
  }

  function totalSteps() {
    return 1 + (session.sheet && session.sheet.stages ? session.sheet.stages.length : 0);
  }

  function updateStepUi() {
    var st = currentStage();
    var total = totalSteps();
    if (session.phase === 'qr') {
      if (kickerEl) kickerEl.textContent = 'Étape 1 / ' + Math.max(total, 2);
      if (stepTitle) stepTitle.textContent = 'Photo du QR';
      if (stepLead) stepLead.textContent = 'Cadre le QR, bien net — pas besoin de toute la feuille.';
      btnAnalyze.textContent = 'Lire le QR';
      if (btnSkipEs) btnSkipEs.hidden = true;
      if (btnRestart) btnRestart.hidden = !session.shots.length;
    } else if (session.phase === 'es' && st) {
      var n = session.esIndex + 1;
      if (kickerEl) kickerEl.textContent = 'Étape ' + (n + 1) + ' / ' + total;
      if (stepTitle) stepTitle.textContent = 'Photo ES ' + st.esNumber;
      if (stepLead) {
        stepLead.textContent = session.awaitingCapture
          ? 'La photo précédente reste affichée. Prends maintenant l’ES ' + st.esNumber + ' (cases temps, de près).'
          : 'Cadre le tableau de l’ES ' + st.esNumber + ' (les ' + st.pilots.length + ' cases temps), de près.';
      }
      btnAnalyze.textContent = 'Lire ES ' + st.esNumber;
      if (btnSkipEs) btnSkipEs.hidden = false;
      if (btnRestart) btnRestart.hidden = false;
    } else {
      if (kickerEl) kickerEl.textContent = 'Vérification';
      if (stepTitle) stepTitle.textContent = 'Photos lues';
      if (stepLead) stepLead.textContent = 'Clique une miniature pour revoir la photo. Corrige les temps à droite, puis enregistre.';
      btnAnalyze.textContent = 'Lire le QR';
      if (btnSkipEs) btnSkipEs.hidden = true;
      if (btnRestart) btnRestart.hidden = false;
    }
    renderProgress();
    updateViewerTag();
  }

  function renderProgress() {
    if (!progressEl) return;
    progressEl.innerHTML = '';
    function chip(label, state, shotKey) {
      var el = document.createElement('button');
      el.type = 'button';
      el.className = 'scan-progress-step' + (state ? ' ' + state : '');
      el.textContent = label;
      if (shotKey && shotByKey(shotKey)) {
        el.addEventListener('click', function () { showArchivedShot(shotKey); });
      }
      progressEl.appendChild(el);
    }
    chip('QR', session.phase === 'qr' ? 'is-current' : (session.sheet ? 'is-done' : ''), 'qr');
    if (!session.sheet) return;
    session.sheet.stages.forEach(function (stage, i) {
      var state = '';
      if (session.phase === 'es' && i === session.esIndex) state = 'is-current';
      else if (esHasReading(stage) || i < session.esIndex || session.phase === 'done') state = 'is-done';
      chip('ES ' + stage.esNumber, state, 'es-' + stage.esNumber);
    });
  }

  async function promptNextPhoto() {
    session.awaitingCapture = true;
    session.viewingArchive = false;
    btnAnalyze.disabled = true;
    if (btnSnap && (video.hidden || !stream)) btnSnap.hidden = true;
    updateStepUi();
    if (session.phase === 'es' && currentStage()) {
      setStatus('Prends une photo de l’ES ' + currentStage().esNumber + ' — la précédente reste affichée.');
    } else if (session.phase === 'qr') {
      setStatus('Prends une photo du QR.');
    }
    updateViewerTag();
  }

  if (btnCamera) {
    btnCamera.addEventListener('click', async function () {
      try {
        usedCamera = true;
        await startCamera();
      } catch (err) {
        usedCamera = false;
        console.error(err);
        setStatus((err && err.message) ? err.message : 'Caméra bloquée. Utilise Importer.', 'is-error');
      }
    });
  }

  if (btnSnap) {
    btnSnap.addEventListener('click', function () {
      var ok = copyToPreview(video);
      stopCamera();
      if (ok) setStatus('Photo prise — lance la lecture.');
      else setStatus('Photo vide — réessaie Caméra, ou Importer.', 'is-error');
    });
  }

  async function loadFileToPreview(file) {
    try {
      if (typeof createImageBitmap === 'function') {
        var bmp;
        try {
          bmp = await createImageBitmap(file, { imageOrientation: 'from-image' });
        } catch (e1) {
          bmp = await createImageBitmap(file);
        }
        var okBmp = copyToPreview(bmp);
        if (bmp.close) bmp.close();
        if (okBmp) return true;
      }
    } catch (e2) { /* fallback Image */ }
    var url = URL.createObjectURL(file);
    try {
      var img = new Image();
      img.decoding = 'async';
      await new Promise(function (resolve, reject) {
        img.onload = resolve;
        img.onerror = reject;
        img.src = url;
      });
      return copyToPreview(img);
    } finally {
      URL.revokeObjectURL(url);
    }
  }

  if (fileInput) {
    fileInput.addEventListener('change', async function () {
      var file = fileInput.files && fileInput.files[0];
      if (!file) return;
      usedCamera = false;
      stopCamera();
      setStatus('Chargement de la photo…');
      try {
        var ok = await loadFileToPreview(file);
        if (ok) setStatus('Photo chargée — lance la lecture.');
        else setStatus('Image illisible. Réessaie une autre photo.', 'is-error');
      } catch (e) {
        console.error(e);
        setStatus('Impossible de lire cette image.', 'is-error');
      }
      fileInput.value = '';
    });
  }

  function cloneCanvas(source) {
    var c = document.createElement('canvas');
    c.width = source.width;
    c.height = source.height;
    c.getContext('2d').drawImage(source, 0, 0);
    return c;
  }

  function rotateCanvas(src, angle) {
    var w = src.width;
    var h = src.height;
    var cos = Math.abs(Math.cos(angle));
    var sin = Math.abs(Math.sin(angle));
    var nw = Math.max(1, Math.round(w * cos + h * sin));
    var nh = Math.max(1, Math.round(w * sin + h * cos));
    var c = document.createElement('canvas');
    c.width = nw;
    c.height = nh;
    var ctx = c.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, nw, nh);
    ctx.translate(nw / 2, nh / 2);
    ctx.rotate(angle);
    ctx.drawImage(src, -w / 2, -h / 2);
    return c;
  }

  function qrCenter(location) {
    if (!location) return null;
    var pts = [location.topLeftCorner, location.topRightCorner, location.bottomLeftCorner, location.bottomRightCorner];
    var x = 0;
    var y = 0;
    var n = 0;
    pts.forEach(function (p) {
      if (!p) return;
      x += p.x;
      y += p.y;
      n++;
    });
    return n ? { x: x / n, y: y / n } : null;
  }

  function deskewFromQr(source, location) {
    if (!location || !location.topLeftCorner || !location.topRightCorner) {
      return cloneCanvas(source);
    }
    var tl = location.topLeftCorner;
    var tr = location.topRightCorner;
    var angle = Math.atan2(tr.y - tl.y, tr.x - tl.x);
    var c = (!isFinite(angle) || Math.abs(angle) < (1 * Math.PI) / 180)
      ? cloneCanvas(source)
      : rotateCanvas(source, -angle);

    var qr2 = decodeQrDetailed(c);
    var mid = qrCenter((qr2 && qr2.location) || location);
    if (!mid) return c;
    var right = mid.x >= c.width * 0.5;
    var top = mid.y < c.height * 0.45;
    if (right && top) return c;
    if (right && !top) return rotateCanvas(c, -Math.PI / 2);
    if (!right && !top) return rotateCanvas(c, Math.PI);
    return rotateCanvas(c, Math.PI / 2);
  }

  function scaleLocation(loc, sx, sy) {
    if (!loc) return null;
    function pt(p) {
      return p ? { x: p.x * sx, y: p.y * sy } : p;
    }
    return {
      topLeftCorner: pt(loc.topLeftCorner),
      topRightCorner: pt(loc.topRightCorner),
      bottomLeftCorner: pt(loc.bottomLeftCorner),
      bottomRightCorner: pt(loc.bottomRightCorner)
    };
  }

  function canvasMaxSide(src, maxSide) {
    var scale = Math.min(1, maxSide / Math.max(src.width, src.height));
    if (scale >= 0.98) return { canvas: src, sx: 1, sy: 1 };
    var c = document.createElement('canvas');
    c.width = Math.max(1, Math.round(src.width * scale));
    c.height = Math.max(1, Math.round(src.height * scale));
    c.getContext('2d').drawImage(src, 0, 0, c.width, c.height);
    return { canvas: c, sx: src.width / c.width, sy: src.height / c.height };
  }

  function decodeQrOnCanvas(canvas) {
    if (typeof jsQR === 'undefined' || !canvas.width || !canvas.height) return null;
    var ctx = canvas.getContext('2d');
    var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    var code = jsQR(imageData.data, imageData.width, imageData.height, {
      inversionAttempts: 'attemptBoth'
    });
    if (!code || !code.data) return null;
    return { data: code.data.trim(), location: code.location || null };
  }

  function decodeQrDetailed(canvas) {
    var sizes = [800, 1100, 560];
    var i;
    for (i = 0; i < sizes.length; i++) {
      var scaled = canvasMaxSide(canvas, sizes[i]);
      var got = decodeQrOnCanvas(scaled.canvas);
      if (got) {
        got.location = scaleLocation(got.location, scaled.sx, scaled.sy);
        return got;
      }
    }
    var direct = decodeQrOnCanvas(canvas);
    if (direct) return direct;
    var corners = [
      { x: canvas.width * 0.52, y: 0, w: canvas.width * 0.48, h: canvas.height * 0.48 },
      { x: 0, y: 0, w: canvas.width * 0.48, h: canvas.height * 0.48 },
      { x: canvas.width * 0.52, y: canvas.height * 0.52, w: canvas.width * 0.48, h: canvas.height * 0.48 },
      { x: 0, y: canvas.height * 0.52, w: canvas.width * 0.48, h: canvas.height * 0.48 }
    ];
    for (i = 0; i < corners.length; i++) {
      var piece = cropCanvas(canvas, corners[i]);
      var pieceScaled = canvasMaxSide(piece, 700);
      var hit = decodeQrOnCanvas(pieceScaled.canvas);
      if (!hit) continue;
      hit.location = scaleLocation(hit.location, pieceScaled.sx, pieceScaled.sy);
      hit.location = scaleLocation(hit.location, 1, 1);
      if (hit.location) {
        ['topLeftCorner', 'topRightCorner', 'bottomLeftCorner', 'bottomRightCorner'].forEach(function (k) {
          if (!hit.location[k]) return;
          hit.location[k] = {
            x: hit.location[k].x + corners[i].x,
            y: hit.location[k].y + corners[i].y
          };
        });
      }
      return hit;
    }
    return null;
  }

  function longestRun(arr, thr) {
    var bestS = 0;
    var bestE = arr.length - 1;
    var best = 0;
    var start = -1;
    var i;
    for (i = 0; i <= arr.length; i++) {
      var ok = i < arr.length && arr[i] >= thr;
      if (ok) {
        if (start < 0) start = i;
      } else if (start >= 0) {
        if (i - start > best) {
          best = i - start;
          bestS = start;
          bestE = i - 1;
        }
        start = -1;
      }
    }
    return { s: bestS, e: bestE, len: best };
  }

  function findSheetRect(canvas) {
    var srcW = canvas.width;
    var srcH = canvas.height;
    if (srcW < 40 || srcH < 40) return { x: 0, y: 0, w: srcW, h: srcH };
    var maxD = 420;
    var scale = Math.min(1, maxD / Math.max(srcW, srcH));
    var w = Math.max(1, Math.round(srcW * scale));
    var h = Math.max(1, Math.round(srcH * scale));
    var tmp = document.createElement('canvas');
    tmp.width = w;
    tmp.height = h;
    var ctx = tmp.getContext('2d');
    ctx.drawImage(canvas, 0, 0, w, h);
    var data = ctx.getImageData(0, 0, w, h).data;
    var row = new Float32Array(h);
    var col = new Float32Array(w);
    var y;
    var x;
    for (y = 0; y < h; y++) {
      for (x = 0; x < w; x++) {
        var i = (y * w + x) * 4;
        if (inkGray(data[i], data[i + 1], data[i + 2]) > 150) {
          row[y]++;
          col[x]++;
        }
      }
    }
    for (y = 0; y < h; y++) row[y] /= w;
    for (x = 0; x < w; x++) col[x] /= h;
    var ry = longestRun(row, 0.20);
    var rx = longestRun(col, 0.20);
    if (ry.len < h * 0.28 || rx.len < w * 0.28) {
      return { x: 0, y: 0, w: srcW, h: srcH };
    }
    var x0 = rx.s / scale;
    var y0 = ry.s / scale;
    var x1 = (rx.e + 1) / scale;
    var y1 = (ry.e + 1) / scale;
    var padX = (x1 - x0) * 0.03;
    var padY = (y1 - y0) * 0.03;
    var rect = {
      x: Math.max(0, x0 - padX),
      y: Math.max(0, y0 - padY),
      w: Math.min(srcW, x1 + padX) - Math.max(0, x0 - padX),
      h: Math.min(srcH, y1 + padY) - Math.max(0, y0 - padY)
    };
    if (rect.w > srcW * 0.92 && rect.h > srcH * 0.92) {
      return { x: 0, y: 0, w: srcW, h: srcH };
    }
    return rect;
  }

  function cropToSheet(canvas) {
    var rect = findSheetRect(canvas);
    var cropped = (rect.x <= 2 && rect.y <= 2 && rect.w >= canvas.width - 4 && rect.h >= canvas.height - 4)
      ? cloneCanvas(canvas)
      : cropCanvas(canvas, rect);
    var m = Math.max(cropped.width, cropped.height);
    var target = 2800;
    if (m >= 1600 && m <= target) return cropped;
    var scale = m < 1600 ? 1600 / m : target / m;
    var c = document.createElement('canvas');
    c.width = Math.max(1, Math.round(cropped.width * scale));
    c.height = Math.max(1, Math.round(cropped.height * scale));
    var ctx = c.getContext('2d');
    ctx.imageSmoothingEnabled = true;
    ctx.drawImage(cropped, 0, 0, c.width, c.height);
    return c;
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

  function inkGray(r, g, b) {
    var lum = 0.299 * r + 0.587 * g + 0.114 * b;
    return Math.min(lum, Math.min(r, g, b));
  }

  function cropCanvas(source, box) {
    var x = Math.max(0, Math.round(box.x));
    var y = Math.max(0, Math.round(box.y));
    var w = Math.max(1, Math.round(box.w));
    var h = Math.max(1, Math.round(box.h));
    if (x + w > source.width) w = source.width - x;
    if (y + h > source.height) h = source.height - y;
    w = Math.max(1, w);
    h = Math.max(1, h);
    var c = document.createElement('canvas');
    c.width = w;
    c.height = h;
    var ctx = c.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, w, h);
    ctx.drawImage(source, x, y, w, h, 0, 0, w, h);
    return c;
  }

  function insetBox(box, frac) {
    var px = box.w * frac;
    var py = box.h * frac;
    return {
      x: box.x + px,
      y: box.y + py,
      w: Math.max(4, box.w - px * 2),
      h: Math.max(4, box.h - py * 2)
    };
  }

  /** Cases temps = rectangles clairs entourés d’un cadre imprimé. */
  function findTimeBoxes(canvas, darkLimit, paperLimit) {
    var darkCut = darkLimit == null ? 100 : darkLimit;
    var paperCut = paperLimit == null ? 155 : paperLimit;
    var w = canvas.width;
    var h = canvas.height;
    var ctx = canvas.getContext('2d');
    var data = ctx.getImageData(0, 0, w, h).data;
    var gray = new Uint8Array(w * h);
    var i;
    for (i = 0; i < w * h; i++) {
      gray[i] = inkGray(data[i * 4], data[i * 4 + 1], data[i * 4 + 2]);
    }

    function isDark(x, y) {
      if (x < 0 || y < 0 || x >= w || y >= h) return true;
      return gray[y * w + x] < darkCut;
    }
    function isPaper(x, y) {
      return gray[y * w + x] > paperCut;
    }

    var step = Math.max(3, Math.round(Math.min(w, h) / 220));
    var raw = [];
    var y;
    var x;
    for (y = Math.round(h * 0.12); y < h * 0.94; y += step) {
      for (x = Math.round(w * 0.03); x < w * 0.97; x += step) {
        if (!isPaper(x, y)) continue;
        var top = y;
        var bottom = y;
        var left = x;
        var right = x;
        while (top > 1 && !isDark(x, top)) top--;
        while (bottom < h - 2 && !isDark(x, bottom)) bottom++;
        while (left > 1 && !isDark(left, y)) left--;
        while (right < w - 2 && !isDark(right, y)) right++;
        var bw = right - left;
        var bh = bottom - top;
        if (bw < w * 0.07 || bw > w * 0.28) continue;
        if (bh < h * 0.02 || bh > h * 0.12) continue;
        var ar = bw / bh;
        if (ar < 1.8 || ar > 8) continue;
        raw.push({ x: left, y: top, w: bw, h: bh });
      }
    }

    raw.sort(function (a, b) { return (b.w * b.h) - (a.w * a.h); });
    var unique = [];
    raw.forEach(function (b) {
      var cx = b.x + b.w / 2;
      var cy = b.y + b.h / 2;
      var dup = unique.some(function (k) {
        return Math.abs(cx - (k.x + k.w / 2)) < k.w * 0.45 &&
          Math.abs(cy - (k.y + k.h / 2)) < k.h * 0.45;
      });
      if (!dup) unique.push(b);
    });
    return unique;
  }

  function findAllTimeBoxes(canvas) {
    var configs = [
      { dark: 70, paper: 95 },
      { dark: 95, paper: 155 },
      { dark: 115, paper: 140 },
      { dark: 125, paper: 175 }
    ];
    var raw = [];
    configs.forEach(function (cfg) {
      raw = raw.concat(findTimeBoxes(canvas, cfg.dark, cfg.paper));
    });
    raw.sort(function (a, b) { return (b.w * b.h) - (a.w * a.h); });
    var unique = [];
    raw.forEach(function (b) {
      var cx = b.x + b.w / 2;
      var cy = b.y + b.h / 2;
      var dup = unique.some(function (k) {
        return Math.abs(cx - (k.x + k.w / 2)) < k.w * 0.45 &&
          Math.abs(cy - (k.y + k.h / 2)) < k.h * 0.45;
      });
      if (!dup) unique.push(b);
    });
    if (unique.length < 2) return unique;
    var areas = unique.map(function (b) { return b.w * b.h; }).sort(function (a, b) { return a - b; });
    var med = areas[Math.floor(areas.length / 2)];
    return unique.filter(function (b) {
      var a = b.w * b.h;
      return a > med * 0.45 && a < med * 2.2;
    });
  }

  function medianNum(arr) {
    if (!arr.length) return 0;
    var s = arr.slice().sort(function (a, b) { return a - b; });
    return s[Math.floor(s.length / 2)];
  }

  function completeFour(group) {
    var sorted = group.slice().sort(function (a, b) { return a.y - b.y; });
    if (sorted.length >= 4) {
      if (sorted.length === 4) return sorted;
      var mx = medianNum(sorted.map(function (b) { return b.x + b.w / 2; }));
      var mw = medianNum(sorted.map(function (b) { return b.w; }));
      var mh = medianNum(sorted.map(function (b) { return b.h; }));
      var scored = sorted.map(function (b) {
        return {
          b: b,
          s: Math.abs((b.x + b.w / 2) - mx) / Math.max(1, mw) +
            Math.abs(b.w - mw) / Math.max(1, mw) +
            Math.abs(b.h - mh) / Math.max(1, mh)
        };
      });
      scored.sort(function (a, b) { return a.s - b.s; });
      return scored.slice(0, 4).map(function (x) { return x.b; }).sort(function (a, b) { return a.y - b.y; });
    }
    if (sorted.length !== 3) return sorted;
    var g0 = sorted[1].y - sorted[0].y;
    var g1 = sorted[2].y - sorted[1].y;
    var mw = medianNum(sorted.map(function (b) { return b.w; }));
    var mh = medianNum(sorted.map(function (b) { return b.h; }));
    var mx = medianNum(sorted.map(function (b) { return b.x; }));
    function make(y) { return { x: mx, y: Math.round(y), w: mw, h: mh }; }
    var gap = (g0 + g1) / 2;
    if (g0 > g1 * 1.4) return [sorted[0], make(sorted[0].y + g1), sorted[1], sorted[2]];
    if (g1 > g0 * 1.4) return [sorted[0], sorted[1], make(sorted[1].y + g0), sorted[2]];
    return [sorted[0], sorted[1], sorted[2], make(sorted[2].y + gap)];
  }

  function clusterTimeBoxes(boxes, W, H) {
    var timed = boxes.filter(function (b) {
      var cy = b.y + b.h / 2;
      return b.w >= W * 0.07 && b.w <= W * 0.22 &&
        b.h >= H * 0.03 && b.h <= H * 0.09 &&
        cy > H * 0.16;
    });
    if (timed.length < 8) timed = boxes;
    var left = timed.filter(function (b) { return b.x + b.w / 2 < W * 0.55; });
    var right = timed.filter(function (b) { return b.x + b.w / 2 >= W * 0.55; });
    function quad(col, top) {
      var y0 = top ? 0 : H * 0.48;
      var y1 = top ? H * 0.52 : H;
      var group = col.filter(function (b) {
        var cy = b.y + b.h / 2;
        return cy >= y0 && cy < y1;
      });
      var mx = medianNum(group.map(function (b) { return b.x + b.w / 2; }));
      var mw = medianNum(group.map(function (b) { return b.w; })) || 1;
      var aligned = group.filter(function (b) {
        return Math.abs(b.x + b.w / 2 - mx) < mw * 0.12;
      });
      return completeFour(aligned.length >= 3 ? aligned : group);
    }
    return quad(left, true).concat(quad(right, true), quad(left, false), quad(right, false));
  }

  function expandBoxLeft(box, W) {
    var extraL = Math.min(140, Math.max(80, box.w * 0.40));
    var extraR = Math.min(90, Math.max(45, box.w * 0.18));
    var x = Math.max(0, box.x - extraL);
    var targetH = Math.min(Math.max(Math.round(box.h * 1.22), 28), 80);
    var cy = box.y + box.h / 2;
    return {
      x: x,
      y: Math.max(0, cy - targetH / 2),
      w: Math.min(box.w + extraL + extraR, W - x),
      h: targetH
    };
  }

  function darkPeaks(arr, thr, minGap) {
    var peaks = [];
    var i;
    for (i = 1; i < arr.length - 1; i++) {
      if (arr[i] < thr || arr[i] < arr[i - 1] || arr[i] < arr[i + 1]) continue;
      if (!peaks.length || i - peaks[peaks.length - 1] >= minGap) {
        peaks.push(i);
      } else if (arr[i] > arr[peaks[peaks.length - 1]]) {
        peaks[peaks.length - 1] = i;
      }
    }
    return peaks;
  }

  function esBoxesFromTableGrid(canvas, nP) {
    var maxD = 1000;
    var inv = 1;
    var src = canvas;
    var maxSide = Math.max(canvas.width, canvas.height);
    if (maxSide > maxD) {
      var scale = maxD / maxSide;
      inv = 1 / scale;
      var tmp = document.createElement('canvas');
      tmp.width = Math.max(1, Math.round(canvas.width * scale));
      tmp.height = Math.max(1, Math.round(canvas.height * scale));
      tmp.getContext('2d').drawImage(canvas, 0, 0, tmp.width, tmp.height);
      src = tmp;
    }
    var w = src.width;
    var h = src.height;
    var ctx = src.getContext('2d');
    var data = ctx.getImageData(0, 0, w, h).data;
    var col = new Float32Array(w);
    var row = new Float32Array(h);
    var x;
    var y;
    for (y = 0; y < h; y++) {
      for (x = 0; x < w; x++) {
        var i = (y * w + x) * 4;
        if (inkGray(data[i], data[i + 1], data[i + 2]) < 120) {
          col[x]++;
          row[y]++;
        }
      }
    }
    for (x = 0; x < w; x++) col[x] /= h;
    for (y = 0; y < h; y++) row[y] /= w;

    var vPeaks = darkPeaks(col, 0.16, Math.max(10, Math.round(w * 0.045)));
    /* minGap large : ignore le cadre interne .time-box, ne garde que les filets de lignes. */
    var hPeaks = darkPeaks(row, 0.10, Math.max(14, Math.round(h * 0.07)));
    if (vPeaks.length < 2 || hPeaks.length < nP) return null;

    var x0 = vPeaks[vPeaks.length - 2];
    var x1 = vPeaks[vPeaks.length - 1];
    if ((x1 - x0) < w * 0.14 && vPeaks.length >= 3) {
      x0 = vPeaks[vPeaks.length - 3];
    }
    if ((x1 - x0) > w * 0.62 && vPeaks.length >= 3) {
      x0 = vPeaks[vPeaks.length - 2];
    }
    if (x0 > w * 0.82) x0 = Math.round(w * 0.52);

    var ys = hPeaks.slice();
    while (ys.length > nP + 2) {
      var dropAt = 0;
      var smallest = Infinity;
      var k;
      for (k = 0; k < ys.length - 1; k++) {
        var gap = ys[k + 1] - ys[k];
        if (gap < smallest) {
          smallest = gap;
          dropAt = k;
        }
      }
      var g0 = ys[1] - ys[0];
      var gLast = ys[ys.length - 1] - ys[ys.length - 2];
      if (dropAt === 0 && g0 <= gLast) ys.shift();
      else if (dropAt === ys.length - 2) ys.pop();
      else ys.splice(dropAt + 1, 1);
    }
    if (ys.length < nP + 1) return null;

    var gapList = [];
    for (r = 0; r < ys.length - 1; r++) gapList.push(ys[r + 1] - ys[r]);
    var medGap = medianNum(gapList) || (h / (nP + 1));
    if (ys.length === nP + 2) {
      if (gapList[0] <= gapList[gapList.length - 1]) ys.shift();
      else ys.pop();
    } else if (ys.length === nP + 1 && gapList[0] < medGap * 0.72) {
      ys.shift();
      ys.push(Math.min(h - 1, ys[ys.length - 1] + medGap));
    }

    var padX = Math.max(4, Math.round((x1 - x0) * 0.08));
    var padY = Math.max(3, Math.round(h * 0.01));
    var boxes = [];
    var r;
    for (r = 0; r < nP; r++) {
      var y0 = ys[r];
      var y1 = ys[r + 1];
      if (y1 - y0 < h * 0.04) return null;
      boxes.push({
        x: (x0 + padX) * inv,
        y: (y0 + padY) * inv,
        w: Math.max(12, (x1 - x0 - padX * 2) * inv),
        h: Math.max(10, (y1 - y0 - padY * 2) * inv)
      });
    }
    return boxes;
  }

  function isCleanTimeStack(boxes, nP, W, H) {
    if (!boxes || boxes.length !== nP) return false;
    var sorted = boxes.slice().sort(function (a, b) { return a.y - b.y; });
    var mx = medianNum(sorted.map(function (b) { return b.x + b.w / 2; }));
    var mw = medianNum(sorted.map(function (b) { return b.w; })) || 1;
    var i;
    for (i = 0; i < nP; i++) {
      var b = sorted[i];
      if (Math.abs(b.x + b.w / 2 - mx) > Math.max(W * 0.10, mw * 0.35)) return false;
      if (b.w < W * 0.12 || b.w > W * 0.96) return false;
      if (b.h < (H || 1) * 0.06) return false;
    }
    var first = sorted[0];
    var last = sorted[nP - 1];
    if (H) {
      if (first.y > H * 0.32) return false;
      if (last.y + last.h < H * 0.58) return false;
    }
    return true;
  }

  function expandEsBox(box, W, H) {
    var extraL = Math.min(28, Math.max(6, box.w * 0.03));
    var extraR = Math.min(18, Math.max(4, box.w * 0.02));
    var extraY = Math.min(16, Math.max(4, box.h * 0.10));
    var x = Math.max(0, box.x - extraL);
    var y = Math.max(0, box.y - extraY);
    return {
      x: x,
      y: y,
      w: Math.min(box.w + extraL + extraR, W - x),
      h: Math.min(box.h + extraY * 2, H - y)
    };
  }

  function findEsTimeBoxes(canvas) {
    var maxD = 960;
    var inv = 1;
    var src = canvas;
    var maxSide = Math.max(canvas.width, canvas.height);
    if (maxSide > maxD) {
      var scale = maxD / maxSide;
      inv = 1 / scale;
      var tmp = document.createElement('canvas');
      tmp.width = Math.max(1, Math.round(canvas.width * scale));
      tmp.height = Math.max(1, Math.round(canvas.height * scale));
      tmp.getContext('2d').drawImage(canvas, 0, 0, tmp.width, tmp.height);
      src = tmp;
    }
    var w = src.width;
    var h = src.height;
    var configs = [
      { dark: 70, paper: 95 },
      { dark: 95, paper: 155 },
      { dark: 115, paper: 140 },
      { dark: 125, paper: 175 },
      { dark: 145, paper: 195 }
    ];
    var ctx = src.getContext('2d');
    var data = ctx.getImageData(0, 0, w, h).data;
    var gray = new Uint8Array(w * h);
    var gi;
    for (gi = 0; gi < w * h; gi++) {
      gray[gi] = inkGray(data[gi * 4], data[gi * 4 + 1], data[gi * 4 + 2]);
    }
    var raw = [];
    configs.forEach(function (cfg) {
      var darkCut = cfg.dark;
      var paperCut = cfg.paper;
      function isDark(x, y) {
        if (x < 0 || y < 0 || x >= w || y >= h) return true;
        return gray[y * w + x] < darkCut;
      }
      function isPaper(x, y) {
        return gray[y * w + x] > paperCut;
      }
      var step = Math.max(3, Math.round(Math.min(w, h) / 160));
      var y;
      var x;
      for (y = Math.round(h * 0.03); y < h * 0.97; y += step) {
        for (x = Math.round(w * 0.04); x < w * 0.96; x += step) {
          if (!isPaper(x, y)) continue;
          var top = y;
          var bottom = y;
          var left = x;
          var right = x;
          while (top > 1 && !isDark(x, top)) top--;
          while (bottom < h - 2 && !isDark(x, bottom)) bottom++;
          while (left > 1 && !isDark(left, y)) left--;
          while (right < w - 2 && !isDark(right, y)) right++;
          var bw = right - left;
          var bh = bottom - top;
          if (bw < w * 0.16 || bw > w * 0.94) continue;
          if (bh < h * 0.06 || bh > h * 0.34) continue;
          var ar = bw / bh;
          if (ar < 1.5 || ar > 9) continue;
          if (w > h * 1.2 && left + bw / 2 < w * 0.40) continue;
          raw.push({ x: left, y: top, w: bw, h: bh });
        }
      }
    });
    raw.sort(function (a, b) { return (b.w * b.h) - (a.w * a.h); });
    var unique = [];
    raw.forEach(function (b) {
      var cx = b.x + b.w / 2;
      var cy = b.y + b.h / 2;
      var dup = unique.some(function (k) {
        return Math.abs(cx - (k.x + k.w / 2)) < k.w * 0.45 &&
          Math.abs(cy - (k.y + k.h / 2)) < k.h * 0.45;
      });
      if (!dup) unique.push(b);
    });
    if (unique.length < 2) {
      return inv === 1 ? unique : unique.map(function (b) {
        return { x: b.x * inv, y: b.y * inv, w: b.w * inv, h: b.h * inv };
      });
    }
    var areas = unique.map(function (b) { return b.w * b.h; }).sort(function (a, b) { return a - b; });
    var med = areas[Math.floor(areas.length / 2)];
    var filtered = unique.filter(function (b) {
      var a = b.w * b.h;
      return a > med * 0.40 && a < med * 2.4;
    });
    if (inv === 1) return filtered;
    return filtered.map(function (b) {
      return { x: b.x * inv, y: b.y * inv, w: b.w * inv, h: b.h * inv };
    });
  }

  function pickEsColumn(boxes, W, H, nP) {
    if (!boxes.length) return [];
    var wide = W > H * 1.15;
    var right = wide ? boxes.filter(function (b) { return b.x + b.w / 2 > W * 0.45; }) : boxes;
    var pool = right.length >= Math.min(3, nP) ? right : boxes;
    var mx = medianNum(pool.map(function (b) { return b.x + b.w / 2; }));
    var mw = medianNum(pool.map(function (b) { return b.w; })) || 1;
    var aligned = pool.filter(function (b) {
      return Math.abs(b.x + b.w / 2 - mx) < mw * 0.28;
    });
    var col = (nP === 4)
      ? completeFour(aligned.length >= 3 ? aligned : pool)
      : (aligned.length >= nP ? aligned : pool).slice().sort(function (a, b) { return a.y - b.y; }).slice(0, nP);
    return col.map(function (b) { return expandEsBox(b, W, H); });
  }

  function longestDarkRun(gray, w, y, darkCut) {
    var bestS = 0;
    var bestE = -1;
    var best = 0;
    var start = -1;
    var x;
    for (x = 0; x <= w; x++) {
      var dark = x < w && gray[y * w + x] < darkCut;
      if (dark) {
        if (start < 0) start = x;
      } else if (start >= 0) {
        if (x - start > best) {
          best = x - start;
          bestS = start;
          bestE = x - 1;
        }
        start = -1;
      }
    }
    return { x0: bestS, x1: bestE, len: best };
  }

  function mergeCloseYs(ys, minGap) {
    var out = [];
    ys.forEach(function (y) {
      if (!out.length || y - out[out.length - 1] > minGap) out.push(y);
      else out[out.length - 1] = Math.round((out[out.length - 1] + y) / 2);
    });
    return out;
  }

  function selectStackedBoxes(cands, nP, H) {
    if (!cands.length) return null;
    cands = cands.slice().sort(function (a, b) { return a.h - b.h; });
    var kept = [];
    cands.forEach(function (b) {
      var overlap = kept.some(function (k) {
        return Math.abs((k.y + k.h / 2) - (b.y + b.h / 2)) < Math.min(k.h, b.h) * 0.45;
      });
      if (!overlap) kept.push(b);
    });
    kept.sort(function (a, b) { return a.y - b.y; });
    if (kept.length > nP) {
      var mh = medianNum(kept.map(function (b) { return b.h; })) || 1;
      kept = kept.filter(function (b) {
        return b.h > mh * 0.55 && b.h < mh * 1.55;
      });
      if (kept.length > nP) kept = kept.slice(0, nP);
    }
    if (kept.length === nP) return kept;
    if (nP === 4 && kept.length === 3) return completeFour(kept);
    return null;
  }

  /** Cadres imprimés .time-box : paires de filets horizontaux de même largeur. */
  function findTimeBoxFrames(canvas, nP) {
    var maxD = 900;
    var inv = 1;
    var src = canvas;
    var maxSide = Math.max(canvas.width, canvas.height);
    if (maxSide > maxD) {
      var scale = maxD / maxSide;
      inv = 1 / scale;
      var tmp = document.createElement('canvas');
      tmp.width = Math.max(1, Math.round(canvas.width * scale));
      tmp.height = Math.max(1, Math.round(canvas.height * scale));
      tmp.getContext('2d').drawImage(canvas, 0, 0, tmp.width, tmp.height);
      src = tmp;
    }
    var w = src.width;
    var h = src.height;
    var ctx = src.getContext('2d');
    var data = ctx.getImageData(0, 0, w, h).data;
    var gray = new Uint8Array(w * h);
    var i;
    for (i = 0; i < w * h; i++) {
      gray[i] = inkGray(data[i * 4], data[i * 4 + 1], data[i * 4 + 2]);
    }
    var darkCut = 118;
    var rawYs = [];
    var y;
    for (y = 1; y < h - 1; y++) {
      var run = longestDarkRun(gray, w, y, darkCut);
      if (run.len > w * 0.22) rawYs.push(y);
    }
    var ys = mergeCloseYs(rawYs, Math.max(3, Math.round(h * 0.008)));
    var cands = [];
    var a;
    var b;
    for (a = 0; a < ys.length; a++) {
      var spanA = longestDarkRun(gray, w, ys[a], darkCut);
      for (b = a + 1; b < ys.length; b++) {
        var bh = ys[b] - ys[a];
        if (bh < h * 0.08) continue;
        if (bh > h * 0.32) break;
        var spanB = longestDarkRun(gray, w, ys[b], darkCut);
        if (spanA.len < w * 0.22 || spanB.len < w * 0.22) continue;
        var x0 = Math.min(spanA.x0, spanB.x0);
        var x1 = Math.max(spanA.x1, spanB.x1);
        var bw = x1 - x0;
        if (bw < w * 0.20 || bw > w * 0.96) continue;
        var ar = bw / bh;
        if (ar < 1.7 || ar > 8.5) continue;
        if (Math.abs(spanA.x0 - spanB.x0) > w * 0.10) continue;
        if (Math.abs(spanA.len - spanB.len) > w * 0.18) continue;
        var pad = Math.max(2, Math.round(bw * 0.04));
        cands.push({
          x: x0 + pad,
          y: ys[a] + Math.max(2, Math.round(bh * 0.06)),
          w: bw - pad * 2,
          h: Math.max(8, bh - Math.round(bh * 0.12))
        });
      }
    }
    var picked = selectStackedBoxes(cands, nP, h);
    if (!picked) return null;
    return picked.map(function (box) {
      return { x: box.x * inv, y: box.y * inv, w: box.w * inv, h: box.h * inv };
    });
  }

  function esBoxesFromInkStack(canvas, nP) {
    var maxD = 900;
    var inv = 1;
    var src = canvas;
    var maxSide = Math.max(canvas.width, canvas.height);
    if (maxSide > maxD) {
      var scale = maxD / maxSide;
      inv = 1 / scale;
      var tmp = document.createElement('canvas');
      tmp.width = Math.max(1, Math.round(canvas.width * scale));
      tmp.height = Math.max(1, Math.round(canvas.height * scale));
      tmp.getContext('2d').drawImage(canvas, 0, 0, tmp.width, tmp.height);
      src = tmp;
    }
    var w = src.width;
    var h = src.height;
    var ctx = src.getContext('2d');
    var data = ctx.getImageData(0, 0, w, h).data;
    var minX = w;
    var minY = h;
    var maxX = 0;
    var maxY = 0;
    var x;
    var y;
    for (y = 0; y < h; y++) {
      for (x = 0; x < w; x++) {
        var p = (y * w + x) * 4;
        if (inkGray(data[p], data[p + 1], data[p + 2]) < 128) {
          if (x < minX) minX = x;
          if (y < minY) minY = y;
          if (x > maxX) maxX = x;
          if (y > maxY) maxY = y;
        }
      }
    }
    if (maxX <= minX || maxY <= minY) {
      minX = Math.round(w * 0.04);
      minY = Math.round(h * 0.04);
      maxX = Math.round(w * 0.96);
      maxY = Math.round(h * 0.96);
    }
    var padX = Math.max(2, Math.round((maxX - minX) * 0.02));
    var padY = Math.max(2, Math.round((maxY - minY) * 0.02));
    minX = Math.max(0, minX - padX);
    maxX = Math.min(w - 1, maxX + padX);
    minY = Math.max(0, minY - padY);
    maxY = Math.min(h - 1, maxY + padY);
    var boxW = maxX - minX;
    var boxH = maxY - minY;
    var x0 = minX;
    var x1 = maxX;
    if (boxW > boxH * 0.90) {
      x0 = minX + Math.round(boxW * 0.56);
    }
    var y0 = minY;
    var y1 = maxY;
    var rowInk = new Float32Array(h);
    for (y = minY; y <= maxY; y++) {
      var dark = 0;
      for (x = minX; x <= maxX; x++) {
        var q = (y * w + x) * 4;
        if (inkGray(data[q], data[q + 1], data[q + 2]) < 120) dark++;
      }
      rowInk[y] = dark / Math.max(1, boxW);
    }
    var headerBand = minY + Math.round(boxH * 0.20);
    var bestLine = -1;
    var bestScore = 0;
    for (y = minY + Math.round(boxH * 0.04); y < headerBand; y++) {
      if (rowInk[y] > 0.18 && rowInk[y] >= (rowInk[y - 1] || 0) && rowInk[y] >= (rowInk[y + 1] || 0)) {
        if (rowInk[y] > bestScore) {
          bestScore = rowInk[y];
          bestLine = y;
        }
      }
    }
    if (bestLine > 0 && (bestLine - minY) / boxH <= 0.18) y0 = bestLine;
    var usable = y1 - y0;
    var rowH = usable / nP;
    var boxes = [];
    var r;
    for (r = 0; r < nP; r++) {
      boxes.push({
        x: (x0 + Math.round((x1 - x0) * 0.05)) * inv,
        y: (y0 + r * rowH + rowH * 0.06) * inv,
        w: Math.max(12, (x1 - x0) * 0.90 * inv),
        h: Math.max(10, rowH * 0.68 * inv)
      });
    }
    return boxes;
  }

  function boxesForEsPhoto(canvas, nP) {
    var work = cloneCanvas(canvas);
    var W = work.width;
    var H = work.height;
    var frames = findTimeBoxFrames(work, nP);
    if (frames && frames.length === nP) {
      return { canvas: work, boxes: frames };
    }
    var found = pickEsColumn(findEsTimeBoxes(work), W, H, nP);
    if (isCleanTimeStack(found, nP, W, H)) {
      return { canvas: work, boxes: found.slice(0, nP) };
    }
    var grid = esBoxesFromTableGrid(work, nP);
    if (isCleanTimeStack(grid, nP, W, H)) {
      return { canvas: work, boxes: grid };
    }
    return { canvas: work, boxes: esBoxesFromInkStack(work, nP) };
  }

  function drawBoxList(canvas, boxes) {
    var ctx = canvas.getContext('2d');
    boxes.forEach(function (b) {
      if (!b) return;
      ctx.save();
      ctx.strokeStyle = '#1f7a3f';
      ctx.lineWidth = 3;
      ctx.strokeRect(b.x + 1, b.y + 1, b.w - 2, b.h - 2);
      ctx.restore();
    });
  }

  function splitEqual(items, n, keyFn) {
    var sorted = items.slice().sort(function (a, b) { return keyFn(a) - keyFn(b); });
    if (n <= 1) return [sorted];
    var out = [];
    var i = 0;
    var g;
    for (g = 0; g < n; g++) {
      var remaining = n - g;
      var take = Math.round((sorted.length - i) / remaining);
      out.push(sorted.slice(i, i + take));
      i += take;
    }
    return out;
  }

  function assignBoxesToSheet(boxes, sheet, canvas) {
    var map = {};
    var stages = sheet.stages || [];
    if (!stages.length) return map;
    var nS = stages.length;
    var nP = (stages[0].pilots || []).length;
    var expected = nS * nP;
    if (!boxes.length || !nP) return map;

    var W = canvas ? canvas.width : 0;
    var H = canvas ? canvas.height : 0;
    var clusters = [];

    if (nS === 4 && nP === 4 && W > 0) {
      var clustered = clusterTimeBoxes(boxes, W, H).map(function (b) {
        return expandBoxLeft(b, W);
      });
      var qi;
      for (qi = 0; qi < 4; qi++) {
        clusters.push(clustered.slice(qi * 4, qi * 4 + 4));
      }
    } else {
      var esRows = nS <= 3 ? 1 : (nP >= 5 ? 1 : 2);
      var esCols = Math.ceil(nS / esRows);
      function cx(b) { return b.x + b.w / 2; }
      function cy(b) { return b.y + b.h / 2; }
      if (esRows === 1) {
        splitEqual(boxes, esCols, cx).forEach(function (col) {
          clusters.push(col.slice().sort(function (a, b) { return a.y - b.y; }));
        });
      } else {
        var rows = splitEqual(boxes, esRows, cy);
        rows.forEach(function (row, ri) {
          var colsThis = ri < esRows - 1 ? esCols : (nS - esCols * (esRows - 1));
          splitEqual(row, colsThis, cx).forEach(function (col) {
            clusters.push(col.slice().sort(function (a, b) { return a.y - b.y; }));
          });
        });
      }
    }

    stages.forEach(function (stage, si) {
      var col = clusters[si] || [];
      if (col.length > nP) col = col.slice(0, nP);
      stage.pilots.forEach(function (p, pi) {
        map[p.id + '_' + stage.esNumber] = col[pi] || null;
      });
    });
    map._expected = expected;
    map._found = boxes.length;
    return map;
  }

  function drawBoxes(canvas, boxMap) {
    var ctx = canvas.getContext('2d');
    Object.keys(boxMap).forEach(function (key) {
      if (key.charAt(0) === '_') return;
      var b = boxMap[key];
      if (!b) return;
      ctx.save();
      ctx.strokeStyle = '#1f7a3f';
      ctx.lineWidth = 2;
      ctx.strokeRect(b.x + 1, b.y + 1, b.w - 2, b.h - 2);
      ctx.restore();
    });
  }

  function normalizeOcrDigits(raw) {
    return String(raw || '')
      .trim()
      .replace(/[OoDQCq]/g, '0')
      .replace(/[Il|!jJ]/g, '1')
      .replace(/[Zz]/g, '2')
      .replace(/[Aa]/g, '4')
      .replace(/[Ss]/g, '5')
      .replace(/[Tt]/g, '7')
      .replace(/[Bb]/g, '8')
      .replace(/[Gg]/g, '9')
      .replace(/[;]/g, ':')
      .replace(/,/g, '.')
      .replace(/\s+/g, '');
  }

  function formatSeconds(seconds) {
    var totalMillis = Math.round(seconds * 1000);
    var abs = Math.abs(totalMillis);
    var h = Math.floor(abs / 3600000);
    var m = Math.floor((abs % 3600000) / 60000);
    var s = Math.floor((abs % 60000) / 1000);
    var ms = abs % 1000;
    function pad(n, width) {
      return String(n).padStart(width, '0');
    }
    if (h > 0) return h + ':' + pad(m, 2) + ':' + pad(s, 2) + '.' + pad(ms, 3);
    if (m > 0) return m + ':' + pad(s, 2) + '.' + pad(ms, 3);
    return s + '.' + pad(ms, 3);
  }

  function parseTimeCandidate(raw) {
    if (!raw) return null;
    var value = normalizeOcrDigits(raw);
    value = value.replace(/[^\d:.]/g, '');
    value = value.replace(/\.{2,}/g, '.').replace(/:{2,}/g, ':').replace(/\.+:/g, ':').replace(/:\.+/g, ':');
    if (!value || value === '-' || value === '.' || value === ':') return null;

    // 12:345 → virgule/point lu comme ":" (secondes.milli, pas mm:ss)
    var colonDec = value.match(/^(\d{1,3}):(\d{2,3})$/);
    if (colonDec && Number(colonDec[2]) > 59) {
      value = colonDec[1] + '.' + colonDec[2];
    }

    var euro = value.match(/^(\d{1,2})\.([0-5]?\d)\.(\d{1,3})$/);
    if (euro) {
      value = euro[1] + ':' + euro[2] + '.' + euro[3];
    }

    // Slot : 1234 / 12345 / 123456 → s.mmm / ss.mmm / sss.mmm
    if (/^\d{4,6}$/.test(value)) {
      var digits = value;
      var msLen = 3;
      var msPart = digits.slice(-msLen);
      var secPart = digits.slice(0, -msLen);
      if (!secPart) return null;
      var asSec = parseInt(secPart, 10) + parseInt((msPart + '000').slice(0, 3), 10) / 1000;
      if (asSec >= 8 && asSec <= 180) {
        return formatSeconds(asSec);
      }
    }

    var seconds = null;
    var hh = value.match(/^(\d+):([0-5]?\d):([0-5]?\d)(?:\.(\d{1,3}))?$/);
    var mm = value.match(/^(\d+):([0-5]?\d)(?:\.(\d{1,3}))?$/);
    var ss = value.match(/^(\d+)(?:\.(\d{1,3}))?$/);
    function frac(d) {
      if (!d) return 0;
      return parseInt((d + '000').slice(0, 3), 10) / 1000;
    }
    if (hh) {
      seconds = (+hh[1]) * 3600 + (+hh[2]) * 60 + (+hh[3]) + frac(hh[4]);
    } else if (mm) {
      seconds = (+mm[1]) * 60 + (+mm[2]) + frac(mm[3]);
    } else if (ss) {
      seconds = (+ss[1]) + frac(ss[2]);
    } else {
      return null;
    }
    if (!isFinite(seconds) || seconds <= 0 || seconds > 180) return null;
    var digitCount = value.replace(/\D/g, '').length;
    if (digitCount < 4 && value.indexOf('.') < 0) return null;
    if (seconds < 8) return null;
    return formatSeconds(seconds);
  }

  function secondsOf(formatted) {
    if (!formatted) return null;
    var p = formatted.split(':');
    var sec = 0;
    if (p.length === 3) {
      sec = (+p[0]) * 3600 + (+p[1]) * 60 + parseFloat(p[2]);
    } else if (p.length === 2) {
      sec = (+p[0]) * 60 + parseFloat(p[1]);
    } else {
      sec = parseFloat(formatted);
    }
    return isFinite(sec) ? sec : null;
  }

  /** Temps rallye slot réels ~ 20–120 s (ex. 59,337) — pas 7.000. */
  function scoreTime(formatted, ocrConf, raw) {
    var sec = secondsOf(formatted);
    if (sec == null) return -10;
    if (sec < 8 || sec > 180) return -6;
    var digits = String(raw || formatted).replace(/\D/g, '').length;
    var score = (ocrConf || 40) / 100;
    if (sec >= 20 && sec <= 120) score += 4;
    if (sec >= 45 && sec <= 95) score += 3;
    else if (sec < 20) score -= 8;
    else if (sec < 8 || sec > 180) score -= 6;
    if (digits === 5) score += 5;
    else if (digits >= 4 && digits <= 6) score += 3;
    if (digits <= 2) score -= 5;
    if (/\.\d{3}$/.test(formatted)) score += 0.8;
    return score;
  }

  function interpretationsFromRaw(raw) {
    var cleaned = normalizeOcrDigits(raw).replace(/[^\d:.]/g, '');
    var tries = [cleaned];
    if (/^1\d{4,5}$/.test(cleaned)) tries.push(cleaned.slice(1));
    if (/^\d{5,6}1$/.test(cleaned)) tries.push(cleaned.slice(0, -1));
    var digitsOnly = cleaned.replace(/\D/g, '');
    if (digitsOnly.length !== 5 && /^[5-8]\d{3}$/.test(cleaned)) {
      tries.push(cleaned.charAt(0) + cleaned);
    }
    if (digitsOnly.length === 5 && digitsOnly.slice(2) !== '999') {
      var k;
      for (k = 2; k < 5; k++) {
        if (digitsOnly.charAt(k) === '9') {
          tries.push(digitsOnly.slice(0, k) + '8' + digitsOnly.slice(k + 1));
        }
      }
    }
    var stripped = digitsOnly;
    var hadLead1 = stripped.length >= 4 && stripped.charAt(0) === '1';
    while (stripped.length >= 4 && stripped.charAt(0) === '1') stripped = stripped.slice(1);
    if (hadLead1 && stripped.length === 3 && stripped.charAt(0) === '7') {
      tries.push('70' + stripped);
    }
    if (digitsOnly.length === 3 && digitsOnly.charAt(0) === '4') {
      tries.push('60' + digitsOnly);
    }
    if (digitsOnly.length === 4 && digitsOnly.charAt(0) === '8') {
      tries.push(digitsOnly.charAt(0) + '4' + digitsOnly.slice(1));
    }
    if (digitsOnly.length === 5 && digitsOnly.charAt(0) === '3') {
      if (digitsOnly.slice(2) === '999') tries.push('77999');
      if (digitsOnly.slice(3) === '22') tries.push('7' + digitsOnly.slice(1, 3) + '77');
      tries.push('7' + digitsOnly.slice(1));
    }
    if (digitsOnly.length === 6 && digitsOnly.charAt(0) === '3') {
      var five = digitsOnly.slice(0, 5);
      if (five.slice(2) === '999') tries.push('77999');
      if (five.charAt(0) === '3') tries.push('7' + five.slice(1));
      if (five.slice(3) === '22') tries.push('7' + five.slice(1, 3) + '77');
    }
    var out = [];
    var seen = {};
    tries.forEach(function (t) {
      var parsed = parseTimeCandidate(t);
      if (!parsed || seen[parsed]) return;
      seen[parsed] = true;
      out.push(parsed);
    });
    return out;
  }

  var workerPoolPromise = null;

  function tessParams(psm) {
    return {
      tessedit_pageseg_mode: String(psm),
      tessedit_char_whitelist: '0123456789:.,',
      preserve_interword_spaces: '0'
    };
  }

  async function makeTessWorker() {
    var worker = await Tesseract.createWorker('eng', 1, { logger: function () {} });
    await worker.setParameters(tessParams('7'));
    worker._psm = '7';
    return worker;
  }

  function getWorkerPool() {
    if (!workerPoolPromise) {
      workerPoolPromise = Promise.all([makeTessWorker(), makeTessWorker()]);
    }
    return workerPoolPromise;
  }

  function getWorker() {
    if (!workerPromise) {
      workerPromise = getWorkerPool().then(function (pool) { return pool[0]; });
    }
    return workerPromise;
  }

  async function setWorkerMode(worker, psm) {
    psm = String(psm);
    if (worker._psm === psm) return;
    await worker.setParameters(tessParams(psm));
    worker._psm = psm;
  }

  function scaleToMinHeight(source, minH) {
    var h = Math.max(1, source.height);
    var scale = minH / h;
    if (scale < 1) scale = 1;
    if (scale > 3) scale = 3;
    var nw = Math.round(source.width * scale);
    var nh = Math.round(source.height * scale);
    if (nw > 900) {
      scale = 900 / source.width;
      nw = 900;
      nh = Math.max(1, Math.round(source.height * scale));
    }
    if (nw === source.width && nh === source.height) return source;
    var c = document.createElement('canvas');
    c.width = Math.max(1, nw);
    c.height = Math.max(1, nh);
    var ctx = c.getContext('2d');
    ctx.imageSmoothingEnabled = false;
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, c.width, c.height);
    ctx.drawImage(source, 0, 0, c.width, c.height);
    return c;
  }

  function cropToHandwriting(source) {
    var ctx = source.getContext('2d');
    var w = source.width;
    var h = source.height;
    var d = ctx.getImageData(0, 0, w, h).data;
    var mx = Math.max(2, Math.floor(w * 0.07));
    var my = Math.max(2, Math.floor(h * 0.1));
    var minX = w;
    var minY = h;
    var maxX = -1;
    var maxY = -1;
    var y;
    var x;
    for (y = my; y < h - my; y++) {
      for (x = mx; x < w - mx; x++) {
        var i = (y * w + x) * 4;
        if (inkGray(d[i], d[i + 1], d[i + 2]) < 155) {
          if (x < minX) minX = x;
          if (y < minY) minY = y;
          if (x > maxX) maxX = x;
          if (y > maxY) maxY = y;
        }
      }
    }
    if (maxX < minX) return source;
    var padX = Math.max(8, Math.round((maxX - minX + 1) * 0.22));
    var padY = Math.max(8, Math.round((maxY - minY + 1) * 0.28));
    return cropCanvas(source, {
      x: Math.max(0, minX - padX),
      y: Math.max(0, minY - padY),
      w: Math.min(w - 1, maxX + padX) - Math.max(0, minX - padX) + 1,
      h: Math.min(h - 1, maxY + padY) - Math.max(0, minY - padY) + 1
    });
  }

  function canvasGray(source) {
    var ctx = source.getContext('2d');
    var w = source.width;
    var h = source.height;
    var d = ctx.getImageData(0, 0, w, h).data;
    var gray = new Uint8Array(w * h);
    var i;
    for (i = 0; i < gray.length; i++) {
      gray[i] = inkGray(d[i * 4], d[i * 4 + 1], d[i * 4 + 2]);
    }
    return { gray: gray, w: w, h: h };
  }

  function cropInsidePrintedBox(img) {
    var g = canvasGray(img);
    var w = g.w;
    var h = g.h;
    var gray = g.gray;
    var vxs = [];
    var x;
    var y;
    for (x = 0; x < w; x++) {
      var best = 0;
      var run = 0;
      for (y = 0; y < h; y++) {
        if (gray[y * w + x] < 118) {
          run++;
          if (run > best) best = run;
        } else run = 0;
      }
      if (best > h * 0.36) vxs.push(x);
    }
    vxs.sort(function (a, b) { return a - b; });
    var groups = [];
    vxs.forEach(function (vx) {
      if (!groups.length || vx - groups[groups.length - 1].max > 7) {
        groups.push({ min: vx, max: vx });
      } else {
        groups[groups.length - 1].max = vx;
      }
    });
    var left = -1;
    groups.forEach(function (gr) {
      var mid = Math.round((gr.min + gr.max) / 2);
      if (mid < w * 0.42) left = mid;
    });
    var right = -1;
    if (left >= 0) {
      groups.forEach(function (gr) {
        var mid = Math.round((gr.min + gr.max) / 2);
        var span = mid - left;
        if (right < 0 && span >= Math.max(80, w * 0.28) && span <= w * 0.84) right = mid;
      });
    }
    var x0 = left >= 0 ? left + 1 : 0;
    var x1 = right >= 0 ? right - 2 : w - 1;
    if (x1 - x0 < w * 0.28) return img;
    return cropCanvas(img, { x: x0, y: 0, w: x1 - x0 + 1, h: h });
  }

  function cropToDigitStrip(img) {
    var g = canvasGray(img);
    var w = g.w;
    var h = g.h;
    var gray = g.gray;
    var frame = new Uint8Array(w);
    var x;
    var y;
    for (x = 0; x < w; x++) {
      var best = 0;
      var run = 0;
      for (y = 0; y < h; y++) {
        if (gray[y * w + x] < 140) {
          run++;
          if (run > best) best = run;
        } else run = 0;
      }
      if (best >= h * 0.55) frame[x] = 1;
    }
    var minX = w;
    var minY = h;
    var maxX = -1;
    var maxY = -1;
    var y0 = Math.round(h * 0.06);
    var y1 = Math.round(h * 0.92);
    for (y = y0; y < y1; y++) {
      for (x = 0; x < w; x++) {
        if (frame[x]) continue;
        if (gray[y * w + x] < 140) {
          if (x < minX) minX = x;
          if (y < minY) minY = y;
          if (x > maxX) maxX = x;
          if (y > maxY) maxY = y;
        }
      }
    }
    if (maxX < minX) return img;
    var bw = maxX - minX + 1;
    var bh = maxY - minY + 1;
    if (bw < w * 0.22 || bh < h * 0.20) return img;
    return cropCanvas(img, {
      x: Math.max(0, minX - 4),
      y: Math.max(0, minY - 3),
      w: Math.min(w, maxX + 4) - Math.max(0, minX - 4) + 1,
      h: Math.min(h, maxY + 3) - Math.max(0, minY - 3) + 1
    });
  }

  function maskToCanvas(mask, w, h) {
    var c = document.createElement('canvas');
    c.width = w;
    c.height = h;
    var ctx = c.getContext('2d');
    var img = ctx.createImageData(w, h);
    var i;
    for (i = 0; i < mask.length; i++) {
      var v = mask[i] ? 0 : 255;
      img.data[i * 4] = v;
      img.data[i * 4 + 1] = v;
      img.data[i * 4 + 2] = v;
      img.data[i * 4 + 3] = 255;
    }
    ctx.putImageData(img, 0, 0);
    c._ink = mask;
    return c;
  }

  function morphCloseCanvas(binCanvas) {
    var ink = inkMask(binCanvas);
    var w = ink.w;
    var h = ink.h;
    var src = ink.mask;
    var dil = new Uint8Array(w * h);
    var x;
    var y;
    var dx;
    var dy;
    for (y = 0; y < h; y++) {
      for (x = 0; x < w; x++) {
        var on = 0;
        for (dy = -1; dy <= 1 && !on; dy++) {
          for (dx = -1; dx <= 1; dx++) {
            var nx = x + dx;
            var ny = y + dy;
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
            if (src[ny * w + nx]) {
              on = 1;
              break;
            }
          }
        }
        dil[y * w + x] = on;
      }
    }
    var out = new Uint8Array(w * h);
    for (y = 1; y < h - 1; y++) {
      for (x = 1; x < w - 1; x++) {
        if (dil[y * w + x] && dil[y * w + x - 1] && dil[y * w + x + 1] && dil[(y - 1) * w + x] && dil[(y + 1) * w + x]) {
          out[y * w + x] = 1;
        }
      }
    }
    return maskToCanvas(out, w, h);
  }

  function enhanceInk(source) {
    var c = scaleToMinHeight(source, 96);
    var ctx = c.getContext('2d');
    var img = ctx.getImageData(0, 0, c.width, c.height);
    var d = img.data;
    var i;
    for (i = 0; i < d.length; i += 4) {
      var g = inkGray(d[i], d[i + 1], d[i + 2]);
      g = Math.max(0, Math.min(255, (g - 24) * 1.85));
      d[i] = d[i + 1] = d[i + 2] = g;
    }
    ctx.putImageData(img, 0, 0);
    return c;
  }

  function adaptiveBin(source, bias) {
    var c = enhanceInk(source);
    var ctx = c.getContext('2d');
    var img = ctx.getImageData(0, 0, c.width, c.height);
    var d = img.data;
    var w = c.width;
    var h = c.height;
    var n = w * h;
    var gray = new Uint8Array(n);
    var i;
    for (i = 0; i < n; i++) gray[i] = d[i * 4];

    var integral = new Float64Array((w + 1) * (h + 1));
    var y;
    var x;
    for (y = 0; y < h; y++) {
      var row = 0;
      for (x = 0; x < w; x++) {
        row += gray[y * w + x];
        integral[(y + 1) * (w + 1) + (x + 1)] = integral[y * (w + 1) + (x + 1)] + row;
      }
    }
    var win = Math.max(16, Math.round(Math.min(w, h) / 3.2));
    var half = Math.floor(win / 2);
    var bin = new Uint8Array(n);
    for (y = 0; y < h; y++) {
      for (x = 0; x < w; x++) {
        var x0 = Math.max(0, x - half);
        var y0 = Math.max(0, y - half);
        var x1 = Math.min(w - 1, x + half);
        var y1 = Math.min(h - 1, y + half);
        var count = (x1 - x0 + 1) * (y1 - y0 + 1);
        var sum = integral[(y1 + 1) * (w + 1) + (x1 + 1)]
          - integral[y0 * (w + 1) + (x1 + 1)]
          - integral[(y1 + 1) * (w + 1) + x0]
          + integral[y0 * (w + 1) + x0];
        var mean = sum / count;
        bin[y * w + x] = gray[y * w + x] < (mean - (bias || 10)) ? 1 : 0;
      }
    }
    // Dilate 1px — stylo fin
    var out = new Uint8Array(n);
    for (y = 0; y < h; y++) {
      for (x = 0; x < w; x++) {
        var ink = bin[y * w + x] === 1;
        if (!ink) {
          var dy;
          var dx;
          for (dy = -1; dy <= 1 && !ink; dy++) {
            for (dx = -1; dx <= 1; dx++) {
              var nx = x + dx;
              var ny = y + dy;
              if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
              if (bin[ny * w + nx]) {
                ink = true;
                break;
              }
            }
          }
        }
        var idx = (y * w + x) * 4;
        var v = ink ? 0 : 255;
        d[idx] = d[idx + 1] = d[idx + 2] = v;
        d[idx + 3] = 255;
        out[y * w + x] = ink ? 1 : 0;
      }
    }
    ctx.putImageData(img, 0, 0);
    c._ink = out;
    return c;
  }

  function inkMask(canvas) {
    if (canvas._ink) return { mask: canvas._ink, w: canvas.width, h: canvas.height };
    var ctx = canvas.getContext('2d');
    var w = canvas.width;
    var h = canvas.height;
    var d = ctx.getImageData(0, 0, w, h).data;
    var mask = new Uint8Array(w * h);
    var i;
    for (i = 0; i < w * h; i++) mask[i] = d[i * 4] < 128 ? 1 : 0;
    return { mask: mask, w: w, h: h };
  }

  function extractBlobs(canvas) {
    var info = inkMask(canvas);
    var mask = info.mask;
    var w = info.w;
    var h = info.h;
    var seen = new Uint8Array(w * h);
    var blobs = [];
    var y;
    var x;
    for (y = 0; y < h; y++) {
      for (x = 0; x < w; x++) {
        var start = y * w + x;
        if (!mask[start] || seen[start]) continue;
        var stack = [start];
        seen[start] = 1;
        var minx = x;
        var maxx = x;
        var miny = y;
        var maxy = y;
        var count = 0;
        while (stack.length) {
          var p = stack.pop();
          var px = p % w;
          var py = (p / w) | 0;
          count++;
          if (px < minx) minx = px;
          if (px > maxx) maxx = px;
          if (py < miny) miny = py;
          if (py > maxy) maxy = py;
          var k;
          var dx8 = [1, -1, 0, 0, 1, 1, -1, -1];
          var dy8 = [0, 0, 1, -1, 1, -1, 1, -1];
          for (k = 0; k < 8; k++) {
            var nx = px + dx8[k];
            var ny = py + dy8[k];
            if (nx < 0 || ny < 0 || nx >= w || ny >= h) continue;
            var ni = ny * w + nx;
            if (mask[ni] && !seen[ni]) {
              seen[ni] = 1;
              stack.push(ni);
            }
          }
        }
        blobs.push({ x: minx, y: miny, w: maxx - minx + 1, h: maxy - miny + 1, count: count });
      }
    }
    return blobs;
  }

  function filterDigitBlobs(blobs, canvasW, canvasH) {
    var digits = [];
    var dots = [];
    blobs.forEach(function (b) {
      if (b.count < 12) return;
      var touchesV = (b.x <= 1 || b.x + b.w >= canvasW - 2) && b.h > canvasH * 0.7;
      var touchesH = (b.y <= 1 || b.y + b.h >= canvasH - 2) && b.w > canvasW * 0.55;
      if (touchesV || touchesH) return;
      var ar = b.w / Math.max(1, b.h);
      if (b.h >= canvasH * 0.32 && ar >= 0.12 && ar <= 1.35) {
        digits.push(b);
        return;
      }
      if (b.h < canvasH * 0.28 && b.w < canvasW * 0.18 && ar > 0.4 && ar < 2.2) {
        dots.push(b);
      }
    });
    digits.sort(function (a, b) { return a.x - b.x; });
    dots.sort(function (a, b) { return a.x - b.x; });
    return { digits: digits, dots: dots };
  }

  function cropBlob(source, blob, pad) {
    pad = pad || 6;
    return cropCanvas(source, {
      x: Math.max(0, blob.x - pad),
      y: Math.max(0, blob.y - pad),
      w: blob.w + pad * 2,
      h: blob.h + pad * 2
    });
  }

  function rebuildCleanLine(binCanvas, digits, dots) {
    var parts = digits.map(function (b) { return { type: 'd', b: b }; })
      .concat(dots.map(function (b) { return { type: '.', b: b }; }));
    parts.sort(function (a, b) { return a.b.x - b.b.x; });
    if (!parts.length) return binCanvas;
    var targetH = 72;
    var gap = 10;
    var totalW = 16;
    parts.forEach(function (p) {
      var scale = targetH / Math.max(8, p.b.h);
      if (p.type === '.') scale = Math.min(scale, 0.45);
      totalW += Math.max(8, Math.round(p.b.w * scale)) + gap;
    });
    var c = document.createElement('canvas');
    c.width = totalW;
    c.height = targetH + 24;
    var ctx = c.getContext('2d');
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, c.width, c.height);
    var x = 8;
    parts.forEach(function (p) {
      var piece = cropBlob(binCanvas, p.b, 2);
      var scale = (p.type === '.') ? 0.35 : (targetH / Math.max(8, piece.height));
      var dw = Math.max(6, Math.round(piece.width * scale));
      var dh = Math.max(6, Math.round(piece.height * scale));
      var dy = p.type === '.' ? c.height - dh - 10 : Math.round((c.height - dh) / 2);
      ctx.imageSmoothingEnabled = false;
      ctx.drawImage(piece, x, dy, dw, dh);
      x += dw + gap;
    });
    return c;
  }

  function collectFromOcr(result) {
    if (!result || !result.data) return { raw: '', conf: 0, values: [] };
    var text = String(result.data.text || '').replace(/\s+/g, ' ').trim();
    var conf = result.data.confidence || 0;
    var words = result.data.words || [];
    var lines = result.data.lines || [];
    var raws = [text];
    words.forEach(function (w) { raws.push(w.text); });
    lines.forEach(function (ln) { raws.push(ln.text); });
    if (words.length >= 2) {
      raws.push(words.map(function (w) { return w.text; }).join(''));
      raws.push(words.map(function (w) { return w.text; }).join('.'));
    }
    var values = [];
    var seen = {};
    raws.forEach(function (r) {
      interpretationsFromRaw(r).forEach(function (v) {
        if (seen[v]) return;
        seen[v] = true;
        values.push(v);
      });
    });
    return { raw: text, conf: conf, values: values };
  }

  function timeDigitKey(formatted) {
    var sec = secondsOf(formatted);
    if (sec == null) return '';
    return String(Math.round(sec * 1000));
  }

  function pickBest(cands) {
    var scored = [];
    cands.forEach(function (c) {
      if (!c || !c.value) return;
      c._s = scoreTime(c.value, c.confidence, c.raw);
      scored.push(c);
    });
    scored.sort(function (a, b) { return b._s - a._s; });
    var i;
    for (i = 1; i < scored.length; i++) {
      if (scored[0]._s - scored[i]._s > 1.8) break;
      var top = timeDigitKey(scored[0].value);
      var other = timeDigitKey(scored[i].value);
      if (top.length !== 5 || other.length !== 5) continue;
      if (top.slice(0, 2) !== other.slice(0, 2)) continue;
      var diff = 0;
      var prefer = false;
      var k;
      for (k = 2; k < 5; k++) {
        if (top.charAt(k) === other.charAt(k)) continue;
        diff++;
        prefer = top.charAt(k) === '9' && other.charAt(k) === '8';
      }
      if (diff === 1 && prefer) {
        var pick = scored.splice(i, 1)[0];
        scored.unshift(pick);
        break;
      }
    }
    return scored[0] || null;
  }

  function isGoodEnough(best) {
    if (!best || !best.value) return false;
    var s = scoreTime(best.value, best.confidence, best.raw);
    var sec = secondsOf(best.value);
    var digits = String(best.raw || best.value).replace(/\D/g, '').length;
    if (sec != null && sec >= 20 && sec <= 120 && digits >= 5 && s >= 5) return true;
    if (s >= 8) return true;
    return false;
  }

  async function ocrCellTime(worker, sourceCanvas, box) {
    var inner = insetBox(box, 0.02);
    var crop0 = cropCanvas(sourceCanvas, inner);
    var crop = cropInsidePrintedBox(crop0);
    var thumb = crop.toDataURL('image/jpeg', 0.62);
    var bin = adaptiveBin(crop, 8);
    var lineImg = scaleToMinHeight(bin, 88);

    var cands = [];
    var rawBits = [];

    async function run(canvas, psm) {
      await setWorkerMode(worker, psm);
      var result = await worker.recognize(canvas);
      var got = collectFromOcr(result);
      if (got.raw) rawBits.push(got.raw);
      got.values.forEach(function (v) {
        cands.push({ value: v, confidence: got.conf, uncertain: got.conf < 68, raw: got.raw });
      });
      return pickBest(cands);
    }

    function finish(best) {
      var raw = rawBits.filter(Boolean).join(' | ');
      if (best) {
        best.thumb = thumb;
        best.raw = raw;
        var sec = secondsOf(best.value);
        if (sec == null || sec < 8 || sec > 180) best.uncertain = true;
        return best;
      }
      return { value: null, confidence: 0, uncertain: true, thumb: thumb, raw: raw };
    }

    try {
      var best = await run(lineImg, '7');
      if (isGoodEnough(best)) return finish(best);

      var tight = cropToDigitStrip(crop);
      best = await run(scaleToMinHeight(adaptiveBin(tight, 8), 88), '7');
      if (isGoodEnough(best)) return finish(best);

      best = await run(lineImg, '13');
      if (isGoodEnough(best) || (best && scoreTime(best.value, best.confidence, best.raw) >= 3.5)) {
        return finish(best);
      }

      var blobs = filterDigitBlobs(extractBlobs(bin), bin.width, bin.height);
      if (blobs.digits.length >= 4) {
        var clean = rebuildCleanLine(bin, blobs.digits, blobs.dots);
        best = await run(clean, '7');
      }
    } catch (e) { /* continue */ }

    return finish(pickBest(cands));
  }

  async function detectAllCells(worker, sourceCanvas, sheet, boxMap) {
    var detections = {};
    var total = 0;
    var done = 0;
    sheet.stages.forEach(function (stage) { total += stage.pilots.length; });

    for (var si = 0; si < sheet.stages.length; si++) {
      var stage = sheet.stages[si];
      for (var pi = 0; pi < stage.pilots.length; pi++) {
        var p = stage.pilots[pi];
        var key = p.id + '_' + stage.esNumber;
        done++;
        if (done % 2 === 0 || done === total) {
          setStatus('OCR des cases… ' + done + '/' + total);
        }
        var box = boxMap[key];
        if (!box) {
          detections[key] = { value: null, thumb: null };
          continue;
        }
        try {
          detections[key] = await ocrCellTime(worker, sourceCanvas, box);
        } catch (e) {
          detections[key] = { value: null, thumb: null };
        }
      }
    }
    return detections;
  }

  function renderReview(sheet, detections) {
    reviewGrid.innerHTML = '';
    badges.innerHTML = '';
    var filled = 0;
    var uncertain = 0;
    var empty = 0;

    sheet.stages.forEach(function (stage, si) {
      var article = document.createElement('article');
      var isCurrent = session.phase === 'es' && si === session.esIndex;
      var justDone = esHasReading(stage) && (session.phase === 'done' || si === session.esIndex - 1);
      article.className = 'scan-stage' + (isCurrent ? ' is-current' : '') + (justDone ? ' is-fresh' : '');
      var header = document.createElement('header');
      header.className = 'scan-stage-header';
      var title = document.createElement('span');
      title.textContent = 'ES ' + stage.esNumber;
      header.appendChild(title);
      var esShot = shotByKey('es-' + stage.esNumber);
      if (esShot) {
        var mini = document.createElement('img');
        mini.className = 'scan-stage-shot';
        mini.alt = 'Photo ES ' + stage.esNumber;
        mini.src = esShot.thumb;
        header.appendChild(mini);
        header.title = 'Afficher cette photo';
        header.addEventListener('click', function () { showArchivedShot('es-' + stage.esNumber); });
      }
      article.appendChild(header);

      var table = document.createElement('table');
      var tbody = document.createElement('tbody');
      stage.pilots.forEach(function (p) {
        var key = p.id + '_' + stage.esNumber;
        var det = detections[key] || {};
        var tr = document.createElement('tr');
        var tdPilot = document.createElement('td');
        tdPilot.innerHTML = '<span class="pilot-name"></span><span class="pilot-meta"></span>';
        tdPilot.querySelector('.pilot-name').textContent = p.name;
        tdPilot.querySelector('.pilot-meta').textContent = (p.car || '') + ' · ' + (p.anchor || '');

        var tdTime = document.createElement('td');
        var wrap = document.createElement('div');
        wrap.className = 'scan-time-wrap';

        if (det.thumb) {
          var img = document.createElement('img');
          img.className = 'scan-thumb';
          img.alt = 'Case scannée';
          img.src = det.thumb;
          wrap.appendChild(img);
        }

        var input = document.createElement('input');
        input.type = 'text';
        input.inputMode = 'decimal';
        input.autocomplete = 'off';
        input.name = 'time_' + p.id + '_' + stage.esNumber;
        input.placeholder = '—';

        var existing = (p.time || '').trim();
        var conf = document.createElement('span');
        conf.className = 'scan-conf';
        input.setAttribute('data-initial', existing);

        if (det.value) {
          input.value = det.value;
          if (det.uncertain) {
            input.classList.add('is-uncertain');
            conf.textContent = 'À vérifier' + (det.raw ? ' · lu « ' + det.raw + ' »' : '');
            uncertain++;
          } else {
            input.classList.add('is-filled');
            conf.textContent = 'OK' + (det.raw ? ' · lu « ' + det.raw + ' »' : '');
            filled++;
          }
          if (window.RallyeTimesSave) {
            window.RallyeTimesSave.initInput(input);
            if (existing !== det.value.trim()) input.classList.add('is-dirty');
          }
        } else if (existing) {
          input.value = existing;
          conf.textContent = det.thumb ? 'Déjà en base — recopie depuis la miniature' : 'Déjà en base';
          filled++;
          if (window.RallyeTimesSave) window.RallyeTimesSave.initInput(input);
        } else {
          conf.textContent = det.raw
            ? 'Non lu (« ' + det.raw + ' ») — recopie la miniature'
            : (det.thumb ? 'Recopie le temps de la miniature' : 'Vide (case non trouvée)');
          empty++;
          if (window.RallyeTimesSave) window.RallyeTimesSave.initInput(input);
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
      ' — clique un ES pour revoir sa photo. Corrige si besoin.';
    reviewSection.hidden = false;
  }

  async function finishSession() {
    session.phase = 'done';
    updateStepUi();
    var expected = 0;
    session.sheet.stages.forEach(function (stage) { expected += stage.pilots.length; });
    var filledCount = Object.keys(session.detections).filter(function (k) {
      return session.detections[k] && session.detections[k].value;
    }).length;
    setStatus(
      'Terminé : ' + filledCount + ' temps lus / ' + expected + '. Vérifie les miniatures puis enregistre.',
      filledCount > 0 ? 'is-ok' : 'is-error'
    );
    renderReview(session.sheet, session.detections);
  }

  async function readQrPhoto() {
    setStatus('Lecture du QR…');
    var qrInfo = decodeQrDetailed(preview);
    if (!qrInfo) {
      throw new Error('QR introuvable. Recadre pour que le QR soit net et entier.');
    }
    var qr = parseQrPayload(qrInfo.data);
    boucleInput.value = String(qr.boucle);
    var upright = deskewFromQr(preview, qrInfo.location);
    copyToPreview(upright);
    qr = parseQrPayload(qrInfo.data);

    var res = await fetch('/rallye/' + rallyeId + '/api/group-sheet?boucle=' + qr.boucle + '&group=' + qr.group);
    if (!res.ok) throw new Error('Impossible de charger la feuille groupe.');
    var sheet = await res.json();
    session.qr = qr;
    session.sheet = sheet;
    session.esIndex = 0;
    session.detections = {};
    session.phase = sheet.stages && sheet.stages.length ? 'es' : 'done';
    saveCurrentShot('qr', 'QR · G' + sheet.groupNumber);
    setStatus('QR OK — Groupe ' + sheet.groupNumber + ' · Boucle ' + sheet.boucle + '.', 'is-ok');
    renderReview(sheet, session.detections);
    if (session.phase === 'done') {
      await finishSession();
      return;
    }
    await promptNextPhoto();
  }

  async function readEsPhoto() {
    var stage = currentStage();
    if (!stage) {
      await finishSession();
      return;
    }
    if (typeof Tesseract === 'undefined') {
      throw new Error('OCR indisponible (Tesseract non chargé). Vérifie la connexion internet.');
    }
    var nP = stage.pilots.length;
    setStatus('ES ' + stage.esNumber + ' — recherche des cases…');
    var packed = boxesForEsPhoto(preview, nP);
    copyToPreview(packed.canvas);
    drawBoxList(preview, packed.boxes);

    var pool = await getWorkerPool();
    var i;
    var filled = 0;
    async function readOne(idx, worker) {
      var p = stage.pilots[idx];
      var key = p.id + '_' + stage.esNumber;
      try {
        session.detections[key] = packed.boxes[idx]
          ? await ocrCellTime(worker, packed.canvas, packed.boxes[idx])
          : { value: null, thumb: null };
      } catch (e) {
        session.detections[key] = { value: null, thumb: null };
      }
      if (session.detections[key] && session.detections[key].value) filled++;
      setStatus('ES ' + stage.esNumber + ' — OCR ' + Math.min(idx + 1, nP) + '/' + nP + '…');
    }
    for (i = 0; i < nP; i += 2) {
      var jobs = [readOne(i, pool[0])];
      if (i + 1 < nP) jobs.push(readOne(i + 1, pool[1]));
      await Promise.all(jobs);
    }
    renderReview(session.sheet, session.detections);
    saveCurrentShot('es-' + stage.esNumber, 'ES ' + stage.esNumber);
    setStatus('ES ' + stage.esNumber + ' : ' + filled + '/' + nP + ' temps lus.', filled > 0 ? 'is-ok' : 'is-error');
    session.esIndex++;
    if (session.esIndex >= session.sheet.stages.length) {
      await finishSession();
      return;
    }
    session.phase = 'es';
    await promptNextPhoto();
  }

  btnAnalyze.addEventListener('click', async function () {
    if (preview.hidden || !preview.width) {
      setStatus('Prends ou charge une photo d’abord.', 'is-error');
      return;
    }
    btnAnalyze.disabled = true;
    try {
      if (session.phase === 'qr') {
        await readQrPhoto();
      } else if (session.phase === 'es') {
        await readEsPhoto();
      } else {
        await finishSession();
      }
    } catch (err) {
      console.error(err);
      setStatus(err.message || 'Analyse échouée.', 'is-error');
    } finally {
      var canRead = session.phase !== 'done' &&
        !session.awaitingCapture &&
        !session.viewingArchive &&
        preview.width &&
        !preview.hidden;
      btnAnalyze.disabled = !canRead;
      updateViewerTag();
    }
  });

  if (btnSkipEs) {
    btnSkipEs.addEventListener('click', async function () {
      if (session.phase !== 'es' || !session.sheet) return;
      var stage = currentStage();
      if (stage) {
        stage.pilots.forEach(function (p) {
          var key = p.id + '_' + stage.esNumber;
          if (!session.detections[key]) {
            session.detections[key] = { value: null, thumb: null };
          }
        });
        renderReview(session.sheet, session.detections);
      }
      session.esIndex++;
      if (session.esIndex >= session.sheet.stages.length) {
        await finishSession();
        return;
      }
      await promptNextPhoto();
    });
  }

  if (btnRestart) {
    btnRestart.addEventListener('click', function () {
      session = newSession();
      if (reviewSection) reviewSection.hidden = true;
      if (reviewGrid) reviewGrid.innerHTML = '';
      if (filmstripEl) {
        filmstripEl.innerHTML = '';
        filmstripEl.hidden = true;
      }
      clearPreview();
      updateStepUi();
      setStatus('Prends une photo du QR.');
    });
  }

  updateStepUi();
  if (typeof Tesseract !== 'undefined') {
    getWorkerPool().catch(function () { workerPoolPromise = null; });
  }
})();
