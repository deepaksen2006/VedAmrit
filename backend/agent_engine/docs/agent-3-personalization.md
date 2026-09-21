# Agent 3 personalization

Agent 3 owns lifestyle education only. Agent 2 remains the sole diet engine.
The adapter merges original intake context with normalized patient values because
the existing diet normalizer retains a narrower contract. Sleep, stress, activity,
age, sex, goals, symptoms, preferences, allergies, intolerances, conditions,
pregnancy context, medication restrictions, season, Prakriti, current Vikriti,
Agni, doctor restrictions and Agent 2 meal schedule are available to retrieval.
Names and contact details are not retrieval queries.

Vikriti takes precedence over baseline Prakriti when considering conflicting
constitution categories. Compound dosha labels are preserved. Recorded digestion
changes meal-timing discussion; sleep/stress/activity change routine suggestions.
This never recalculates portions or changes Agent 2 foods.

Each recommendation records recommendation, reason, factors, Ayurvedic basis,
wellness basis, safety, AI origin and doctor-review status. Conditions and
restrictions require clinical review. No disease, medicine or diet generator
is invoked by Agent 3.

Full context reaches `get_lifestyle_rag_evidence(patient_context=...)`.
Retrieved topic matches retain source/page/excerpt and remain explicitly
unverified: keyword matching is not proof of recommendation support. Missing
retrieval records a visible review flag and exception class, never invented citations.

The requested VedAmrit lifestyle document is absent. Current retrieval uses the
existing classical knowledge base. Import and verify the actual document before
claiming VedAmrit-specific grounding. Automated semantic evidence verification
is not implemented; doctor verification remains required.
