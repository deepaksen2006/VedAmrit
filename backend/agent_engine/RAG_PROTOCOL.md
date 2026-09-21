# AyurGenix RAG Protocol

**Effective Date:** September 6, 2026  
**Status:** Official Specification  
**Version:** 1.0

---

## Overview

The AyurGenix RAG (Retrieval-Augmented Generation) Protocol ensures that all Ayurvedic recommendations are **grounded in approved classical sources and evidence**, not generated and then justified retroactively.

### Core Principle

```
Evidence-First Retrieval:

USER/AGENT CONTEXT
        ↓
IDENTIFY INFORMATION REQUIRED
        ↓
FORMULATE PRECISE RETRIEVAL QUERY
        ↓
RETRIEVE RELEVANT SOURCES
        ↓
FILTER IRRELEVANT EVIDENCE
        ↓
USE ONLY SUPPORTED INFORMATION
        ↓
GENERATE GROUNDED OUTPUT
        ↓
RETURN SOURCE + BOOK + PAGE + AUTHOR
```

**Never:** Generate Ayurvedic facts first and search for supporting evidence.  
**Always:** Retrieve evidence first, then use only that evidence to build recommendations.

---

## 1. Approved Ayurvedic Knowledge Sources

### Primary Classical Sources (Highest Priority)

1. **Charaka Samhita** (Agnivesha, Caraka, Dridhabala) — Comprehensive medical encyclopedia, especially:
   - Sutra Sthana: General principles
   - Sharira Sthana: Body systems
   - Nidana Sthana: Disease diagnosis
   - Chikitsa Sthana: Treatment

2. **Sushruta Samhita** (Susruta) — Surgical and medical text, especially:
   - Sutra Sthana: General principles
   - Sharira Sthana: Anatomy
   - Uttara Tantra: General medicine

3. **Ashtanga Hridaya** (Vagbhata) — Concise digest covering all major topics

4. **Ashtanga Sangraha** (Vagbhata) — Expanded version of Ashtanga Hridaya

5. **Bhavaprakasha Nighantu** (Bhava Misra) — Materia medica and food properties

### Secondary Sources (Lower Priority)

- Peer-reviewed Ayurvedic research papers
- Validated assessment methodologies (prakriti, vikriti assessment studies)
- Secondary explanatory material from established institutions

### Not Approved

- Non-peer-reviewed blog posts
- Commercial product claims
- Unattributed "Ayurvedic" statements
- Fabricated classical references

---

## 2. Query Formation Rules

### Rule 1: Queries Must Be Specific, Not Broad

**❌ INCORRECT:**
```
"Ayurvedic information about Vata"
"Tell me about Pitta"
"What is Kapha?"
```

**✅ CORRECT:**
```
"Ayurvedic Dinacharya recommendations for Vata imbalance including sleep, daily routine, and physical activity"

"Pitta Vikriti characteristics, aggravation signs, and lifestyle modifications"

"Kapha balancing dietary principles and suitable food properties"
```

### Rule 2: Queries Must Include Context

Every query must specify:

1. **Ayurvedic Concept** (what principle/topic)
2. **Dosha/Patient Context** (which dosha, prakriti, vikriti)
3. **Agni Status** (if relevant to digestion/diet)
4. **Application Area** (diet, lifestyle, assessment, exercise, etc.)
5. **Desired Evidence Type** (principles, properties, effects, recommendations, characteristics)

### Example Query Structure

```
"[AYURVEDIC CONCEPT] for [PATIENT CONTEXT] with [AGNI/CONSTRAINT] 
according to [SOURCE FOCUS]. Include [SPECIFIC APPLICATION AREA]"
```

### Examples by Agent

#### Agent 1 — Assessment

```
Query 1:
"Prakriti assessment characteristics and identification of Vata, Pitta, Kapha 
constitution types according to classical Ayurvedic texts"

Query 2:
"Vikriti assessment methodology: how to identify dosha aggravation, imbalance signs, 
and current disease state distinct from Prakriti"

Query 3:
"Agni assessment: Samagni Tikshnagni Mandagni Vishamagni types, characteristics, 
and diagnostic methods"
```

#### Agent 2 — Personalized Diet

