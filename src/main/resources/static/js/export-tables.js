/**
 * Export tableaux → Excel (.xlsx) et PDF.
 * Dépendances CDN optionnelles : XLSX, jspdf, jspdf-autotable.
 */
(function (global) {
    'use strict';

    function textOf(el) {
        return (el && el.textContent ? el.textContent : '').replace(/\s+/g, ' ').trim();
    }

    function tableToMatrix(table) {
        if (!table) return [];
        const rows = [];
        table.querySelectorAll('tr').forEach(tr => {
            const cells = [];
            tr.querySelectorAll('th, td').forEach(cell => {
                const colspan = parseInt(cell.getAttribute('colspan') || '1', 10);
                cells.push(textOf(cell));
                for (let i = 1; i < colspan; i++) cells.push('');
            });
            if (cells.some(c => c !== '')) rows.push(cells);
        });
        return rows;
    }

    function downloadBlob(blob, filename) {
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        a.remove();
        setTimeout(() => URL.revokeObjectURL(url), 1500);
    }

    function slugify(name) {
        return String(name || 'export')
            .normalize('NFD').replace(/[\u0300-\u036f]/g, '')
            .replace(/[^a-zA-Z0-9._-]+/g, '_')
            .replace(/^_+|_+$/g, '')
            .slice(0, 80) || 'export';
    }

    function getJsPDFCtor() {
        if (global.jspdf && typeof global.jspdf.jsPDF === 'function') return global.jspdf.jsPDF;
        if (typeof global.jsPDF === 'function') return global.jsPDF;
        return null;
    }

    /** sheets: [{ name: 'Qualifs', table: HTMLTableElement|string selector, rows?: string[][] }] */
    function exportToExcel(sheets, filename) {
        const list = (sheets || []).map(s => {
            const table = typeof s.table === 'string' ? document.querySelector(s.table) : s.table;
            const rows = s.rows || tableToMatrix(table);
            return { name: (s.name || 'Feuille').slice(0, 31), rows };
        }).filter(s => s.rows && s.rows.length);

        if (!list.length) {
            alert('Rien à exporter.');
            return;
        }

        const file = slugify(filename || 'export') + '.xlsx';

        if (typeof XLSX !== 'undefined') {
            const wb = XLSX.utils.book_new();
            list.forEach(s => {
                const ws = XLSX.utils.aoa_to_sheet(s.rows);
                XLSX.utils.book_append_sheet(wb, ws, s.name);
            });
            XLSX.writeFile(wb, file);
            return;
        }

        // Fallback CSV (Excel-compatible, UTF-8 BOM)
        const parts = list.map(s => {
            const csv = s.rows.map(r => r.map(cell => {
                const v = String(cell ?? '');
                if (/[",;\n]/.test(v)) return '"' + v.replace(/"/g, '""') + '"';
                return v;
            }).join(';')).join('\n');
            return '### ' + s.name + '\n' + csv;
        }).join('\n\n');
        const blob = new Blob(['\uFEFF' + parts], { type: 'text/csv;charset=utf-8' });
        downloadBlob(blob, slugify(filename || 'export') + '.csv');
    }

    function metaLinesFrom(opts) {
        if (Array.isArray(opts.meta) && opts.meta.length) {
            return opts.meta
                .filter(function (item) { return item && (item.line || item.label); })
                .map(function (item) {
                    if (item.line) return item.line;
                    return item.label + ' : ' + (item.value || '—');
                });
        }
        return opts.subtitle ? [opts.subtitle] : [];
    }

    function metaItemsFrom(opts) {
        if (Array.isArray(opts.meta) && opts.meta.length) {
            return opts.meta
                .filter(function (item) { return item && (item.line || item.label || item.value); })
                .map(function (item) {
                    if (item.line) return { label: '', value: item.line };
                    return { label: item.label || '', value: item.value || '—' };
                });
        }
        if (opts.subtitle) return [{ label: '', value: opts.subtitle }];
        return [];
    }

    function drawPdfDoc(JsPDF, prepared, opts, fontSize) {
        const title = opts.title || 'Export';
        const singlePage = !!opts.singlePage;
        const margin = singlePage ? 24 : 40;
        const doc = new JsPDF({ orientation: 'landscape', unit: 'pt', format: 'a4' });
        let y = 28;

        doc.setFont('helvetica', 'bold');
        doc.setFontSize(15);
        doc.text(title, margin, y);
        y += 16;

        const metaLines = metaLinesFrom(opts);
        if (metaLines.length) {
            doc.setFont('helvetica', 'normal');
            doc.setFontSize(10);
            const line = metaLines.join('     ·     ');
            const maxWidth = doc.internal.pageSize.getWidth() - margin * 2;
            const wrapped = doc.splitTextToSize(line, maxWidth);
            doc.text(wrapped, margin, y);
            y += wrapped.length * 13 + 6;
        }

        const pad = Math.max(1.2, fontSize * 0.32);
        prepared.forEach(function (s, idx) {
            if (!singlePage && idx > 0) {
                doc.addPage();
                y = 40;
            }
            doc.setFont('helvetica', 'bold');
            doc.setFontSize(singlePage ? 10 : 12);
            doc.text(s.name, margin, y);
            y += singlePage ? 4 : 8;
            const raceMode = isRaceResultRows(s.rows);
            let head = [s.rows[0]];
            let body = s.rows.slice(1);
            if (raceMode && s.rows[1] && /tours/i.test(s.rows[1].join(' ')) && /temps/i.test(s.rows[1].join(' '))) {
                head = [s.rows[0], s.rows[1]];
                body = s.rows.slice(2);
            }
            if (typeof doc.autoTable === 'function') {
                const tableOpts = {
                    startY: y + 4,
                    head: head,
                    body: body,
                    theme: raceMode ? 'plain' : 'grid',
                    styles: {
                        fontSize: fontSize,
                        cellPadding: pad,
                        overflow: 'linebreak',
                        halign: 'center',
                        valign: 'middle',
                        lineColor: [216, 211, 201],
                        lineWidth: raceMode ? 0.4 : 0.2
                    },
                    columnStyles: { 1: { halign: 'left' } },
                    headStyles: {
                        fillColor: ASPHALT,
                        textColor: 255,
                        fontSize: fontSize,
                        cellPadding: pad,
                        fontStyle: 'bold'
                    },
                    margin: { left: margin, right: margin, bottom: 20 },
                    tableWidth: 'auto',
                    pageBreak: 'auto',
                    rowPageBreak: singlePage ? 'avoid' : 'auto',
                    showHead: 'everyPage'
                };
                if (raceMode) {
                    tableOpts.didParseCell = function (data) {
                        styleRaceAutoTableCell(data, {}, clubTheme(opts));
                    };
                    tableOpts.willDrawCell = function (data) {
                        paintAutoTableCell(doc, data, {}, clubTheme(opts));
                    };
                    tableOpts.didDrawCell = function (data) {
                        doc.setDrawColor(216, 211, 201);
                        doc.setLineWidth(0.35);
                        doc.rect(data.cell.x, data.cell.y, data.cell.width, data.cell.height);
                    };
                }
                doc.autoTable(tableOpts);
                y = (doc.lastAutoTable && doc.lastAutoTable.finalY ? doc.lastAutoTable.finalY : y) + 14;
            } else {
                doc.setFont('helvetica', 'normal');
                doc.setFontSize(fontSize);
                body.slice(0, 40).forEach(function (row, i) {
                    doc.text(row.join(' | ').slice(0, 140), margin, y + 16 + i * 11);
                });
            }
        });
        return doc;
    }

    function exportToPdf(options) {
        const opts = options || {};
        const title = opts.title || 'Export';
        const sheets = opts.sheets || [];
        const file = slugify(opts.filename || title) + '.pdf';
        const singlePage = !!opts.singlePage;

        const prepared = sheets.map(function (s) {
            const table = typeof s.table === 'string' ? document.querySelector(s.table) : s.table;
            return {
                name: s.name || 'Tableau',
                rows: s.rows || tableToMatrix(table)
            };
        }).filter(function (s) { return s.rows && s.rows.length; });

        if (!prepared.length) {
            alert('Rien à exporter.');
            return;
        }

        const JsPDF = getJsPDFCtor();
        if (typeof JsPDF === 'function') {
            let fontSize = singlePage ? 8 : 8;
            let doc = drawPdfDoc(JsPDF, prepared, opts, fontSize);
            if (singlePage) {
                while (doc.getNumberOfPages() > 1 && fontSize > 5) {
                    fontSize -= 0.5;
                    doc = drawPdfDoc(JsPDF, prepared, opts, fontSize);
                }
            }
            doc.save(file);
            return;
        }

        const w = window.open('', '_blank');
        if (!w) {
            alert('Autorise les pop-ups pour l’export PDF.');
            return;
        }
        const metaHtml = metaLinesFrom(opts).map(function (line) {
            return '<span>' + escapeHtml(line) + '</span>';
        }).join('');
        const sections = prepared.map(function (s) {
            const rowsHtml = s.rows.map(function (r, i) {
                const tag = i === 0 ? 'th' : 'td';
                return '<tr>' + r.map(function (c) {
                    return '<' + tag + '>' + escapeHtml(c) + '</' + tag + '>';
                }).join('') + '</tr>';
            }).join('');
            return '<h2>' + escapeHtml(s.name) + '</h2><table>' + rowsHtml + '</table>';
        }).join('');
        const printCss = prepared.some(function (s) { return isRaceResultRows(s.rows); })
            ? lanePrintCss()
            : 'body{font-family:system-ui,sans-serif;padding:16px;color:#1c1c1e}' +
              'h1{font-size:1.2rem;margin:0 0 .4rem}h2{font-size:.9rem;margin:1rem 0 .35rem}' +
              '.meta{display:flex;flex-wrap:wrap;gap:.75rem 1.5rem;margin:0 0 .75rem;font-size:.9rem}' +
              '.meta span{font-weight:600}table{border-collapse:collapse;width:100%;font-size:10px}' +
              'th,td{border:1px solid #d8d3c9;padding:3px 5px;text-align:center}th{background:#2b2b2e;color:#fff}' +
              'td:nth-child(2),th:nth-child(2){text-align:left}' +
              '*{-webkit-print-color-adjust:exact;print-color-adjust:exact}' +
              '@page{size:A4 landscape;margin:10mm}' +
              '@media print{body{padding:0}h1,h2,.meta{break-after:avoid}}';
        w.document.write(
            '<!DOCTYPE html><html><head><title>' + escapeHtml(title) + '</title>' +
            '<style>' + printCss + '</style></head><body>' +
            '<h1>' + escapeHtml(title) + '</h1>' +
            (metaHtml ? '<p class="meta">' + metaHtml + '</p>' : '') +
            sections +
            '<script>window.onload=function(){window.print();}</' + 'script></body></html>'
        );
        w.document.close();
    }

    var RACE_LANES = [
        { head: [198, 40, 40], ink: [255, 255, 255], soft: [253, 232, 232], alt: [246, 207, 207], name: 'P1 Rouge' },
        { head: [244, 244, 242], ink: [28, 28, 30], soft: [243, 242, 238], alt: [231, 229, 223], name: 'P2 Blanc' },
        { head: [124, 179, 66], ink: [28, 28, 30], soft: [234, 246, 216], alt: [212, 236, 179], name: 'P3 Vert' },
        { head: [255, 152, 0], ink: [28, 28, 30], soft: [255, 232, 204], alt: [255, 209, 153], name: 'P4 Orange' },
        { head: [79, 195, 247], ink: [28, 28, 30], soft: [227, 246, 253], alt: [197, 235, 250], name: 'P5 Bleu' },
        { head: [253, 216, 53], ink: [28, 28, 30], soft: [255, 248, 209], alt: [255, 239, 153], name: 'P6 Jaune' }
    ];
    var ASPHALT = [43, 43, 46];
    var PAPER = [247, 247, 245];
    var INK = [28, 28, 30];
    var ACCENT = [200, 16, 46];
    var CLUB_THEMES = {
        slot4000: {
            name: 'SLOT 4000',
            banner: [30, 30, 30],
            accent: [252, 16, 26],
            chip: [48, 30, 30],
            chipLabel: [232, 168, 168]
        },
        srcs: {
            name: 'SRCS',
            banner: [8, 47, 92],
            accent: [3, 94, 182],
            chip: [14, 62, 118],
            chipLabel: [168, 200, 232]
        },
        sco: {
            name: 'RALLYES SLOT',
            banner: [44, 20, 12],
            accent: [154, 52, 18],
            chip: [64, 32, 20],
            chipLabel: [232, 184, 152]
        }
    };

    function clubTheme(opts) {
        const code = String((opts && opts.club) || '').toLowerCase().trim();
        return CLUB_THEMES[code] || {
            name: 'BELGIAN SLOT CLUB',
            banner: ASPHALT,
            accent: ACCENT,
            chip: [55, 55, 59],
            chipLabel: [168, 168, 173]
        };
    }
    var SCRATCH = [201, 162, 39];
    var PODIUM_BG = [[251, 243, 222], [240, 240, 242], [247, 236, 226]];
    var PODIUM_BADGE = [[232, 160, 32], [154, 154, 160], [184, 115, 51]];

    function raceBodyFromTable(table) {
        const body = [];
        const scratches = {};
        if (!table) return { body: body, scratches: scratches };
        table.querySelectorAll('tbody tr').forEach(function (tr, ri) {
            const cells = Array.from(tr.querySelectorAll('td'));
            body.push(cells.map(function (td) { return textOf(td); }));
            cells.forEach(function (td, ci) {
                if (td.classList.contains('is-scratch')) scratches[ri + ',' + ci] = true;
            });
        });
        return { body: body, scratches: scratches };
    }

    function isRaceResultRows(rows) {
        if (!rows || !rows[0] || rows[0].length < 15) return false;
        const header = rows[0].join(' ').toLowerCase();
        return header.indexOf('pilote') !== -1 && (header.indexOf('p1') !== -1 || header.indexOf('total') !== -1);
    }

    function lanePrintCss() {
        const rules = [
            '*{-webkit-print-color-adjust:exact;print-color-adjust:exact}',
            'body{font-family:system-ui,sans-serif;padding:16px;color:#1c1c1e}',
            'h1{font-size:1.2rem;margin:0 0 .4rem}h2{font-size:.9rem;margin:1rem 0 .35rem}',
            '.meta{display:flex;flex-wrap:wrap;gap:.75rem 1.5rem;margin:0 0 .75rem;font-size:.9rem}',
            '.meta span{font-weight:600}',
            'table{border-collapse:collapse;width:100%;font-size:10px}',
            'th,td{border:1px solid #d8d3c9;padding:3px 5px;text-align:center}',
            'td:nth-child(2),th:nth-child(2){text-align:left}',
            'th:nth-child(-n+3){background:#2b2b2e;color:#fff}',
            '@page{size:A4 landscape;margin:10mm}',
            '@media print{body{padding:0}h1,h2,.meta{break-after:avoid}}'
        ];
        RACE_LANES.forEach(function (lane, i) {
            const a = 4 + i * 2;
            const b = 5 + i * 2;
            const head = 'rgb(' + lane.head.join(',') + ')';
            const ink = 'rgb(' + lane.ink.join(',') + ')';
            const soft = 'rgb(' + lane.soft.join(',') + ')';
            const alt = 'rgb(' + lane.alt.join(',') + ')';
            rules.push('th:nth-child(' + a + '),th:nth-child(' + b + '){background:' + head + ';color:' + ink + '}');
            rules.push('td:nth-child(' + a + '),td:nth-child(' + b + '){background:' + soft + '}');
            rules.push('tr:nth-child(even) td:nth-child(' + a + '),tr:nth-child(even) td:nth-child(' + b + '){background:' + alt + '}');
        });
        return rules.join('');
    }

    function raceCellColors(section, row, col, scratches, theme) {
        scratches = scratches || {};
        const identity = (theme && theme.banner) || ASPHALT;
        if (section === 'head') {
            if (col < 3) return { fill: identity, text: [255, 255, 255] };
            const lane = RACE_LANES[Math.floor((col - 3) / 2)];
            if (lane) return { fill: lane.head, text: lane.ink };
            return { fill: identity, text: [255, 255, 255] };
        }
        const even = row % 2 === 1;
        if (col <= 2) {
            if (col === 0 && row < 3) return { fill: PODIUM_BADGE[row], text: [255, 255, 255], bold: true };
            if (row < 3) return { fill: PODIUM_BG[row], text: INK, bold: true };
            return { fill: even ? PAPER : [255, 255, 255], text: INK };
        }
        const lane = RACE_LANES[Math.floor((col - 3) / 2)];
        if (!lane) return { fill: [255, 255, 255], text: INK };
        return {
            fill: even ? lane.alt : lane.soft,
            text: INK,
            bold: !!scratches[row + ',' + col]
        };
    }

    function styleRaceAutoTableCell(data, scratches, theme) {
        const colors = raceCellColors(data.section, data.row.index, data.column.index, scratches, theme);
        if (!colors) return null;
        data.cell.styles.fillColor = colors.fill;
        data.cell.styles.textColor = colors.text;
        if (colors.bold) data.cell.styles.fontStyle = 'bold';
        if (data.column.index === 1) data.cell.styles.halign = 'left';
        return colors;
    }

    function paintAutoTableCell(doc, data, scratches, theme) {
        const colors = styleRaceAutoTableCell(data, scratches, theme);
        if (!colors) return;
        doc.setFillColor(colors.fill[0], colors.fill[1], colors.fill[2]);
        doc.setTextColor(colors.text[0], colors.text[1], colors.text[2]);
        if (colors.bold) doc.setFont('helvetica', 'bold');
    }

    function applyRaceCellStyle(cell, col, row, scratches) {
        const colors = raceCellColors('body', row, col, scratches);
        cell.styles.fillColor = colors.fill;
        cell.styles.textColor = colors.text;
        if (colors.bold) cell.styles.fontStyle = 'bold';
        if (col === 1) cell.styles.halign = 'left';
    }

    function drawRaceLegend(doc, margin, pageHeight, fontSize) {
        const y = pageHeight - 16;
        let x = margin;
        doc.setFont('helvetica', 'normal');
        doc.setFontSize(Math.max(6, fontSize - 1));
        RACE_LANES.forEach(function (lane) {
            doc.setFillColor.apply(doc, lane.head);
            doc.setDrawColor(0, 0, 0);
            doc.setLineWidth(0.3);
            doc.rect(x, y - 5, 7, 7, 'FD');
            doc.setTextColor.apply(doc, INK);
            doc.text(lane.name, x + 10, y);
            x += 72;
        });
        doc.setDrawColor.apply(doc, SCRATCH);
        doc.setLineWidth(1.1);
        doc.rect(x, y - 5, 7, 7);
        doc.setTextColor.apply(doc, INK);
        doc.text('Cadre doré = scratch', x + 10, y);
    }

    function paintSectionKicker(doc, text, margin, y, theme) {
        const accent = (theme && theme.accent) || ACCENT;
        doc.setFillColor.apply(doc, accent);
        doc.rect(margin, y - 8, 2.6, 11, 'F');
        doc.setFont('helvetica', 'bold');
        doc.setFontSize(10);
        doc.setTextColor.apply(doc, INK);
        doc.text(String(text), margin + 9, y);
        return y + 8;
    }

    function paintRaceBanner(doc, opts, heading, margin, pageWidth) {
        const items = metaItemsFrom(opts);
        const winner = String(opts.winner || '').trim();
        const theme = clubTheme(opts);
        const bannerH = 68;
        const stripH = 4;

        doc.setFillColor.apply(doc, theme.banner);
        doc.rect(0, 0, pageWidth, bannerH, 'F');
        doc.setFillColor.apply(doc, theme.accent);
        doc.rect(0, 0, pageWidth, 3.2, 'F');

        doc.setFont('helvetica', 'bold');
        doc.setFontSize(7);
        doc.setTextColor.apply(doc, theme.accent);
        if (typeof doc.setCharSpace === 'function') doc.setCharSpace(0.7);
        doc.text(theme.name, margin, 16);
        if (typeof doc.setCharSpace === 'function') doc.setCharSpace(0);

        doc.setFont('helvetica', 'bold');
        doc.setFontSize(16);
        doc.setTextColor(255, 255, 255);
        const titleMax = winner ? pageWidth - margin * 2 - 140 : pageWidth - margin * 2;
        doc.text(doc.splitTextToSize(String(heading || 'Résultats de course'), titleMax)[0], margin, 34);

        if (winner) {
            doc.setFont('helvetica', 'normal');
            doc.setFontSize(6.5);
            doc.setTextColor(176, 176, 180);
            if (typeof doc.setCharSpace === 'function') doc.setCharSpace(0.5);
            doc.text('VAINQUEUR', pageWidth - margin, 16, { align: 'right' });
            if (typeof doc.setCharSpace === 'function') doc.setCharSpace(0);
            doc.setFont('helvetica', 'bold');
            doc.setFontSize(13);
            doc.setTextColor.apply(doc, PODIUM_BADGE[0]);
            doc.text(doc.splitTextToSize(winner, 150)[0], pageWidth - margin, 34, { align: 'right' });
        }

        const chipY = 40;
        const chipH = 18;
        const avail = pageWidth - margin * 2;
        const gap = 7;
        const n = Math.max(items.length, 1);
        const chipW = Math.min(170, (avail - gap * (n - 1)) / n);

        items.forEach(function (item, i) {
            const x = margin + i * (chipW + gap);
            doc.setFillColor.apply(doc, theme.chip);
            if (typeof doc.roundedRect === 'function') {
                doc.roundedRect(x, chipY, chipW, chipH, 2.2, 2.2, 'F');
            } else {
                doc.rect(x, chipY, chipW, chipH, 'F');
            }
            doc.setFillColor.apply(doc, theme.accent);
            doc.rect(x, chipY, 3, chipH, 'F');
            doc.setFont('helvetica', 'normal');
            doc.setFontSize(6);
            doc.setTextColor.apply(doc, theme.chipLabel);
            doc.text(String(item.label || '').toUpperCase(), x + 8, chipY + 6.5);
            doc.setFont('helvetica', 'bold');
            doc.setFontSize(9);
            doc.setTextColor(255, 255, 255);
            const value = doc.splitTextToSize(String(item.value || '—'), chipW - 14)[0];
            doc.text(value, x + 8, chipY + 15);
        });

        const stripY = bannerH - stripH;
        const stripW = pageWidth / RACE_LANES.length;
        RACE_LANES.forEach(function (lane, i) {
            doc.setFillColor.apply(doc, lane.head);
            doc.rect(i * stripW, stripY, stripW + 0.4, stripH, 'F');
        });

        return bannerH;
    }

    function addQualifPage(doc, opts, fontSize, margin, pageWidth) {
        const qualifTable = typeof opts.qualifTable === 'string'
            ? document.querySelector(opts.qualifTable) : opts.qualifTable;
        const qualifRows = tableToMatrix(qualifTable);
        if (qualifRows.length < 2 || typeof doc.autoTable !== 'function') return;
        const pad = Math.max(2.2, fontSize * 0.45);
        const colPos = 44;
        const colPilot = 200;
        const colTime = 110;
        const tableW = colPos + colPilot + colTime;
        doc.addPage();
        const bannerH = paintRaceBanner(doc, opts, 'Qualifications', margin, pageWidth);
        const sectionY = paintSectionKicker(doc, 'Grille de départ — meilleur temps', margin, bannerH + 12, clubTheme(opts));
        doc.autoTable({
            startY: sectionY,
            head: [qualifRows[0]],
            body: qualifRows.slice(1),
            theme: 'plain',
            styles: {
                fontSize: Math.max(fontSize, 9),
                cellPadding: pad,
                valign: 'middle',
                textColor: INK,
                lineColor: [216, 211, 201],
                lineWidth: 0.4
            },
            headStyles: { fontStyle: 'bold', halign: 'center' },
            columnStyles: {
                0: { halign: 'center', cellWidth: colPos },
                1: { halign: 'left', cellWidth: colPilot },
                2: { halign: 'center', cellWidth: colTime, fontStyle: 'bold' }
            },
            didParseCell: function (data) {
                const theme = clubTheme(opts);
                styleRaceAutoTableCell(data, {}, theme);
                if (data.section === 'body' && data.column.index === 2) {
                    data.cell.styles.textColor = theme.accent;
                    data.cell.styles.fontStyle = 'bold';
                }
            },
            willDrawCell: function (data) {
                const theme = clubTheme(opts);
                paintAutoTableCell(doc, data, {}, theme);
                if (data.section === 'body' && data.column.index === 2) {
                    doc.setTextColor(theme.accent[0], theme.accent[1], theme.accent[2]);
                }
            },
            didDrawCell: function (data) {
                doc.setDrawColor(216, 211, 201);
                doc.setLineWidth(0.35);
                doc.rect(data.cell.x, data.cell.y, data.cell.width, data.cell.height);
            },
            margin: { left: margin, right: pageWidth - margin - tableW, bottom: 24 },
            tableWidth: tableW
        });
    }

    function drawRacePdf(JsPDF, opts, fontSize) {
        const title = opts.title || 'Résultats de course';
        const margin = 20;
        const doc = new JsPDF({ orientation: 'landscape', unit: 'pt', format: 'a4' });
        const pageWidth = doc.internal.pageSize.getWidth();
        const pageHeight = doc.internal.pageSize.getHeight();
        const pad = Math.max(1.1, fontSize * 0.28);
        const bannerH = paintRaceBanner(doc, opts, title, margin, pageWidth);
        const theme = clubTheme(opts);

        let y = paintSectionKicker(doc, 'Classement final', margin, bannerH + 12, theme);
        const raceTable = typeof opts.raceTable === 'string'
            ? document.querySelector(opts.raceTable) : opts.raceTable;
        const race = raceBodyFromTable(raceTable);
        if (race.body.length && typeof doc.autoTable === 'function') {

            const identityHead = { fillColor: theme.banner, textColor: 255, fontStyle: 'bold', halign: 'center' };
            const head1 = [
                { content: 'Pos', rowSpan: 2, styles: identityHead },
                { content: 'Pilote', rowSpan: 2, styles: Object.assign({}, identityHead, { halign: 'left' }) },
                { content: 'Total tours', rowSpan: 2, styles: identityHead }
            ];
            RACE_LANES.forEach(function (lane, i) {
                head1.push({
                    content: 'P' + (i + 1),
                    colSpan: 2,
                    styles: { fillColor: lane.head, textColor: lane.ink, fontStyle: 'bold', halign: 'center' }
                });
            });
            const head2 = [];
            RACE_LANES.forEach(function (lane) {
                const st = { fillColor: lane.head, textColor: lane.ink, fontStyle: 'bold', halign: 'center' };
                head2.push({ content: 'Tours', styles: st });
                head2.push({ content: 'Temps', styles: st });
            });

            doc.autoTable({
                startY: y + 2,
                head: [head1, head2],
                body: race.body,
                theme: 'plain',
                styles: {
                    fontSize: fontSize,
                    cellPadding: pad,
                    overflow: 'ellipsize',
                    halign: 'center',
                    valign: 'middle',
                    textColor: INK,
                    lineColor: [216, 211, 201],
                    lineWidth: 0.4
                },
                headStyles: { fontSize: fontSize, cellPadding: pad, valign: 'middle' },
                columnStyles: {
                    0: { cellWidth: 28, fontStyle: 'bold' },
                    1: { halign: 'left', cellWidth: 78 },
                    2: { fontStyle: 'bold', cellWidth: 48 }
                },
                didParseCell: function (data) {
                    styleRaceAutoTableCell(data, race.scratches, theme);
                },
                willDrawCell: function (data) {
                    paintAutoTableCell(doc, data, race.scratches, theme);
                },
                didDrawCell: function (data) {
                    doc.setDrawColor(216, 211, 201);
                    doc.setLineWidth(0.35);
                    doc.rect(data.cell.x, data.cell.y, data.cell.width, data.cell.height);
                    if (data.section !== 'body') return;
                    if (!race.scratches[data.row.index + ',' + data.column.index]) return;
                    doc.setDrawColor.apply(doc, SCRATCH);
                    doc.setLineWidth(1.15);
                    doc.rect(data.cell.x + 0.7, data.cell.y + 0.7, data.cell.width - 1.4, data.cell.height - 1.4);
                },
                margin: { left: margin, right: margin, bottom: 28 },
                tableWidth: 'auto',
                pageBreak: 'auto',
                rowPageBreak: 'avoid'
            });
        }

        drawRaceLegend(doc, margin, pageHeight, fontSize);
        return doc;
    }

    function exportRaceResultsPdf(options) {
        const opts = options || {};
        const file = slugify(opts.filename || opts.title || 'resultats') + '.pdf';
        const JsPDF = getJsPDFCtor();
        if (typeof JsPDF !== 'function') {
            exportToPdf(Object.assign({}, opts, {
                sheets: [
                    { name: 'Course', table: opts.raceTable },
                    { name: 'Qualifications', table: opts.qualifTable }
                ]
            }));
            return;
        }
        let fontSize = 7.5;
        let doc = drawRacePdf(JsPDF, opts, fontSize);
        while (doc.getNumberOfPages() > 1 && fontSize > 5) {
            fontSize -= 0.4;
            doc = drawRacePdf(JsPDF, opts, fontSize);
        }
        addQualifPage(doc, opts, fontSize, 20, doc.internal.pageSize.getWidth());
        doc.save(file);
    }

    function escapeHtml(s) {
        return String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    global.BscExport = {
        tableToMatrix: tableToMatrix,
        exportToExcel: exportToExcel,
        exportToPdf: exportToPdf,
        exportRaceResultsPdf: exportRaceResultsPdf,
        slugify: slugify
    };
})(window);
