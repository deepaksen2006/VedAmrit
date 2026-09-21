# Report Schema and Duplicate Prevention

`ReportMetadata` contains `patient_id`, `session_id`, `report_id`, `report_type`, `report_version`, `status`, `created_at`, `updated_at`, and `approval_timestamp`. `AssessmentData`, `Agent2DietData`, `Agent3LifestyleData`, `DoctorReviewData`, and `FinalApprovedData` remain separate objects.

Report 1 contains header, patient profile, pre-consultation summary, Prakriti, Vikriti, Agni/digestion, screening, score summary, limitations, AI status, and safety disclaimer only. It excludes diet, lifestyle, doctor profile, prescriptions, follow-up, signatures, and final approval.

Report 2 contains a relevant, doctor-reviewed assessment summary, nutrition analysis, restrictions, Agent 2 diet, daily nutrition summary, Agent 3 lifestyle advice, doctor-approved advice, doctor-entered or approved medicines, follow-up, safety review, evidence, disclaimer, and approval/signature. Re-generation updates a versioned report rather than merging complete reports or duplicating sections.
