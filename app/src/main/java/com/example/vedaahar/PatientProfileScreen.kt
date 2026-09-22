package com.example.vedaahar

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.agentic.AgenticApiResult
import com.example.vedaahar.profile.PatientProfileGateway
import com.example.vedaahar.profile.PatientProfileLoadResult
import com.example.vedaahar.profile.StoredPatientProfile
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val ProfileGreen = Color(0xFF7A4E32)
private val ProfileDeepGreen = Color(0xFF26332D)
private val ProfileCard = Color(0xFFFFFFFF)
private val ProfileFieldTint = Color(0xFFFFFFFF)
private val ProfileGold = Color(0xFFF1E6DC)
private val ProfileBorder = Color(0xFFF1E6DC)
private val ProfileError = Color(0xFF7A4E32)

@Composable
fun PatientProfileScreen(
    onSaveContinue: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PatientProfileViewModel
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current

    val fullNameFocus = remember { FocusRequester() }
    val ageFocus = remember { FocusRequester() }
    val heightFocus = remember { FocusRequester() }
    val weightFocus = remember { FocusRequester() }

    LaunchedEffect(viewModel) {
        viewModel.loadProfile()
    }

    LaunchedEffect(uiState.submitAttempts) {
        if (uiState.submitAttempts > 0 && !uiState.isFormValid) {
            scrollState.animateScrollTo(scrollTargetFor(uiState.firstInvalidField))
        }
    }

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(modifier = Modifier.fillMaxSize()) {
            ProfileBackground()
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 18.dp, vertical = 16.dp)
            ) {
                ProfileHeaderCard()
                Spacer(modifier = Modifier.height(18.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(12.dp, RoundedCornerShape(24.dp), ambientColor = SageGreen.copy(alpha = 0.12f), spotColor = ProfileGold.copy(alpha = 0.08f))
                        .clip(RoundedCornerShape(24.dp))
                        .background(ProfileCard.copy(alpha = 0.94f))
                        .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.72f)), RoundedCornerShape(24.dp))
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ValidatedField(
                        field = PatientProfileField.FullName,
                        label = "Full Name",
                        value = uiState.fullName,
                        onValueChange = { viewModel.updateField(PatientProfileField.FullName, it) },
                        placeholder = "Enter your full name",
                        uiState = uiState,
                        focusRequester = fullNameFocus,
                        imeAction = ImeAction.Next,
                        onNext = { ageFocus.requestFocus() }
                    )

                    ResponsivePair {
                        ValidatedField(
                            field = PatientProfileField.Age,
                            label = "Age",
                            value = uiState.age,
                            onValueChange = { viewModel.updateField(PatientProfileField.Age, it) },
                            placeholder = "Enter your age",
                            uiState = uiState,
                            keyboardType = KeyboardType.Number,
                            focusRequester = ageFocus,
                            imeAction = ImeAction.Next,
                            onNext = { heightFocus.requestFocus() },
                            modifier = it
                        )
                        ValidatedDropdown(
                            field = PatientProfileField.Gender,
                            label = "Gender",
                            value = uiState.gender,
                            options = listOf("Male", "Female", "Other"),
                            placeholder = "Select gender",
                            onValueChange = { viewModel.updateField(PatientProfileField.Gender, it) },
                            uiState = uiState,
                            modifier = it
                        )
                    }

                    ResponsivePair {
                        ValidatedField(
                            field = PatientProfileField.Height,
                            label = "Height (cm)",
                            value = uiState.height,
                            onValueChange = { viewModel.updateField(PatientProfileField.Height, it) },
                            placeholder = "Enter your height",
                            uiState = uiState,
                            keyboardType = KeyboardType.Decimal,
                            focusRequester = heightFocus,
                            imeAction = ImeAction.Next,
                            onNext = { weightFocus.requestFocus() },
                            modifier = it
                        )
                        ValidatedField(
                            field = PatientProfileField.Weight,
                            label = "Weight (kg)",
                            value = uiState.weight,
                            onValueChange = { viewModel.updateField(PatientProfileField.Weight, it) },
                            placeholder = "Enter your weight",
                            uiState = uiState,
                            keyboardType = KeyboardType.Decimal,
                            focusRequester = weightFocus,
                            imeAction = ImeAction.Done,
                            onDone = { focusManager.clearFocus() },
                            modifier = it
                        )
                    }

                    ProfileSectionTitle("Diet and goals")
                    ResponsivePair {
                        ValidatedDropdown(
                            field = PatientProfileField.DietaryPreference,
                            label = "Dietary preference",
                            value = uiState.dietaryPreference,
                            options = PatientProfileOptions.DietaryPreferences,
                            placeholder = "Select preference",
                            onValueChange = { viewModel.updateField(PatientProfileField.DietaryPreference, it) },
                            uiState = uiState,
                            modifier = it
                        )
                        ValidatedDropdown(
                            field = PatientProfileField.Goal,
                            label = "Wellness goal",
                            value = uiState.goal,
                            options = PatientProfileOptions.Goals,
                            placeholder = "Select goal",
                            onValueChange = { viewModel.updateField(PatientProfileField.Goal, it) },
                            uiState = uiState,
                            modifier = it
                        )
                    }

                    ResponsivePair {
                        ValidatedDropdown(
                            field = PatientProfileField.ActivityLevel,
                            label = "Activity level",
                            value = uiState.activityLevel,
                            options = PatientProfileOptions.ActivityLevels,
                            placeholder = "Select activity",
                            onValueChange = { viewModel.updateField(PatientProfileField.ActivityLevel, it) },
                            uiState = uiState,
                            modifier = it
                        )
                        ValidatedDropdown(
                            field = PatientProfileField.PregnancyInformation,
                            label = "Pregnancy information",
                            value = uiState.pregnancyInformation,
                            options = PatientProfileOptions.PregnancyInformation,
                            placeholder = "Select status",
                            onValueChange = { viewModel.updateField(PatientProfileField.PregnancyInformation, it) },
                            uiState = uiState,
                            modifier = it
                        )
                    }

                    ProfileSectionTitle("Safety context")
                    TagInputField(
                        label = "Allergies",
                        values = uiState.allergies,
                        placeholder = "Add allergy or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.Allergies, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.Allergies, it) }
                    )
                    TagInputField(
                        label = "Food intolerances",
                        values = uiState.foodIntolerances,
                        placeholder = "Add intolerance or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.FoodIntolerances, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.FoodIntolerances, it) }
                    )
                    TagInputField(
                        label = "Foods to avoid",
                        values = uiState.foodsToAvoid,
                        placeholder = "Add food or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.FoodsToAvoid, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.FoodsToAvoid, it) }
                    )
                    TagInputField(
                        label = "Health conditions",
                        values = uiState.healthConditions,
                        placeholder = "Add condition or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.HealthConditions, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.HealthConditions, it) }
                    )
                    TagInputField(
                        label = "Medicine allergies",
                        values = uiState.medicineAllergies,
                        placeholder = "Add medicine allergy or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.MedicineAllergies, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.MedicineAllergies, it) }
                    )
                    TagInputField(
                        label = "Current medicines",
                        values = uiState.currentMedicines,
                        placeholder = "Add medicine or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.CurrentMedicines, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.CurrentMedicines, it) }
                    )
                    TagInputField(
                        label = "Medication restrictions",
                        values = uiState.medicationRestrictions,
                        placeholder = "Add restriction or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.MedicationRestrictions, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.MedicationRestrictions, it) }
                    )
                    TagInputField(
                        label = "Doctor restrictions",
                        values = uiState.doctorRestrictions,
                        placeholder = "Add doctor instruction or leave empty if none",
                        onAdd = { viewModel.addListValue(PatientProfileField.DoctorRestrictions, it) },
                        onRemove = { viewModel.removeListValue(PatientProfileField.DoctorRestrictions, it) }
                    )

                    uiState.syncMessage?.let { message ->
                        ProfileStatusText(message = message, isError = false)
                    }
                    uiState.saveError?.let { message ->
                        ProfileStatusText(message = message, isError = true)
                    }

                    SaveContinueButton(
                        formValid = uiState.isFormValid,
                        saving = uiState.saving,
                        onClick = {
                            focusManager.clearFocus()
                            if (!uiState.isFormValid) {
                                viewModel.submit()
                                scope.launch { scrollState.animateScrollTo(scrollTargetFor(viewModel.uiState.value.firstInvalidField)) }
                            } else {
                                viewModel.saveProfile(onSaveContinue)
                            }
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun ProfileStatusText(message: String, isError: Boolean) {
    Text(
        text = message,
        color = if (isError) ProfileError else DarkForestGreen,
        style = MaterialTheme.typography.bodySmall.copy(
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 0.sp
        )
    )
}

private fun scrollTargetFor(field: PatientProfileField?): Int {
    return when (field) {
        PatientProfileField.FullName -> 0
        PatientProfileField.Age, PatientProfileField.Gender -> 160
        PatientProfileField.Height, PatientProfileField.Weight -> 300
        PatientProfileField.DietaryPreference, PatientProfileField.Goal -> 440
        PatientProfileField.ActivityLevel, PatientProfileField.PregnancyInformation -> 560
        PatientProfileField.Allergies,
        PatientProfileField.FoodIntolerances,
        PatientProfileField.FoodsToAvoid,
        PatientProfileField.HealthConditions,
        PatientProfileField.MedicineAllergies,
        PatientProfileField.CurrentMedicines,
        PatientProfileField.MedicationRestrictions,
        PatientProfileField.DoctorRestrictions -> 680
        null -> 0
    }
}

@Composable
private fun ProfileHeaderCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(14.dp, RoundedCornerShape(24.dp), ambientColor = ProfileGreen.copy(alpha = 0.24f), spotColor = ProfileGreen.copy(alpha = 0.18f))
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 20.dp, bottomEnd = 20.dp))
            .background(Brush.horizontalGradient(listOf(ProfileGreen, ProfileDeepGreen)))
            .padding(18.dp)
    ) {
        FloatingProfileLeaves(modifier = Modifier.align(Alignment.TopEnd).size(118.dp), alpha = 0.14f)
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            PatientIcon()
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Complete Your Patient Profile",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp,
                        lineHeight = 28.sp
                    )
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Finish this final onboarding step to unlock your dashboard.",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = PureWhite.copy(alpha = 0.9f),
                        lineHeight = 21.sp
                    )
                )
            }
        }
    }
}