```
Query 1:
"Foods and dietary principles suitable for Pitta Vikriti with Tikshnagni 
according to Rasa Guna Virya Vipaka properties"

Query 2:
"Kapha balancing foods and Ahara principles for Kapha-dominant constitution 
with Mandagni digestion"

Query 3:
"Vata-stabilizing dietary guidelines, meal timing, and food properties 
for Vata Prakriti with Vishamagni"

Query 4:
"Food incompatibilities and contraindications: which foods should not be combined, 
which food combinations support digestion"
```

#### Agent 3 — Lifestyle & Wellness

```
Query 1:
"Ayurvedic Dinacharya for Vata imbalance including sleep, daily routine, 
physical activity, meal timing, and stress management"

Query 2:
"Ritucharya: seasonal routine and lifestyle adjustments for Pitta Vikriti 
during summer and heat seasons"

Query 3:
"Sleep guidelines for Kapha-dominant Prakriti: sleep timing, duration, 
and sleeping habits"

Query 4:
"Physical activity and exercise recommendations for Mandagni Agni: 
type of exercise, intensity, timing"
```

---

## 3. Retrieval Execution

### k Value (Number of Documents)

- **k=5** for specific agent queries (standard)
- **k=3** for very specific narrow questions
- **k=10** for broader assessment questions

### Relevance Threshold

All retrieved chunks must pass relevance assessment **before use**.

---

## 4. Relevance Filtering

### Keep (HIGH Relevance)

✅ Directly discusses the requested Ayurvedic concept  
✅ Provides characteristics, properties, effects, or recommendations relevant to the user  
✅ Comes from an approved classical source  
✅ Contains useful contextual information  
✅ Addresses the specific dosha or condition  
✅ Relevant to the application area (diet, lifestyle, assessment)

### Reject (LOW Relevance)

❌ Mentions the requested term only incidentally  
❌ Discusses unrelated diseases  
❌ Discusses unrelated medicines  
❌ Pharmaceutical preparation focus when task is diet/lifestyle  
❌ Panchakarma procedures when task is lifestyle (unless specifically requested)  
❌ Surgery content when task is diet/lifestyle  
❌ Contains only historical or introductory information  
❌ Discusses unrelated social customs or unrelated rituals

### Filtering Process

1. Read each retrieved chunk
2. Ask: "Does this directly support the current agent's task?"
3. If no → remove
4. If yes → check for source traceability (see Section 5)
5. If traceable → keep; else reject

---

## 5. Source Traceability Requirements

**Every important recommendation must include:**

```
Source Book:     [Classical Source Name]
Page:            [Page Number]
Author:          [Original Author]
Content:         [Direct Quote or Faithful Summary]
Relevance:       [high/medium]
Support Status:  [directly_supported / reasonably_inferred]
```

### What Counts as "Important"

- Any dosha-specific recommendation
- Any Prakriti/Vikriti assessment finding
- Any Agni-related guidance
- Any dietary principle or food effect
- Any lifestyle or daily routine recommendation
- Any claim about food properties (rasa, guna, virya, vipaka)

### Example

```
Source Book:   Sushruta Samhita
Page:          423 (Uttara Tantra)
Author:        Susruta
Content:       "Tikshnagni leads to rapid digestion and heat; foods should 
               be cooling and moist. Vishamagni requires warm, grounding foods 
               at consistent times to stabilize digestion."
Relevance:     high
Support Status: directly_supported
Recommendation: Pitta Vikriti with Tikshnagni should emphasize cooling foods 
                (cucumber, coconut, ghee) at regular intervals.
```

---

## 6. Agent-Specific Retrieval Priorities

### Agent 1 — Prakriti & Vikriti Assessment

**Retrieve For:**
- Prakriti assessment methods and characteristics
- Vikriti assessment and dosha aggravation signs
- Agni assessment and types
- Constitutional differences between Vata, Pitta, Kapha

**Do NOT Retrieve:**
- Disease treatment protocols
- Medicines and pharmaceutical preparations
- Surgical techniques
- Panchakarma procedures

**Example Query:**
```
"Classical Ayurvedic method for identifying Prakriti: how to assess Vata, Pitta, 
Kapha constitutional type based on physical, mental, and behavioral characteristics 
according to Charaka and Sushruta Samhita"
```

### Agent 2 — Personalized Diet Planning

**Retrieve For:**
- Ahara (food) properties and effects
- Rasa, Guna, Virya, Vipaka classifications
- Dosha effects of specific foods
- Agni compatibility and digestion
- Seasonal dietary principles (Ritucharya)
- Food combinations and incompatibilities
- Meal timing recommendations

