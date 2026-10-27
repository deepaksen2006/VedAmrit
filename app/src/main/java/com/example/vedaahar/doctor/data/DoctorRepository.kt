package com.example.vedaahar.doctor.data

import com.example.vedaahar.doctor.domain.DoctorConsultation
import com.example.vedaahar.doctor.domain.DoctorDietPlanDraft

class DoctorRepository {
    fun consultations(): List<DoctorConsultation> = listOf(
        DoctorConsultation(101, "Arjun Kumar", "Vata-Pitta", "Acidity, stress, irregular sleep", "New"),
        DoctorConsultation(102, "Priya Nair", "Kapha", "Weight gain, slow digestion", "Follow-up")
    )

    fun recentDietPlans(): List<DoctorDietPlanDraft> = listOf(
        DoctorDietPlanDraft(
            patientName = "Arjun Kumar",
            prakriti = "Vata-Pitta",
            vikriti = "Pitta ↑",
            agni = "Tikshna Agni",
            ama = "Mild",
            breakfast = "Moong chilla with coriander chutney",
            lunch = "Rice, moong dal, bottle gourd sabzi",
            dinner = "Light khichdi before 8:30 PM",
            foodsToEat = "Moong dal, bottle gourd, cucumber, coconut water",
            foodsToAvoid = "Pickles, fried snacks, excess tea",
            hydrationAdvice = "Sip room-temperature coriander-fennel water",
            yogaRecommendation = "Sheetali pranayama and gentle twists",
            sleepAdvice = "Sleep before 10:30 PM"
        )
    )
}