@Composable
private fun PatientIcon() {
    Box(
        modifier = Modifier
            .size(48.dp)
            .background(PureWhite.copy(alpha = 0.18f), CircleShape)
            .border(BorderStroke(1.dp, PureWhite.copy(alpha = 0.34f)), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(27.dp)) {
            drawCircle(PureWhite, radius = size.minDimension * 0.17f, center = Offset(size.width * 0.5f, size.height * 0.28f))
            drawArc(
                color = PureWhite,
                startAngle = 205f,
                sweepAngle = 130f,
                useCenter = false,
                topLeft = Offset(size.width * 0.18f, size.height * 0.42f),
                size = Size(size.width * 0.64f, size.height * 0.52f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.2f, cap = StrokeCap.Round)
            )
            drawLine(PureWhite, Offset(size.width * 0.74f, size.height * 0.26f), Offset(size.width * 0.74f, size.height * 0.48f), strokeWidth = 3f, cap = StrokeCap.Round)
            drawLine(PureWhite, Offset(size.width * 0.63f, size.height * 0.37f), Offset(size.width * 0.85f, size.height * 0.37f), strokeWidth = 3f, cap = StrokeCap.Round)
        }
    }
}

@Composable
private fun ResponsivePair(content: @Composable (Modifier) -> Unit) {
    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
        if (maxWidth >= 320.dp) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.fillMaxWidth()) {
                content(Modifier.weight(1f))
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                content(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun ValidatedField(
    field: PatientProfileField,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    uiState: PatientProfileUiState,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    ProfileField(
        field = field,
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        error = uiState.errors[field],
        showError = uiState.shouldShowError(field),
        valid = uiState.isValid(field),
        submitAttempts = uiState.submitAttempts,
        keyboardType = keyboardType,
        focusRequester = focusRequester,
        imeAction = imeAction,
        onNext = onNext,
        onDone = onDone,
        modifier = modifier
    )
}

@Composable
private fun OptionalField(
    field: PatientProfileField,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    ProfileField(
        field = field,
        label = label,
        value = value,
        onValueChange = onValueChange,
        placeholder = placeholder,
        error = null,
        showError = false,
        valid = value.isNotBlank(),
        submitAttempts = 0,
        keyboardType = keyboardType,
        focusRequester = focusRequester,
        imeAction = imeAction,
        onNext = onNext,
        onDone = onDone,
        modifier = modifier
    )
}

@Composable
private fun ProfileField(
    field: PatientProfileField,
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    error: String?,
    showError: Boolean,
    valid: Boolean,
    submitAttempts: Int,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Next,
    onNext: () -> Unit = {},
    onDone: () -> Unit = {}
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val shake = remember(field) { Animatable(0f) }
    val borderColor = when {
        showError -> ProfileError
        focused || valid -> ProfileGreen
        else -> ProfileBorder
    }
    val glow by animateDpAsState(
        targetValue = if (focused || valid) 5.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.74f, stiffness = 360f),
        label = "profile-field-glow"
    )

    LaunchedEffect(submitAttempts, showError) {
        if (submitAttempts > 0 && showError) {
            shake.snapTo(0f)
            listOf(-8f, 8f, -6f, 6f, -3f, 3f, 0f).forEach { offset ->
                shake.animateTo(offset, animationSpec = tween(34))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shake.value.roundToInt(), 0) }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (showError) ProfileError else DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        )
        Spacer(modifier = Modifier.height(7.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .shadow(glow, RoundedCornerShape(18.dp), ambientColor = SageGreen.copy(alpha = 0.14f), spotColor = SageGreen.copy(alpha = 0.12f)),
            placeholder = {
                Text(text = placeholder, color = SoftBlueGray.copy(alpha = 0.68f), fontSize = 14.sp)
            },
            trailingIcon = { ValidationTrailingIcon(showError = showError, valid = valid) },
            interactionSource = interactionSource,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onDone() }),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            isError = showError,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DarkForestGreen,
                unfocusedTextColor = DarkForestGreen,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                errorBorderColor = ProfileError,
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = ProfileFieldTint,
                cursorColor = ProfileGreen
            )
        )
        if (showError && error != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = error,
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ProfileError,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp
                )
            )
        }
    }
}

