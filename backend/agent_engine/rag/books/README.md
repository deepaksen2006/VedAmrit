RAG source books vendored from Chandana-2024/ayurdevic_report.

Included:
- 10Personalized+Ahara+and+Vihara_+Aligning(66-69).pdf
- 40.+ijaar_vol_ii_isuue_11_1691_1695-+REVIEW.pdf
- Charaka-Samhita-Acharya-Charaka (1).pdf

The upstream repository also lists larger source files that could not be
reliably downloaded in this environment during integration:
- Sushruta Samhita 1.pdf
- VedAmrit_Ayurvedic_Prakriti_Lifestyle_RAG_Document.docx
- astanga-hridaya-sutrasthan-handbook.pdf
- bhavprakash-1.pdf

Runtime RAG uses agent_engine/rag/vectorstore when available. If no vector
store exists, Agent 3 falls back to doctor-review-required evidence status.
