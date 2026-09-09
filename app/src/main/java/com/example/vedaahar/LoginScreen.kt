package com.example.vedaahar

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.vedaahar.ui.theme.BeigeBorder
import com.example.vedaahar.ui.theme.Cream
import com.example.vedaahar.ui.theme.DarkForestGreen
import com.example.vedaahar.ui.theme.ForestGreen
import com.example.vedaahar.ui.theme.PureWhite
import com.example.vedaahar.ui.theme.SageGreen
import com.example.vedaahar.ui.theme.SoftBlueGray

private val LoginGold = Color(0xFFE6C978)
private val LoginCard = Color(0xFFFFFBF4)
private val LoginErrorRed = Color(0xFFC8483D)
private val CinzelDecorative = FontFamily(Font(R.font.cinzel_decorative_regular))

private fun validateLoginIdentifier(value: String): String? {
    if (value.isBlank()) return "Enter email or phone number"

    return if (value.contains("@")) {
        val emailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
        if (emailPattern.matches(value)) null else "Enter a valid email address"
    } else {
        val phonePattern = Regex("^[6-9][0-9]{9}$")
        if (phonePattern.matches(value)) null else "Enter a valid 10-digit phone number"
    }
}

private val LoginEyeIcon: ImageVector = ImageVector.Builder(
    name = "LoginEyeIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(3.5f, 12f)
        curveTo(5.6f, 8.5f, 8.5f, 6.8f, 12f, 6.8f)
        curveTo(15.5f, 6.8f, 18.4f, 8.5f, 20.5f, 12f)
        curveTo(18.4f, 15.5f, 15.5f, 17.2f, 12f, 17.2f)
        curveTo(8.5f, 17.2f, 5.6f, 15.5f, 3.5f, 12f)
        moveTo(9.4f, 12f)
        curveTo(9.4f, 10.6f, 10.6f, 9.4f, 12f, 9.4f)
        curveTo(13.4f, 9.4f, 14.6f, 10.6f, 14.6f, 12f)
        curveTo(14.6f, 13.4f, 13.4f, 14.6f, 12f, 14.6f)
        curveTo(10.6f, 14.6f, 9.4f, 13.4f, 9.4f, 12f)
    }
}.build()

private val LoginEyeOffIcon: ImageVector = ImageVector.Builder(
    name = "LoginEyeOffIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f
).apply {
    path(
        fill = SolidColor(Color.Transparent),
        stroke = SolidColor(Color.Black),
        strokeLineWidth = 1.9f,
        strokeLineCap = StrokeCap.Round,
        strokeLineJoin = StrokeJoin.Round
    ) {
        moveTo(4f, 4f)
        lineTo(20f, 20f)
        moveTo(9.4f, 5.5f)
        curveTo(10.2f, 5.3f, 11.1f, 5.2f, 12f, 5.2f)
        curveTo(15.8f, 5.2f, 18.9f, 7.4f, 21f, 12f)
        curveTo(20.3f, 13.5f, 19.4f, 14.8f, 18.4f, 15.8f)
        moveTo(14.2f, 18.5f)
        curveTo(13.5f, 18.7f, 12.8f, 18.8f, 12f, 18.8f)
        curveTo(8.2f, 18.8f, 5.1f, 16.6f, 3f, 12f)
        curveTo(3.8f, 10.3f, 4.8f, 8.9f, 6f, 7.8f)
        moveTo(9.9f, 9.9f)
        curveTo(9.5f, 10.5f, 9.3f, 11.2f, 9.3f, 12f)
        curveTo(9.3f, 13.5f, 10.5f, 14.7f, 12f, 14.7f)
        curveTo(12.8f, 14.7f, 13.5f, 14.5f, 14.1f, 14.1f)
    }
}.build()

