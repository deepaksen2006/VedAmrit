from pathlib import Path
from typing import Any
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import Paragraph, SimpleDocTemplate, Table, TableStyle

from agent_engine.services.report_validation import NUTRIENTS, validate_report_state


class VedAmritPDFReport:
    """Build one English-only, printable report from one workflow state."""

    def __init__(self, output_dir: str = "reports") -> None:
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        styles = getSampleStyleSheet()
        self.font_name = "Helvetica"
        self.title_style = ParagraphStyle("ReportTitle", parent=styles["Title"], alignment=TA_CENTER, fontSize=18, leading=21, textColor=colors.HexColor("#234E52"), spaceAfter=3)
        self.subtitle_style = ParagraphStyle("ReportSubtitle", parent=styles["Normal"], alignment=TA_CENTER, fontSize=9, leading=11, textColor=colors.HexColor("#4A5568"), spaceAfter=8)
        self.section_style = ParagraphStyle("Section", parent=styles["Heading2"], fontSize=11, leading=14, textColor=colors.HexColor("#234E52"), spaceBefore=9, spaceAfter=4)
        self.subsection_style = ParagraphStyle("Subsection", parent=styles["Heading3"], fontSize=9.5, leading=11, textColor=colors.HexColor("#2F5D62"), spaceBefore=5, spaceAfter=3)
        self.body_style = ParagraphStyle("Body", parent=styles["BodyText"], fontSize=8.2, leading=10.5, spaceAfter=2)
        self.small_style = ParagraphStyle("Small", parent=styles["BodyText"], fontSize=7.2, leading=9, textColor=colors.HexColor("#374151"))

    def _p(self, value: Any, style: ParagraphStyle | None = None) -> Paragraph:
        text = "Not provided" if value is None or str(value).strip() in {"", "None", "N/A"} else str(value)
        return Paragraph(escape(text).replace("\n", "<br/>"), style or self.body_style)

    def _list_text(self, value: Any) -> str:
        if isinstance(value, (list, tuple, set)):
            return ", ".join(str(item) for item in value) if value else "Not provided"
        return str(value) if value not in (None, "") else "Not provided"

    def _table(self, rows: list[list[Any]], widths: list[float], header: bool = False) -> Table:
        table = Table(rows, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT")
        commands = [("GRID", (0, 0), (-1, -1), 0.35, colors.HexColor("#A0AEC0")), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 4), ("RIGHTPADDING", (0, 0), (-1, -1), 4), ("TOPPADDING", (0, 0), (-1, -1), 3), ("BOTTOMPADDING", (0, 0), (-1, -1), 3)]
        if header:
            commands.append(("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#DCEDEA")))
        table.setStyle(TableStyle(commands))
        return table

    def _section(self, story: list[Any], number: int, title: str) -> None:
        story.append(Paragraph(f"{number}. {escape(title).upper()}", self.section_style))

    def _footer(self, canvas, document) -> None:
        canvas.saveState()
        canvas.setStrokeColor(colors.HexColor("#CBD5E0"))
        canvas.line(12 * mm, 12 * mm, A4[0] - 12 * mm, 12 * mm)
        canvas.setFont(self.font_name, 7)
        canvas.setFillColor(colors.HexColor("#4A5568"))
        canvas.drawString(12 * mm, 7 * mm, "VedAmrit | AI-assisted wellness report | Doctor verification required")
        canvas.drawRightString(A4[0] - 12 * mm, 7 * mm, f"Page {document.page}")
        canvas.restoreState()

    def generate(self, state: dict[str, Any], filename: str = "VedAmrit_Wellness_Report.pdf") -> str:
        from agent_engine.services.two_report_pdf import TwoReportPDFGenerator
        return TwoReportPDFGenerator(str(self.output_dir)).generate_final_report(state, filename)


# Historical import name shares the same approval gate.
AyurGenixPDFReport = VedAmritPDFReport
