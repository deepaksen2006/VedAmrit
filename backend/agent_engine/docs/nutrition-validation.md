# Agent 2 Nutrition Validation

The report adapter validates, but does not modify, Agent 2 output. Every displayed food must have a name, portion, calories, protein, carbohydrates, fat, and fibre. The adapter recalculates food-level, meal, daily, weekly, full-plan, and target-versus-actual totals. It checks displayed portions against nutrition calculations and PDF values against report data.

Missing, inconsistent, or unreliable nutrition is recorded and surfaces **NUTRITION REVIEW REQUIRED**. The system never silently corrects Agent 2 data, does not display false precision, and records discrepancies for doctor review.