**Patient Context to Use:**
- Prakriti (constitution type)
- Vikriti (current imbalance)
- Agni status (Samagni, Tikshnagni, Mandagni, Vishamagni)
- Dietary preferences (vegan, vegetarian)
- Allergies and food intolerances
- Foods to avoid (personal or cultural)
- Health goals or constraints

**Do NOT Retrieve:**
- Disease treatment diets (unless generically applicable)
- Medicines
- Panchakarma preparation diets
- Non-food supplements

**Example Query:**
```
"Dietary principles and suitable foods for Vata Vikriti with Mandagni Agni 
according to Rasa Guna Virya Vipaka: include warming, grounding, easily-digestible 
foods; avoid cold, light, rough foods. Meal timing recommendations for weak digestion."
```

### Agent 3 — Lifestyle & Wellness

**Retrieve For:**
- Dinacharya (daily routine)
- Waking and sleeping practices
- Sleep timing, duration, and quality
- Physical activity and exercise
- Meal timing for each dosha
- Stress management and mental wellness
- Seasonal routine (Ritucharya)
- Preventive wellness habits
- Behavioral recommendations

**Patient Context to Use:**
- Prakriti (constitution type)
- Vikriti (current imbalance)
- Agni status
- Lifestyle constraints (work, family, climate)
- Health goals

**Do NOT Retrieve:**
- Disease treatment protocols
- Medicines
- Panchakarma procedures (unless wellness-focused)
- Surgical techniques

**Example Query:**
```
"Ayurvedic Dinacharya for Kapha-dominant Prakriti with Kapha Vikriti: 
daily routine, waking time, sleep duration, physical activity intensity, 
meal timing, and stress management. Include stimulating, warming practices 
to balance heavy Kapha qualities."
```

---

## 7. Source Priority Hierarchy

Use sources in this order:

1. **Direct Classical Source** (Charaka Samhita, Sushruta Samhita, etc.)
   - Most authoritative; most preferred

2. **Relevant Peer-Reviewed Ayurvedic Research**
   - Modern validation of classical principles

3. **Validated Assessment Methodology**
   - Standardized prakriti/vikriti assessment tools from recognized institutions

4. **Secondary Explanatory Material**
   - Commentaries or explanations from established Ayurvedic institutions
   - Least preferred; use only if primary sources insufficient

**Do not treat secondary sources as equivalent to primary classical sources.**

---

## 8. Evidence vs. Inference

Clearly distinguish between three levels:

### A. DIRECTLY SUPPORTED
Information explicitly stated in the retrieved classical source.

Example:
```
Source: Sushruta Samhita, Page 423
Direct: "Tikshnagni individuals should consume moist, cooling foods."
Support: Directly Supported
```

### B. REASONABLE INFERENCE
A conclusion derived logically from multiple retrieved pieces of evidence.

Example:
```
Evidence 1: Pitta is hot and sharp (Source: Charaka Samhita)
Evidence 2: Ghee is cool and grounding (Source: Bhavaprakasha)
Evidence 3: Pitta Vikriti requires cooling foods (Source: Sushruta Samhita)

Inference: Ghee is suitable for Pitta Vikriti.
Support: Reasonably Inferred from multiple sources

Note: This inference is valid ONLY when multiple sources support each 
component. Never present as "directly from classical text."
```

### C. NOT SUPPORTED
Information for which adequate evidence was not retrieved.

Example:
```
Question: "Is turmeric specifically recommended for Vata arthritis?"
Result: No retrieved sources discuss turmeric + Vata arthritis specifically.
Status: NOT SUPPORTED

Action: Explicitly state: "Insufficient evidence was retrieved from the 
approved AyurGenix knowledge base for this specific recommendation."
```

**Never present C as if it were A or B.**

---

## 9. Handling Conflicts

If two retrieved sources appear to provide conflicting recommendations:

1. **Identify the conflict explicitly** (do not silently choose one)
2. **Check the primary sources** (Charaka vs. Sushruta; which is more direct?)
3. **Consider context** (one may apply to a specific condition; another to general wellness)
4. **Report both views** (let the clinical team decide)
5. **Prefer the more directly relevant primary source**

Example:
```
Conflict Identified:

Source 1: Charaka Samhita
"Vata individuals should eat warm, oily foods at consistent times."

Source 2: Sushruta Samhita  
"Vata imbalance requires grounding foods, but in small quantities to avoid heaviness."

Resolution:
Both are correct in context:
- Charaka emphasizes warmth and consistency (Vata prevention)
- Sushruta emphasizes grounding + portion control (Vata treatment)

Recommendation:
For Vata Prakriti (prevention): warm, oily foods at consistent times
For Vata Vikriti (treatment): grounding foods in small, frequent quantities

Sources support both recommendations depending on context.
```

