"""Gera o relatório U1 de SO com os documentos e medições atuais."""

import re
from html import escape
from pathlib import Path

import reportlab
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import (
    Image, PageBreak, Paragraph, Preformatted,
    SimpleDocTemplate, Spacer, Table, TableStyle,
)

from plot_speedup import aggregate_measurements, read_csv


ROOT = Path(__file__).resolve().parent.parent
WIDTH = A4[0] - 84
NAVY = colors.HexColor("#183247")
RED = colors.HexColor("#A82738")
GRAY = colors.HexColor("#52616D")


def inline(text: str) -> str:
    text = re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r"\1 (\2)", text)
    text = escape(text.replace("<br>", "").strip())
    text = re.sub(r"`([^`]+)`", r'<font name="Courier">\1</font>', text)
    return re.sub(r"\*\*([^*]+)\*\*", r"<b>\1</b>", text)


def styles():
    fonts_dir = Path(reportlab.__file__).resolve().parent / "fonts"
    for name, file in [
        ("RV", "Vera.ttf"), ("RV-Bold", "VeraBd.ttf"),
        ("RV-Italic", "VeraIt.ttf"), ("RV-BoldItalic", "VeraBI.ttf"),
    ]:
        pdfmetrics.registerFont(TTFont(name, str(fonts_dir / file)))
    pdfmetrics.registerFontFamily("RV", normal="RV", bold="RV-Bold",
                                  italic="RV-Italic", boldItalic="RV-BoldItalic")
    result = getSampleStyleSheet()
    result.add(ParagraphStyle("RVBody", fontName="RV", fontSize=9.3, leading=13,
                              textColor=NAVY, spaceAfter=7))
    result.add(ParagraphStyle("RVTitle", fontName="RV-Bold", fontSize=19, leading=24,
                              textColor=RED, spaceAfter=12))
    result.add(ParagraphStyle("RVHeading", fontName="RV-Bold", fontSize=13, leading=17,
                              textColor=RED, spaceBefore=5, spaceAfter=10, keepWithNext=True))
    result.add(ParagraphStyle("RVSubheading", fontName="RV-Bold", fontSize=10, leading=14,
                              textColor=NAVY, spaceBefore=8, spaceAfter=5, keepWithNext=True))
    result.add(ParagraphStyle("RVSmall", fontName="RV", fontSize=8, leading=11,
                              textColor=GRAY, spaceAfter=8))
    result.add(ParagraphStyle("RVCell", parent=result["RVBody"], fontSize=8.1, leading=11,
                              spaceAfter=0))
    return result


def table(rows, style, column_widths=None):
    content = [[Paragraph(inline(cell), style["RVCell"]) for cell in row] for row in rows]
    result = Table(content, colWidths=column_widths or [WIDTH / len(rows[0])] * len(rows[0]),
                   repeatRows=1, hAlign="LEFT")
    result.setStyle(TableStyle([
        ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E8EDF1")),
        ("ROWBACKGROUNDS", (0, 1), (-1, -1), [colors.white, colors.HexColor("#F7F9FA")]),
        ("GRID", (0, 0), (-1, -1), 0.4, colors.HexColor("#CED8DE")),
        ("VALIGN", (0, 0), (-1, -1), "TOP"),
        ("LEFTPADDING", (0, 0), (-1, -1), 7),
        ("RIGHTPADDING", (0, 0), (-1, -1), 7),
        ("TOPPADDING", (0, 0), (-1, -1), 6),
        ("BOTTOMPADDING", (0, 0), (-1, -1), 6),
    ]))
    return [result, Spacer(1, 10)]


def markdown(path, style, title):
    lines = path.read_text(encoding="utf-8").splitlines()
    result = []
    index = 0
    while index < len(lines):
        line = lines[index].strip()
        if not line:
            index += 1
            continue
        if line.startswith("# "):
            result.append(Paragraph(inline(title), style["RVHeading"]))
        elif line.startswith("## "):
            if line[3:] == "Escala do enunciado e limites":
                result.append(PageBreak())
            result.append(Paragraph(inline(line[3:]), style["RVSubheading"]))
        elif line.startswith("!["):
            result.append(Image(str(ROOT / "bench" / "speedup.png"),
                                width=WIDTH, height=WIDTH * 5 / 12))
            result.append(Spacer(1, 10))
        elif line.startswith("|"):
            rows = []
            while index < len(lines) and lines[index].strip().startswith("|"):
                cells = [cell.strip() for cell in lines[index].strip().strip("|").split("|")]
                if not all(re.fullmatch(r":?-+:?", cell) for cell in cells):
                    rows.append(cells)
                index += 1
            result.extend(table(rows, style))
            continue
        elif line.startswith("```"):
            index += 1
            code = []
            while index < len(lines) and not lines[index].strip().startswith("```"):
                code.append(lines[index])
                index += 1
            result.append(Preformatted("\n".join(code), ParagraphStyle(
                "RVCode", fontName="Courier", fontSize=8, leading=11,
                textColor=NAVY, spaceAfter=10), maxLineLength=94))
        else:
            result.append(Paragraph(inline(line), style["RVBody"]))
        index += 1
    return result


def footer(canvas, document):
    canvas.setStrokeColor(colors.HexColor("#D4DDE3"))
    canvas.line(42, 35, A4[0] - 42, 35)
    canvas.setFont("RV", 7)
    canvas.setFillColor(GRAY)
    canvas.drawString(42, 23, "Rota Vital | Infraestrutura de Software | Unidade 1")
    canvas.drawRightString(A4[0] - 42, 23, str(document.page))


