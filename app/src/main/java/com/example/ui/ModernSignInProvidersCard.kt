package com.example.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import android.widget.Toast
import com.example.utils.AuthValidator
import com.example.utils.PasswordStrength
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun ModernSignInProvidersCard(
    userRole: String, // "PASSENGER" or "DRIVER"
    activeRole: String,
    onSelectRole: (String) -> Unit,
    isDark: Boolean = false,
    isAuthenticating: Boolean = false,
    onGoogleAuthClick: () -> Unit,
    onAppleAuthClick: () -> Unit = {},
    onEmailLoginSubmit: (email: String, pass: String) -> Unit,
    onEmailRegisterSubmit: (email: String, pass: String, name: String, vehicleType: String, vehiclePlate: String, licenseNum: String, onError: (String) -> Unit) -> Unit = { _, _, _, _, _, _, _ -> },
    onRequestOtp: (String) -> Unit,
    onRequestOtpWithProfile: (phone: String, name: String) -> Unit = { p, _ -> onRequestOtp(p) },
    onVerifyOtp: (String) -> Unit,
    otpRequested: Boolean = false,
    isOtpSending: Boolean = false,
    generatedOtp: String = "",
    smsGatewayStatus: String = "",
    authError: String = "",
    isAdminLoggedIn: Boolean = false,
    isSecretAdminUnlocked: Boolean = false,
    onLongPressHeader: () -> Unit = {},
    onQuickSelectAccount: (String, String) -> Unit = { _, _ -> }
) {
    var phoneInput by remember { mutableStateOf("") }
    var otpCodeInput by remember { mutableStateOf("") }
    var countryPrefix by remember { mutableStateOf("+220") }
    var showCountryPicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current

    val smsPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { _ ->
        onRequestOtp("$countryPrefix$phoneInput")
    }

    var countdownSeconds by remember { mutableIntStateOf(60) }
    var isTimerActive by remember { mutableStateOf(false) }

    LaunchedEffect(otpRequested, isTimerActive) {
        if (otpRequested && isTimerActive) {
            while (countdownSeconds > 0) {
                delay(1000L)
                countdownSeconds--
            }
            isTimerActive = false
        }
    }

    var isEmailModeExpanded by remember { mutableStateOf(false) }
    var isRegisterMode by remember { mutableStateOf(false) }
    var isPhoneRegisterMode by remember { mutableStateOf(false) }
    var phoneRegisterName by remember { mutableStateOf("") }
    var emailInput by remember { mutableStateOf("") }
    var passwordInput by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var vehicleTypeInput by remember { mutableStateOf("CAR") }
    var vehiclePlateInput by remember { mutableStateOf("") }
    var licenseNumInput by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var showForgotPasswordDialog by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }

    LaunchedEffect(authError) {
        if (authError.isNotBlank()) {
            isSubmitting = false
            localError = authError
        }
    }

    val cardBg = if (isDark) Color(0xFF1E293B) else PureWhite
    val textPrimary = if (isDark) PureWhite else BrandBlueDark
    val textSecondary = if (isDark) Color(0xFF94A3B8) else NeutralGray
    val inputBg = if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
    val borderCol = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .testTag("modern_login_card"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Role Section Switcher Tabs (Passenger vs Driver vs Admin Gate)
            AuthRoleSectionTabs(
                activeRole = activeRole,
                onSelectRole = onSelectRole,
                isDarkBg = isDark,
                isAdminLoggedIn = isAdminLoggedIn,
                isSecretAdminUnlocked = isSecretAdminUnlocked,
                onLongPressHeader = onLongPressHeader
            )

            Spacer(modifier = Modifier.height(20.dp))

            // SIGN-IN PROVIDERS BUTTONS (EXACTLY MATCHING USER MOCKUP IMAGE)
            // 1. Continue with Apple
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF0F172A) else PureWhite,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onAppleAuthClick()
                    }
                    .testTag("provider_apple_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Apple",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Continue with Google
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF0F172A) else PureWhite,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable {
                        focusManager.clearFocus()
                        keyboardController?.hide()
                        onGoogleAuthClick()
                    }
                    .testTag("provider_google_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF4285F4).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", fontSize = 16.sp, fontWeight = FontWeight.Black, color = Color(0xFF4285F4))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Continue with Google",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Continue with Email
            Surface(
                shape = CircleShape,
                color = if (isDark) Color(0xFF0F172A) else PureWhite,
                border = BorderStroke(1.dp, borderCol),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clickable {
                        isEmailModeExpanded = !isEmailModeExpanded
                        localError = ""
                    }
                    .testTag("provider_email_btn")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Email,
                        contentDescription = "Email Sign In",
                        tint = BrandBluePrimary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = if (isEmailModeExpanded) "Hide Email Login" else "Continue with Email",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimary
                    )
                }
            }

            // EXPANDABLE EMAIL SIGN IN / REGISTER FORM
            AnimatedVisibility(
                visible = isEmailModeExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                // Real-time validation results
                val emailValidation = remember(emailInput) {
                    if (emailInput.isNotBlank()) AuthValidator.validateEmail(emailInput) else null
                }
                val passwordValidation = remember(passwordInput) {
                    if (passwordInput.isNotBlank()) AuthValidator.validatePassword(passwordInput) else null
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 14.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(inputBg)
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isRegisterMode) "Create $userRole Account" else "Email Sign In",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        TextButton(onClick = {
                            isRegisterMode = !isRegisterMode
                            localError = ""
                        }) {
                            Text(
                                text = if (isRegisterMode) "Sign In instead" else "Register",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBluePrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    if (isRegisterMode) {
                        OutlinedTextField(
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                if (localError.isNotBlank()) localError = ""
                            },
                            label = { Text("Full Name", color = textSecondary) },
                            placeholder = { Text("e.g. Lamin Touray", color = textSecondary.copy(alpha = 0.6f)) },
                            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandBluePrimary) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = borderCol,
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = BrandBluePrimary,
                                focusedLabelColor = BrandBluePrimary,
                                unfocusedLabelColor = textSecondary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("register_name_input_field"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Email Address Field with real-time format indicator
                    OutlinedTextField(
                        value = emailInput,
                        onValueChange = {
                            emailInput = it
                            if (localError.isNotBlank()) localError = ""
                        },
                        label = { Text("Email Address", color = textSecondary) },
                        placeholder = { Text("e.g. user@gmail.com", color = textSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandBluePrimary) },
                        trailingIcon = {
                            if (emailInput.isNotBlank()) {
                                val isValid = emailValidation?.isValid == true
                                Icon(
                                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                    contentDescription = if (isValid) "Valid Email" else "Invalid Email",
                                    tint = if (isValid) Color(0xFF10B981) else Color(0xFFEF4444),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        },
                        isError = emailInput.isNotBlank() && emailValidation?.isValid == false,
                        supportingText = if (emailInput.isNotBlank() && emailValidation?.isValid == false) {
                            { Text(emailValidation.errorMessage ?: "Invalid email format", color = Color(0xFFEF4444), fontSize = 11.sp) }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBluePrimary,
                            unfocusedBorderColor = borderCol,
                            errorBorderColor = Color(0xFFEF4444),
                            focusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                            unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = BrandBluePrimary,
                            focusedLabelColor = BrandBluePrimary,
                            unfocusedLabelColor = textSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("email_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Password Field
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = {
                            passwordInput = it
                            if (localError.isNotBlank()) localError = ""
                        },
                        label = { Text("Password", color = textSecondary) },
                        placeholder = { Text("At least 6 characters", color = textSecondary.copy(alpha = 0.6f)) },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandBluePrimary) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = textSecondary
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        isError = isRegisterMode && passwordInput.isNotBlank() && passwordValidation?.isValid == false,
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = textPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandBluePrimary,
                            unfocusedBorderColor = borderCol,
                            errorBorderColor = Color(0xFFEF4444),
                            focusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                            unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                            focusedTextColor = textPrimary,
                            unfocusedTextColor = textPrimary,
                            cursorColor = BrandBluePrimary,
                            focusedLabelColor = BrandBluePrimary,
                            unfocusedLabelColor = textSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("password_input_field"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Forgot Password Button in Login Mode
                    if (!isRegisterMode) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = { showForgotPasswordDialog = true },
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.testTag("email_forgot_password_btn")
                            ) {
                                Text(
                                    text = "Forgot Password?",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBluePrimary
                                )
                            }
                        }
                    }

                    // Registration Password Requirements and Strength Indicator
                    if (isRegisterMode && passwordInput.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        val strength = passwordValidation?.strength ?: PasswordStrength.TOO_SHORT
                        val strengthColor = when (strength) {
                            PasswordStrength.TOO_SHORT -> Color(0xFFEF4444)
                            PasswordStrength.WEAK -> Color(0xFFF97316)
                            PasswordStrength.MEDIUM -> Color(0xFFEAB308)
                            PasswordStrength.STRONG -> Color(0xFF10B981)
                        }
                        val strengthProgress = when (strength) {
                            PasswordStrength.TOO_SHORT -> 0.25f
                            PasswordStrength.WEAK -> 0.5f
                            PasswordStrength.MEDIUM -> 0.75f
                            PasswordStrength.STRONG -> 1.0f
                        }

                        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Password Strength: ${strength.label}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = strengthColor
                                )
                                Text(
                                    text = "${passwordInput.length}/6+ chars",
                                    fontSize = 10.5.sp,
                                    color = if (passwordInput.length >= 6) Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            }
                            Spacer(modifier = Modifier.height(3.dp))
                            LinearProgressIndicator(
                                progress = { strengthProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(2.dp)),
                                color = strengthColor,
                                trackColor = borderCol
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            // Checklist items
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val minLenMet = passwordValidation?.hasMinLength == true
                                val letterMet = passwordValidation?.hasLetter == true
                                val digitMet = passwordValidation?.hasDigitOrSymbol == true

                                RequirementPill(label = "6+ Chars", isMet = minLenMet, isDark = isDark)
                                RequirementPill(label = "Letters", isMet = letterMet, isDark = isDark)
                                RequirementPill(label = "Numbers/Symbols", isMet = digitMet, isDark = isDark)
                            }
                        }
                    }

                    if (userRole == "DRIVER" && isRegisterMode) {
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = vehicleTypeInput,
                            onValueChange = { vehicleTypeInput = it },
                            label = { Text("Vehicle Type (CAR, TAXI, TRICYCLE)", color = textSecondary) },
                            placeholder = { Text("CAR", color = textSecondary.copy(alpha = 0.6f)) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = borderCol,
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = BrandBluePrimary,
                                focusedLabelColor = BrandBluePrimary,
                                unfocusedLabelColor = textSecondary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = vehiclePlateInput,
                            onValueChange = { vehiclePlateInput = it },
                            label = { Text("License Plate", color = textSecondary) },
                            placeholder = { Text("e.g. BJL 1234 A", color = textSecondary.copy(alpha = 0.6f)) },
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = textPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium
                            ),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = borderCol,
                                focusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                                unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else PureWhite,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary,
                                cursorColor = BrandBluePrimary,
                                focusedLabelColor = BrandBluePrimary,
                                unfocusedLabelColor = textSecondary
                            ),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        )
                    }

                    // Visible UI Feedback for errors
                    AnimatedVisibility(visible = localError.isNotBlank()) {
                        Column {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFEF4444).copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Warning,
                                        contentDescription = "Error",
                                        tint = Color(0xFFEF4444),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = localError,
                                        color = Color(0xFFEF4444),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            localError = ""
                            focusManager.clearFocus()
                            keyboardController?.hide()

                            if (isRegisterMode) {
                                // Explicit Pre-Firebase Validation Layer
                                val validation = AuthValidator.validateSignUp(
                                    email = emailInput,
                                    password = passwordInput,
                                    name = nameInput,
                                    isDriver = (userRole == "DRIVER"),
                                    vehiclePlate = vehiclePlateInput
                                )

                                if (!validation.isValid) {
                                    val err = validation.errorMessage ?: "Please verify all registration fields."
                                    localError = err
                                    Toast.makeText(context, "⚠️ $err", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSubmitting = true
                                Toast.makeText(context, "Creating account with Firebase...", Toast.LENGTH_SHORT).show()
                                onEmailRegisterSubmit(
                                    emailInput.trim(),
                                    passwordInput.trim(),
                                    nameInput.trim(),
                                    vehicleTypeInput.trim(),
                                    vehiclePlateInput.trim(),
                                    licenseNumInput.trim()
                                ) { err ->
                                    isSubmitting = false
                                    localError = err
                                    Toast.makeText(context, "⚠️ $err", Toast.LENGTH_LONG).show()
                                }
                            } else {
                                // Pre-Firebase Login Validation
                                val emailCheck = AuthValidator.validateEmail(emailInput)
                                if (!emailCheck.isValid) {
                                    val err = emailCheck.errorMessage ?: "Please enter a valid email address."
                                    localError = err
                                    Toast.makeText(context, "⚠️ $err", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                if (passwordInput.isBlank()) {
                                    val err = "Please enter your password."
                                    localError = err
                                    Toast.makeText(context, "⚠️ $err", Toast.LENGTH_SHORT).show()
                                    return@Button
                                }

                                isSubmitting = true
                                onEmailLoginSubmit(emailInput.trim(), passwordInput.trim())
                            }
                        },
                        enabled = !isSubmitting && !isAuthenticating,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("email_submit_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandBluePrimary,
                            disabledContainerColor = BrandBluePrimary.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isSubmitting || isAuthenticating) {
                            CircularProgressIndicator(
                                color = PureWhite,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isRegisterMode) "Creating Account..." else "Signing In...",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        } else {
                            Text(
                                text = if (isRegisterMode) "Register & Sign In" else "Sign In with Email",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // DIVIDER LINE (--- OR ---)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HorizontalDivider(modifier = Modifier.weight(1f), color = borderCol)
                Text(
                    text = "  OR  ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = textSecondary
                )
                HorizontalDivider(modifier = Modifier.weight(1f), color = borderCol)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // BOTTOM PHONE SECTION: Firebase Phone Authentication (Login & Registration)
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (isPhoneRegisterMode) "Register with Phone" else "Sign in with Phone",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = textPrimary
                        )
                        Text(
                            text = "Firebase SMS Verification",
                            fontSize = 12.sp,
                            color = BrandBluePrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    if (!otpRequested) {
                        // Switch between Login and Register tabs
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(inputBg)
                                .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (!isPhoneRegisterMode) BrandBluePrimary else Color.Transparent)
                                    .clickable {
                                        isPhoneRegisterMode = false
                                        localError = ""
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("phone_auth_tab_login"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Sign In",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (!isPhoneRegisterMode) PureWhite else textSecondary
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(if (isPhoneRegisterMode) BrandBluePrimary else Color.Transparent)
                                    .clickable {
                                        isPhoneRegisterMode = true
                                        localError = ""
                                    }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                                    .testTag("phone_auth_tab_register"),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Register",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isPhoneRegisterMode) PureWhite else textSecondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (!otpRequested) {
                    // Registration-only fields: Full Name & Driver metadata
                    if (isPhoneRegisterMode) {
                        OutlinedTextField(
                            value = phoneRegisterName,
                            onValueChange = { phoneRegisterName = it },
                            label = { Text("Full Name") },
                            placeholder = { Text(if (activeRole == "DRIVER") "e.g. Alieu Bah" else "e.g. Lamin Touray") },
                            leadingIcon = {
                                Icon(Icons.Default.Person, contentDescription = null, tint = BrandBluePrimary)
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = borderCol,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("phone_register_name_input")
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Phone Number Input Row (Country Picker + Number)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Country Selector Pill
                        val currentCountry = wayGoCountryList.find { it.code == countryPrefix } ?: wayGoCountryList.first()

                        Box(
                            modifier = Modifier
                                .height(56.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(inputBg)
                                .border(1.dp, borderCol, RoundedCornerShape(14.dp))
                                .clickable { showCountryPicker = true }
                                .padding(horizontal = 12.dp)
                                .testTag("country_code_selector_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(currentCountry.flag, fontSize = 18.sp)
                                Text(
                                    text = currentCountry.code,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                Icon(
                                    imageVector = if (showCountryPicker) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Country dropdown toggle",
                                    tint = textSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Number Field Pill
                        OutlinedTextField(
                            value = phoneInput,
                            onValueChange = { phoneInput = it },
                            placeholder = {
                                Text(
                                    text = "7599593",
                                    fontSize = 15.sp,
                                    color = textSecondary.copy(alpha = 0.6f)
                                )
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(14.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = borderCol,
                                focusedContainerColor = inputBg,
                                unfocusedContainerColor = inputBg,
                                focusedTextColor = textPrimary,
                                unfocusedTextColor = textPrimary
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                fontSize = 15.5.sp,
                                fontWeight = FontWeight.SemiBold
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("phone_number_pill_input")
                        )
                    }

                    if (showCountryPicker) {
                        CountryCodePickerDialog(
                            selectedCode = countryPrefix,
                            onCountrySelected = { country ->
                                countryPrefix = country.code
                                showCountryPicker = false
                            },
                            onDismissRequest = { showCountryPicker = false },
                            isDark = isDark
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Helpful security & instruction label
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.padding(horizontal = 4.dp)
                    ) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = null,
                            tint = textSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = if (isPhoneRegisterMode)
                                "Enter your phone to register. An SMS verification code will be sent."
                            else
                                "Enter your registered phone to receive a 6-digit SMS verification code.",
                            fontSize = 11.5.sp,
                            color = textSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Action Button for Phone
                    Button(
                        onClick = {
                            localError = ""
                            val cleanDigits = phoneInput.trim().replace(" ", "")
                            if (cleanDigits.isBlank()) {
                                localError = "Please enter your mobile phone number."
                                return@Button
                            }
                            if (isPhoneRegisterMode && phoneRegisterName.trim().isBlank()) {
                                localError = "Please enter your full name to register."
                                return@Button
                            }
                            val fullPhone = "$countryPrefix$cleanDigits"
                            onRequestOtpWithProfile(fullPhone, phoneRegisterName.trim())
                            countdownSeconds = 60
                            isTimerActive = true
                        },
                        enabled = !isOtpSending,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag(if (isPhoneRegisterMode) "phone_register_submit_btn" else "continue_phone_sms_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        if (isOtpSending) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = PureWhite,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Sending SMS Code...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        } else {
                            Icon(
                                if (isPhoneRegisterMode) Icons.Default.PersonAdd else Icons.Default.Sms,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = PureWhite
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isPhoneRegisterMode) "Register & Send Verification Code" else "Sign In with SMS Code",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                } else {
                    // OTP Verification Step
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(inputBg)
                            .border(1.dp, borderCol, RoundedCornerShape(18.dp))
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (isPhoneRegisterMode) "Complete Registration" else "Verify SMS Code",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textPrimary
                                )
                                val formattedPhoneDisplay = remember(countryPrefix, phoneInput) {
                                    com.example.data.SmsOtpGatewayManager.formatE164PhoneNumber("$countryPrefix$phoneInput")
                                }
                                Text(
                                    text = if (isPhoneRegisterMode && phoneRegisterName.isNotBlank())
                                        "Registering $phoneRegisterName • $formattedPhoneDisplay"
                                    else
                                        "Sent via Firebase SMS to $formattedPhoneDisplay",
                                    fontSize = 11.5.sp,
                                    color = textSecondary
                                )
                            }
                            TextButton(
                                onClick = {
                                    // Reset OTP state to allow changing phone number
                                    otpCodeInput = ""
                                    localError = ""
                                    onRequestOtp("") // Trigger resetting otpRequested
                                }
                            ) {
                                Text("Edit Phone", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Numeric 6-Digit Box OTP Input Field
                        NumericOtp6BoxInput(
                            otpValue = otpCodeInput,
                            onOtpChange = { newValue ->
                                otpCodeInput = newValue
                            },
                            onOtpComplete = { code ->
                                onVerifyOtp(code)
                            },
                            isDark = isDark
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Countdown Timer & Resend Button
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isTimerActive && countdownSeconds > 0) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Timer,
                                        contentDescription = "Countdown Timer",
                                        tint = textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Resend code in 00:${if (countdownSeconds < 10) "0" else ""}$countdownSeconds",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = textSecondary
                                    )
                                }
                            } else {
                                TextButton(
                                    onClick = {
                                        localError = ""
                                        val cleanDigits = phoneInput.trim().replace(" ", "")
                                        val fullPhone = if (cleanDigits.isNotBlank()) "$countryPrefix$cleanDigits" else "$countryPrefix 7712345"
                                        onRequestOtpWithProfile(fullPhone, phoneRegisterName.trim())
                                        countdownSeconds = 60
                                        isTimerActive = true
                                    },
                                    enabled = !isOtpSending,
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    if (isOtpSending) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(14.dp),
                                            strokeWidth = 2.dp,
                                            color = BrandBluePrimary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Sending...",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBluePrimary
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Refresh,
                                            contentDescription = "Resend Code",
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Resend SMS Code",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBluePrimary
                                        )
                                    }
                                }
                            }

                            Text(
                                text = "${otpCodeInput.length}/6 digits",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (otpCodeInput.length == 6) BrandBluePrimary else textSecondary
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Verify Button
                        Button(
                            onClick = {
                                if (otpCodeInput.isNotBlank()) {
                                    onVerifyOtp(otpCodeInput)
                                }
                            },
                            enabled = otpCodeInput.length >= 4 && !isOtpSending,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("verify_otp_pill_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandBluePrimary,
                                disabledContainerColor = BrandBluePrimary.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp), tint = PureWhite)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isPhoneRegisterMode) "Verify & Register Account" else "Verify & Complete Sign In",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }

                if (smsGatewayStatus.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = smsGatewayStatus, fontSize = 11.5.sp, color = textSecondary)
                }
            }

            // Error Display
            val errToDisplay = if (localError.isNotBlank()) localError else authError
            if (errToDisplay.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = errToDisplay,
                    color = ErrorRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    if (showForgotPasswordDialog) {
        ForgotPasswordDialog(
            initialEmail = emailInput,
            isDark = isDark,
            onDismiss = { showForgotPasswordDialog = false }
        )
    }
}

@Composable
fun NumericOtp6BoxInput(
    otpValue: String,
    onOtpChange: (String) -> Unit,
    onOtpComplete: (String) -> Unit,
    length: Int = 6,
    isDark: Boolean = false
) {
    BasicTextField(
        value = otpValue,
        onValueChange = { newValue ->
            val filtered = newValue.filter { it.isDigit() }.take(length)
            onOtpChange(filtered)
            if (filtered.length == length) {
                onOtpComplete(filtered)
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 0 until length) {
                    val char = otpValue.getOrNull(i)?.toString() ?: ""
                    val isFocused = otpValue.length == i || (i == length - 1 && otpValue.length == length)
                    val boxBg = if (isDark) Color(0xFF0F172A) else PureWhite
                    val boxBorder = when {
                        isFocused -> BrandBluePrimary
                        char.isNotEmpty() -> BrandBluePrimary.copy(alpha = 0.6f)
                        else -> if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1)
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(boxBg)
                            .border(
                                width = if (isFocused) 2.dp else 1.dp,
                                color = boxBorder,
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = char,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) PureWhite else BrandBlueDark
                        )
                    }
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("otp_code_pill_input")
    )
}

data class WayGoCountryCode(
    val flag: String,
    val name: String,
    val code: String
)

val wayGoCountryList = listOf(
    WayGoCountryCode("🇬🇲", "Gambia", "+220"),
    WayGoCountryCode("🇸🇳", "Senegal", "+221"),
    WayGoCountryCode("🇳🇬", "Nigeria", "+234"),
    WayGoCountryCode("🇬🇭", "Ghana", "+233"),
    WayGoCountryCode("🇸🇱", "Sierra Leone", "+232"),
    WayGoCountryCode("🇰🇪", "Kenya", "+254"),
    WayGoCountryCode("🇿🇦", "South Africa", "+27"),
    WayGoCountryCode("🇬🇧", "United Kingdom", "+44"),
    WayGoCountryCode("🇺🇸", "United States", "+1"),
    WayGoCountryCode("🇨🇦", "Canada", "+1"),
    WayGoCountryCode("🇩🇪", "Germany", "+49"),
    WayGoCountryCode("🇫🇷", "France", "+33"),
    WayGoCountryCode("🇦🇪", "UAE", "+971"),
    WayGoCountryCode("🇮🇳", "India", "+91"),
    WayGoCountryCode("🇨🇳", "China", "+86")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryCodePickerDialog(
    selectedCode: String,
    onCountrySelected: (WayGoCountryCode) -> Unit,
    onDismissRequest: () -> Unit,
    isDark: Boolean = false
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredCountries = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            wayGoCountryList
        } else {
            val query = searchQuery.trim().lowercase()
            wayGoCountryList.filter {
                it.name.lowercase().contains(query) || it.code.lowercase().contains(query)
            }
        }
    }

    val dialogBg = if (isDark) Color(0xFF0F172A) else PureWhite
    val textColor = if (isDark) PureWhite else BrandBlueDark
    val subTextColor = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)

    AlertDialog(
        onDismissRequest = onDismissRequest,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 500.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(18.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Select Country Code",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    IconButton(
                        onClick = onDismissRequest,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = subTextColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search Input Field
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search country or calling code...", fontSize = 13.sp, color = subTextColor) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = subTextColor,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = subTextColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = if (isDark) Color(0xFF334155) else Color(0xFFCBD5E1),
                        focusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        unfocusedContainerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC),
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                )

                Spacer(modifier = Modifier.height(12.dp))

                if (filteredCountries.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No countries match \"$searchQuery\"",
                            fontSize = 13.5.sp,
                            color = subTextColor
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredCountries, key = { it.code + it.name }) { country ->
                            val isSelected = country.code == selectedCode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSelected) BrandBluePrimary.copy(alpha = 0.15f)
                                        else Color.Transparent
                                    )
                                    .clickable { onCountrySelected(country) }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(country.flag, fontSize = 20.sp)
                                Text(
                                    text = country.name,
                                    fontSize = 14.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = textColor,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = country.code,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBluePrimary
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RequirementPill(
    label: String,
    isMet: Boolean,
    isDark: Boolean
) {
    val pillBg = if (isMet) {
        Color(0xFF10B981).copy(alpha = 0.15f)
    } else {
        if (isDark) Color(0xFF334155).copy(alpha = 0.4f) else Color(0xFFE2E8F0)
    }
    val pillTextColor = if (isMet) {
        Color(0xFF10B981)
    } else {
        if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
    }

    Surface(
        shape = RoundedCornerShape(6.dp),
        color = pillBg,
        modifier = Modifier.height(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = if (isMet) Icons.Default.Check else Icons.Default.Close,
                contentDescription = null,
                tint = pillTextColor,
                modifier = Modifier.size(11.dp)
            )
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = if (isMet) FontWeight.Bold else FontWeight.Medium,
                color = pillTextColor
            )
        }
    }
}