---

## 10. Safety & Scope

### This System Is For

✅ Educational Ayurvedic wellness guidance  
✅ Personalized diet and lifestyle recommendations  
✅ Prakriti and Vikriti assessment support  
✅ General preventive wellness  

### This System Is NOT For

❌ Diagnosing medical diseases  
❌ Claiming to cure diseases  
❌ Prescribing Ayurvedic medicines  
❌ Prescribing Panchakarma procedures  
❌ Replacing a qualified Ayurvedic physician  
❌ Replacing conventional medical care  

### Safety Flags

**STOP and flag for physician review if:**

- Patient has a significant medical condition (diabetes, heart disease, cancer, etc.)
- Patient is allergic to common ingredients or foods
- Patient is taking pharmaceutical medications (check interactions)
- Patient is pregnant or breastfeeding
- Patient has medication interactions concerns
- Patient reports severe symptoms or emergency

**Format:**
```
🚨 SAFETY NOTE:
[Specific concern]
→ Requires review by qualified Ayurvedic physician or MD
→ Do not proceed without professional assessment
```

---

## 11. Output Format

### Structured Evidence Output

All Agent 2 and Agent 3 outputs must include:

```json
{
    "query": "[The exact retrieval query used]",
    
    "agent": "[Agent 1 | Agent 2 | Agent 3]",
    
    "relevant_sources": [
        {
            "source_book": "Charaka Samhita",
            "page": 45,
            "author": "Caraka",
            "section": "Sutra Sthana, Chapter 1",
            "content": "[direct quote or faithful summary, max 2-3 sentences]",
            "relevance": "high",
            "support_type": "directly_supported"
        }
    ],
    
    "evidence_summary": "[1-2 paragraph synthesis of all retrieved evidence]",
    
    "supported_recommendations": [
        {
            "recommendation": "[specific actionable advice]",
            "evidence_sources": ["Charaka Samhita", "Sushruta Samhita"],
            "confidence": "high | medium"
        }
    ],
    
    "unsupported_items": [
        "[Any items requested but not found in approved sources]"
    ],
    
    "safety_notes": [
        "[Any relevant safety considerations]"
    ]
}
```

### Minimal Output Example

```
Query: "Kapha balancing foods for Kapha Prakriti with Mandagni"

Evidence:

Source 1:
Book: Bhavaprakasha Nighantu
Page: 234
Author: Bhava Misra
Content: "Foods with warming, light, and dry qualities balance Kapha. 
Avoid sweet, heavy, and oily foods."

Source 2:
Book: Charaka Samhita
Page: 156 (Chikitsa Sthana)
Author: Caraka
Content: "Mandagni requires frequently eaten small meals of easily 
digestible foods with digestive spices."

Recommendation:
For Kapha Prakriti with Mandagni, prioritize:
- Warming grains (barley, millet)
- Light proteins (mung dal, split peas)
- Spices (ginger, cumin, black pepper)
- Avoid: ghee, oils, dairy, heavy grains in large quantities
- Eat 3-4 small meals spaced regularly

Sources: Bhavaprakasha Nighantu, Charaka Samhita
```

---

## 12. No Fabrication Rule

**Absolute rules:**

❌ Never invent Ayurvedic properties  
❌ Never make up dosha effects  
❌ Never fabricate agni effects  
❌ Never create prakriti characteristics from imagination  
❌ Never invent vikriti signs  
❌ Never classify foods without source  
❌ Never suggest treatments unsupported by sources  
❌ Never cite research findings that weren't retrieved  
❌ Never quote classical texts without source  
❌ Never reference page numbers you haven't verified  

**If sources are insufficient:**

```
"Insufficient evidence was retrieved from the approved AyurGenix knowledge base 
for this specific recommendation. 

Requested: [specific question]
Status: Not supported by retrieved classical sources
Action: Refer to qualified Ayurvedic physician for expert guidance"
```

---

## 13. Compliance Checklist

Before returning any recommendation from Agent 2 or Agent 3:

- [ ] Query was specific and contextual (not broad)
- [ ] Retrieved sources are from approved list
- [ ] Evidence was filtered for relevance
- [ ] Low-relevance chunks removed
- [ ] Source traceability verified (book, page, author, content)
- [ ] No broad generalizations (Vata ≠ always dry food)
- [ ] Patient context considered (Prakriti + Vikriti + Agni)
- [ ] Evidence vs. Inference clearly distinguished
- [ ] Conflicts identified and reported
- [ ] No fabricated properties or effects
- [ ] Safety concerns flagged
- [ ] Output includes sources and page numbers
- [ ] Insufficient evidence explicitly stated if needed

---

## 14. Implementation Timeline

### Phase 1: Documentation ✅
- RAG_PROTOCOL.md created and adopted
- Team training on protocol

### Phase 2: Query Templates
- Agent 1 templates (Prakriti, Vikriti, Agni)
- Agent 2 templates (Foods, Nutrition, Dietary Principles)
- Agent 3 templates (Dinacharya, Sleep, Exercise, Lifestyle)

### Phase 3: RAG Service Enhancement
- Query validation (prevents broad queries)
- Automatic relevance filtering
- Source traceability enforcement
- Structured output formatter

### Phase 4: Compliance Testing
- Test suite for protocol adherence
- Validation of source citations
- Conflict detection tests
- Safety flag detection

---

## 15. Governance

### Who Reviews RAG Outputs?

1. **Agent 2 & Agent 3 outputs** → Clinical review recommended before patient-facing
2. **Safety flags** → Always require qualified physician review
3. **Novel recommendations** → Cross-check against original sources

### When to Update This Protocol?

- If new approved sources are added
- If retrieval strategies prove insufficient
- If a major conflict emerges between sources
- If safety concerns are identified
- Annually or as directed by clinical leadership

### Questions or Conflicts?

Reference this protocol document. If interpretation is unclear, escalate to clinical leadership and Ayurvedic consulting team.

---

## Appendix A: Classical Source Abbreviations

| Abbreviation | Full Title | Author |
|---|---|---|
| CS | Charaka Samhita | Caraka |
| SS | Sushruta Samhita | Susruta |
| AH | Ashtanga Hridaya | Vagbhata |
| AS | Ashtanga Sangraha | Vagbhata |
| BPN | Bhavaprakasha Nighantu | Bhava Misra |

---

## Appendix B: Sample Retrieval Queries (Copy-Paste Ready)

### Agent 1 — Prakriti Assessment
```
"Classical Ayurvedic Prakriti assessment: characteristics and identification methods 
for Vata, Pitta, and Kapha constitution types according to physical, mental, and 
behavioral features"
```

### Agent 1 — Vikriti Assessment
```
"Vikriti assessment methodology: how to identify current dosha imbalance and 
aggravation signs distinct from constitutional Prakriti"
```

### Agent 1 — Agni Assessment
```
"Agni types and assessment: Samagni Tikshnagni Mandagni Vishamagni characteristics, 
diagnostic methods, and relationship to food digestion"
```

### Agent 2 — Diet for Pitta Vikriti
```
"Dietary principles and food properties for Pitta Vikriti according to Rasa Guna 
Virya Vipaka: cooling, pacifying foods and meal timing for Pitta imbalance"
```

### Agent 2 — Diet for Vata Vikriti
```
"Vata-balancing dietary principles: warm, grounding, nourishing foods with consistent 
meal timing; foods to avoid for Vata imbalance according to Rasa Guna Virya Vipaka"
```

### Agent 2 — Diet for Kapha Vikriti
```
"Kapha-balancing dietary principles: light, warming, stimulating foods; foods to 
limit for Kapha excess according to Rasa Guna Virya Vipaka"
```

### Agent 3 — Lifestyle for Vata
```
"Ayurvedic Dinacharya for Vata Prakriti and Vata Vikriti: daily routine, sleep, 
exercise, meal timing, stress management, and grounding practices"
```

### Agent 3 — Lifestyle for Pitta
```
"Ayurvedic Dinacharya for Pitta Prakriti and Pitta Vikriti: cooling daily routine, 
adequate rest, exercise timing, meal frequency, and stress management"
```

### Agent 3 — Lifestyle for Kapha
```
"Ayurvedic Dinacharya for Kapha Prakriti and Kapha Vikriti: stimulating daily routine, 
early waking, vigorous exercise, frequent meals, and mental engagement"
```

---

**Document Version:** 1.0  
**Last Updated:** September 6, 2026  
**Next Review:** September 6, 2027  
**Owner:** AyurGenix Clinical Leadership