@Composable
private fun ValidatedDropdown(
    field: PatientProfileField,
    label: String,
    value: String,
    options: List<String>,
    placeholder: String,
    onValueChange: (String) -> Unit,
    uiState: PatientProfileUiState,
    modifier: Modifier = Modifier
) {
    val showError = uiState.shouldShowError(field)
    val valid = uiState.isValid(field)
    val shake = remember(field) { Animatable(0f) }
    val borderColor = when {
        showError -> ProfileError
        valid -> ProfileGreen
        else -> ProfileBorder
    }
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.submitAttempts, showError) {
        if (uiState.submitAttempts > 0 && showError) {
            shake.snapTo(0f)
            listOf(-8f, 8f, -6f, 6f, -3f, 3f, 0f).forEach { offset ->
                shake.animateTo(offset, animationSpec = tween(34))
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .offset { IntOffset(shake.value.roundToInt(), 0) }
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = if (showError) ProfileError else DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        )
        Spacer(modifier = Modifier.height(7.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(ProfileFieldTint)
                .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(18.dp))
                .clickable { expanded = true }
                .padding(horizontal = 16.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = value.ifBlank { placeholder },
                    color = if (value.isBlank()) SoftBlueGray.copy(alpha = 0.68f) else DarkForestGreen,
                    modifier = Modifier.weight(1f)
                )
                Text("v", color = DarkForestGreen, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.width(10.dp))
                ValidationTrailingIcon(showError = showError, valid = valid)
            }
            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option, color = DarkForestGreen) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        }
                    )
                }
            }
        }
        if (showError) {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = uiState.errors[field].orEmpty(),
                style = MaterialTheme.typography.labelSmall.copy(
                    color = ProfileError,
                    fontSize = 12.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.sp
                )
            )
        }
    }
}

