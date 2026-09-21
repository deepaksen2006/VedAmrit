"""Report 2 layout using the approved patient-facing projection."""
from agent_engine.services.report_workflow import TwoReportWorkflow
from agent_engine.services.final_report_content import patient_report_content
from reportlab.platypus import Paragraph
from reportlab.lib.units import mm


def generate_report_2(pdf, state, filename):
    workflow = TwoReportWorkflow(state)
    if not workflow.approval_is_current():
        workflow.invalidate_if_changed()
        raise PermissionError('DOCTOR VERIFICATION REQUIRED')
    data = patient_report_content(state['approved_snapshot'])
    story = []
    def section(title, values):
        if values:
            story.append(Paragraph(title, pdf.section))
            for label, value in values:
                story.append(pdf._p(f'{label}: {value}'))
    section('Patient Information', data['patient'])
    section('Short Assessment Summary', data['assessment'])
    story.append(Paragraph('Personalized Diet Timetable', pdf.section))
    columns = data['columns']
    widths = [14*mm] + [172*mm / (len(columns)-1)]*(len(columns)-1) if len(columns)>1 else [186*mm]
    story.append(pdf._table([[pdf._p(c, pdf.small) for c in row] for row in [columns, *data['rows']]], widths))
    section('Food Guidance', data['food_guidance'])
    section('Lifestyle Recommendations', [(item['label'], item['recommendation'] + ('\n' + item['safety'] if item['safety'] else '')) for item in data['lifestyle']])
    section('Doctor Review / Approval', data['doctor'])
    if data['medicines']:
        story.append(Paragraph('Doctor-entered Medicines / Prescriptions', pdf.section))
        story.append(pdf._table([[pdf._p(c) for c in row] for row in [['Medicine','Dosage','Frequency','Duration','Instructions'], *data['medicines']]], [35*mm,30*mm,30*mm,30*mm,61*mm]))
    story.append(pdf._p('Approved by Doctor: ' + data['doctor_name']))
    story.append(pdf._p('Typed Doctor Name: ' + data['typed_name']))
    story.append(pdf._p('Doctor Signature: __________________'))
    story.append(pdf._p('This wellness plan supports your care. Follow the reviewing doctor instructions and seek qualified care for severe symptoms.'))
    path = pdf._build(pdf.output_dir / filename, 'FINAL PERSONALIZED WELLNESS REPORT', 'DOCTOR APPROVED', story)
    workflow.create_final_report()
    return path
