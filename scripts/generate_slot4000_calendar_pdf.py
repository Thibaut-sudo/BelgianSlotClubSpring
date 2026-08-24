#!/usr/bin/env python3
"""Génère le calendrier mural A3 paysage Slot 4000 / Hobby 2000 (2026)."""

from __future__ import annotations

import calendar
from datetime import date
from pathlib import Path

from reportlab.lib.colors import Color, HexColor, white, black
from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/static/docs/Calendrier_Slot4000_Hobby2000_2026.pdf"

FONT_DIR = Path("/System/Library/Fonts/Supplemental")
pdfmetrics.registerFont(TTFont("GeorgiaBold", str(FONT_DIR / "Georgia Bold.ttf")))
pdfmetrics.registerFont(TTFont("Arial", str(FONT_DIR / "Arial.ttf")))
pdfmetrics.registerFont(TTFont("ArialBold", str(FONT_DIR / "Arial Bold.ttf")))
pdfmetrics.registerFont(TTFont("ArialNarrowBold", str(FONT_DIR / "Arial Narrow Bold.ttf")))

MONTHS = [
    "JANVIER", "FÉVRIER", "MARS", "AVRIL", "MAI", "JUIN",
    "JUILLET", "AOÛT", "SEPTEMBRE", "OCTOBRE", "NOVEMBRE", "DÉCEMBRE",
]
WEEKDAYS = ["LU", "MA", "ME", "JE", "VE", "SA", "DI"]
MONTH_COLORS = [
    HexColor("#2E5F8A"), HexColor("#3BA8C9"), HexColor("#1F8A7A"), HexColor("#7CB342"),
    HexColor("#E8C41A"), HexColor("#E67E22"), HexColor("#E59866"), HexColor("#C0392B"),
    HexColor("#C2185B"), HexColor("#5E35B1"), HexColor("#6A1B9A"), HexColor("#1A365D"),
]
DARK_HEADER_MONTHS = {4, 6}

EVENTS_2026 = {
    date(2026, 1, 6): "PROTO32",
    date(2026, 1, 13): "GT24",
    date(2026, 1, 16): "GT24",
    date(2026, 1, 23): "GT32",
    date(2026, 1, 30): "PROTO24",
    date(2026, 2, 6): "PROTO32",
    date(2026, 2, 20): "GR5",
    date(2026, 2, 27): "PROTO24",
    date(2026, 3, 6): "SLOT.IT",
    date(2026, 3, 13): "GT24",
    date(2026, 3, 20): "GT32",
    date(2026, 3, 24): "Soirée VAB",
    date(2026, 3, 27): "PROTO32",
    date(2026, 4, 3): "GR5",
    date(2026, 4, 10): "PROTO24",
    date(2026, 4, 17): "SLOT.IT",
    date(2026, 4, 24): "GT24",
    date(2026, 5, 1): "GT32",
    date(2026, 5, 8): "PROTO24",
    date(2026, 5, 15): "PROTO32",
    date(2026, 5, 22): "GT24",
    date(2026, 5, 29): "GR5",
    date(2026, 6, 5): "PROTO24",
    date(2026, 6, 12): "SLOT.IT",
    date(2026, 6, 19): "GT24",
    date(2026, 6, 26): "GT32",
    date(2026, 7, 3): "PROTO24",
    date(2026, 9, 4): "PROTO32",
    date(2026, 9, 11): "GT24",
    date(2026, 9, 18): "GR5",
    date(2026, 9, 25): "PROTO24",
    date(2026, 10, 2): "SLOT.IT",
    date(2026, 10, 9): "TCR ALL",
    date(2026, 10, 16): "PROTO32",
    date(2026, 10, 23): "GT24",
    date(2026, 10, 30): "GT32",
    date(2026, 11, 6): "PROTO24",
    date(2026, 11, 13): "GR5",
    date(2026, 11, 20): "TCR ALL",
    date(2026, 11, 27): "SLOT.IT",
    date(2026, 12, 4): "GT24",
    date(2026, 12, 11): "PROTO32",
    date(2026, 12, 18): "Soirée Fun",
}

SHORT_LABEL = {
    "PROTO32": "Proto32",
    "GT32": "GT32",
    "SLOT.IT": "Slot.it",
    "GR5": "Gr5",
    "PROTO24": "Proto24",
    "GT24": "GT24",
    "TCR ALL": "TCR ALL",
    "Soirée VAB": "Soirée VAB",
    "Soirée Fun": "Soirée fun",
}

EVENT_COLORS = {
    "PROTO32": HexColor("#1a7a4c"),
    "GT32": HexColor("#c8102e"),
    "SLOT.IT": HexColor("#6b4fa0"),
    "GR5": HexColor("#3d3d42"),
    "PROTO24": HexColor("#e8650a"),
    "GT24": HexColor("#7c3aed"),
    "TCR ALL": HexColor("#0f766e"),
    "Soirée VAB": HexColor("#be185d"),
    "Soirée Fun": HexColor("#be185d"),
}

TITLE_RED = HexColor("#9B1C1C")
GRID = HexColor("#D0D5DD")
INK = HexColor("#1A1A1A")
MUTED = HexColor("#667085")
PAGE_BG = HexColor("#FFFDF8")


def mix(color: Color, other: Color, amount: float) -> Color:
    return Color(
        color.red + (other.red - color.red) * amount,
        color.green + (other.green - color.green) * amount,
        color.blue + (other.blue - color.blue) * amount,
    )


