/**
 * Scan feuille groupe : QR (identité + orientation) + cases imprimées + OCR manuscrit.
 * 1) Lit le QR → groupe / boucle, redresse la photo
 * 2) Trouve les cadres des cases temps (bordures imprimées)
 * 3) OCR isolé de l’intérieur de chaque case
 * 4) Review avec miniature — doute → case orange
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

  function copyToPreview(source) {
    var w = source.videoWidth || source.naturalWidth || source.width;
    var h = source.videoHeight || source.naturalHeight || source.height;
    if (!w || !h) return false;
    var maxSide = 2400;
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

  btnCamera.addEventListener('click', async function () {
    try {
      await stopCamera();
      stream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: { ideal: 'environment' }, width: { ideal: 1920 } },
        audio: false
      });
      video.srcObject = stream;
      video.hidden = false;
      preview.hidden = true;
      placeholder.hidden = true;
      btnSnap.hidden = false;
      btnAnalyze.disabled = true;
      await video.play();
      setStatus('Caméra prête — cadre toute la feuille, QR bien net.');
    } catch (err) {
      setStatus('Caméra indisponible : utilise « Choisir une photo ».', 'is-error');
    }
  });

  btnSnap.addEventListener('click', function () {
    copyToPreview(video);
    stopCamera();
    setStatus('Photo prise — lance l’analyse.');
  });

  fileInput.addEventListener('change', async function () {
    var file = fileInput.files && fileInput.files[0];
    if (!file) return;
    stopCamera();
    try {
      if (typeof createImageBitmap === 'function') {
        var bmp = await createImageBitmap(file, { imageOrientation: 'from-image' });
        copyToPreview(bmp);
        if (bmp.close) bmp.close();
      } else {
        var url = URL.createObjectURL(file);
        var img = new Image();
        await new Promise(function (resolve, reject) {
          img.onload = resolve;
          img.onerror = reject;
          img.src = url;
        });
        copyToPreview(img);
        URL.revokeObjectURL(url);
      }
      setStatus('Photo chargée — lance l’analyse.');
    } catch (e) {
      setStatus('Impossible de lire cette image.', 'is-error');
    }
  });

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

  function deskewFromQr(source, location) {
    if (!location || !location.topLeftCorner || !location.topRightCorner) {
      return cloneCanvas(source);
    }
    var tl = location.topLeftCorner;
    var tr = location.topRightCorner;
    var angle = Math.atan2(tr.y - tl.y, tr.x - tl.x);
    if (!isFinite(angle) || Math.abs(angle) < (2 * Math.PI) / 180) {
      return cloneCanvas(source);
    }
    return rotateCanvas(source, -angle);
  }

  function decodeQrDetailed(canvas) {
    if (typeof jsQR === 'undefined') return null;
    var ctx = canvas.getContext('2d');
    var imageData = ctx.getImageData(0, 0, canvas.width, canvas.height);
    var code = jsQR(imageData.data, imageData.width, imageData.height, {
      inversionAttempts: 'attemptBoth'
    });
    if (!code || !code.data) return null;
    return { data: code.data.trim(), location: code.location || null };
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
  function findTimeBoxes(canvas, darkLimit) {
    var darkCut = darkLimit == null ? 100 : darkLimit;
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
      return gray[y * w + x] > 168;
    }

    var step = Math.max(3, Math.round(Math.min(w, h) / 280));
    var raw = [];
    var y;
    var x;
    for (y = 12; y < h - 12; y += step) {
      for (x = 12; x < w - 12; x += step) {
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
        if (bw < 48 || bh < 18) continue;
        if (bw > w * 0.42 || bh > h * 0.16) continue;
        var ar = bw / bh;
        if (ar < 1.45 || ar > 7.5) continue;
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

    if (unique.length < 2) return unique;

    var areas = unique.map(function (b) { return b.w * b.h; }).sort(function (a, b) { return a - b; });
    var median = areas[Math.floor(areas.length / 2)];
    return unique.filter(function (b) {
      var a = b.w * b.h;
      return a > median * 0.4 && a < median * 2.4;
    });
  }

  function splitByGaps(values, groups) {
    if (groups <= 1 || values.length === 0) {
      return [values.slice()];
    }
    var indexed = values.map(function (v, i) { return { v: v, i: i }; });
    indexed.sort(function (a, b) { return a.v - b.v; });
    var gaps = [];
    var g;
    for (g = 1; g < indexed.length; g++) {
      gaps.push({ at: g, size: indexed[g].v - indexed[g - 1].v });
    }
    gaps.sort(function (a, b) { return b.size - a.size; });
    var cuts = gaps.slice(0, groups - 1).map(function (x) { return x.at; }).sort(function (a, b) { return a - b; });
    var out = [];
    var start = 0;
    cuts.forEach(function (cut) {
      out.push(indexed.slice(start, cut).map(function (x) { return x.i; }));
      start = cut;
    });
    out.push(indexed.slice(start).map(function (x) { return x.i; }));
    return out;
  }

  function assignBoxesToSheet(boxes, sheet) {
    var map = {};
    var stages = sheet.stages || [];
    if (!stages.length) return map;
    var nS = stages.length;
    var nP = (stages[0].pilots || []).length;
    var expected = nS * nP;
    if (!boxes.length || !nP) return map;

    var esRows = nS <= 3 ? 1 : (nP >= 5 ? 1 : 2);
    var esCols = Math.ceil(nS / esRows);

    function boxesOf(indices) {
      return indices.map(function (i) { return boxes[i]; });
    }

    var clusters = [];
    if (esRows === 1) {
      var colIdx = splitByGaps(boxes.map(function (b) { return b.x + b.w / 2; }), esCols);
      colIdx.sort(function (a, b) {
        var xa = a.length ? boxes[a[0]].x : 0;
        var xb = b.length ? boxes[b[0]].x : 0;
        var ma = a.reduce(function (s, i) { return s + boxes[i].x; }, 0) / Math.max(1, a.length);
        var mb = b.reduce(function (s, i) { return s + boxes[i].x; }, 0) / Math.max(1, b.length);
        return ma - mb || xa - xb;
      });
      clusters = colIdx.map(function (idx) {
        return boxesOf(idx).sort(function (a, b) { return a.y - b.y; });
      });
    } else {
      var rowIdx = splitByGaps(boxes.map(function (b) { return b.y + b.h / 2; }), esRows);
      rowIdx.sort(function (a, b) {
        var ma = a.reduce(function (s, i) { return s + boxes[i].y; }, 0) / Math.max(1, a.length);
        var mb = b.reduce(function (s, i) { return s + boxes[i].y; }, 0) / Math.max(1, b.length);
        return ma - mb;
      });
      var colsFirst = esCols;
      var colsSecond = nS - esCols;
      rowIdx.forEach(function (row, ri) {
        var colsWanted = ri === 0 ? colsFirst : colsSecond;
        var colIdx2 = splitByGaps(row.map(function (i) { return boxes[i].x + boxes[i].w / 2; }), colsWanted);
        var rowBoxes = row;
        colIdx2.sort(function (a, b) {
          function mid(list) {
            if (!list.length) return 0;
            return list.reduce(function (s, local) {
              return s + boxes[rowBoxes[local]].x;
            }, 0) / list.length;
          }
          return mid(a) - mid(b);
        });
        colIdx2.forEach(function (localIdx) {
          var col = localIdx.map(function (li) { return boxes[rowBoxes[li]]; })
            .sort(function (a, b) { return a.y - b.y; });
          clusters.push(col);
        });
      });
    }

    stages.forEach(function (stage, si) {
      var col = clusters[si] || [];
      if (col.length > nP) {
        col = col.slice(col.length - nP);
      }
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

    var euro = value.match(/^(\d{1,2})\.([0-5]?\d)\.(\d{1,3})$/);
    if (euro) {
      value = euro[1] + ':' + euro[2] + '.' + euro[3];
    }

    if (/^\d{4,6}$/.test(value)) {
      var digits = value;
      var msLen = digits.length === 4 ? 3 : 3;
      if (digits.length === 4) {
        // 1234 → 1.234 (slot) plutôt que 12.340
        msLen = 3;
      }
      var msPart = digits.slice(-msLen);
      var secPart = digits.slice(0, -msLen);
      if (!secPart) return null;
      var asSec = parseInt(secPart, 10) + parseInt((msPart + '000').slice(0, 3), 10) / 1000;
      if (asSec > 0 && asSec <= MAX_TIME_SECONDS) {
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
    if (!isFinite(seconds) || seconds <= 0 || seconds > MAX_TIME_SECONDS) return null;
    if (seconds < 2 && value.indexOf('.') < 0) return null;
    return formatSeconds(seconds);
  }

  function getWorker() {
    if (!workerPromise) {
      workerPromise = Tesseract.createWorker('eng', 1, { logger: function () {} });
    }
    return workerPromise;
  }

  async function setWorkerMode(worker, psm) {
    await worker.setParameters({
      tessedit_pageseg_mode: String(psm),
      tessedit_char_whitelist: '0123456789:.,',
      preserve_interword_spaces: '0'
    });
  }

  function scaleToMinHeight(source, minH) {
    var scale = Math.max(3, minH / Math.max(1, source.height));
    var c = document.createElement('canvas');
    c.width = Math.max(1, Math.round(source.width * scale));
    c.height = Math.max(1, Math.round(source.height * scale));
    var ctx = c.getContext('2d');
    ctx.imageSmoothingEnabled = false;
    ctx.fillStyle = '#fff';
    ctx.fillRect(0, 0, c.width, c.height);
    ctx.drawImage(source, 0, 0, c.width, c.height);
    return c;
  }

  function enhanceInk(source) {
    var c = scaleToMinHeight(source, 88);
    var ctx = c.getContext('2d');
    var img = ctx.getImageData(0, 0, c.width, c.height);
    var d = img.data;
    var i;
    for (i = 0; i < d.length; i += 4) {
      var g = inkGray(d[i], d[i + 1], d[i + 2]);
      g = Math.max(0, Math.min(255, (g - 30) * 1.7));
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
    var win = Math.max(12, Math.round(Math.min(w, h) / 4));
    var half = Math.floor(win / 2);
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
        var v = gray[y * w + x] < (mean - (bias || 8)) ? 0 : 255;
        var idx = (y * w + x) * 4;
        d[idx] = d[idx + 1] = d[idx + 2] = v;
        d[idx + 3] = 255;
      }
    }
    ctx.putImageData(img, 0, 0);
    return c;
  }

  function extractTimeFromOcr(result) {
    if (!result || !result.data) return null;
    var text = result.data.text || '';
    var conf = result.data.confidence || 0;
    var words = result.data.words || [];
    var lines = result.data.lines || [];
    var candidates = [text];
    words.forEach(function (w) { candidates.push(w.text); });
    lines.forEach(function (ln) { candidates.push(ln.text); });
    if (words.length >= 2) {
      candidates.push(words.map(function (w) { return w.text; }).join('.'));
      candidates.push(words.map(function (w) { return w.text; }).join(''));
    }
    var best = null;
    var bestConf = -1;
    candidates.forEach(function (c) {
      var t = parseTimeCandidate(c);
      if (!t) return;
      if (conf > bestConf) {
        best = t;
        bestConf = conf;
      }
    });
    if (!best) return null;
    return { value: best, confidence: bestConf < 0 ? 50 : bestConf, uncertain: bestConf < 62 };
  }

  async function ocrCellTime(worker, sourceCanvas, box) {
    var inner = insetBox(box, 0.12);
    var crop = cropCanvas(sourceCanvas, inner);
    var thumb = crop.toDataURL('image/jpeg', 0.7);
    var variants = [
      { canvas: enhanceInk(crop), psm: '13' },
      { canvas: adaptiveBin(crop, 6), psm: '13' },
      { canvas: adaptiveBin(crop, 14), psm: '7' },
      { canvas: enhanceInk(crop), psm: '8' }
    ];
    var best = null;
    var v;
    for (v = 0; v < variants.length; v++) {
      try {
        await setWorkerMode(worker, variants[v].psm);
        var result = await worker.recognize(variants[v].canvas);
        var det = extractTimeFromOcr(result);
        if (!det) continue;
        if (!best || det.confidence > best.confidence) best = det;
        if (det.confidence >= 70) break;
      } catch (e) {
        /* variante suivante */
      }
    }
    if (best) {
      best.thumb = thumb;
      return best;
    }
    return { value: null, confidence: 0, uncertain: true, thumb: thumb };
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
            conf.textContent = 'À vérifier (' + Math.round(det.confidence) + '%)';
            uncertain++;
          } else {
            input.classList.add('is-filled');
            conf.textContent = 'OK · ' + Math.round(det.confidence) + '%';
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
          conf.textContent = det.thumb ? 'Recopie le temps de la miniature' : 'Vide (case non trouvée)';
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
      ' — la miniature montre ce qui a été lu. Corrige si besoin.';
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
      if (typeof Tesseract === 'undefined') {
        throw new Error('OCR indisponible (Tesseract non chargé). Vérifie la connexion internet.');
      }
      var qrInfo = decodeQrDetailed(preview);
      if (!qrInfo) {
        throw new Error('QR introuvable. Recadre pour que le QR soit net et entier.');
      }
      var qr = parseQrPayload(qrInfo.data);
      boucleInput.value = String(qr.boucle);
      setStatus('QR OK — Groupe ' + qr.group + ' · redressement de la feuille…');

      var upright = deskewFromQr(preview, qrInfo.location);
      copyToPreview(upright);
      var qr2 = decodeQrDetailed(preview) || qrInfo;
      qr = parseQrPayload(qr2.data);

      var res = await fetch('/rallye/' + rallyeId + '/api/group-sheet?boucle=' + qr.boucle + '&group=' + qr.group);
      if (!res.ok) throw new Error('Impossible de charger la feuille groupe.');
      var sheet = await res.json();

      setStatus('Recherche des cases temps…');
      var work = cloneCanvas(preview);
      var expected = qr.pilots.length * qr.stages.length;
      var boxes = findTimeBoxes(work);
      if (boxes.length < Math.max(2, Math.floor(expected * 0.5))) {
        var looser = findTimeBoxes(work, 130);
        if (looser.length > boxes.length) boxes = looser;
      }
      var boxMap = assignBoxesToSheet(boxes, sheet);
      drawBoxes(preview, boxMap);

      var assigned = Object.keys(boxMap).filter(function (k) {
        return k.charAt(0) !== '_' && boxMap[k];
      }).length;
      if (assigned === 0) {
        setStatus(
          'Aucune case temps détectée (' + boxes.length + ' cadre(s)). Photo trop de biais / floue — tu pourras quand même saisir à la main.',
          'is-error'
        );
      } else if (assigned < expected) {
        setStatus('Cases trouvées : ' + assigned + '/' + expected + ' — OCR…', 'is-error');
      } else {
        setStatus('Cases OK (' + assigned + ') — lecture des temps manuscrits…');
      }

      var worker = await getWorker();
      var detections = await detectAllCells(worker, work, sheet, boxMap);

      var filledCount = Object.keys(detections).filter(function (k) {
        return detections[k] && detections[k].value;
      }).length;
      setStatus(
        'Analyse terminée : ' + filledCount + ' temps lus / ' + expected +
          ' · ' + assigned + ' cases cadrées. Vérifie les miniatures puis enregistre.',
        filledCount > 0 ? 'is-ok' : 'is-error'
      );

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
