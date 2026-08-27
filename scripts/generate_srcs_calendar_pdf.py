#!/usr/bin/env python3
"""Génère le calendrier mural A3 paysage SRCS (soirées du mardi, 2026–2027)."""

from __future__ import annotations

import calendar
from datetime import date, timedelta
from pathlib import Path

from reportlab.lib.colors import Color, HexColor, white, black
from reportlab.lib.pagesizes import A3, landscape
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.pdfgen import canvas

ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "src/main/resources/static/docs/Calendrier_SRCS_mardis_2026-2027.pdf"

FONT_DIR = Path("/System/Library/Fonts/Supplemental")
pdfmetrics.registerFont(TTFont("GeorgiaBold", str(FONT_DIR / "Georgia Bold.ttf")))
pdfmetrics.registerFont(TTFont("Arial", str(FONT_DIR / "Arial.ttf")))
pdfmetrics.registerFont(TTFont("ArialBold", str(FONT_DIR / "Arial Bold.ttf")))
pdfmetrics.registerFont(TTFont("ArialItalic", str(FONT_DIR / "Arial Italic.ttf")))
pdfmetrics.registerFont(TTFont("ArialNarrow", str(FONT_DIR / "Arial Narrow.ttf")))
pdfmetrics.registerFont(TTFont("ArialNarrowBold", str(FONT_DIR / "Arial Narrow Bold.ttf")))

MONTHS = [
    "JANVIER",
    "FÉVRIER",
    "MARS",
    "AVRIL",
    "MAI",
    "JUIN",
    "JUILLET",
    "AOÛT",
    "SEPTEMBRE",
    "OCTOBRE",
    "NOVEMBRE",
    "DÉCEMBRE",
]

WEEKDAYS = ["LU", "MA", "ME", "JE", "VE", "SA", "DI"]

MONTH_COLORS = [
    HexColor("#2E5F8A"),
    HexColor("#3BA8C9"),
    HexColor("#1F8A7A"),
    HexColor("#7CB342"),
    HexColor("#E8C41A"),
    HexColor("#E67E22"),
    HexColor("#E59866"),
    HexColor("#C0392B"),
    HexColor("#C2185B"),
    HexColor("#5E35B1"),
    HexColor("#6A1B9A"),
    HexColor("#1A365D"),
]

# En-têtes jaunes / pêche : texte sombre pour le contraste.
DARK_HEADER_MONTHS = {4, 6}  # mai, juillet (0-based)

ROTATION = [
    "Scaleauto",
    "GT24 / Proto 24",
    "BPC",
    "Revoslot",
    "BRM",
]

SHORT_LABEL = {
    "GT24 / Proto 24": "GT24/P24",
    "Scaleauto": "Scaleauto",
    "BPC": "BPC",
    "Revoslot": "Revoslot",
    "BRM": "BRM",
}

EVENT_COLORS = {
    "GT24 / Proto 24": HexColor("#7c3aed"),
    "Scaleauto": HexColor("#b8860b"),
    "BPC": HexColor("#805ad5"),
    "Revoslot": HexColor("#c0392b"),
    "BRM": HexColor("#2c5282"),
}

TITLE_RED = HexColor("#9B1C1C")
GRID = HexColor("#D0D5DD")
INK = HexColor("#1A1A1A")
MUTED = HexColor("#667085")
BREAK_FILL = HexColor("#F2F4F7")
PAGE_BG = HexColor("#FFFDF8")


def mix(color: Color, other: Color, amount: float) -> Color:
    return Color(
        color.red + (other.red - color.red) * amount,
        color.green + (other.green - color.green) * amount,
        color.blue + (other.blue - color.blue) * amount,
    )


def is_christmas_break(day: date) -> bool:
    return day.month == 12 and day.day >= 21


def tuesday_events() -> dict[date, str]:
    start = date(2026, 9, 1)
    end = date(2027, 6, 29)
    events: dict[date, str] = {}
    index = 0
    current = start
    while current <= end:
        if not is_christmas_break(current):
            events[current] = ROTATION[index % len(ROTATION)]
            index += 1
        current += timedelta(weeks=1)
    return events


