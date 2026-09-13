package com.example.vedaahar.symptoms

data class SymptomFeature(
    val key: String,
    val label: String
)

data class SymptomsAnalysisResult(
    val symptoms: Map<String, Int>,
    val reportedSymptoms: List<SymptomFeature>,
    val possibleCondition: String?,
    val completedAtMillis: Long
)

val symptomFeatures = listOf(
    SymptomFeature("acidity", "Acidity"),
    SymptomFeature("indigestion", "Indigestion"),
    SymptomFeature("headache", "Headache"),
    SymptomFeature("blurred_and_distorted_vision", "Blurred and Distorted Vision"),
    SymptomFeature("excessive_hunger", "Excessive Hunger"),
    SymptomFeature("muscle_weakness", "Muscle Weakness"),
    SymptomFeature("stiff_neck", "Stiff Neck"),
    SymptomFeature("swelling_joints", "Swelling Joints"),
    SymptomFeature("movement_stiffness", "Movement Stiffness"),
    SymptomFeature("depression", "Depression"),
    SymptomFeature("irritability", "Irritability"),
    SymptomFeature("visual_disturbances", "Visual Disturbances"),
    SymptomFeature("painful_walking", "Painful Walking"),
    SymptomFeature("abdominal_pain", "Abdominal Pain"),
    SymptomFeature("nausea", "Nausea"),
    SymptomFeature("vomiting", "Vomiting"),
    SymptomFeature("blood_in_mucus", "Blood in Mucus"),
    SymptomFeature("fatigue", "Fatigue"),
    SymptomFeature("fever", "Fever"),
    SymptomFeature("dehydration", "Dehydration"),
    SymptomFeature("loss_of_appetite", "Loss of Appetite"),
    SymptomFeature("cramping", "Cramping"),
    SymptomFeature("blood_in_stool", "Blood in Stool"),
    SymptomFeature("gnawing", "Gnawing"),
    SymptomFeature("upper_abdomain_pain", "Upper Abdominal Pain"),
    SymptomFeature("fullness_feeling", "Fullness Feeling"),
    SymptomFeature("hiccups", "Hiccups"),
    SymptomFeature("abdominal_bloating", "Abdominal Bloating"),
    SymptomFeature("heartburn", "Heartburn"),
    SymptomFeature("belching", "Belching"),
    SymptomFeature("burning_ache", "Burning Ache")
)

fun emptySymptomsInput(): Map<String, Int> {
    return symptomFeatures.associate { it.key to 0 }
}

fun generateSymptomsAnalysisResult(
    symptoms: Map<String, Int>,
    completedAtMillis: Long = System.currentTimeMillis()
): SymptomsAnalysisResult {
    validateSymptomsInput(symptoms)
    val reported = symptomFeatures.filter { symptoms.getValue(it.key) == 1 }
    return SymptomsAnalysisResult(
        symptoms = symptomFeatures.associate { it.key to symptoms.getValue(it.key) },
        reportedSymptoms = reported,
        possibleCondition = null,
        completedAtMillis = completedAtMillis
    )
}

fun validateSymptomsInput(symptoms: Map<String, Int>) {
    val expectedKeys = symptomFeatures.map { it.key }.toSet()
    require(symptoms.keys == expectedKeys) { "All 31 symptom features must be present in the expected order." }
    require(symptoms.values.all { it == 0 || it == 1 }) { "Symptoms must use binary values: Yes = 1, No = 0." }
}