def draw_year(c: canvas.Canvas) -> None:
    width, height = landscape(A3)
    c.setFillColor(PAGE_BG)
    c.rect(0, 0, width, height, fill=1, stroke=0)

    top = height - 36
    c.setFillColor(mix(TITLE_RED, black, 0.25))
    c.setFont("GeorgiaBold", 36)
    c.drawCentredString(width / 2 + 1.2, top - 1.2, "Calendrier 2026")
    c.setFillColor(TITLE_RED)
    c.drawCentredString(width / 2, top, "Calendrier 2026")

    c.setFillColor(INK)
    c.setFont("ArialBold", 10)
    c.drawString(22, top + 8, "SLOT 4000")
    c.setFont("Arial", 8)
    c.setFillColor(MUTED)
    c.drawString(22, top - 4, "Hobby 2000 · Liège")

    c.setFillColor(INK)
    c.setFont("ArialBold", 9)
    c.drawRightString(width - 22, top + 8, "Soirées club du vendredi")
    c.setFont("Arial", 8)
    c.setFillColor(MUTED)
    c.drawRightString(width - 22, top - 4, "Proposition sept.–déc. 2026")

    margin_x = 16
    grid_top = height - 62
    header_h = 20
    grid_bottom = 52
    col_w = (width - 2 * margin_x) / 12
    row_h = (grid_top - header_h - grid_bottom) / 31

    for month in range(1, 13):
        col = month - 1
        x0 = margin_x + col * col_w
        color = MONTH_COLORS[col]
        header_ink = INK if col in DARK_HEADER_MONTHS else white

        c.setFillColor(color)
        c.rect(x0, grid_top - header_h, col_w, header_h, fill=1, stroke=0)
        c.setFillColor(header_ink)
        c.setFont("ArialBold", 8)
        c.drawCentredString(x0 + col_w / 2, grid_top - 14, MONTHS[col])

        days_in_month = calendar.monthrange(2026, month)[1]
        weekend_tint = mix(color, white, 0.70)

        for day_n in range(1, 32):
            y = grid_top - header_h - day_n * row_h
            if day_n > days_in_month:
                continue
            day = date(2026, month, day_n)
            weekday = day.weekday()
            event = EVENTS_2026.get(day)
            if event:
                c.setFillColor(EVENT_COLORS[event])
                c.rect(x0, y, col_w, row_h, fill=1, stroke=0)
            elif weekday >= 5:
                c.setFillColor(weekend_tint)
                c.rect(x0, y, col_w, row_h, fill=1, stroke=0)

        for day_n in range(1, 32):
            y = grid_top - header_h - day_n * row_h
            c.setStrokeColor(GRID)
            c.setLineWidth(0.35)
            c.line(x0, y, x0 + col_w, y)
            if day_n > days_in_month:
                continue
            day = date(2026, month, day_n)
            weekday = day.weekday()
            event = EVENTS_2026.get(day)
            ink = white if event else INK
            c.setFillColor(ink)
            c.setFont("ArialBold" if weekday >= 5 or event else "Arial", 6.5)
            c.drawString(x0 + 3.5, y + 6.2, WEEKDAYS[weekday])
            c.setFont("ArialBold", 8)
            c.drawString(x0 + 18, y + 5.6, str(day_n))
            if event:
                c.setFillColor(white)
                c.setFont("ArialNarrowBold", 7)
                c.drawRightString(x0 + col_w - 3.5, y + 5.8, SHORT_LABEL[event])

        c.setStrokeColor(mix(color, black, 0.15))
        c.setLineWidth(0.8)
        c.rect(x0, grid_bottom, col_w, grid_top - grid_bottom, fill=0, stroke=1)

    items = [
        ("Proto32", EVENT_COLORS["PROTO32"]),
        ("GT32", EVENT_COLORS["GT32"]),
        ("Slot.it", EVENT_COLORS["SLOT.IT"]),
        ("Gr5", EVENT_COLORS["GR5"]),
        ("Proto24", EVENT_COLORS["PROTO24"]),
        ("GT24", EVENT_COLORS["GT24"]),
        ("TCR ALL", EVENT_COLORS["TCR ALL"]),
        ("Soirée", EVENT_COLORS["Soirée Fun"]),
    ]
    box, gap, y = 8, 14, 28
    c.setFont("Arial", 8)
    measures = [(label, color, box + 6 + c.stringWidth(label, "Arial", 8)) for label, color in items]
    total = sum(w for _, _, w in measures) + gap * (len(items) - 1)
    x = (width - total) / 2
    for label, color, w in measures:
        c.setFillColor(color)
        c.roundRect(x, y, box, box, 1.5, fill=1, stroke=0)
        c.setFillColor(INK)
        c.drawString(x + box + 6, y + 1, label)
        x += w + gap

    c.setFillColor(MUTED)
    c.setFont("Arial", 7)
    c.drawCentredString(
        width / 2,
        y - 14,
        "Quai de la Boverie 78-87, 4020 Liège  ·  au-dessus de Hobby 2000  ·  belgianslotclub.com  ·  format A3 paysage",
    )


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    c = canvas.Canvas(str(OUTPUT), pagesize=landscape(A3))
    c.setTitle("Calendrier Slot 4000 / Hobby 2000 2026")
    c.setAuthor("Slot 4000")
    c.setSubject("Courses club du vendredi, proposition sept.–déc. 2026")
    draw_year(c)
    c.save()
    print(f"PDF écrit : {OUTPUT}")


if __name__ == "__main__":
    main()
