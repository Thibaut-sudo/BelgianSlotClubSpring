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

    function exportToPdf(options) {
        const opts = options || {};
        const title = opts.title || 'Export';
        const subtitle = opts.subtitle || '';
        const sheets = opts.sheets || [];
        const file = slugify(opts.filename || title) + '.pdf';

        const prepared = sheets.map(s => {
            const table = typeof s.table === 'string' ? document.querySelector(s.table) : s.table;
            return {
                name: s.name || 'Tableau',
                rows: s.rows || tableToMatrix(table)
            };
        }).filter(s => s.rows && s.rows.length);

        if (!prepared.length) {
            alert('Rien à exporter.');
            return;
        }

        const JsPDF = global.jspdf && global.jspdf.jsPDF;
        if (JsPDF && global.jspdf && typeof global.jspdf.jsPDF === 'function') {
            // handled below
        }
        if (typeof JsPDF === 'function') {
            const doc = new JsPDF({ orientation: 'landscape', unit: 'pt', format: 'a4' });
            let y = 40;
            doc.setFontSize(14);
            doc.text(title, 40, y);
            y += 18;
            if (subtitle) {
                doc.setFontSize(10);
                doc.setTextColor(90);
                doc.text(subtitle, 40, y);
                doc.setTextColor(0);
                y += 16;
            }
            prepared.forEach((s, idx) => {
                if (idx > 0) {
                    doc.addPage();
                    y = 40;
                }
                doc.setFontSize(12);
                doc.text(s.name, 40, y);
                y += 8;
                const head = [s.rows[0]];
                const body = s.rows.slice(1);
                if (typeof doc.autoTable === 'function') {
                    doc.autoTable({
                        startY: y + 6,
                        head: head,
                        body: body,
                        styles: { fontSize: 8, cellPadding: 3 },
                        headStyles: { fillColor: [43, 43, 46] },
                        margin: { left: 40, right: 40 }
                    });
                } else {
                    doc.setFontSize(8);
                    body.slice(0, 40).forEach((row, i) => {
                        doc.text(row.join(' | ').slice(0, 140), 40, y + 20 + i * 12);
                    });
                }
            });
            doc.save(file);
            return;
        }

        // Fallback : fenêtre imprimable
        const w = window.open('', '_blank');
        if (!w) {
            alert('Autorise les pop-ups pour l’export PDF.');
            return;
        }
        const sections = prepared.map(s => {
            const rowsHtml = s.rows.map((r, i) => {
                const tag = i === 0 ? 'th' : 'td';
                return '<tr>' + r.map(c => '<' + tag + '>' + escapeHtml(c) + '</' + tag + '>').join('') + '</tr>';
            }).join('');
            return '<h2>' + escapeHtml(s.name) + '</h2><table>' + rowsHtml + '</table>';
        }).join('');
        w.document.write(
            '<!DOCTYPE html><html><head><title>' + escapeHtml(title) + '</title>' +
            '<style>body{font-family:system-ui,sans-serif;padding:24px;color:#1c1c1e}' +
            'h1{font-size:1.25rem;margin:0 0 .25rem}h2{font-size:1rem;margin:1.5rem 0 .5rem}' +
            'p{color:#6b6b70;margin:0 0 1rem}table{border-collapse:collapse;width:100%;font-size:11px}' +
            'th,td{border:1px solid #d8d3c9;padding:4px 6px;text-align:left}th{background:#2b2b2e;color:#fff}' +
            '@media print{body{padding:0}}</style></head><body>' +
            '<h1>' + escapeHtml(title) + '</h1>' +
            (subtitle ? '<p>' + escapeHtml(subtitle) + '</p>' : '') +
            sections +
            '<script>window.onload=function(){window.print();}</' + 'script></body></html>'
        );
        w.document.close();
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
        slugify: slugify
    };
})(window);
