package com.example.vedaahar

import android.content.Context
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.vedaahar.dosha.DoshaAssessmentRoute
import com.example.vedaahar.dosha.DoshaResultStore
import com.example.vedaahar.dosha.RetakeDoshaAssessmentRoute
import com.example.vedaahar.doctor.ui.DoctorModuleRoute
import com.example.vedaahar.document.ui.UploadMedicalDocumentScreen
import com.example.vedaahar.document.ui.MyMedicalDocumentsScreen
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.ForestGreen

private object VedaAhaarRoute {
    const val Loading = "loading"
    const val Welcome = "welcome"
    const val Login = "login"
    const val ConsentPrivacy = "consent_privacy"
    const val PatientProfile = "patient_profile"
    const val DoshaAssessment = "dosha_assessment"
    const val DoshaRetake = "dosha_retake"
    const val DietAssessment = "diet_assessment"
    const val PatientDashboard = "patient_dashboard"
    const val Shopping = "shopping"
    const val YogaMeditation = "yoga_meditation"
    const val HealthReminder = "health_reminder"
    const val IngredientBook = "ingredient_book"
    const val SymptomsAnalysis = "symptoms_analysis"
    const val CommunityCare = "community_care"
    const val DoctorModule = "doctor_module"
    const val UploadMedicalDocument = "upload_medical_document"
    const val MyMedicalDocuments = "my_medical_documents"
}

private object OnboardingPrefs {
    const val Name = "vedaahaar_onboarding"
    const val IsLoggedIn = "is_logged_in"
    const val ConsentCompleted = "consent_completed"
    const val ProfileCompleted = "profile_completed"
    const val DoshaTestCompleted = "dosha_test_completed"
    const val PatientFullName = "patient_full_name"
}

private data class OnboardingState(
    val loading: Boolean = true,
    val isLoggedIn: Boolean = false,
    val consentCompleted: Boolean = false,
    val profileCompleted: Boolean = false,
    val doshaTestCompleted: Boolean = false
) {
    val protectedRoute: String
        get() = when {
            !isLoggedIn -> VedaAhaarRoute.Welcome
            !consentCompleted -> VedaAhaarRoute.ConsentPrivacy
            !profileCompleted -> VedaAhaarRoute.PatientProfile
            !doshaTestCompleted -> VedaAhaarRoute.DoshaAssessment
            else -> VedaAhaarRoute.PatientDashboard
        }
}