def draw_title(c: canvas.Canvas, width: float, year: int, top: float) -> None:
    title = f"Calendrier {year}"
    c.setFillColor(mix(TITLE_RED, black, 0.25))
    c.setFont("GeorgiaBold", 36)
    c.drawCentredString(width / 2 + 1.2, top - 1.2, title)
    c.setFillColor(TITLE_RED)
    c.drawCentredString(width / 2, top, title)

    c.setFillColor(INK)
    c.setFont("ArialBold", 10)
    c.drawString(22, top + 8, "SRCS")
    c.setFont("Arial", 8)
    c.setFillColor(MUTED)
    c.drawString(22, top - 4, "Slot Racing Club Seraing")

    c.setFillColor(INK)
    c.setFont("ArialBold", 9)
    right = "Soirées club du mardi · dès 18h"
    c.drawRightString(width - 22, top + 8, right)
    c.setFont("Arial", 8)
    c.setFillColor(MUTED)
    if year == 2026:
        subtitle = "Reprise le 1er septembre 2026"
    else:
        subtitle = "Saison jusqu'au 29 juin 2027"
    c.drawRightString(width - 22, top - 4, subtitle)


def draw_legend(c: canvas.Canvas, width: float, y: float) -> None:
    items = [
        ("Scaleauto", EVENT_COLORS["Scaleauto"]),
        ("GT24 / Proto 24", EVENT_COLORS["GT24 / Proto 24"]),
        ("BPC", EVENT_COLORS["BPC"]),
        ("Revoslot", EVENT_COLORS["Revoslot"]),
        ("BRM", EVENT_COLORS["BRM"]),
        ("Congés de Noël", MUTED),
    ]
    box = 8
    gap = 18
    total = 0
    measures = []
    c.setFont("Arial", 8)
    for label, color in items:
        w = box + 6 + c.stringWidth(label, "Arial", 8)
        measures.append((label, color, w))
        total += w
    total += gap * (len(items) - 1)
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
        "Jemeppe-sur-Meuse  ·  hors semaines du 22 et 29 décembre 2026  ·  belgianslotclub.com  ·  format A3 paysage",
    )


def draw_year(c: canvas.Canvas, year: int, events: dict[date, str]) -> None:
    width, height = landscape(A3)
    c.setFillColor(PAGE_BG)
    c.rect(0, 0, width, height, fill=1, stroke=0)

    title_top = height - 36
    draw_title(c, width, year, title_top)

    margin_x = 16
    grid_top = height - 62
    legend_y = 28
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

        days_in_month = calendar.monthrange(year, month)[1]
        weekend_tint = mix(color, white, 0.70)

        for day_n in range(1, 32):
            y = grid_top - header_h - day_n * row_h
            if day_n > days_in_month:
                continue

            day = date(year, month, day_n)
            weekday = day.weekday()  # 0 = lundi
            event = events.get(day)
            on_break = weekday == 1 and is_christmas_break(day) and year == 2026

            if event:
                c.setFillColor(EVENT_COLORS[event])
                c.rect(x0, y, col_w, row_h, fill=1, stroke=0)
            elif on_break:
                c.setFillColor(BREAK_FILL)
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

            day = date(year, month, day_n)
            weekday = day.weekday()
            abbr = WEEKDAYS[weekday]
            event = events.get(day)
            on_break = weekday == 1 and is_christmas_break(day) and year == 2026
            ink = white if event else INK

            c.setFillColor(ink)
            c.setFont("ArialBold" if weekday >= 5 or event else "Arial", 6.5)
            c.drawString(x0 + 3.5, y + 6.2, abbr)

            c.setFont("ArialBold", 8)
            c.drawString(x0 + 18, y + 5.6, str(day_n))

            if event:
                c.setFillColor(white)
                c.setFont("ArialNarrowBold", 7.2)
                c.drawRightString(x0 + col_w - 3.5, y + 5.8, SHORT_LABEL[event])
            elif on_break:
                c.setFillColor(MUTED)
                c.setFont("ArialItalic", 6.5)
                c.drawRightString(x0 + col_w - 3.5, y + 5.8, "Congés")

        c.setStrokeColor(mix(color, black, 0.15))
        c.setLineWidth(0.8)
        c.rect(x0, grid_bottom, col_w, grid_top - grid_bottom, fill=0, stroke=1)

    draw_legend(c, width, legend_y)


def main() -> None:
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    events = tuesday_events()
    c = canvas.Canvas(str(OUTPUT), pagesize=landscape(A3))
    c.setTitle("Calendrier SRCS 2026–2027 — soirées du mardi")
    c.setAuthor("Slot Racing Club Seraing")
    c.setSubject("Courses club du mardi, reprise 1er septembre 2026")

    draw_year(c, 2026, events)
    c.showPage()
    draw_year(c, 2027, events)
    c.save()
    print(f"PDF écrit : {OUTPUT}")


if __name__ == "__main__":
    main()
