# AyurGenix — Research-Grounded Multi-Agent Ayurvedic Personalization System

**AyurGenix** is a research-grounded, multi-agent AI system designed for personalized Ayurvedic assessment, dietary planning, and holistic lifestyle wellness. It combines deterministic Python scoring models, classical Ayurvedic Knowledge Base RAG retrieval, structured nutritional data, and safety fact-checking within a LangGraph orchestration workflow.

---

## 🏛️ System Architecture

```
                               PATIENT
                                  │
                                  ▼
                           PATIENT PROFILE
   (Name, Age, Gender, Height, Weight, Preference, Allergies, Exclusions, Conditions, Goal)
                                  │
                                  ▼
                   AGENT 1 — AYURVEDIC ASSESSMENT
               (Prakriti 21-Q, Vikriti 21-Q, Agni 11-Q)
                                  │
                                  ▼
                    STRUCTURED AYURVEDIC PROFILE
                      (Prakriti + Vikriti + Agni)
                                  │
                  ┌───────────────┴───────────────┐
                  ▼                               ▼
       AGENT 2 — DIET AGENT            AGENT 3 — LIFESTYLE AGENT
        (Personalized Filters,          (Dinacharya, Sleep, Routine,
         Food RAG, Nutrition DB)         Stress, Activity, Lifestyle RAG)
                  │                               │
                  ▼                               ▼
        PERSONALIZED DIET PLAN          PERSONALIZED LIFESTYLE PLAN
                  │                               │
                  └───────────────┬───────────────┘
                                  ▼
                     SAFETY CHECK (safety.py)
                                  │
                    ┌─────────────┴─────────────┐
                    ▼                           ▼
                 [PASS]                      [FAIL]
                    │                           │
                    ▼                           ▼
              FINAL REPORT               REVISION LOOP
```

---

## 🤖 Core Agents

### 1. Agent 1 — Ayurvedic Assessment Agent
- **Prakriti Assessment**: 21-question assessment determining innate constitution (`Vata`, `Pitta`, `Kapha`, or dual-dosha combinations).
- **Vikriti Assessment**: 21-question assessment measuring current dosha imbalance.
- **Agni Assessment**: 11-item validated Agnibala self-assessment tool (*Singh A et al., 2017*) categorizing digestive strength into `Mandagni`, `Vishamagni`, `Tikshnagni`, or `Samagni`.
- **Output**: Structured Ayurvedic Assessment payload for downstream agents.

### 2. Agent 2 — Personalized Ayurvedic Diet Agent
- **Hard Filters**: Enforces zero-tolerance exclusion of allergens, explicit foods to avoid, dietary preferences (`Vegetarian`, `Vegan`, `Non-vegetarian`), and unsafe condition restrictions.
- **Deterministic Energy Targets**: Calculates BMR and TDEE using the validated Mifflin-St Jeor formula (*Mifflin MD et al., 1990*) based on patient age, gender, height, weight, and goal.
- **Transparent Ayurvedic Ranking**: Scores dataset food candidates using Dosha compatibility, Agni adaptations, and health condition tags.
- **Nutritional Summary**: Computes meal-level and daily macro totals deterministically.

### 3. Agent 3 — Ayurvedic Lifestyle & Wellness Agent
- **Non-Diet Guidance**: Provides classical evidence-backed recommendations covering Dinacharya (morning routine), Nidra (sleep routine), Vyayama (exercise/activity), meal timing, stress management, and Ritucharya (seasonal routine).
- **Lifestyle RAG Knowledge Base**: Retrieves classical literature references (*Charaka Samhita*, *Sushruta Samhita*, *Ashtanga Hridayam*) attached to specific recommendations.

### 4. Safety Fact-Checker Agent
- Validates candidate food IDs, meal structure, nutrition consistency, and patient constraints.
- Emits explicit `PASS`, `REVIEW_REQUIRED`, or `FAIL` statuses.

---

## 🔬 Research & Reference Foundations

1. **Agni Assessment**: Singh A, Singh G, Patwardhan K, Gehlot S. *Development, Validation, and Verification of a Self-Assessment Tool to Estimate Agnibala (Digestive Strength)*. 2017.
2. **Energy Expenditure**: Mifflin MD, St Jeor ST, Hill LA, Scott BJ, Daugherty SA, Koh YO. *A new predictive equation for resting energy expenditure in healthy individuals*. Am J Clin Nutr. 1990;51(2):241-247.
3. **Classical Ayurvedic Texts**: *Charaka Samhita*, *Sushruta Samhita*, *Ashtanga Hridayam*.

---

## 🚀 Running the Application

### Execution
Install dependencies with `python -m pip install -r requirements.txt` (Python 3.11+).
The current report workflow is specified in [docs/implementation-contract.md](docs/implementation-contract.md)
and [docs/report-workflow.md](docs/report-workflow.md); these supersede the older
automatic-final-report diagram above. Safety PASS alone is never approval.

Agent 2 was inspected but not modified because its personalized diet generation is already working correctly.

Run the main interactive terminal application:
```bash
python -m agent_engine.main
```

### Running Unit & Scenario Tests
Execute the full test suite covering profile validation, scoring models, energy calculations, and Scenarios A–D:
```bash
python -m unittest discover -s tests -p "test_*.py"
```

---

## ⚠️ Important Disclaimer
AyurGenix is an educational wellness research prototype. It does not provide medical diagnosis, treatment, or medical advice, and is not a substitute for consultation with a qualified Ayurvedic physician or medical professional.