@Composable
private fun ProfileSectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall.copy(
            color = ProfileDeepGreen,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp,
            letterSpacing = 0.sp
        )
    )
}

@Composable
private fun TagInputField(
    label: String,
    values: List<String>,
    placeholder: String,
    onAdd: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    var draft by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                color = DarkForestGreen,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.2.sp
            )
        )
        Spacer(modifier = Modifier.height(7.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(text = placeholder, color = SoftBlueGray.copy(alpha = 0.68f), fontSize = 14.sp)
                },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = DarkForestGreen,
                    unfocusedTextColor = DarkForestGreen,
                    focusedBorderColor = ProfileGreen,
                    unfocusedBorderColor = ProfileBorder,
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = ProfileFieldTint,
                    cursorColor = ProfileGreen
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = {
                        onAdd(draft)
                        draft = ""
                    }
                )
            )
            Button(
                onClick = {
                    onAdd(draft)
                    draft = ""
                },
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = ProfileDeepGreen),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 12.dp)
            ) {
                Text("Add", color = PureWhite, fontWeight = FontWeight.Bold)
            }
        }
        if (values.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                values.forEach { value ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(ProfileGold.copy(alpha = 0.62f))
                            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.8f)), RoundedCornerShape(14.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = value,
                            color = DarkForestGreen,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium.copy(letterSpacing = 0.sp)
                        )
                        Text(
                            text = "Remove",
                            color = ProfileError,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { onRemove(value) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ValidationTrailingIcon(showError: Boolean, valid: Boolean) {
    when {
        showError -> Box(
            modifier = Modifier
                .size(20.dp)
                .background(ProfileError.copy(alpha = 0.12f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text("!", color = ProfileError, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }

        valid -> Box(
            modifier = Modifier
                .size(20.dp)
                .background(ProfileGreen.copy(alpha = 0.14f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.Check, contentDescription = null, tint = ProfileGreen, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun SaveContinueButton(formValid: Boolean, saving: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.65f, stiffness = 420f),
        label = "save-profile-scale"
    )
    val gradient = if (formValid) {
        Brush.horizontalGradient(listOf(ProfileDeepGreen, ProfileGreen))
    } else {
        Brush.horizontalGradient(listOf(ProfileDeepGreen.copy(alpha = 0.58f), ProfileGreen.copy(alpha = 0.58f)))
    }

    Button(
        onClick = onClick,
        enabled = !saving,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .shadow(12.dp, RoundedCornerShape(50), ambientColor = ProfileGreen.copy(alpha = 0.28f), spotColor = ProfileGreen.copy(alpha = 0.22f)),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(gradient, RoundedCornerShape(50)),
            contentAlignment = Alignment.Center
        ) {
            FloatingProfileLeaves(modifier = Modifier.align(Alignment.CenterEnd).size(82.dp), alpha = if (formValid) 0.12f else 0.06f)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = when {
                        saving -> "Saving..."
                        formValid -> "Save and Continue"
                        else -> "Complete Required Fields"
                    },
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = PureWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    )
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.Filled.Check, contentDescription = null, tint = PureWhite, modifier = Modifier.size(18.dp))
            }
        }
    }
}

@Composable
private fun ProfileBackground() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawRect(Color(0xFFFFFFFF))
        drawCircle(
            brush = Brush.radialGradient(listOf(ProfileGold.copy(alpha = 0.18f), Color.Transparent)),
            radius = size.width * 0.58f,
            center = Offset(size.width * 0.04f, size.height * 0.12f)
        )
        drawCircle(
            brush = Brush.radialGradient(listOf(SageGreen.copy(alpha = 0.16f), Color.Transparent)),
            radius = size.width * 0.62f,
            center = Offset(size.width * 0.94f, size.height * 0.84f)
        )
        repeat(8) { index ->
            val x = size.width * (0.08f + index * 0.13f)
            val y = size.height * (0.12f + (index % 4) * 0.22f)
            drawOval(
                color = SageGreen.copy(alpha = 0.045f),
                topLeft = Offset(x, y),
                size = Size(size.width * 0.11f, size.width * 0.2f)
            )
        }
    }
}

@Composable
private fun FloatingProfileLeaves(modifier: Modifier = Modifier, alpha: Float) {
    Canvas(modifier = modifier) {
        repeat(5) { index ->
            val left = size.width * (0.18f + index * 0.12f)
            val top = size.height * (0.16f + (index % 2) * 0.18f)
            drawOval(
                color = PureWhite.copy(alpha = alpha),
                topLeft = Offset(left, top),
                size = Size(size.width * 0.17f, size.height * 0.32f)
            )
            drawLine(
                color = ProfileGold.copy(alpha = alpha * 0.9f),
                start = Offset(left + size.width * 0.08f, top + size.height * 0.25f),
                end = Offset(size.width * 0.86f, size.height * 0.82f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun PatientProfileScreenPreview() {
    PatientProfileScreen(
        onSaveContinue = { _ -> },
        viewModel = PatientProfileViewModel(PreviewPatientProfileGateway)
    )
}

private object PreviewPatientProfileGateway : PatientProfileGateway {
    override suspend fun loadProfile(): PatientProfileLoadResult {
        return PatientProfileLoadResult.Cache(
            StoredPatientProfile(
                fullName = "",
                age = "",
                gender = "",
                heightCm = "",
                weightKg = ""
            ),
            "Preview"
        )
    }

    override suspend fun saveProfile(profile: StoredPatientProfile): AgenticApiResult<StoredPatientProfile> {
        return AgenticApiResult.Success(profile)
    }
}