@Composable
fun LoginScreen(
    onLogin: () -> Unit,
    onCreateAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var emailTouched by remember { mutableStateOf(false) }
    val emailError = validateLoginIdentifier(email)
    val isEmailValid = emailError == null
    val isLoginEnabled = isEmailValid && password.isNotEmpty()

    Surface(modifier = modifier.fillMaxSize(), color = Cream) {
        Box(modifier = Modifier.fillMaxSize()) {
            LoginBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .statusBarsPadding()
                    .imePadding()
                    .padding(horizontal = 22.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(22.dp))
                LoginBrandHeader()
                Spacer(modifier = Modifier.height(24.dp))

                LoginCard(
                    email = email,
                    onEmailChange = {
                        emailTouched = true
                        email = it.filterNot(Char::isWhitespace)
                    },
                    emailError = emailError,
                    showEmailError = emailTouched && emailError != null,
                    isEmailValid = emailTouched && isEmailValid,
                    password = password,
                    onPasswordChange = { password = it },
                    passwordVisible = passwordVisible,
                    onPasswordVisibilityChange = { passwordVisible = !passwordVisible },
                    onLogin = onLogin,
                    isLoginEnabled = isLoginEnabled,
                    onCreateAccount = onCreateAccount
                )

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun LoginBrandHeader() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "VedAmrit",
            style = MaterialTheme.typography.titleLarge.copy(
                color = DarkForestGreen,
                fontFamily = CinzelDecorative,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Normal
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "॥ आयुर्वेदा अमृतं ॥",
            style = MaterialTheme.typography.bodyLarge.copy(
                color = DarkForestGreen,
                fontSize = 18.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Medium
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LoginCard(
    email: String,
    onEmailChange: (String) -> Unit,
    emailError: String?,
    showEmailError: Boolean,
    isEmailValid: Boolean,
    password: String,
    onPasswordChange: (String) -> Unit,
    passwordVisible: Boolean,
    onPasswordVisibilityChange: () -> Unit,
    onLogin: () -> Unit,
    isLoginEnabled: Boolean,
    onCreateAccount: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(28.dp),
                ambientColor = LoginGold.copy(alpha = 0.1f),
                spotColor = SageGreen.copy(alpha = 0.08f)
            )
            .clip(RoundedCornerShape(28.dp))
            .background(LoginCard)
            .border(BorderStroke(1.dp, BeigeBorder.copy(alpha = 0.9f)), RoundedCornerShape(28.dp))
            .padding(20.dp)
    ) {
        LoginField(
            label = "Email / Phone Number",
            value = email,
            onValueChange = onEmailChange,
            placeholder = "Enter your email or phone number",
            leadingIcon = { Icon(Icons.Filled.Email, contentDescription = null) },
            trailingIcon = if (isEmailValid) {
                {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else {
                null
            },
            keyboardType = if (email.isNotEmpty() && email.all(Char::isDigit)) KeyboardType.Phone else KeyboardType.Email,
            errorText = emailError,
            showError = showEmailError,
            isValid = isEmailValid
        )

        Spacer(modifier = Modifier.height(16.dp))

        LoginField(
            label = "Password",
            value = password,
            onValueChange = onPasswordChange,
            placeholder = "Enter your password",
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            trailingIcon = {
                IconButton(onClick = onPasswordVisibilityChange) {
                    Icon(
                        imageVector = if (passwordVisible) LoginEyeOffIcon else LoginEyeIcon,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardType = KeyboardType.Password
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Forgot Password?",
            style = MaterialTheme.typography.bodyMedium.copy(
                color = ForestGreen,
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.align(Alignment.End)
        )

        Spacer(modifier = Modifier.height(22.dp))
        PremiumLoginButton(text = "Login", enabled = isLoginEnabled, onClick = onLogin)
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                style = MaterialTheme.typography.bodyMedium.copy(color = SoftBlueGray)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Row(
                modifier = Modifier.clickable(onClick = onCreateAccount),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Create Account",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = ForestGreen,
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = ForestGreen,
                    modifier = Modifier.size(17.dp)
                )
            }
        }
    }
}

@Composable
private fun LoginField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    leadingIcon: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    trailingIcon: (@Composable () -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardType: KeyboardType = KeyboardType.Text,
    errorText: String? = null,
    showError: Boolean = false,
    isValid: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val focused by interactionSource.collectIsFocusedAsState()
    val glow by animateDpAsState(
        targetValue = if (focused || isValid) 4.dp else 0.dp,
        animationSpec = spring(dampingRatio = 0.72f, stiffness = 360f),
        label = "field-glow"
    )
    val borderColor = when {
        showError -> LoginErrorRed
        isValid -> SageGreen
        else -> BeigeBorder
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(
                color = DarkForestGreen,
                fontWeight = FontWeight.Bold
            )
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    glow,
                    RoundedCornerShape(18.dp),
                    ambientColor = if (showError) LoginErrorRed.copy(alpha = 0.1f) else SageGreen.copy(alpha = 0.12f),
                    spotColor = if (showError) LoginErrorRed.copy(alpha = 0.08f) else SageGreen.copy(alpha = 0.1f)
                ),
            placeholder = {
                Text(
                    text = placeholder,
                    color = SoftBlueGray,
                    maxLines = 1,
                    softWrap = false
                )
            },
            leadingIcon = leadingIcon,
            trailingIcon = trailingIcon,
            interactionSource = interactionSource,
            isError = showError,
            visualTransformation = visualTransformation,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            singleLine = true,
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = DarkForestGreen,
                unfocusedTextColor = DarkForestGreen,
                focusedLeadingIconColor = ForestGreen,
                unfocusedLeadingIconColor = SoftBlueGray,
                focusedTrailingIconColor = ForestGreen,
                unfocusedTrailingIconColor = SoftBlueGray,
                errorBorderColor = LoginErrorRed,
                errorLeadingIconColor = LoginErrorRed,
                errorTrailingIconColor = LoginErrorRed,
                focusedBorderColor = borderColor,
                unfocusedBorderColor = borderColor,
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                errorContainerColor = PureWhite,
                cursorColor = ForestGreen
            )
        )
        if (showError && errorText != null) {
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(RoundedCornerShape(50))
                        .background(LoginErrorRed.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "!",
                        style = MaterialTheme.typography.labelSmall.copy(
                            color = LoginErrorRed,
                            fontWeight = FontWeight.Bold
                        )
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = LoginErrorRed,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@Composable
private fun PremiumLoginButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.975f else 1f,
        animationSpec = spring(dampingRatio = 0.62f, stiffness = 420f),
        label = "login-scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .scale(scale)
            .shadow(14.dp, RoundedCornerShape(50), ambientColor = SageGreen.copy(alpha = 0.38f), spotColor = SageGreen.copy(alpha = 0.28f)),
        shape = RoundedCornerShape(50),
        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
        contentPadding = androidx.compose.foundation.layout.PaddingValues()
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        if (enabled) {
                            listOf(ForestGreen, DarkForestGreen)
                        } else {
                            listOf(SageGreen.copy(alpha = 0.45f), DarkForestGreen.copy(alpha = 0.42f))
                        }
                    ),
                    RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            FloatingLeafCluster(
                modifier = Modifier.align(Alignment.CenterEnd).size(88.dp),
                alpha = if (enabled) 0.16f else 0.08f
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    color = PureWhite.copy(alpha = if (enabled) 1f else 0.78f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                )
            )
        }
    }
}

@Composable
private fun LoginBackground() {
    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(Brush.verticalGradient(listOf(Cream, Color(0xFFFFFBF3), Color(0xFFF3E9D8))))
            drawCircle(
                brush = Brush.radialGradient(listOf(PureWhite.copy(alpha = 0.78f), Color.Transparent)),
                radius = size.width * 0.82f,
                center = Offset(size.width * 0.5f, size.height * 0.28f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(LoginGold.copy(alpha = 0.16f), Color.Transparent)),
                radius = size.width * 0.6f,
                center = Offset(size.width * 0.05f, size.height * 0.08f)
            )
            drawCircle(
                brush = Brush.radialGradient(listOf(SageGreen.copy(alpha = 0.12f), Color.Transparent)),
                radius = size.width * 0.52f,
                center = Offset(size.width * 0.95f, size.height * 0.9f)
            )
        }
        FloatingLeafCluster(
            modifier = Modifier
                .size(118.dp)
                .padding(top = 18.dp)
                .align(Alignment.TopEnd),
            alpha = 0.28f
        )
    }
}

@Composable
private fun FloatingLeafCluster(modifier: Modifier = Modifier, alpha: Float) {
    Canvas(modifier = modifier) {
        repeat(4) { index ->
            val left = size.width * (0.18f + index * 0.13f)
            val top = size.height * (0.16f + (index % 2) * 0.18f)
            drawOval(
                color = SageGreen.copy(alpha = alpha),
                topLeft = Offset(left, top),
                size = Size(size.width * 0.18f, size.height * 0.32f)
            )
            drawLine(
                color = ForestGreen.copy(alpha = alpha),
                start = Offset(left + size.width * 0.09f, top + size.height * 0.26f),
                end = Offset(size.width * 0.82f, size.height * 0.82f),
                strokeWidth = 2f,
                cap = StrokeCap.Round
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun LoginScreenPreview() {
    LoginScreen(onLogin = {}, onCreateAccount = {})
}