def main():
    expected = aggregate_measurements(read_csv(ROOT / "bench" / "measurements.csv"))
    if read_csv(ROOT / "bench" / "measurements_agg.csv") != expected:
        raise ValueError("Regenere o agregado e o gráfico com python bench/plot_speedup.py.")
    style = styles()
    story = [
        Paragraph("Rota Vital", style["RVTitle"]),
        Paragraph("Paralelização na camada de aplicação - Unidade 1", style["RVSubheading"]),
        Paragraph("Eduardo Borges · Luiz Henrique Rocha · Eliziane Mota · Pedro Iranildo · "
                  "Ricardo Severiano de Souza Filho", style["RVSmall"]),
        Paragraph("Série publicada em 06/10/2026: 40 medições de resposta HTTP. "
                  "Fonte: bench/measurements.csv.", style["RVSmall"]),
    ]
    story.extend(markdown(ROOT / "docs" / "justificativa.md", style, "1. Justificativa"))
    story.extend([
        PageBreak(),
        Paragraph("2. Serviço e versões", style["RVHeading"]),
        Paragraph("A requisição chega ao endpoint Spring Boot, os dados são gerados, "
                  "a camada de aplicação detecta os pares e retorna um JSON com "
                  "duplicatesFound, processingTimeMs, dataGenerationTimeMs e status.", style["RVBody"]),
        Preformatted("GET /api/duplicates/detect?size=8000&threads=8&mode=platform",
                     ParagraphStyle("Endpoint", fontName="Courier", fontSize=8.2, leading=12)),
        Spacer(1, 10),
    ])
    story.extend(table([
        ["Classe", "Responsabilidade no código"],
        ["DuplicateController", "Valida size, threads e mode; seleciona a versão sequencial "
         "para platform com uma thread e a versão paralela para mais threads."],
        ["DataGeneratorService", "Gera nomes sintéticos de medicamentos com seed 42 "
         "e possíveis erros de digitação."],
        ["DuplicateDetectionService", "Executa detectSequential, detectParallel e "
         "runPartitioned; soma contagens após Future.get()."],
        ["FuzzyMatcher", "Calcula a distância de Levenshtein usando duas linhas "
         "de programação dinâmica."],
    ], style, [145, WIDTH - 145]))
    story.extend([
        Paragraph("Sequencial e threads de plataforma", style["RVSubheading"]),
        Paragraph("detectSequential percorre cada par i &lt; j uma vez. detectParallel "
                  "cria Executors.newFixedThreadPool(t). runPartitioned divide os índices "
                  "externos em t fatias contíguas de tamanho ceil(n/t). Cada Callable "
                  "lê a lista e atualiza apenas seu contador local; invokeAll aguarda "
                  "as tarefas e a thread chamadora agrega os resultados.", style["RVBody"]),
        Paragraph("A validação limita size a 20.000 e threads a quatro vezes os "
                  "processadores disponíveis à JVM. O serviço também oferece virtual threads "
                  "como opção; esta série mede apenas platform, com 1, 2, 4 e 8 threads.",
                  style["RVBody"]),
        Paragraph("Equivalência funcional da série atual", style["RVSubheading"]),
    ])
    story.extend(table([
        ["n", "Sequencial", "2 threads", "4 threads", "8 threads"],
        ["8000", "3198218", "3198218", "3198218", "3198218"],
        ["16000", "12801507", "12801507", "12801507", "12801507"],
    ], style))
    story.extend([
        Paragraph("Cada célula corresponde à contagem repetida nas cinco execuções "
                  "da configuração. Todas as 40 observações têm status SUCCESS. Os testes "
                  "JUnit de DuplicateDetectionServiceTest verificam equivalência e casos "
                  "de borda.", style["RVBody"]),
        PageBreak(),
    ])
    story.extend(markdown(ROOT / "docs" / "medicoes.md", style, "3. Medições"))
    story.extend([PageBreak(), Paragraph("4. Análise - 18 linhas", style["RVHeading"])])
    analysis = [
        line.strip().replace("<br>", "")
        for line in (ROOT / "docs" / "analise.md").read_text(encoding="utf-8").splitlines()
        if line.strip() and not line.startswith("#")
    ]
    if len(analysis) != 18:
        raise ValueError("A análise deve conter as 18 linhas documentadas.")
    plain_lines = [re.sub(r"\[([^\]]+)\]\(([^)]+)\)", r"\1", line).replace("`", "")
                   for line in analysis]
    widest = max(pdfmetrics.stringWidth(line, "RV", 10) for line in plain_lines)
    font_size = min(10, 10 * WIDTH / (widest + 8))
    if font_size < 8:
        raise ValueError("As linhas da análise estão muito longas para a página.")
    analysis_style = ParagraphStyle("RVAnalysis", fontName="RV", fontSize=font_size,
                                   leading=15, textColor=NAVY, spaceAfter=0)
    for line in plain_lines:
        story.append(Paragraph(escape(line), analysis_style))
    story.extend([
        Spacer(1, 18),
        Paragraph("Fontes da entrega", style["RVSubheading"]),
        Paragraph("bench/measurements.csv; bench/measurements_agg.csv; "
                  "bench/speedup.png; docs/justificativa.md; docs/medicoes.md; "
                  "docs/analise.md; código em src/main/java/com/rotavital; "
                  "testes em src/test/java/com/rotavital.", style["RVSmall"]),
    ])
    output = ROOT / "docs" / "RotaVital_Threads_Relatorio.pdf"
    document = SimpleDocTemplate(
        str(output), pagesize=A4, rightMargin=42, leftMargin=42,
        topMargin=38, bottomMargin=47, title="Rota Vital - Threads - U1 SO",
        author="Equipe Rota Vital", subject="Resposta HTTP, threads e análise de complexidade",
    )
    document.build(story, onFirstPage=footer, onLaterPages=footer)
    print(f"Relatório salvo em {output}")


if __name__ == "__main__":
    main()
