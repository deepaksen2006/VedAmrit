"""Private, development-only export of persisted drafts. Never creates FinalReport."""
from io import BytesIO
from pathlib import Path
from xml.sax.saxutils import escape

from django.conf import settings
from django.utils import timezone
from reportlab import __file__ as reportlab_file
from reportlab.lib import colors
from reportlab.lib.pagesizes import A4
from reportlab.lib.styles import getSampleStyleSheet, ParagraphStyle
from reportlab.lib.units import mm
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.platypus import SimpleDocTemplate, Paragraph, Spacer, Table, TableStyle, PageBreak, KeepTogether

from agent_engine.services.draft_validation import draft_failures
from .assessment_sync import assessment_version, content_version
from .generation import inputs_current

DISCLAIMER = "AI-GENERATED DRAFT — NOT DOCTOR REVIEWED"
LEGACY_WARNING = ("REVIEW REQUIRED: significant dietary restrictions is unknown; "
                  "record an explicit none or not applicable when confirmed.")
WARNING_EXPLANATION = (
    "The legacy validator asks for a separate significant dietary restrictions field "
    "that the patient profile schema does not supply. This export uses the explicitly "
    "saved food intolerances, foods to avoid and doctor restrictions instead. "
    "The original warning remains recorded. This is not clinical clearance or doctor approval."
)
CONTEXT_LISTS = ("allergies", "food_intolerances", "foods_to_avoid", "health_conditions",
                 "current_medicines", "medicine_allergies", "medication_restrictions", "doctor_restrictions")


def development_preview(request):
    return bool(settings.DEBUG and settings.AGENTIC_DRAFT_PREVIEW_ENABLED
                and request and request.headers.get("X-VedAmrit-Draft-Preview") == "1")


def pdf_block_reasons(plan):
    """Fail closed for unknown warnings; only reconcile the exact proven schema mismatch."""
    reasons = []
    run = plan.agent_run if plan.agent_run_id else None
    if not run or run.status != "COMPLETED":
        return ["A completed, persisted agent run is required."]
    runtime = run.request_payload.get("runtime") or {}
    profile = run.request_payload.get("patient_profile") or {}
    if (run.user_id != plan.user_id or run.assessment_id != plan.assessment_id
            or runtime.get("assessment_version") != assessment_version(plan.assessment)
            or content_version(run.request_payload.get("assessment")) != runtime.get("assessment_version")
            or content_version(profile) != runtime.get("profile_version") or not inputs_current(plan)):
        reasons.append("Profile or assessment lineage is outdated. Reload/save the profile, sync assessments and generate a current plan.")
    if plan.doctor_status != "PENDING":
        reasons.append("Draft export is only available for pending plans; rejected or changed plans cannot be exported.")
    validation = plan.validation or {}
    diet_validation = validation.get("diet_validation") or {}
    if (plan.validation_status not in ("PASS", "REVIEW_REQUIRED") or validation.get("status") not in ("PASS", "REVIEW_REQUIRED")
            or validation.get("draft_view_allowed") is not True or validation.get("failed_checks")
            or validation.get("issues") or diet_validation.get("valid") is not True
            or diet_validation.get("issues")
            or any(diet_validation.get("checks", {}).get(key) != "PASS" for key in
                   ("food_data", "nutrition", "meal_structure", "ayurvedic_evidence", "patient_constraints"))):
        reasons.append("Nutrition and safety validation must pass before a personalized PDF can be exported.")
    reasons.extend(draft_failures(plan.diet_plan, plan.lifestyle_plan, profile))
    if any(not isinstance(profile.get(field), list) for field in CONTEXT_LISTS):
        reasons.append("Safety information is unanswered. Save explicit answers in the health profile before generating a new plan.")
    # These contexts need clinical reconciliation even if an older run omitted its warning.
    if any(profile.get(field) for field in ("health_conditions", "current_medicines", "medicine_allergies",
                                            "medication_restrictions", "doctor_restrictions")):
        reasons.append("Recorded clinical/medicine restrictions require doctor review before PDF export.")
    if str(profile.get("pregnancy_information", "")).strip().lower() not in ("none", "not applicable", "not_applicable", "not_pregnant"):
        reasons.append("Pregnancy context requires doctor review or an explicit not-applicable answer.")
    warnings = validation.get("warnings") or []
    other = [str(item) for item in warnings if item != LEGACY_WARNING]
    if other:
        reasons.append("Unresolved validation warning requires review: " + "; ".join(other))
    # Missing evidence remains a review requirement even if old validation lost the warning.
    if (plan.lifestyle_plan or {}).get("safety_flags"):
        reasons.append("Lifestyle safety flags require doctor review before PDF export.")
    if plan.validation_status == "REVIEW_REQUIRED" and not warnings:
        reasons.append("Review is required but its reason is missing. Doctor review is needed.")
    return list(dict.fromkeys(reasons))


