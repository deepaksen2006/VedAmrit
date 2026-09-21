# Agent 2 protection

Agent 2 was not modified. Its existing personalized diet functionality was preserved.

The patient workflow calls the existing nutrition, conditions, food retrieval,
diet planning and safety stages through a transient adapter. No prompts, scoring,
targets, food selection, portions or nutrition algorithms are changed. Agent 3
does not generate foods, portions or competing meal plans. New preference options
are recorded verbatim; unsupported or uncertain preferences require doctor review.