@Composable
fun VedaAhaarNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(OnboardingPrefs.Name, Context.MODE_PRIVATE)
    }
    var onboardingState by remember { mutableStateOf(OnboardingState()) }
    var dashboardTab by rememberSaveable { mutableStateOf("Home") }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    fun persistAndUpdate(
        isLoggedIn: Boolean = onboardingState.isLoggedIn,
        consentCompleted: Boolean = onboardingState.consentCompleted,
        profileCompleted: Boolean = onboardingState.profileCompleted,
        doshaTestCompleted: Boolean = onboardingState.doshaTestCompleted
    ): OnboardingState {
        val updatedState = OnboardingState(
            loading = false,
            isLoggedIn = isLoggedIn,
            consentCompleted = consentCompleted,
            profileCompleted = profileCompleted,
            doshaTestCompleted = doshaTestCompleted
        )

        preferences.edit()
            .putBoolean(OnboardingPrefs.IsLoggedIn, isLoggedIn)
            .putBoolean(OnboardingPrefs.ConsentCompleted, consentCompleted)
            .putBoolean(OnboardingPrefs.ProfileCompleted, profileCompleted)
            .putBoolean(OnboardingPrefs.DoshaTestCompleted, doshaTestCompleted)
            .apply()

        onboardingState = updatedState
        return updatedState
    }

    fun navigateToAllowedRoute(state: OnboardingState = onboardingState) {
        navController.navigate(state.protectedRoute) {
            popUpTo(0)
            launchSingleTop = true
        }
    }

    LaunchedEffect(Unit) {
        onboardingState = OnboardingState(
            loading = false,
            isLoggedIn = preferences.getBoolean(OnboardingPrefs.IsLoggedIn, false),
            consentCompleted = preferences.getBoolean(OnboardingPrefs.ConsentCompleted, false),
            profileCompleted = preferences.getBoolean(OnboardingPrefs.ProfileCompleted, false),
            doshaTestCompleted = preferences.getBoolean(OnboardingPrefs.DoshaTestCompleted, false)
        )
    }

    LaunchedEffect(onboardingState, currentRoute) {
        if (onboardingState.loading || currentRoute == null) return@LaunchedEffect

        if (currentRoute == VedaAhaarRoute.Loading) {
            navController.navigate(onboardingState.protectedRoute) {
                popUpTo(VedaAhaarRoute.Loading) { inclusive = true }
                launchSingleTop = true
            }
            return@LaunchedEffect
        }

        val routeIsPublic = currentRoute == VedaAhaarRoute.Welcome || currentRoute == VedaAhaarRoute.Login || currentRoute == VedaAhaarRoute.DoctorModule
        if (!onboardingState.isLoggedIn) {
            if (!routeIsPublic) {
                navController.navigate(VedaAhaarRoute.Welcome) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }
            return@LaunchedEffect
        }

        val allowedRoute = onboardingState.protectedRoute
        val routeIsAllowed = currentRoute == allowedRoute ||
            (
                onboardingState.profileCompleted &&
                    onboardingState.consentCompleted &&
                    onboardingState.doshaTestCompleted &&
                    (
                        currentRoute == VedaAhaarRoute.Shopping ||
                            currentRoute == VedaAhaarRoute.DoshaRetake ||
                            currentRoute == VedaAhaarRoute.DietAssessment ||
                            currentRoute == VedaAhaarRoute.YogaMeditation ||
                            currentRoute == VedaAhaarRoute.HealthReminder ||
                            currentRoute == VedaAhaarRoute.IngredientBook ||
                            currentRoute == VedaAhaarRoute.SymptomsAnalysis ||
                            currentRoute == VedaAhaarRoute.CommunityCare ||
                            currentRoute == VedaAhaarRoute.DoctorModule ||
                            currentRoute.startsWith(VedaAhaarRoute.UploadMedicalDocument) ||
                            currentRoute.startsWith(VedaAhaarRoute.MyMedicalDocuments)
                        )
                )

        if (!routeIsAllowed) {
            navController.navigate(allowedRoute) {
                popUpTo(0)
                launchSingleTop = true
            }
        }
    }

    fun handleBack() {
        when (currentRoute) {
            VedaAhaarRoute.ConsentPrivacy -> {
                persistAndUpdate(
                    isLoggedIn = false,
                    consentCompleted = false,
                    profileCompleted = false,
                    doshaTestCompleted = false
                )
                navController.navigate(VedaAhaarRoute.Login) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }

            VedaAhaarRoute.PatientProfile -> {
                persistAndUpdate(consentCompleted = false, profileCompleted = false)
                navController.navigate(VedaAhaarRoute.ConsentPrivacy) {
                    popUpTo(0)
                    launchSingleTop = true
                }
            }

            else -> {
                if (!navController.popBackStack()) {
                    navController.navigate(VedaAhaarRoute.Welcome) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            }
        }
    }

    val routesWithLocalBackButton = setOf(
        VedaAhaarRoute.Shopping,
        VedaAhaarRoute.DietAssessment,
        VedaAhaarRoute.SymptomsAnalysis,
        VedaAhaarRoute.DoshaAssessment,
        VedaAhaarRoute.DoshaRetake,
        VedaAhaarRoute.HealthReminder,
        VedaAhaarRoute.IngredientBook,
        VedaAhaarRoute.CommunityCare,
        VedaAhaarRoute.DoctorModule,
        VedaAhaarRoute.ConsentPrivacy,
        VedaAhaarRoute.UploadMedicalDocument,
        VedaAhaarRoute.MyMedicalDocuments
    )

    val isMainAppRoute = currentRoute != null &&
        currentRoute != VedaAhaarRoute.Loading &&
        currentRoute != VedaAhaarRoute.Welcome &&
        currentRoute != VedaAhaarRoute.Login &&
        currentRoute != VedaAhaarRoute.ConsentPrivacy &&
        currentRoute != VedaAhaarRoute.PatientProfile &&
        currentRoute != VedaAhaarRoute.DoshaAssessment &&
        currentRoute != VedaAhaarRoute.DoshaRetake &&
        currentRoute != VedaAhaarRoute.DietAssessment &&
        currentRoute != VedaAhaarRoute.SymptomsAnalysis

    val activeNavTab = when (currentRoute) {
        VedaAhaarRoute.PatientDashboard -> dashboardTab
        VedaAhaarRoute.Shopping -> "Shopping"
        VedaAhaarRoute.YogaMeditation, VedaAhaarRoute.HealthReminder, VedaAhaarRoute.IngredientBook -> "Wellness"
        VedaAhaarRoute.CommunityCare -> "Consult"
        VedaAhaarRoute.MyMedicalDocuments, VedaAhaarRoute.UploadMedicalDocument -> "Profile"
        else -> dashboardTab
    }

    fun handleTabSelected(tab: String) {
        dashboardTab = tab
        if (currentRoute != VedaAhaarRoute.PatientDashboard) {
            navController.navigate(VedaAhaarRoute.PatientDashboard) {
                popUpTo(VedaAhaarRoute.PatientDashboard) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    val showGlobalBackButton = currentRoute != null &&
        currentRoute != VedaAhaarRoute.Loading &&
        currentRoute != VedaAhaarRoute.Welcome &&
        currentRoute != VedaAhaarRoute.PatientDashboard &&
        currentRoute !in routesWithLocalBackButton

    Box(modifier = modifier.fillMaxSize()) {
        NavHost(
            navController = navController,
            startDestination = VedaAhaarRoute.Loading,
            modifier = Modifier
                .fillMaxSize()
                .padding(top = if (showGlobalBackButton) 52.dp else 0.dp)
        ) {
            composable(route = VedaAhaarRoute.Loading) {
                LoadingScreen()
            }

            composable(
                route = VedaAhaarRoute.Welcome,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 280, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            }
            ) {
                WelcomeScreen(
                    onStartAssessment = {
                        navController.navigate(VedaAhaarRoute.Login) {
                            launchSingleTop = true
                        }
                    },
                    onJoinAsPatient = {
                        navController.navigate(VedaAhaarRoute.Login) {
                            launchSingleTop = true
                        }
                    },
                    onJoinAsDoctor = {
                        navController.navigate(VedaAhaarRoute.DoctorModule) {
                            launchSingleTop = true
                        }
                    }
                )
            }
        composable(
            route = VedaAhaarRoute.DoctorModule,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            DoctorModuleRoute(onBack = { navController.popBackStack() })
        }

        composable(
            route = VedaAhaarRoute.Login,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            LoginScreen(
                onLogin = {
                    navigateToAllowedRoute(persistAndUpdate(isLoggedIn = true))
                },
                onCreateAccount = {
                    navigateToAllowedRoute(persistAndUpdate(isLoggedIn = true))
                }
            )
        }

        composable(
            route = VedaAhaarRoute.ConsentPrivacy,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            ConsentPrivacyScreen(
                onAgreeContinue = {
                    navigateToAllowedRoute(persistAndUpdate(consentCompleted = true))
                },
                onDecline = {
                    persistAndUpdate(
                        isLoggedIn = false,
                        consentCompleted = false,
                        profileCompleted = false,
                        doshaTestCompleted = false
                    )
                    navController.navigate(VedaAhaarRoute.Welcome) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                },
                onBack = {
                    persistAndUpdate(
                        isLoggedIn = false,
                        consentCompleted = false,
                        profileCompleted = false,
                        doshaTestCompleted = false
                    )
                    navController.navigate(VedaAhaarRoute.Login) {
                        popUpTo(0)
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = VedaAhaarRoute.PatientProfile,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
            },
            exitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popEnterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            PatientProfileScreen(
                onSaveContinue = { fullName ->
                    preferences.edit()
                        .putString(OnboardingPrefs.PatientFullName, fullName)
                        .apply()
                    navigateToAllowedRoute(persistAndUpdate(profileCompleted = true))
                }
            )
        }

        composable(
            route = VedaAhaarRoute.DoshaAssessment,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            DoshaAssessmentRoute(
                onBackToWelcome = { navController.popBackStack() },
                onContinueToDashboard = {
                    navigateToAllowedRoute(persistAndUpdate(doshaTestCompleted = true))
                }
            )
        }
        composable(
            route = VedaAhaarRoute.PatientDashboard,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            PatientDashboardScreen(
                selectedTab = dashboardTab,
                onTabSelected = ::handleTabSelected,
                onShoppingClick = {
                    navController.navigate(VedaAhaarRoute.Shopping) {
                        launchSingleTop = true
                    }
                },
                onYogaMeditationClick = {
                    navController.navigate(VedaAhaarRoute.YogaMeditation) {
                        launchSingleTop = true
                    }
                },
                onHealthReminderClick = {
                    navController.navigate(VedaAhaarRoute.HealthReminder) {
                        launchSingleTop = true
                    }
                },
                onIngredientBookClick = {
                    navController.navigate(VedaAhaarRoute.IngredientBook) {
                        launchSingleTop = true
                    }
                },
                onCommunityCareClick = {
                    navController.navigate(VedaAhaarRoute.CommunityCare) {
                        launchSingleTop = true
                    }
                },
                onRetakeDoshaClick = {
                    navController.navigate(VedaAhaarRoute.DoshaRetake) {
                        launchSingleTop = true
                    }
                },
                onDietPlanClick = {
                    navController.navigate(VedaAhaarRoute.DietAssessment) {
                        launchSingleTop = true
                    }
                },
                onSymptomsAnalysisClick = {
                    navController.navigate(VedaAhaarRoute.SymptomsAnalysis) {
                        launchSingleTop = true
                    }
                },
                onUploadDocumentClick = {
                    navController.navigate(VedaAhaarRoute.UploadMedicalDocument) {
                        launchSingleTop = true
                    }
                },
                onViewAllDocumentsClick = {
                    navController.navigate(VedaAhaarRoute.MyMedicalDocuments) {
                        launchSingleTop = true
                    }
                },
                onViewDocumentClick = { doc ->
                    navController.navigate("${VedaAhaarRoute.MyMedicalDocuments}?docId=${doc.id}") {
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = VedaAhaarRoute.UploadMedicalDocument,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            UploadMedicalDocumentScreen(
                onBack = { navController.popBackStack() },
                onNavigateToMyDocuments = {
                    navController.navigate(VedaAhaarRoute.MyMedicalDocuments) {
                        popUpTo(VedaAhaarRoute.UploadMedicalDocument) { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onViewDocument = { doc ->
                    navController.navigate("${VedaAhaarRoute.MyMedicalDocuments}?docId=${doc.id}") {
                        popUpTo(VedaAhaarRoute.UploadMedicalDocument) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = "${VedaAhaarRoute.MyMedicalDocuments}?docId={docId}",
            arguments = listOf(
                navArgument("docId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                }
            ),
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) { backStackEntry ->
            val docId = backStackEntry.arguments?.getString("docId")
            MyMedicalDocumentsScreen(
                onBack = { navController.popBackStack() },
                onUploadClick = {
                    navController.navigate(VedaAhaarRoute.UploadMedicalDocument) {
                        launchSingleTop = true
                    }
                },
                initialViewDocId = docId
            )
        }

        composable(
            route = VedaAhaarRoute.MyMedicalDocuments,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            MyMedicalDocumentsScreen(
                onBack = { navController.popBackStack() },
                onUploadClick = {
                    navController.navigate(VedaAhaarRoute.UploadMedicalDocument) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = VedaAhaarRoute.DietAssessment,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            val context = LocalContext.current
            val currentDosha = remember { DoshaResultStore.current(context)?.profileName ?: "Vata-Pitta" }
            DietAssessmentScreen(
                prakriti = currentDosha,
                onBack = { navController.popBackStack() },
                onEditPrakriti = {
                    navController.navigate(VedaAhaarRoute.DoshaRetake) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = VedaAhaarRoute.SymptomsAnalysis,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            SymptomsAnalysisScreen(
                onBack = { navController.popBackStack() },
                onConsultDoctor = {
                    navController.navigate(VedaAhaarRoute.CommunityCare) {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = VedaAhaarRoute.DoshaRetake,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            RetakeDoshaAssessmentRoute(
                onBackToDashboard = { navController.popBackStack() },
                onContinueToDashboard = {
                    navController.navigate(VedaAhaarRoute.PatientDashboard) {
                        popUpTo(VedaAhaarRoute.DoshaRetake) { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable(
            route = VedaAhaarRoute.YogaMeditation,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            YogaMeditationScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = VedaAhaarRoute.HealthReminder,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            HealthReminderScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = VedaAhaarRoute.IngredientBook,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            IngredientBookScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = VedaAhaarRoute.CommunityCare,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            CommunityCareScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = VedaAhaarRoute.Shopping,
            enterTransition = {
                fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)) +
                    slideIntoContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Left,
                        animationSpec = tween(durationMillis = 360, easing = FastOutSlowInEasing)
                    )
            },
            popExitTransition = {
                fadeOut(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)) +
                    slideOutOfContainer(
                        towards = AnimatedContentTransitionScope.SlideDirection.Right,
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    )
            }
        ) {
            ShoppingScreen(
                onBack = { navController.popBackStack() }
            )
        }
        }

        AnimatedVisibility(
            visible = showGlobalBackButton,
            modifier = Modifier
                .align(Alignment.TopStart)
                .statusBarsPadding()
                .padding(start = 16.dp, top = 12.dp)
                .zIndex(2f),
            enter = fadeIn(animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)),
            exit = fadeOut(animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing))
        ) {
            BackButton(onClick = ::handleBack)
        }

        if (isMainAppRoute) {
            VedamritBottomNavigationBar(
                selectedLabel = activeNavTab,
                onTabSelected = ::handleTabSelected,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Surface(modifier = Modifier.fillMaxSize(), color = Cream) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = ForestGreen)
        }
    }
}