def render_draft_pdf(plan):
    """Render saved data only; deterministic bytes, no graph or model invocation."""
    font_dir = Path(reportlab_file).parent / "fonts"
    for name, filename in (("DraftVera", "Vera.ttf"), ("DraftVeraBold", "VeraBd.ttf")):
        if name not in pdfmetrics.getRegisteredFontNames():
            pdfmetrics.registerFont(TTFont(name, str(font_dir / filename)))
    styles = getSampleStyleSheet()
    body = ParagraphStyle("DraftBody", parent=styles["BodyText"], fontName="DraftVera",
                          fontSize=9, leading=13, spaceAfter=6)
    heading = ParagraphStyle("DraftHeading", parent=body, fontName="DraftVeraBold", fontSize=14,
                             leading=19, spaceBefore=8, spaceAfter=10, textColor=colors.HexColor("#225640"))
    small = ParagraphStyle("DraftSmall", parent=body, fontSize=8, leading=11)
    supported = pdfmetrics.getFont("DraftVera").face.charToGlyph

    def text(value):
        if isinstance(value, list):
            return "; ".join(map(str, value)) if value else "None recorded"
        if value is None or value == "":
            return "Not recorded"
        return str(value)

    def p(value, style=body):
        value = text(value)
        if any(ord(c) not in supported for c in value if c not in "\n\r\t"):
            raise ValueError("A saved field contains characters unsupported by the installed PDF font. Contact support for font configuration.")
        return Paragraph(escape(value).replace("\n", "<br/>"), style)

    def nutrition(data):
        data = data or {}
        return " | ".join(f"{label}: {data.get(key, data.get('carbohydrates_g') if key == 'carbs_g' else 'Not recorded')}"
                          for key, label in (("calories", "kcal"), ("protein_g", "Protein g"), ("carbs_g", "Carbs g"),
                                             ("fat_g", "Fat g"), ("fiber_g", "Fiber g")))

    profile = plan.agent_run.request_payload["patient_profile"]
    runtime = plan.agent_run.request_payload["runtime"]
    date = timezone.localtime(plan.created_at).strftime("%Y-%m-%d %H:%M %Z")
    story = [p("VedAmrit", heading), p("Personalized seven-day diet and lifestyle draft", heading),
             p(DISCLAIMER), p("Educational wellness draft only. Not a diagnosis, prescription or substitute for a qualified clinician."),
             p(f"Plan #{plan.pk} | Plan version {plan.version} | Generated {date}"),
             p(f"Patient: {profile.get('name')}"), p(f"Assessment #{plan.assessment_id} | Agent run #{plan.agent_run_id}"),
             p("Assessment version: " + runtime["assessment_version"], small),
             p("Profile version: " + runtime["profile_version"], small), p("Assessment summary", heading)]
    for label, result, keys in (
        ("Prakriti", plan.assessment.prakriti_result, ("prakritiType", "constitution", "primaryDosha")),
        ("Vikriti", plan.assessment.vikriti_result, ("dominantDosha", "dominant_dosha")),
        ("Agni", plan.assessment.agni_result, ("dominantAgni", "status", "displayResult"))):
        story.append(p(label + ": " + text(next((result[key] for key in keys if result.get(key)), None))))
    story += [p("Validation and restrictions", heading), p(f"Validation: {plan.validation_status} | Doctor status: {plan.doctor_status}")]
    for warning in (plan.validation or {}).get("warnings") or []:
        story.append(p(warning))
        if warning == LEGACY_WARNING:
            story.append(p(WARNING_EXPLANATION))
    for field in CONTEXT_LISTS:
        story.append(p(field.replace("_", " ").title() + ": " + text(profile.get(field))))
    story += [p("Daily nutrition target", heading), p(nutrition(plan.diet_plan.get("daily_target")), small)]
    for day in plan.diet_plan["days"]:
        story += [PageBreak(), p(f"Day {day['day']} - Meal schedule", heading)]
        for meal in day["meals"]:
            story.append(p(str(meal["meal"]).title(), heading))
            rows = [[p("Food and portion", small), p("Nutrition", small)]]
            for food in meal["foods"]:
                rows.append([p(f"{food['food_name']} - {food['portion_g']} g", small), p(nutrition(food.get("nutrition")), small)])
            table = Table(rows, colWidths=[75*mm, 99*mm], repeatRows=1, splitInRow=1, hAlign="LEFT")
            table.setStyle(TableStyle([("GRID", (0, 0), (-1, -1), .4, colors.HexColor("#C8D8CF")),
                                       ("BACKGROUND", (0, 0), (-1, 0), colors.HexColor("#E9F1EC")),
                                       ("VALIGN", (0, 0), (-1, -1), "TOP"),
                                       ("LEFTPADDING", (0, 0), (-1, -1), 7), ("RIGHTPADDING", (0, 0), (-1, -1), 7)]))
            story += [table, Spacer(1, 5*mm)]
        story += [p("Daily total", heading), p(nutrition(day.get("daily_total")), small)]
    story += [PageBreak(), p("Lifestyle recommendations", heading)]
    for item in plan.lifestyle_plan["recommendations"]:
        recommendation = [p(str(item.get("category", "Lifestyle")).replace("_", " ").title(), heading)]
        for key in ("personalized_recommendation", "patient_specific_reason", "safety_consideration"):
            if item.get(key):
                recommendation.append(p(item[key]))
        source = item.get("ayurvedic_basis") or {}
        if source.get("source") and source["source"] not in ("Not provided", "unknown"):
            recommendation.append(p(f"Retrieved reference: {source['source']} | Page: {source.get('page', 'Not recorded')}", small))
            recommendation.append(p("Retrieved evidence for review; this citation does not verify the recommendation.", small))
        story.append(KeepTogether(recommendation))
    rag = plan.agent_run.rag_status or {}
    story.append(p(f"RAG: {rag.get('status', 'unavailable')} | Evidence matches: {rag.get('evidence_count', 'Not recorded')}", small))
    stream = BytesIO()

    def page(canvas, doc):
        canvas.saveState()
        canvas.setFont("DraftVeraBold", 8)
        canvas.setFillColor(colors.HexColor("#9C2D24"))
        canvas.drawCentredString(A4[0]/2, A4[1]-13*mm, DISCLAIMER)
        canvas.setFillColor(colors.HexColor("#46574E"))
        canvas.setFont("DraftVera", 8)
        canvas.drawString(18*mm, 12*mm, f"VedAmrit | Draft plan #{plan.pk}")
        canvas.drawRightString(A4[0]-18*mm, 12*mm, f"Page {doc.page}")
        canvas.restoreState()

    SimpleDocTemplate(stream, pagesize=A4, leftMargin=18*mm, rightMargin=18*mm,
                      topMargin=23*mm, bottomMargin=22*mm, title="VedAmrit - Personalized AI Draft",
                      author="VedAmrit", invariant=1).build(story, onFirstPage=page, onLaterPages=page)
    return stream.getvalue()

