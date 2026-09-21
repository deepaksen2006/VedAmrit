# Two Report and Doctor Review Workflow

1. Generate **AI ASSESSMENT REPORT** with status `AI_ASSESSMENT_GENERATED`.
2. Display **PRELIMINARY AI ASSESSMENT — NOT DOCTOR APPROVED** and **Doctor review is required before personalized recommendations are finalized.**
3. Generate Agent 3 recommendations and move to `AGENT_3_RECOMMENDATIONS_GENERATED`, then `UNDER_DOCTOR_REVIEW`.
4. The doctor reviews the patient information, assessment, Agent 2 diet, and Agent 3 guidance. The doctor may correct patient information, assessment values, symptoms, diet and portions, restrictions, lifestyle advice, medicines, dosage, frequency, duration, instructions, follow-up, monitoring, comments, and sign-off; they may approve, reject, or request changes.
5. Only an explicit review record with qualified doctor confirmation, identity, approval status, approval date, sign-off, approved report version, and exact approved content can move the workflow to `DOCTOR_APPROVED`.
6. Generate **FINAL PERSONALIZED WELLNESS REPORT**, then optionally mark it `FINALIZED` or `SENT_TO_PATIENT`.

An unapproved workflow displays **DOCTOR VERIFICATION REQUIRED**. No empty field, AI result, missing signature, or automatic action is approval. Any edit to approved assessment, diet, lifestyle, restriction, medicine, or doctor content invalidates approval and requires a fresh review.
