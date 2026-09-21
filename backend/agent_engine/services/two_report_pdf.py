"""Printable A4 Report 1 and Report 2 builders for the doctor-gated workflow."""
from __future__ import annotations

from pathlib import Path
import unicodedata
from typing import Any
from xml.sax.saxutils import escape

from reportlab.lib import colors
from reportlab.lib.enums import TA_CENTER
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import ParagraphStyle, getSampleStyleSheet
from reportlab.lib.units import mm
from reportlab.platypus import KeepTogether, Paragraph, SimpleDocTemplate, Spacer, Table, TableStyle

from agent_engine.services.report_workflow import TwoReportWorkflow


class TwoReportPDFGenerator:
    """Builds distinct, English-only documents. Report 2 cannot bypass approval."""
    def __init__(self, output_dir: str = "reports") -> None:
        self.output_dir = Path(output_dir)
        self.output_dir.mkdir(parents=True, exist_ok=True)
        styles = getSampleStyleSheet()
        self.title = ParagraphStyle("title", parent=styles["Title"], alignment=TA_CENTER, fontName="Helvetica-Bold", fontSize=17, leading=21, textColor=colors.HexColor("#234E52"), spaceAfter=4)
        self.sub = ParagraphStyle("sub", parent=styles["Normal"], alignment=TA_CENTER, fontSize=9, leading=12, textColor=colors.HexColor("#475569"), spaceAfter=7)
        self.section = ParagraphStyle("section", parent=styles["Heading2"], fontName="Helvetica-Bold", fontSize=10.5, leading=14, textColor=colors.HexColor("#234E52"), backColor=colors.HexColor("#E7F1ED"), borderPadding=4, spaceBefore=8, spaceAfter=5)
        self.body = ParagraphStyle("body", parent=styles["BodyText"], fontSize=8.4, leading=11, spaceAfter=3)
        self.small = ParagraphStyle("small", parent=self.body, fontSize=7.5, leading=9)
        self.section.keepWithNext = True

    def _p(self, value: Any, style: ParagraphStyle | None = None) -> Paragraph:
        text = "Not provided" if value in (None, "", [], {}) else str(value)
        if any(char.isalpha() and 'LATIN' not in unicodedata.name(char, '') for char in text):
            raise ValueError("English-only report required: translate supplied content before export.")
        return Paragraph(escape(text).replace("\n", "<br/>"), style or self.body)

    def _list(self, value: Any) -> str:
        if isinstance(value, dict):
            return ", ".join(f"{str(key).replace('_', ' ').title()}: {self._list(item)}" for key, item in value.items()) or "Not provided"
        if isinstance(value, (list, tuple, set)):
            return "; ".join(self._list(item) for item in value) or "Not provided"
        return str(value) if value not in (None, "") else "Not provided"

    def _table(self, rows: list[list[Any]], widths: list[float], header: bool = True) -> Table:
        table = Table(rows, colWidths=widths, repeatRows=1 if header else 0, hAlign="LEFT", splitInRow=1)
        commands = [("GRID", (0, 0), (-1, -1), .35, colors.HexColor("#B8C7C9")), ("VALIGN", (0, 0), (-1, -1), "TOP"), ("LEFTPADDING", (0, 0), (-1, -1), 4), ("RIGHTPADDING", (0, 0), (-1, -1), 4), ("TOPPADDING", (0, 0), (-1, -1), 4), ("BOTTOMPADDING", (0, 0), (-1, -1), 4)]
        if header:
            commands += [("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#DCEDEA")), ("FONTNAME", (0, 0), (-1, 0), "Helvetica-Bold")]
        table.setStyle(TableStyle(commands))
        return table

    def _footer(self, canvas, doc) -> None:
        canvas.saveState(); canvas.setStrokeColor(colors.HexColor("#CBD5E1")); canvas.line(12*mm, 12*mm, A4[0]-12*mm, 12*mm)
        canvas.setFont("Helvetica", 7); canvas.setFillColor(colors.HexColor("#475569")); canvas.drawString(12*mm, 7*mm, "VedAmrit | English-only AI-assisted wellness workflow")
        canvas.drawString(12*mm, A4[1]-8*mm, doc.title); canvas.drawRightString(A4[0]-12*mm, 7*mm, f"Page {doc.page}"); canvas.restoreState()

    def _build(self, path: Path, title: str, status: str, story: list[Any]) -> str:
        doc = SimpleDocTemplate(str(path), pagesize=A4, leftMargin=12*mm, rightMargin=12*mm, topMargin=13*mm, bottomMargin=18*mm, title=title)
        prefix = [Paragraph("VedAmrit", self.title), Paragraph(title, self.sub), self._table([[self._p("Report status"), self._p(status)]], [45*mm, 141*mm], header=False), Spacer(1, 3)]
        doc.build(prefix + story, onFirstPage=self._footer, onLaterPages=self._footer)
        return str(path)

    def generate_assessment_report(self, state: dict[str, Any], filename: str = "AI_Assessment_Report.pdf") -> str:
        workflow = TwoReportWorkflow(state)
        report = workflow.create_assessment_report(); data = report["assessment_data"]; meta = report["metadata"]; profile = data["patient_profile"]
        story: list[Any] = [Paragraph("PRELIMINARY AI ASSESSMENT — NOT DOCTOR APPROVED", self.section), Paragraph("Doctor review is required before personalized recommendations are finalized.", self.body)]
        story += [Paragraph("2. Patient Profile", self.section), self._table([[self._p("Field"), self._p("Information")], *[[self._p(k.replace("_", " ").title()), self._p(profile.get(k))] for k in ("name", "patient_id", "age", "gender", "height_cm", "weight_kg", "dietary_preference")]], [55*mm, 131*mm])]
        story += [Paragraph("3. Pre-consultation Summary", self.section), self._table([[self._p("Item"), self._p("Patient-provided information")], *[[self._p(k.replace("_", " ").title()), self._p(self._list(v))] for k, v in data["pre_consultation"].items()]], [55*mm, 131*mm])]
        score_rows = [[self._p("Assessment"), self._p("Recorded scores")]]
        for number, name in enumerate(("prakriti", "vikriti", "agni"), start=4):
            result = data[name]
            label = "Agni / Digestion" if name == "agni" else name.title()
            summary = result.get("constitution") or result.get("primary_dosha") or result.get("dominant_dosha") or result.get("status") or "REVIEW REQUIRED"
            story += [Paragraph(f"{number}. {label} Assessment", self.section), self._p(summary)]
            score_rows.append([self._p(label), self._p(self._list(data["assessment_scores"].get(name)))])
        story += [Paragraph("7. SYMPTOM-BASED DISEASE SCREENING", self.section), self._p("SCREENING / RESEARCH ONLY — NOT A MEDICAL DIAGNOSIS"), self._p(self._list(data["disease_screening"]))]
        story += [Paragraph("8. Assessment Scoring Summary", self.section), self._table(score_rows, [55*mm, 131*mm])]
        story += [Paragraph("9. Assessment Limitations", self.section), *[self._p(f"- {item}") for item in data["limitations"]], Paragraph("10. AI Assessment Status", self.section), self._p("AI-GENERATED — DOCTOR REVIEW REQUIRED"), Paragraph("11. Safety Disclaimer", self.section), self._p("This preliminary assessment is educational and supportive only. It is not diagnosis, treatment, a prescription, or a substitute for a qualified doctor.")]
        story.insert(0, self._p(f"Report {meta['report_id']} | Version {meta['report_version']} | Created {meta['created_at']}"))
        return self._build(self.output_dir / filename, "AI ASSESSMENT REPORT", "AI_ASSESSMENT_GENERATED", story)

    def generate_final_report(self, state: dict[str, Any], filename: str = "Final_Personalized_Wellness_Report.pdf") -> str:
        from agent_engine.services.final_report import generate_report_2
        return generate_report_2(self, state, filename)
