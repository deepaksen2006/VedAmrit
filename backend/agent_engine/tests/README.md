The original upstream test
`test_rejects_displayed_score_that_does_not_match_answers` has been replaced.

It assumed Python receives questionnaire answers and independently recalculates
Prakriti, Vikriti, and Agni to detect mismatches. That is not VedAmrit's
integration contract. VedAmrit Android/Kotlin assessment engines are canonical,
and the backend agent engine must consume those stored results without
rescoring them.

The replacement tests assert canonical pass-through behavior, no Python
rescoring, and rejection of malformed or incomplete canonical result payloads.
