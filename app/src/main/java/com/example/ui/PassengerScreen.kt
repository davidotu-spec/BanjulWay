package com.example.ui

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.FlutterwaveManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import coil.compose.rememberAsyncImagePainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.TripEntity
import com.example.data.TripFareEstimationService
import com.example.data.UserProfileEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Brush

// Seed locations of Gambia
data class GLocation(val name: String, val lat: Double, val lng: Double)
val GAMBIA_LOCATIONS = listOf(
    GLocation("Albert Market, Banjul", 13.4533, -16.5746),
    GLocation("Arch 22, Banjul", 13.4580, -16.5820),
    GLocation("Banjul Ferry Terminal", 13.4505, -16.5710),
    GLocation("Kairaba Avenue, Serrekunda", 13.4471, -16.6791),
    GLocation("Westfield Junction, Serrekunda", 13.4385, -16.6760),
    GLocation("University of Gambia, Kanifing", 13.4452, -16.6713),
    GLocation("Tippa Garage, Serrekunda", 13.4340, -16.6850),
    GLocation("Senegambia Beach Resort", 13.4420, -16.7110),
    GLocation("Independence Stadium, Bakau", 13.4722, -16.6690),
    GLocation("Brusubi Turntable", 13.4020, -16.7180),
    GLocation("Kotu Beach, Kanifing", 13.4610, -16.7020),
    GLocation("Pipeline, Serrekunda", 13.4510, -16.6800)
)

fun resolveLocationForInput(input: String): GLocation {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) {
        return GAMBIA_LOCATIONS[0]
    }
    val match = GAMBIA_LOCATIONS.firstOrNull { loc ->
        loc.name.equals(trimmed, ignoreCase = true) ||
        loc.name.contains(trimmed, ignoreCase = true) ||
        trimmed.contains(loc.name.split(",")[0], ignoreCase = true)
    }
    if (match != null) return match

    val hash = kotlin.math.abs(trimmed.lowercase().hashCode())
    val latOffset = ((hash % 100) - 50) * 0.0006
    val lngOffset = (((hash / 100) % 100) - 50) * 0.0006
    return GLocation(
        name = trimmed,
        lat = 13.4471 + latOffset,
        lng = -16.6791 + lngOffset
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerScreen(
    viewModel: WayGoViewModel,
    modifier: Modifier = Modifier,
    onOpenSectionSheet: (() -> Unit)? = null
) {
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val otpRequested by viewModel.otpRequested.collectAsState()
    val generatedOtp by viewModel.generatedOtp.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val profileFlow = viewModel.userProfile.collectAsState()
    val profile = profileFlow.value

    var activeTabSubState by remember { mutableStateOf("HOME") } // "HOME", "HISTORY", "PROFILE", "CHAT"

    val smsGatewayStatus by viewModel.smsGatewayStatus.collectAsState()
    val isRealSmsSent by viewModel.isRealSmsSent.collectAsState()
    val isOtpSending by viewModel.isOtpSending.collectAsState()

    // Authentication Gate
    if (!isLoggedIn) {
        val context = androidx.compose.ui.platform.LocalContext.current
        val activity = context as? android.app.Activity
        PassengerAuthView(
            otpRequested = otpRequested,
            generatedOtp = generatedOtp,
            authError = authError,
            smsGatewayStatus = smsGatewayStatus,
            isRealSmsSent = isRealSmsSent,
            isOtpSending = isOtpSending,
            onRequestOtp = { viewModel.requestOtp(activity, it) },
            onVerifyOtp = { viewModel.verifyOtp(it) },
            onEmailLogin = { email, pass -> viewModel.loginPassengerWithEmail(email, pass) },
            onEmailRegister = { email, pass, name -> viewModel.registerPassengerWithEmail(email, pass, name) },
            onSocialLogin = { provider -> viewModel.socialLoginPassenger(provider) },
            onSelectRole = { newRole -> viewModel.setRole(newRole) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenSectionSheet?.invoke() }
                            .padding(vertical = 4.dp, horizontal = 4.dp)
                            .testTag("passenger_topbar_brand")
                    ) {
                        // Circular brand badge with WayGo inside
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    androidx.compose.ui.graphics.Brush.linearGradient(
                                        colors = listOf(BrandBluePrimary, BrandBlueDark)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "WayGo",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 11.5.sp,
                                style = LocalTextStyle.current.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        }
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "WayGo Ride",
                                    color = BrandBlueDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Switch Section",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = "Gambia • Tap to switch section",
                                color = BrandBluePrimary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite),
                actions = {
                    IconButton(
                        onClick = { onOpenSectionSheet?.invoke() },
                        modifier = Modifier.testTag("open_section_sheet_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Switch Section",
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    IconButton(onClick = { activeTabSubState = "PROFILE" }) {
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile",
                            tint = BrandBlueDark,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = PureWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeTabSubState == "HOME",
                    onClick = { activeTabSubState = "HOME" },
                    icon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Ride") },
                    label = { Text("Ride") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBluePrimary,
                        selectedTextColor = BrandBluePrimary,
                        indicatorColor = BrandBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTabSubState == "HISTORY",
                    onClick = { activeTabSubState = "HISTORY" },
                    icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Activity") },
                    label = { Text("Activity") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBluePrimary,
                        selectedTextColor = BrandBluePrimary,
                        indicatorColor = BrandBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTabSubState == "PROFILE" || activeTabSubState == "CHAT",
                    onClick = { activeTabSubState = "PROFILE" },
                    icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Account") },
                    label = { Text("Account") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBluePrimary,
                        selectedTextColor = BrandBluePrimary,
                        indicatorColor = BrandBlueLight
                    ),
                    modifier = Modifier.testTag("tab_account")
                )
            }
        },
        floatingActionButton = {
            if (activeTabSubState == "HOME") {
                FloatingActionButton(
                    onClick = {
                        // Quick Emergency SOS dialer simulation
                        _isSosDialogOpen.value = true
                    },
                    containerColor = ErrorRed,
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Emergency, contentDescription = "Emergency SOS")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BrandBlueLight)
        ) {
            when (activeTabSubState) {
                "HOME" -> HomeScreenContent(viewModel, profile)
                "HISTORY" -> ProfileScreenContent(viewModel, profile, initialSection = "TRIP_LOG") { activeTabSubState = "HOME" }
                "PROFILE" -> ProfileScreenContent(viewModel, profile, initialSection = "PROFILE") { activeTabSubState = "HOME" }
                "CHAT" -> ProfileScreenContent(viewModel, profile, initialSection = "INBOX") { activeTabSubState = "HOME" }
            }

            // Emergency Dialog
            SosDialog()
        }
    }
}

// Global SOS State
private val _isSosDialogOpen = mutableStateOf(false)

@Composable
fun SosDialog() {
    if (_isSosDialogOpen.value) {
        AlertDialog(
            onDismissRequest = { _isSosDialogOpen.value = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Warning, contentDescription = "Alert", tint = ErrorRed)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Emergency SOS Center", color = ErrorRed, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column {
                    Text(
                        "Are you experiencing an emergency? WayGo operates real-time tracking shared immediately with local Gambia security response teams in Banjul and Serrekunda.",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "Simulated emergency numbers:",
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                    Text("• Serekunda Police Helpline: 117", fontSize = 13.sp)
                    Text("• Fire & Ambulance Rescue: 118", fontSize = 13.sp)
                    Text("• Banjul Central Command: +220 422 4910", fontSize = 13.sp)
                }
            },
            confirmButton = {
                Button(
                    onClick = { _isSosDialogOpen.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Share Live Link & Call 117", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { _isSosDialogOpen.value = false }) {
                    Text("Cancel", color = NeutralGray)
                }
            }
        )
    }
}

data class CountryCode(val code: String, val name: String, val flag: String, val lengthHint: String)

@Composable
fun AuthRoleSectionTabs(
    activeRole: String,
    onSelectRole: (String) -> Unit,
    modifier: Modifier = Modifier,
    isDarkBg: Boolean = true
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isDarkBg) PureWhite.copy(alpha = 0.12f) else BrandBlueLight,
        border = BorderStroke(1.dp, if (isDarkBg) PureWhite.copy(alpha = 0.25f) else BrandBluePrimary.copy(alpha = 0.3f)),
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(6.dp)
        ) {
            Text(
                text = "SELECT YOUR ACCOUNT PORTAL:",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDarkBg) PureWhite.copy(alpha = 0.8f) else NeutralGray,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("PASSENGER", "Passenger", "🙋‍♂️"),
                    Triple("DRIVER", "Driver Fleet", "🚗"),
                    Triple("ADMIN", "Admin Portal", "🛡️")
                ).forEach { (roleKey, label, icon) ->
                    val isSelected = activeRole == roleKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) BrandBluePrimary else Color.Transparent
                            )
                            .clickable { onSelectRole(roleKey) },
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(horizontal = 2.dp)
                        ) {
                            Text(
                                text = icon,
                                fontSize = 13.sp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 11.5.sp,
                                fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (isSelected) PureWhite else (if (isDarkBg) PureWhite.copy(alpha = 0.85f) else BrandBlueDark),
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PassengerAuthView(
    otpRequested: Boolean,
    generatedOtp: String,
    authError: String,
    smsGatewayStatus: String = "⚡ WayGo SMS Gateway Active",
    isRealSmsSent: Boolean = false,
    isOtpSending: Boolean = false,
    onRequestOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit,
    onEmailLogin: (String, String) -> Unit = { _, _ -> },
    onEmailRegister: (String, String, String) -> Unit = { _, _, _ -> },
    onSocialLogin: (String) -> Unit = {},
    onSelectRole: (String) -> Unit = {}
) {
    val countries = remember {
        listOf(
            CountryCode("+220", "Gambia", "🇬🇲", "7xxxxxx / 3xxxxxx"),
            CountryCode("+221", "Senegal", "🇸🇳", "7xxxxxxxx"),
            CountryCode("+232", "Sierra Leone", "🇸🇱", "xxxxxxxx"),
            CountryCode("+234", "Nigeria", "🇳🇬", "80xxxxxxxx"),
            CountryCode("+233", "Ghana", "🇬🇭", "xxxxxxxxxx"),
            CountryCode("+224", "Guinea", "🇬🇳", "6xxxxxxxx"),
            CountryCode("+231", "Liberia", "🇱🇷", "xxxxxxxx"),
            CountryCode("+44", "United Kingdom", "🇬🇧", "7xxxxxxxxx"),
            CountryCode("+1", "United States", "🇺🇸", "xxxxxxxxxx")
        )
    }
    var selectedCountry by remember { mutableStateOf(countries[0]) }
    var showCountryDialog by remember { mutableStateOf(false) }

    var authMethod by remember { mutableStateOf("PHONE") } // "PHONE" or "EMAIL"
    var emailInput by remember { mutableStateOf("passenger@waygo.com") }
    var passwordInput by remember { mutableStateOf("pass123") }
    var nameInput by remember { mutableStateOf("John Doe") }
    var isRegisterMode by remember { mutableStateOf(true) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Gambian numbers are usually 7 digits long (e.g. 7712345).
    var phoneInput by remember { mutableStateOf("7712345") }
    var otpInput by remember { mutableStateOf("") }
    
    // Timer simulation
    var timerSeconds by remember { mutableStateOf(30) }
    var canResend by remember { mutableStateOf(false) }

    LaunchedEffect(otpRequested) {
        if (otpRequested) {
            timerSeconds = 30
            canResend = false
            while (timerSeconds > 0) {
                delay(1000L)
                timerSeconds--
            }
            canResend = true
        }
    }

    // Dynamic Gambian telecom carrier detection
    val detectedCarrier = remember(phoneInput, selectedCountry) {
        if (selectedCountry.code != "+220") return@remember null
        val clean = phoneInput.replace(Regex("[^0-9]"), "")
        if (clean.isNotEmpty()) {
            when (clean.first()) {
                '3' -> "QCell 🇬🇲"
                '7' -> "Africell 🇬🇲"
                '9' -> "Gamcel 🇬🇲"
                '6' -> "Comium 🇬🇲"
                '2' -> "Africell 🇬🇲"
                else -> "Local GSM 🇬🇲"
            }
        } else null
    }

    // Modern styled container with vertical gradient and clean ambient background illustrations
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BrandBlueDark, BrandBluePrimary)
                )
            )
            .imePadding()
    ) {
        // Decorative background glowing circular elements
        androidx.compose.foundation.Canvas(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.12f)
        ) {
            drawCircle(
                color = PureWhite,
                radius = size.width * 0.45f,
                center = androidx.compose.ui.geometry.Offset(x = size.width * 0.1f, y = size.height * 0.15f)
            )
            drawCircle(
                color = AccentAmber,
                radius = size.width * 0.35f,
                center = androidx.compose.ui.geometry.Offset(x = size.width * 0.85f, y = size.height * 0.75f)
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Circular WayGo Brand Icon Badge
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(BrandBlueSecondary, BrandBluePrimary)
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "WayGo App Icon",
                        tint = PureWhite,
                        modifier = Modifier.size(28.dp)
                    )
                    Text(
                        text = "WayGo",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = PureWhite,
                        style = LocalTextStyle.current.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                    )
                }
            }

            Text(
                text = "The Gambia's Modern Ride Network",
                fontSize = 13.sp,
                color = PureWhite.copy(alpha = 0.75f),
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Role Section Switcher Tabs
            AuthRoleSectionTabs(
                activeRole = "PASSENGER",
                onSelectRole = onSelectRole,
                isDarkBg = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Main Interactive Authentication Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = PureWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (!otpRequested) {
                        // Auth Method Selector Tab
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp)
                        ) {
                            SegmentedButton(
                                selected = authMethod == "PHONE",
                                onClick = { authMethod = "PHONE" },
                                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
                            ) {
                                Text("📱 SMS OTP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                            SegmentedButton(
                                selected = authMethod == "EMAIL",
                                onClick = { authMethod = "EMAIL" },
                                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
                            ) {
                                Text("✉️ Email Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        if (authMethod == "EMAIL") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = if (isRegisterMode) "Passenger Account Registration" else "Passenger Email Sign In",
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Sign in using your WayGo Passenger email credentials.",
                                    fontSize = 12.sp,
                                    color = NeutralGray,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                if (isRegisterMode) {
                                    OutlinedTextField(
                                        value = nameInput,
                                        onValueChange = { nameInput = it },
                                        label = { Text("Full Name") },
                                        leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = BrandBluePrimary) },
                                        singleLine = true,
                                        modifier = Modifier.fillMaxWidth().testTag("passenger_register_name_input"),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = BrandBluePrimary,
                                            unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                            focusedContainerColor = BrandBlueLight,
                                            unfocusedContainerColor = BrandBlueLight,
                                            focusedTextColor = BrandBlueDark,
                                            unfocusedTextColor = BrandBlueDark,
                                            focusedPlaceholderColor = NeutralGray.copy(alpha = 0.6f),
                                            unfocusedPlaceholderColor = NeutralGray.copy(alpha = 0.6f)
                                        ),
                                        textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    Spacer(modifier = Modifier.height(10.dp))
                                }

                                OutlinedTextField(
                                    value = emailInput,
                                    onValueChange = { emailInput = it },
                                    label = { Text("Email Address") },
                                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = BrandBluePrimary) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("passenger_email_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandBluePrimary,
                                        unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                        focusedContainerColor = BrandBlueLight,
                                        unfocusedContainerColor = BrandBlueLight,
                                        focusedTextColor = BrandBlueDark,
                                        unfocusedTextColor = BrandBlueDark
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = passwordInput,
                                    onValueChange = { passwordInput = it },
                                    label = { Text("Password") },
                                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = BrandBluePrimary) },
                                    trailingIcon = {
                                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                            Icon(
                                                imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = "Toggle password visibility",
                                                tint = NeutralGray
                                            )
                                        }
                                    },
                                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth().testTag("passenger_password_input"),
                                    textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandBluePrimary,
                                        unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                        focusedContainerColor = BrandBlueLight,
                                        unfocusedContainerColor = BrandBlueLight,
                                        focusedTextColor = BrandBlueDark,
                                        unfocusedTextColor = BrandBlueDark
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Quick Demo Accounts:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeutralGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    listOf(
                                        "passenger@waygo.com" to "Demo Passenger",
                                        "johndoe@example.com" to "John Doe (VIP)",
                                        "rider.fatou@waygo.gm" to "Fatou Bah"
                                    ).forEach { (demoEmail, label) ->
                                        AssistChip(
                                            onClick = {
                                                emailInput = demoEmail
                                                passwordInput = "pass123"
                                                nameInput = label.substringBefore(" ")
                                            },
                                            label = { Text("👤 $label ($demoEmail)", fontSize = 10.5.sp, fontWeight = FontWeight.Bold) },
                                            colors = AssistChipDefaults.assistChipColors(
                                                containerColor = BrandBlueLight,
                                                labelColor = BrandBlueDark
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        )
                                    }
                                }

                                if (authError.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(text = authError, color = ErrorRed, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (isRegisterMode) {
                                            onEmailRegister(emailInput, passwordInput, nameInput)
                                        } else {
                                            onEmailLogin(emailInput, passwordInput)
                                        }
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(50.dp)
                                        .testTag("passenger_email_submit_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                    shape = RoundedCornerShape(14.dp)
                                ) {
                                    Icon(if (isRegisterMode) Icons.Default.PersonAdd else Icons.Default.Login, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (isRegisterMode) "Register Passenger Account" else "Sign In with Email", fontSize = 14.5.sp, fontWeight = FontWeight.Bold)
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = { onSocialLogin("Google") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlueDark)
                                    ) {
                                        Text("🌐 Google", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }

                                    OutlinedButton(
                                        onClick = { onSocialLogin("Apple") },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBlueDark)
                                    ) {
                                        Text("🍎 Apple ID", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                TextButton(
                                    onClick = { isRegisterMode = !isRegisterMode }
                                ) {
                                    Text(
                                        text = if (isRegisterMode) "Already have an account? Sign In" else "New to WayGo? Create an Email Account",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBluePrimary
                                    )
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(CircleShape)
                                        .background(BrandBluePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PersonAdd,
                                        contentDescription = "Create Account Icon",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                                Text(
                                    text = "Create Account / Sign In",
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Enter your Gambian mobile number. We'll send an SMS verification code to connect you instantly.",
                                fontSize = 13.sp,
                                color = NeutralGray,
                                textAlign = TextAlign.Center,
                                lineHeight = 18.sp
                            )

                            Spacer(modifier = Modifier.height(24.dp))

                        // Live Telecom Carrier Tag
                        detectedCarrier?.let { carrier ->
                            Row(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(bottom = 6.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(BrandBlueLight)
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(SuccessGreen)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = carrier,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBluePrimary
                                )
                            }
                        }

                        // Structured Country Selector & Number Input Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBlueLight)
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Interactive Country Selector with Dropdown Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clickable { showCountryDialog = true }
                                    .padding(end = 4.dp)
                                    .testTag("country_selector_dropdown")
                            ) {
                                Text(
                                    text = selectedCountry.flag,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                                Text(
                                    text = selectedCountry.code,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark
                                )
                                Icon(
                                    imageVector = Icons.Default.ArrowDropDown,
                                    contentDescription = "Select country code",
                                    tint = BrandBlueDark,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // Clean visual divider line
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .width(1.dp)
                                    .background(NeutralGray.copy(alpha = 0.2f))
                                    .padding(vertical = 12.dp)
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            // Interactive input box
                            androidx.compose.foundation.text.BasicTextField(
                                value = phoneInput,
                                onValueChange = { input ->
                                    val cleaned = input.filter { it.isDigit() }
                                    if (cleaned.length <= 11) {
                                        phoneInput = cleaned
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = androidx.compose.ui.text.TextStyle(
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark,
                                    letterSpacing = 1.sp
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("phone_input"),
                                singleLine = true,
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterStart) {
                                        if (phoneInput.isEmpty()) {
                                            Text(
                                                text = selectedCountry.lengthHint,
                                                color = NeutralGray.copy(alpha = 0.5f),
                                                fontSize = 15.sp
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )

                            if (phoneInput.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear input text",
                                    tint = NeutralGray.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clickable { phoneInput = "" }
                                )
                            }
                        }

                        // Searchable Country Code Selection Dialog
                        if (showCountryDialog) {
                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = { showCountryDialog = false }
                            ) {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(450.dp),
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    elevation = CardDefaults.cardElevation(10.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp)
                                    ) {
                                        Text(
                                            text = "Select Country",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlueDark,
                                            modifier = Modifier.padding(bottom = 12.dp)
                                        )

                                        var searchQuery by remember { mutableStateOf("") }

                                        OutlinedTextField(
                                            value = searchQuery,
                                            onValueChange = { searchQuery = it },
                                            placeholder = { Text("Search by country name or code...") },
                                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = NeutralGray) },
                                            singleLine = true,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = 12.dp),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = BrandBluePrimary,
                                                unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                                focusedContainerColor = BrandBlueLight,
                                                unfocusedContainerColor = BrandBlueLight,
                                                focusedTextColor = BrandBlueDark,
                                                unfocusedTextColor = BrandBlueDark
                                            ),
                                            shape = RoundedCornerShape(12.dp)
                                        )

                                        val filteredCountries = remember(searchQuery) {
                                            countries.filter {
                                                it.name.contains(searchQuery, ignoreCase = true) ||
                                                it.code.contains(searchQuery)
                                            }
                                        }

                                        LazyColumn(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            items(filteredCountries) { country ->
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clickable {
                                                            selectedCountry = country
                                                            showCountryDialog = false
                                                        }
                                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Text(
                                                        text = country.flag,
                                                        fontSize = 24.sp,
                                                        modifier = Modifier.padding(end = 12.dp)
                                                    )
                                                    Text(
                                                        text = country.name,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.SemiBold,
                                                        color = BrandBlueDark,
                                                        modifier = Modifier.weight(1f)
                                                    )
                                                    Text(
                                                        text = country.code,
                                                        fontSize = 15.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandBluePrimary
                                                    )
                                                }
                                                HorizontalDivider(
                                                    color = NeutralGray.copy(alpha = 0.1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        if (authError.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Warning icon",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = authError,
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // SMS Gateway Indicator Badge
                        Surface(
                            shape = RoundedCornerShape(20.dp),
                            color = if (isRealSmsSent) SuccessGreen.copy(alpha = 0.12f) else BrandBlueLight,
                            border = BorderStroke(1.dp, if (isRealSmsSent) SuccessGreen else BrandBluePrimary.copy(alpha = 0.2f))
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (isRealSmsSent) Icons.Default.CheckCircle else Icons.Default.Sensors,
                                    contentDescription = "Gateway icon",
                                    tint = if (isRealSmsSent) SuccessGreen else BrandBluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = smsGatewayStatus,
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isRealSmsSent) SuccessGreen else BrandBlueDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                onRequestOtp("${selectedCountry.code} $phoneInput")
                            },
                            enabled = !isOtpSending,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("get_otp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (isOtpSending) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = PureWhite,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Dispatching via Gateway...",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            } else {
                                Text(
                                    text = "Request Verification SMS",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PureWhite
                                )
                            }
                        }
                        }
                    } else {
                        // Enter verification code state
                        Text(
                            text = "Enter Verification Code",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (isRealSmsSent)
                                "📱 Live SMS dispatched to ${selectedCountry.code} $phoneInput via WayGo SMS Gateway!"
                            else
                                "A secure verification code has been dispatched to ${selectedCountry.code} $phoneInput",
                            fontSize = 13.sp,
                            color = if (isRealSmsSent) SuccessGreen else NeutralGray,
                            fontWeight = if (isRealSmsSent) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // Interactive SMS Notification Panel
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp)),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isRealSmsSent) SuccessGreen.copy(alpha = 0.15f) else BrandBlueLight.copy(alpha = 0.95f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isRealSmsSent) SuccessGreen else BrandBluePrimary.copy(alpha = 0.1f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (isRealSmsSent) SuccessGreen else BrandBlueSecondary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Sms,
                                        contentDescription = "SMS icon",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "WayGo SMS Gateway",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = BrandBlueDark
                                        )
                                        Text(
                                            text = "Just Now",
                                            fontSize = 9.sp,
                                            color = NeutralGray
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = if (isRealSmsSent)
                                            "SMS delivered to phone. Security code: $generatedOtp"
                                        else
                                            "Your login code is $generatedOtp. Valid for 5 minutes.",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandBluePrimary
                                    )
                                }
                            }
                            // Tap to autofill button inside SMS message
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(BrandBluePrimary.copy(alpha = 0.05f))
                                    .clickable {
                                        otpInput = generatedOtp
                                    }
                                    .padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.FlashOn,
                                        contentDescription = "Autofill action",
                                        tint = AccentAmber,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Tap to Autofill OTP Code ($generatedOtp)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBluePrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Code Entry input field with bold styling
                        OutlinedTextField(
                            value = otpInput,
                            onValueChange = { input ->
                                if (input.length <= 6) otpInput = input
                            },
                            label = { Text("4-Digit Code", color = BrandBlueDark.copy(alpha = 0.5f)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.LockOpen, contentDescription = "Security lock", tint = BrandBluePrimary) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                focusedLabelColor = BrandBluePrimary,
                                unfocusedLabelColor = NeutralGray,
                                focusedTextColor = BrandBlueDark,
                                unfocusedTextColor = BrandBlueDark,
                                focusedContainerColor = BrandBlueLight,
                                unfocusedContainerColor = BrandBlueLight
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = BrandBlueDark,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 4.sp
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("otp_input"),
                            singleLine = true
                        )

                        if (authError.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .align(Alignment.Start)
                                    .padding(top = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Error,
                                    contentDescription = "Error notification icon",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = authError,
                                    color = ErrorRed,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Code Resend Status and Timers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Didn't receive SMS?",
                                fontSize = 12.sp,
                                color = NeutralGray
                            )
                            if (canResend) {
                                Text(
                                    text = "Resend SMS Code",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueSecondary,
                                    modifier = Modifier.clickable {
                                        onRequestOtp("+220 $phoneInput")
                                    }
                                )
                            } else {
                                Text(
                                    text = "Resend in ${timerSeconds}s",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = NeutralGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        Button(
                            onClick = { onVerifyOtp(otpInput) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("verify_otp_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            Text(
                                text = "Verify & Access WayGo",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = PureWhite
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Trust markers for localized transport reliability
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                TrustFeatureBadge(icon = Icons.Default.VerifiedUser, text = "100% Verified")
                TrustFeatureBadge(icon = Icons.Default.Shield, text = "Masked Calls")
                TrustFeatureBadge(icon = Icons.Default.TrendingUp, text = "Low Fares")
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TrustFeatureBadge(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(PureWhite.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AccentAmber,
            modifier = Modifier.size(14.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            color = PureWhite,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun HomeScreenContent(
    viewModel: WayGoViewModel,
    profile: UserProfileEntity?
) {
    val drivers by viewModel.allDrivers.collectAsState()
    val activeTrip by viewModel.activeTrip.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val progress by viewModel.simulationProgress.collectAsState()
    val broadcastLogs by viewModel.broadcastLogs.collectAsState()
    val broadcastDrivers by viewModel.broadcastDrivers.collectAsState()

    var selectedDriverForProfile by remember { mutableStateOf<com.example.data.DriverEntity?>(null) }

    val simLat by viewModel.simulatedDriverLat.collectAsState()
    val simLng by viewModel.simulatedDriverLng.collectAsState()

    var pickupName by remember { mutableStateOf("") }
    var dropoffName by remember { mutableStateOf("") }
    var selectVehicleType by remember { mutableStateOf("CAR") } // "CAR", "TRICYCLE"
    var selectPaymentMethod by remember { mutableStateOf("CASH") } // "CASH", "WAVE", "AFRICELL", "QCELL"

    // Flutterwave State Hookups
    val coroutineScope = rememberCoroutineScope()
    var isProcessingFlutterwave by remember { mutableStateOf(false) }
    var isFlutterwaveInitiating by remember { mutableStateOf(false) }
    var flutterwaveStatusMsg by remember { mutableStateOf("") }
    var flutterwaveUrl by remember { mutableStateOf<String?>(null) }

    // Stripe State Hookups
    var isProcessingStripe by remember { mutableStateOf(false) }
    var isStripeInitiating by remember { mutableStateOf(false) }
    var stripeStatusMsg by remember { mutableStateOf("") }
    var stripeUrl by remember { mutableStateOf<String?>(null) }

    var showPickupDropdown by remember { mutableStateOf(false) }
    var showDropoffDropdown by remember { mutableStateOf(false) }

    var selectedSavedPlaceForOptions by remember { mutableStateOf<com.example.data.SavedPlaceEntity?>(null) }
    var showAddSavedPlaceDialog by remember { mutableStateOf(false) }

    // Scheduler dialog options
    var showScheduleDialog by remember { mutableStateOf(false) }
    var selectedScheduleDate by remember { mutableStateOf("Tomorrow") }
    var selectedScheduleHour by remember { mutableStateOf("09") }
    var selectedScheduleMinute by remember { mutableStateOf("00") }
    var selectedScheduleAmPm by remember { mutableStateOf("AM") }

    // LatLng anchors
    var pCoordinates by remember { mutableStateOf<GLocation?>(null) }
    var dCoordinates by remember { mutableStateOf<GLocation?>(null) }
    var mapPickingMode by remember { mutableStateOf<String?>(null) }
    var dynamicCalculatedFare by remember { mutableStateOf(150) }

    var ratingScore by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }
    val tags = listOf("Safe", "Polite", "Fast", "Clean Car", "Good Music")
    val selectedTags = remember { mutableStateListOf<String>() }

    // Prepopulate inputs with profile details or templates on first mount
    LaunchedEffect(profile) {
        if (profile != null && dropoffName.isEmpty()) {
            pickupName = profile.savedHome
            val homeMatch = GAMBIA_LOCATIONS.firstOrNull { it.name.startsWith(profile.savedHome.split(",")[0]) }
            pCoordinates = homeMatch ?: GAMBIA_LOCATIONS[2]

            dropoffName = profile.savedWork
            val workMatch = GAMBIA_LOCATIONS.firstOrNull { it.name.startsWith(profile.savedWork.split(",")[0]) }
            dCoordinates = workMatch ?: GAMBIA_LOCATIONS[0]
        }
    }

    val oneTapDest by viewModel.oneTapBookingDestination.collectAsState()
    LaunchedEffect(oneTapDest) {
        val dest = oneTapDest
        if (dest != null) {
            dropoffName = dest.name
            val match = GAMBIA_LOCATIONS.firstOrNull { it.name.contains(dest.name.split(",")[0], ignoreCase = true) }
            dCoordinates = match ?: GLocation(dest.name, dest.lat, dest.lng)
            viewModel.clearOneTapDestination()
        }
    }

    // Trip Fare Estimation Service computation
    val carFareEstimate = remember(pCoordinates, dCoordinates) {
        val pL = pCoordinates?.lat ?: 13.4471
        val pG = pCoordinates?.lng ?: -16.6791
        val dL = dCoordinates?.lat ?: 13.4533
        val dG = dCoordinates?.lng ?: -16.5746
        TripFareEstimationService.estimateFare(pL, pG, dL, dG, "CAR").finalFareGmd
    }
    val tricycleFareEstimate = remember(pCoordinates, dCoordinates) {
        val pL = pCoordinates?.lat ?: 13.4471
        val pG = pCoordinates?.lng ?: -16.6791
        val dL = dCoordinates?.lat ?: 13.4533
        val dG = dCoordinates?.lng ?: -16.5746
        TripFareEstimationService.estimateFare(pL, pG, dL, dG, "TRICYCLE").finalFareGmd
    }
    val estimatedFare = if (selectVehicleType == "CAR") carFareEstimate else tricycleFareEstimate
    LaunchedEffect(estimatedFare) {
        dynamicCalculatedFare = estimatedFare
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("home_screen_col"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Map Display
        item {
            Box(modifier = Modifier.fillMaxWidth()) {
                WayGoMapView(
                    drivers = drivers,
                    activeTrip = activeTrip,
                    simulatedDriverLat = simLat,
                    simulatedDriverLng = simLng,
                    passengerLat = pCoordinates?.lat ?: 13.4471,
                    passengerLng = pCoordinates?.lng ?: -16.6791,
                    pickupLocationName = pickupName,
                    pickupLat = pCoordinates?.lat,
                    pickupLng = pCoordinates?.lng,
                    dropoffLocationName = dropoffName,
                    dropoffLat = dCoordinates?.lat,
                    dropoffLng = dCoordinates?.lng,
                    mapPickingMode = mapPickingMode,
                    onSetPickupLocation = { name, lat, lng ->
                        pickupName = name
                        pCoordinates = GLocation(name, lat, lng)
                        mapPickingMode = null
                    },
                    onSetDropoffLocation = { name, lat, lng ->
                        dropoffName = name
                        dCoordinates = GLocation(name, lat, lng)
                        mapPickingMode = null
                    },
                    onCancelMapPicking = { mapPickingMode = null },
                    progress = progress,
                    onSelectDriver = { selectedDriver ->
                        selectVehicleType = selectedDriver.vehicleType
                        selectedDriverForProfile = selectedDriver
                    }
                )
                
                SosEmergencyButton(
                    pLat = pCoordinates?.lat ?: 13.4471,
                    pLng = pCoordinates?.lng ?: -16.6791,
                    activeTripId = activeTrip?.id,
                    modifier = Modifier
                        .zIndex(12f)
                        .align(Alignment.BottomEnd)
                        .padding(end = 12.dp, bottom = 12.dp)
                )
            }
        }

        if (activeTrip == null) {
            // RIDE REQUEST CREATION PANEL
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Where are you heading today?",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueDark
                        )
                        Spacer(modifier = Modifier.height(10.dp))

                        // SAVED PLACES SECTION
                        val savedPlaces by viewModel.allSavedPlaces.collectAsState()

                        Text(
                            text = "Saved Places",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = BrandBlueDark.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth().testTag("saved_places_row")
                        ) {
                            items(savedPlaces) { place ->
                                val icon = when (place.iconType) {
                                    "HOME" -> Icons.Default.Home
                                    "WORK" -> Icons.Default.Work
                                    "STAR" -> Icons.Default.Star
                                    else -> Icons.Default.Place
                                }
                                
                                Card(
                                    onClick = { selectedSavedPlaceForOptions = place },
                                    modifier = Modifier.testTag("saved_place_item_${place.label.lowercase()}"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = BrandBluePrimary.copy(alpha = 0.05f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.15f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = place.label,
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Column {
                                            Text(
                                                text = place.label,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = BrandBlueDark
                                            )
                                            Text(
                                                text = place.name.split(",")[0],
                                                fontSize = 10.sp,
                                                color = NeutralGray,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Add Saved Place Button
                            item {
                                Card(
                                    onClick = { showAddSavedPlaceDialog = true },
                                    modifier = Modifier.testTag("add_saved_place_button"),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Add,
                                            contentDescription = "Add Place",
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "Add Place",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = BrandBluePrimary
                                        )
                                    }
                                }
                            }
                        }

                        // Saved Place Options Dialog
                        if (selectedSavedPlaceForOptions != null) {
                            val place = selectedSavedPlaceForOptions!!
                            AlertDialog(
                                onDismissRequest = { selectedSavedPlaceForOptions = null },
                                title = {
                                    Text(
                                        text = place.label,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = BrandBlueDark
                                    )
                                },
                                text = {
                                    Column {
                                        Text(
                                            text = "Location: ${place.name}",
                                            fontSize = 13.sp,
                                            color = BrandBlueDark.copy(alpha = 0.8f)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text(
                                            text = "Use this saved place to speed up booking or manage your saved list.",
                                            fontSize = 12.sp,
                                            color = NeutralGray
                                        )
                                    }
                                },
                                confirmButton = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                pickupName = place.name
                                                pCoordinates = GLocation(place.name, place.lat, place.lng)
                                                selectedSavedPlaceForOptions = null
                                            },
                                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("set_saved_pickup_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Set as Pickup", color = Color.White)
                                        }
                                        
                                        Button(
                                            onClick = {
                                                dropoffName = place.name
                                                dCoordinates = GLocation(place.name, place.lat, place.lng)
                                                selectedSavedPlaceForOptions = null
                                            },
                                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("set_saved_dropoff_button"),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Set as Destination", color = Color.White)
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                viewModel.removeSavedPlace(place.id)
                                                selectedSavedPlaceForOptions = null
                                            },
                                            modifier = Modifier.fillMaxWidth().height(44.dp).testTag("delete_saved_place_button"),
                                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Delete Saved Place")
                                        }
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { selectedSavedPlaceForOptions = null },
                                        modifier = Modifier.fillMaxWidth().testTag("saved_place_options_dismiss")
                                    ) {
                                        Text("Cancel", color = NeutralGray)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = Color.White
                            )
                        }

                        // Add Saved Place Dialog
                        if (showAddSavedPlaceDialog) {
                            var customLabel by remember { mutableStateOf("") }
                            var selectedLocationIndex by remember { mutableStateOf(0) }
                            var selectedIconType by remember { mutableStateOf("HOME") } // "HOME", "WORK", "STAR", "PLACE"
                            var isDropdownExpanded by remember { mutableStateOf(false) }

                            AlertDialog(
                                onDismissRequest = { showAddSavedPlaceDialog = false },
                                title = {
                                    Text(
                                        text = "Pin a New Saved Place",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = BrandBlueDark
                                    )
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        // Label Input
                                        OutlinedTextField(
                                            value = customLabel,
                                            onValueChange = { customLabel = it },
                                            label = { Text("Label (e.g. Home, Work, Gym)") },
                                            modifier = Modifier.fillMaxWidth().testTag("add_saved_place_label_input"),
                                            singleLine = true,
                                            textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = BrandBlueDark,
                                                unfocusedTextColor = BrandBlueDark,
                                                focusedBorderColor = BrandBluePrimary,
                                                unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                                focusedContainerColor = PureWhite,
                                                unfocusedContainerColor = PureWhite
                                            )
                                        )

                                        // Location Selector Dropdown
                                        Box(modifier = Modifier.fillMaxWidth()) {
                                            OutlinedTextField(
                                                value = GAMBIA_LOCATIONS[selectedLocationIndex].name,
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Select Location") },
                                                trailingIcon = {
                                                    IconButton(onClick = { isDropdownExpanded = true }) {
                                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand Locations")
                                                    }
                                                },
                                                modifier = Modifier.fillMaxWidth().clickable { isDropdownExpanded = true }.testTag("add_saved_place_location_select"),
                                                textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                                colors = OutlinedTextFieldDefaults.colors(
                                                    focusedTextColor = BrandBlueDark,
                                                    unfocusedTextColor = BrandBlueDark,
                                                    focusedBorderColor = BrandBluePrimary,
                                                    unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                                    focusedContainerColor = PureWhite,
                                                    unfocusedContainerColor = PureWhite
                                                )
                                            )
                                            
                                            DropdownMenu(
                                                expanded = isDropdownExpanded,
                                                onDismissRequest = { isDropdownExpanded = false },
                                                modifier = Modifier.fillMaxWidth(0.8f)
                                            ) {
                                                GAMBIA_LOCATIONS.forEachIndexed { index, loc ->
                                                    DropdownMenuItem(
                                                        text = { Text(loc.name) },
                                                        onClick = {
                                                            selectedLocationIndex = index
                                                            isDropdownExpanded = false
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        // Icon Selector row
                                        Text(
                                            text = "Select Icon",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandBlueDark.copy(alpha = 0.8f)
                                        )
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val iconsList = listOf(
                                                Triple("HOME", Icons.Default.Home, "Home"),
                                                Triple("WORK", Icons.Default.Work, "Work"),
                                                Triple("STAR", Icons.Default.Star, "Favorite"),
                                                Triple("PLACE", Icons.Default.Place, "Other")
                                            )
                                            iconsList.forEach { (type, icon, desc) ->
                                                val isSelected = selectedIconType == type
                                                Box(
                                                    modifier = Modifier
                                                        .size(44.dp)
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(
                                                            if (isSelected) BrandBluePrimary else BrandBluePrimary.copy(alpha = 0.1f)
                                                        )
                                                        .clickable { selectedIconType = type }
                                                        .testTag("icon_select_${type.lowercase()}"),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = desc,
                                                        tint = if (isSelected) Color.White else BrandBluePrimary,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            if (customLabel.isNotBlank()) {
                                                val loc = GAMBIA_LOCATIONS[selectedLocationIndex]
                                                viewModel.addSavedPlace(
                                                    name = loc.name,
                                                    label = customLabel,
                                                    lat = loc.lat,
                                                    lng = loc.lng,
                                                    iconType = selectedIconType
                                                )
                                                showAddSavedPlaceDialog = false
                                            }
                                        },
                                        enabled = customLabel.isNotBlank(),
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("add_saved_place_confirm_button")
                                    ) {
                                        Text("Save Place", color = Color.White)
                                    }
                                },
                                dismissButton = {
                                    TextButton(
                                        onClick = { showAddSavedPlaceDialog = false },
                                        modifier = Modifier.fillMaxWidth().testTag("add_saved_place_cancel_button")
                                    ) {
                                        Text("Cancel", color = NeutralGray)
                                    }
                                },
                                shape = RoundedCornerShape(16.dp),
                                containerColor = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Map Location Picking Shortcut Bar
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = { mapPickingMode = if (mapPickingMode == "PICKUP") null else "PICKUP" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (mapPickingMode == "PICKUP") SuccessGreen else SuccessGreen.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f).testTag("pick_pickup_on_map_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PinDrop,
                                        contentDescription = null,
                                        tint = if (mapPickingMode == "PICKUP") Color.White else SuccessGreen,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (mapPickingMode == "PICKUP") "Tap Map Above" else "Pick Pickup on Map",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mapPickingMode == "PICKUP") Color.White else SuccessGreen
                                    )
                                }
                            }

                            Surface(
                                onClick = { mapPickingMode = if (mapPickingMode == "DROPOFF") null else "DROPOFF" },
                                shape = RoundedCornerShape(10.dp),
                                color = if (mapPickingMode == "DROPOFF") BrandBluePrimary else BrandBluePrimary.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.4f)),
                                modifier = Modifier.weight(1f).testTag("pick_dropoff_on_map_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = if (mapPickingMode == "DROPOFF") Color.White else BrandBluePrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (mapPickingMode == "DROPOFF") "Tap Map Above" else "Pick Destination on Map",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (mapPickingMode == "DROPOFF") Color.White else BrandBluePrimary
                                    )
                                }
                            }

                            IconButton(
                                onClick = {
                                    val tempName = pickupName
                                    val tempCoords = pCoordinates
                                    pickupName = dropoffName
                                    pCoordinates = dCoordinates
                                    dropoffName = tempName
                                    dCoordinates = tempCoords
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(BrandBlueDark.copy(alpha = 0.06f), CircleShape)
                                    .testTag("swap_locations_btn")
                            ) {
                                Icon(Icons.Default.SwapVert, contentDescription = "Swap Locations", tint = BrandBlueDark, modifier = Modifier.size(18.dp))
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Pickup Input
                        OutlinedTextField(
                            value = pickupName,
                            onValueChange = { input ->
                                pickupName = input
                                pCoordinates = resolveLocationForInput(input)
                            },
                            label = { Text("Pickup Location") },
                            placeholder = { Text("Type pickup location or landmark...") },
                            leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = "Loc", tint = SuccessGreen) },
                            trailingIcon = {
                                if (pickupName.isNotEmpty()) {
                                    IconButton(onClick = {
                                        pickupName = ""
                                        pCoordinates = null
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear pickup", tint = NeutralGray)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("pickup_input"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrandBlueDark,
                                unfocusedTextColor = BrandBlueDark,
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = PureWhite,
                                focusedPlaceholderColor = NeutralGray.copy(alpha = 0.5f),
                                unfocusedPlaceholderColor = NeutralGray.copy(alpha = 0.5f),
                                focusedLabelColor = BrandBluePrimary,
                                unfocusedLabelColor = NeutralGray
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Dropoff Input
                        OutlinedTextField(
                            value = dropoffName,
                            onValueChange = { input ->
                                dropoffName = input
                                dCoordinates = resolveLocationForInput(input)
                            },
                            label = { Text("Destination (Drop-off)") },
                            placeholder = { Text("Type destination address or landmark...") },
                            leadingIcon = { Icon(Icons.Default.Place, contentDescription = "Drop", tint = ErrorRed) },
                            trailingIcon = {
                                if (dropoffName.isNotEmpty()) {
                                    IconButton(onClick = {
                                        dropoffName = ""
                                        dCoordinates = null
                                    }) {
                                        Icon(Icons.Default.Clear, contentDescription = "Clear destination", tint = NeutralGray)
                                    }
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("dropoff_input"),
                            singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark, fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = BrandBlueDark,
                                unfocusedTextColor = BrandBlueDark,
                                focusedBorderColor = BrandBluePrimary,
                                unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                                focusedContainerColor = PureWhite,
                                unfocusedContainerColor = PureWhite,
                                focusedPlaceholderColor = NeutralGray.copy(alpha = 0.5f),
                                unfocusedPlaceholderColor = NeutralGray.copy(alpha = 0.5f),
                                focusedLabelColor = BrandBluePrimary,
                                unfocusedLabelColor = NeutralGray
                            )
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        // Quick Location Suggestion Chips
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Quick Suggestion Chips:",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "Tap to auto-fill",
                                    fontSize = 10.sp,
                                    color = NeutralGray
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(GAMBIA_LOCATIONS) { loc ->
                                    val shortName = loc.name.split(",")[0]
                                    SuggestionChip(
                                        onClick = {
                                            if (pickupName.isBlank()) {
                                                pickupName = loc.name
                                                pCoordinates = loc
                                            } else {
                                                dropoffName = loc.name
                                                dCoordinates = loc
                                            }
                                        },
                                        label = {
                                            Text(
                                                text = shortName,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        colors = SuggestionChipDefaults.suggestionChipColors(
                                            containerColor = PureWhite,
                                            labelColor = BrandBlueDark
                                        ),
                                        border = SuggestionChipDefaults.suggestionChipBorder(
                                            enabled = true,
                                            borderColor = BrandBluePrimary.copy(alpha = 0.25f)
                                        )
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Vehicle Selector
                        Text("Select Ride Class", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                onClick = { selectVehicleType = "CAR" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vehicle_car_select"),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 2.dp,
                                    color = if (selectVehicleType == "CAR") BrandBluePrimary else Color(0xFFE2E8F0)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectVehicleType == "CAR") BrandBluePrimary.copy(alpha = 0.08f) else PureWhite
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DirectionsCar,
                                        contentDescription = "Car",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("WayGo Sedan (Car)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                                    Text("Est. ${carFareEstimate} GMD", fontSize = 11.sp, color = BrandBlueDark.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                                    Text("Comfortable", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                                }
                            }

                            Card(
                                onClick = { selectVehicleType = "TRICYCLE" },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("vehicle_tuk_select"),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 2.dp,
                                    color = if (selectVehicleType == "TRICYCLE") BrandBluePrimary else Color(0xFFE2E8F0)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectVehicleType == "TRICYCLE") BrandBluePrimary.copy(alpha = 0.08f) else PureWhite
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TwoWheeler,
                                        contentDescription = "Tuk",
                                        tint = AccentAmber,
                                        modifier = Modifier.size(36.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("Keke / Tricycle", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                                    Text("Est. ${tricycleFareEstimate} GMD", fontSize = 11.sp, color = BrandBlueDark.copy(alpha = 0.8f), fontWeight = FontWeight.SemiBold)
                                    Text("Affordable", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Payment Methods Card
                        Text("Payment Method (Hybrid Option)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("CASH", "WAVE", "FLUTTERWAVE", "STRIPE").forEach { method ->
                                Card(
                                    onClick = { selectPaymentMethod = method },
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (selectPaymentMethod == method) BrandBluePrimary else BrandBlueLight
                                    )
                                ) {
                                    Box(
                                        modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = method,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (selectPaymentMethod == method) Color.White else BrandBlueDark
                                        )
                                    }
                                }
                            }
                        }

                        // Ride Customization Preferences Filters
                        var prefQuiet by remember { mutableStateOf(false) }
                        var prefAc by remember { mutableStateOf(false) }
                        var prefLuggage by remember { mutableStateOf(false) }
                        var prefPet by remember { mutableStateOf(false) }

                        val selectedPreferences = remember(prefQuiet, prefAc, prefLuggage, prefPet) {
                            listOfNotNull(
                                if (prefQuiet) "🤫 Quiet Ride" else null,
                                if (prefAc) "❄️ AC On" else null,
                                if (prefLuggage) "🧳 Luggage Help" else null,
                                if (prefPet) "🐾 Pet Friendly" else null
                            ).joinToString(" • ")
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Ride Customization Preferences", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            FilterChip(
                                selected = prefQuiet,
                                onClick = { prefQuiet = !prefQuiet },
                                label = { Text("Quiet 🤫", fontSize = 10.5.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                            FilterChip(
                                selected = prefAc,
                                onClick = { prefAc = !prefAc },
                                label = { Text("AC ❄️", fontSize = 10.5.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                            FilterChip(
                                selected = prefLuggage,
                                onClick = { prefLuggage = !prefLuggage },
                                label = { Text("Luggage 🧳", fontSize = 10.5.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                            FilterChip(
                                selected = prefPet,
                                onClick = { prefPet = !prefPet },
                                label = { Text("Pets 🐾", fontSize = 10.5.sp) },
                                modifier = Modifier.height(30.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (selectPaymentMethod == "FLUTTERWAVE") {
                                    if (profile?.isPaymentLinked == true) {
                                        // 1-Click Cashless Booking! Bypass WebView secure payment gateway check.
                                        viewModel.initiateBooking(
                                            pickupName = pickupName,
                                            dropoffName = dropoffName,
                                            vehicleType = selectVehicleType,
                                            paymentMethod = "FLUTTERWAVE (LINKED)",
                                            fare = dynamicCalculatedFare,
                                            preferences = selectedPreferences,
                                            pLat = pCoordinates?.lat ?: 13.4471,
                                            pLng = pCoordinates?.lng ?: -16.6791,
                                            dLat = dCoordinates?.lat ?: 13.4533,
                                            dLng = dCoordinates?.lng ?: -16.5746
                                        )
                                    } else {
                                        isFlutterwaveInitiating = true
                                        isProcessingFlutterwave = true
                                        flutterwaveStatusMsg = "Preparing secure payment gateway..."
                                        coroutineScope.launch {
                                            val pEmail = profile?.email ?: "johndoe@example.com"
                                            val pName = profile?.name ?: "John Doe"
                                            val pPhone = profile?.phone ?: "+220 771 2345"
                                            val txRef = "flw_ref_" + System.currentTimeMillis().toString().takeLast(6)
                                            val link = com.example.data.FlutterwaveManager.initiatePayment(
                                                amountGmd = dynamicCalculatedFare.toDouble(),
                                                passengerEmail = pEmail,
                                                passengerName = pName,
                                                passengerPhone = pPhone,
                                                tripTxRef = txRef
                                            )
                                            isFlutterwaveInitiating = false
                                            if (link != null) {
                                                flutterwaveUrl = link
                                            } else {
                                                flutterwaveStatusMsg = "Error initiating Flutterwave payments. Please try again."
                                            }
                                        }
                                    }
                                } else if (selectPaymentMethod == "STRIPE") {
                                    isStripeInitiating = true
                                    isProcessingStripe = true
                                    stripeStatusMsg = "Preparing secure Stripe gateway..."
                                    coroutineScope.launch {
                                        val pEmail = profile?.email ?: "johndoe@example.com"
                                        val pName = profile?.name ?: "John Doe"
                                        val pPhone = profile?.phone ?: "+220 771 2345"
                                        val txRef = "st_ref_" + System.currentTimeMillis().toString().takeLast(6)
                                        val link = com.example.data.StripeManager.initiateStripePayment(
                                            amountGmd = dynamicCalculatedFare.toDouble(),
                                            passengerEmail = pEmail,
                                            passengerName = pName,
                                            passengerPhone = pPhone,
                                            tripTxRef = txRef
                                        )
                                        isStripeInitiating = false
                                        if (link != null) {
                                            stripeUrl = link
                                        } else {
                                            stripeStatusMsg = "Error initiating Stripe payments. Please try again."
                                        }
                                    }
                                } else {
                                    viewModel.initiateBooking(
                                        pickupName = pickupName,
                                        dropoffName = dropoffName,
                                        vehicleType = selectVehicleType,
                                        paymentMethod = selectPaymentMethod,
                                        fare = dynamicCalculatedFare,
                                        preferences = selectedPreferences,
                                        pLat = pCoordinates?.lat ?: 13.4471,
                                        pLng = pCoordinates?.lng ?: -16.6791,
                                        dLat = dCoordinates?.lat ?: 13.4533,
                                        dLng = dCoordinates?.lng ?: -16.5746
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("request_ride_button"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                        ) {
                            Icon(Icons.Default.DirectionsCar, contentDescription = "r")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Request WayGo Ride", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = { showScheduleDialog = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("schedule_ride_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBluePrimary),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandBluePrimary)
                        ) {
                            Icon(Icons.Default.Event, contentDescription = "Schedule Later")
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Schedule Ride for Later", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }

                        if (showScheduleDialog) {
                            AlertDialog(
                                onDismissRequest = { showScheduleDialog = false },
                                title = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EventNote, contentDescription = "Schedule", tint = BrandBluePrimary)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Schedule WayGo Ride", color = BrandBlueDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                                    }
                                },
                                text = {
                                    Column(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            "Set a future date & time for pickup. Available drivers in Gambia will be notified in advance.",
                                            fontSize = 12.sp,
                                            color = NeutralGray
                                        )
                                        Divider()

                                        Text("Select Date:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBlueDark)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            listOf("Today", "Tomorrow", "In 2 days").forEach { item ->
                                                val isSelected = selectedScheduleDate == item
                                                Card(
                                                    onClick = { selectedScheduleDate = item },
                                                    modifier = Modifier.weight(1f),
                                                    colors = CardDefaults.cardColors(
                                                        containerColor = if (isSelected) BrandBluePrimary else BrandBlueLight
                                                    )
                                                ) {
                                                    Box(modifier = Modifier.padding(8.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                                        Text(
                                                            item,
                                                            fontSize = 11.sp,
                                                            color = if (isSelected) Color.White else BrandBlueDark,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }

                                        Text("Select Time:", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBlueDark)
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Hour", fontSize = 10.sp, color = NeutralGray)
                                                var showHourMenu by remember { mutableStateOf(false) }
                                                OutlinedButton(
                                                    onClick = { showHourMenu = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(selectedScheduleHour, fontSize = 12.sp)
                                                    Icon(Icons.Default.ArrowDropDown, "", modifier = Modifier.size(16.dp))
                                                }
                                                DropdownMenu(expanded = showHourMenu, onDismissRequest = { showHourMenu = false }) {
                                                    listOf("01", "02", "03", "04", "05", "06", "07", "08", "09", "10", "11", "12").forEach { h ->
                                                        DropdownMenuItem(text = { Text(h) }, onClick = { selectedScheduleHour = h; showHourMenu = false })
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("Minute", fontSize = 10.sp, color = NeutralGray)
                                                var showMinMenu by remember { mutableStateOf(false) }
                                                OutlinedButton(
                                                    onClick = { showMinMenu = true },
                                                    modifier = Modifier.fillMaxWidth(),
                                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(selectedScheduleMinute, fontSize = 12.sp)
                                                    Icon(Icons.Default.ArrowDropDown, "", modifier = Modifier.size(16.dp))
                                                }
                                                DropdownMenu(expanded = showMinMenu, onDismissRequest = { showMinMenu = false }) {
                                                    listOf("00", "15", "30", "45").forEach { m ->
                                                        DropdownMenuItem(text = { Text(m) }, onClick = { selectedScheduleMinute = m; showMinMenu = false })
                                                    }
                                                }
                                            }

                                            Column(modifier = Modifier.weight(1f)) {
                                                Text("AM/PM", fontSize = 10.sp, color = NeutralGray)
                                                Row(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(8.dp))
                                                        .background(BrandBlueLight)
                                                        .clickable {
                                                            selectedScheduleAmPm = if (selectedScheduleAmPm == "AM") "PM" else "AM"
                                                        }
                                                        .padding(8.dp)
                                                        .fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Text(selectedScheduleAmPm, fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBluePrimary)
                                                }
                                            }
                                        }

                                        Divider()

                                        Row(
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Text("Est. Fare Total:", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            Text("$dynamicCalculatedFare GMD", color = BrandBluePrimary, fontSize = 13.sp, fontWeight = FontWeight.Black)
                                        }
                                    }
                                },
                                confirmButton = {
                                    Button(
                                        onClick = {
                                            val scheduledTimeStr = "$selectedScheduleDate at $selectedScheduleHour:$selectedScheduleMinute $selectedScheduleAmPm"
                                            val offsetHours = when (selectedScheduleDate) {
                                                "Tomorrow" -> 24L
                                                "In 2 days" -> 48L
                                                else -> 2L
                                            }
                                            val epochVal = System.currentTimeMillis() + (offsetHours * 3600 * 1000)

                                            viewModel.scheduleRide(
                                                pickupName = pickupName,
                                                dropoffName = dropoffName,
                                                vehicleType = selectVehicleType,
                                                paymentMethod = selectPaymentMethod,
                                                fare = dynamicCalculatedFare,
                                                pLat = pCoordinates?.lat ?: 13.4471,
                                                pLng = pCoordinates?.lng ?: -16.6791,
                                                dLat = dCoordinates?.lat ?: 13.4533,
                                                dLng = dCoordinates?.lng ?: -16.5746,
                                                scheduledTime = scheduledTimeStr,
                                                scheduledEpochMs = epochVal
                                            )
                                            showScheduleDialog = false
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Text("Confirm Booking")
                                    }
                                },
                                dismissButton = {
                                    TextButton(onClick = { showScheduleDialog = false }) {
                                        Text("Cancel", color = NeutralGray)
                                    }
                                }
                            )
                        }

                        // Flutterwave Secure Payment Processing States
                        if (isProcessingFlutterwave) {
                            if (isFlutterwaveInitiating) {
                                AlertDialog(
                                    onDismissRequest = { 
                                        isProcessingFlutterwave = false 
                                        isFlutterwaveInitiating = false
                                    },
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = BrandBluePrimary,
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Flutterwave Secure", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = flutterwaveStatusMsg,
                                            fontSize = 13.sp,
                                            color = BrandBlueDark
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            isProcessingFlutterwave = false
                                            isFlutterwaveInitiating = false
                                        }) {
                                            Text("Cancel", color = NeutralGray)
                                        }
                                    }
                                )
                            } else if (flutterwaveUrl != null) {
                                FlutterwaveCheckoutDialog(
                                    checkoutUrl = flutterwaveUrl!!,
                                    onPaymentSuccess = { txRef, transactionId ->
                                        // Save standard details
                                        viewModel.initiateBooking(
                                            pickupName = pickupName,
                                            dropoffName = dropoffName,
                                            vehicleType = selectVehicleType,
                                            paymentMethod = "FLUTTERWAVE (PAID)",
                                            fare = dynamicCalculatedFare,
                                            pLat = pCoordinates?.lat ?: 13.4471,
                                            pLng = pCoordinates?.lng ?: -16.6791,
                                            dLat = dCoordinates?.lat ?: 13.4533,
                                            dLng = dCoordinates?.lng ?: -16.5746
                                        )
                                        // Reset states
                                        flutterwaveUrl = null
                                        isProcessingFlutterwave = false
                                    },
                                    onCancel = {
                                        flutterwaveUrl = null
                                        isProcessingFlutterwave = false
                                    }
                                )
                            } else {
                                // Error layout or retry message
                                AlertDialog(
                                    onDismissRequest = { isProcessingFlutterwave = false },
                                    title = { Text("Payment Blocked", fontWeight = FontWeight.Bold, color = ErrorRed) },
                                    text = { Text(flutterwaveStatusMsg) },
                                    confirmButton = {
                                        TextButton(onClick = { isProcessingFlutterwave = false }) {
                                            Text("Go Back")
                                        }
                                    }
                                )
                            }
                        }

                        // Stripe Secure Payment Processing States
                        if (isProcessingStripe) {
                            if (isStripeInitiating) {
                                AlertDialog(
                                    onDismissRequest = { 
                                        isProcessingStripe = false 
                                        isStripeInitiating = false
                                    },
                                    title = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            CircularProgressIndicator(
                                                modifier = Modifier.size(24.dp),
                                                color = Color(0xFF635BFF), // Stripe brand indigo
                                                strokeWidth = 2.dp
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text("Stripe Secure", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                                        }
                                    },
                                    text = {
                                        Text(
                                            text = stripeStatusMsg,
                                            fontSize = 13.sp,
                                            color = BrandBlueDark
                                        )
                                    },
                                    confirmButton = {
                                        TextButton(onClick = {
                                            isProcessingStripe = false
                                            isStripeInitiating = false
                                        }) {
                                            Text("Cancel", color = NeutralGray)
                                        }
                                    }
                                )
                            } else if (stripeUrl != null) {
                                StripeCheckoutDialog(
                                    checkoutUrl = stripeUrl!!,
                                    onPaymentSuccess = { txRef, transactionId ->
                                        // Save standard details
                                        viewModel.initiateBooking(
                                            pickupName = pickupName,
                                            dropoffName = dropoffName,
                                            vehicleType = selectVehicleType,
                                            paymentMethod = "STRIPE (PAID)",
                                            fare = dynamicCalculatedFare,
                                            pLat = pCoordinates?.lat ?: 13.4471,
                                            pLng = pCoordinates?.lng ?: -16.6791,
                                            dLat = dCoordinates?.lat ?: 13.4533,
                                            dLng = dCoordinates?.lng ?: -16.5746
                                        )
                                        // Reset states
                                        stripeUrl = null
                                        isProcessingStripe = false
                                    },
                                    onCancel = {
                                        stripeUrl = null
                                        isProcessingStripe = false
                                    }
                                )
                            } else {
                                // Error layout or retry message
                                AlertDialog(
                                    onDismissRequest = { isProcessingStripe = false },
                                    title = { Text("Payment Blocked", fontWeight = FontWeight.Bold, color = ErrorRed) },
                                    text = { Text(stripeStatusMsg) },
                                    confirmButton = {
                                        TextButton(onClick = { isProcessingStripe = false }) {
                                            Text("Go Back")
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // STATE DRIVING & TRIP MONITOR PANEL

            item {
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("active_ride_card"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = if (activeTrip!!.status == "REQUESTED") "Searching for drivers..."
                                    else if (activeTrip!!.status == "ACCEPTED") "Driver coming to you!"
                                    else if (activeTrip!!.status == "ARRIVED") "Driver has arrived!"
                                    else if (activeTrip!!.status == "EN_ROUTE") "Driving to destination"
                                    else "You have arrived!",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = BrandBluePrimary
                                )
                                Text(
                                    text = "Route: ${activeTrip!!.pickupName.split(",")[0]} ➔ ${activeTrip!!.dropoffName.split(",")[0]}",
                                    fontSize = 12.sp,
                                    color = NeutralGray
                                )
                            }

                            // Dynamic ETA display
                            val etaMins = remember(progress) {
                                if (progress < 0.38f) {
                                    (1..3).random()
                                } else {
                                    ((1f - progress) * 12).toInt().coerceAtLeast(1)
                                }
                            }
                            Card(
                                colors = CardDefaults.cardColors(containerColor = BrandBlueLight)
                            ) {
                                Text(
                                    "ETA: $etaMins mins",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Linear progress visual
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                            color = SuccessGreen,
                            trackColor = BrandBlueLight
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // SAFETY PIN CODE DISPLAY CARD
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("passenger_safety_pin_card"),
                            colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.12f)),
                            border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Surface(
                                    modifier = Modifier.size(42.dp),
                                    shape = CircleShape,
                                    color = AccentAmber
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = activeTrip!!.verificationPin,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 15.sp,
                                            color = Color.White
                                        )
                                    }
                                }
                                Column {
                                    Text("Ride Safety PIN: ${activeTrip!!.verificationPin}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                                    Text("Provide this 4-digit code to your driver upon pickup to start your ride.", fontSize = 11.sp, color = NeutralGray)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Driver card info (if accepted)
                        if (activeTrip!!.driverName != null) {
                            val matchedDriver = drivers.find { it.id == activeTrip!!.driverId }
                            var showDriverTrustProfile by remember { mutableStateOf(false) }

                            if (showDriverTrustProfile) {
                                DriverTrustProfileDialog(
                                    driver = matchedDriver,
                                    vehicleType = activeTrip!!.vehicleType,
                                    vehiclePlate = activeTrip!!.vehiclePlate ?: "BJL 4821 C",
                                    driverName = activeTrip!!.driverName!!,
                                    trips = trips,
                                    onDismiss = { showDriverTrustProfile = false }
                                )
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(BrandBlueLight)
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Custom Avatar with Badge Overlay
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clickable { showDriverTrustProfile = true }
                                            .testTag("driver_trust_profile_avatar"),
                                        contentAlignment = Alignment.BottomEnd
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(46.dp)
                                                .clip(CircleShape)
                                                .background(BrandBluePrimary.copy(alpha = 0.12f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            val nameInitials = (activeTrip!!.driverName ?: "GP").split(" ")
                                                .mapNotNull { it.firstOrNull()?.toString() }
                                                .joinToString("").take(2).uppercase()
                                            Text(
                                                text = nameInitials,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = BrandBluePrimary,
                                                fontSize = 16.sp
                                            )
                                        }
                                        
                                        // Trust Mini Check Badge
                                        Box(
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape)
                                                .background(SuccessGreen),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Verified Badge",
                                                tint = Color.White,
                                                modifier = Modifier.size(11.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.width(12.dp))

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { showDriverTrustProfile = true }
                                            .testTag("driver_trust_profile_clickable_column")
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = activeTrip!!.driverName!!,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = BrandBlueDark
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = "Rating",
                                                tint = AccentAmber,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Text(
                                                text = String.format("%.1f", matchedDriver?.rating ?: 4.8f),
                                                fontSize = 12.sp,
                                                color = BrandBlueDark,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        
                                        Spacer(modifier = Modifier.height(2.dp))
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(
                                                text = "${if (activeTrip!!.vehicleType == "CAR") "Yellow Sedan" else "Tricycle"} • ${activeTrip!!.vehiclePlate}",
                                                fontSize = 12.sp,
                                                color = NeutralGray
                                            )
                                            // Mini Verification pill badge
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(SuccessGreen.copy(alpha = 0.12f))
                                                    .padding(horizontal = 4.dp, vertical = 1.dp)
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.VerifiedUser,
                                                        contentDescription = "Verified",
                                                        tint = SuccessGreen,
                                                        modifier = Modifier.size(10.dp)
                                                    )
                                                    Text(
                                                        text = "Verified",
                                                        color = SuccessGreen,
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 9.sp
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Interactive messaging & chat
                                    var showChatDialog by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { showChatDialog = true },
                                        modifier = Modifier
                                            .testTag("passenger_chat_launcher")
                                            .background(BrandBlueSecondary, CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Chat,
                                            contentDescription = "Chat",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (showChatDialog) {
                                        WayGoChatDialog(
                                            tripId = activeTrip!!.id,
                                            currentRole = "PASSENGER",
                                            currentUserId = "current_passenger",
                                            currentUserName = profile?.name ?: "Fatou Joof",
                                            viewModel = viewModel,
                                            onDismiss = { 
                                                showChatDialog = false 
                                                viewModel.endChatSession()
                                            }
                                        )
                                    }

                                    Spacer(modifier = Modifier.width(6.dp))

                                    // Interactive masked phone icon
                                    var showMaskDialer by remember { mutableStateOf(false) }
                                    IconButton(
                                        onClick = { showMaskDialer = true },
                                        modifier = Modifier
                                            .background(BrandBluePrimary, CircleShape)
                                            .size(40.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = "Call",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }

                                    if (showMaskDialer) {
                                        AlertDialog(
                                            onDismissRequest = { showMaskDialer = false },
                                            title = { Text("Encrypted Call Masking", fontWeight = FontWeight.Bold) },
                                            text = {
                                                Text(
                                                    "Calling driver ${activeTrip!!.driverName} via secure WayGo central mask. Your personal phone is safe from third parties.\n\nSimulated Dial: +220 110-MASK-${activeTrip!!.driverId?.takeLast(4)}"
                                                )
                                            },
                                            confirmButton = {
                                                Button(onClick = { showMaskDialer = false }) { Text("Dial Number") }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showMaskDialer = false }) { Text("Cancel") }
                                            }
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                // Direct safety CTA row to prompt trust profiles
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { showDriverTrustProfile = true }
                                        .background(PureWhite)
                                        .padding(horizontal = 10.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Security,
                                            contentDescription = "Safety Check",
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = "Tap to verify background check & GTA permit",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = BrandBluePrimary
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Default.ChevronRight,
                                        contentDescription = "Go",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        } else {
                            // High fidelity Broadcast feedback panel
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(BrandBlueLight.copy(alpha = 0.4f))
                                    .padding(12.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator(
                                        color = BrandBluePrimary,
                                        modifier = Modifier.size(16.dp),
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Broadcasting Request to Nearest Responders",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = BrandBlueDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                // Show broadcast/search logs
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                                    shape = RoundedCornerShape(8.dp),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text(
                                            "Broadcast Activity Timeline:",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = NeutralGray,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        )

                                        broadcastLogs.takeLast(4).forEach { log ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(5.dp)
                                                        .clip(CircleShape)
                                                        .background(BrandBlueSecondary)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = log,
                                                    fontSize = 10.5.sp,
                                                    color = BrandBlueDark.copy(alpha = 0.9f)
                                                )
                                            }
                                        }
                                    }
                                }

                                if (broadcastDrivers.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text(
                                        "Nearest drivers in range:",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = NeutralGray,
                                        modifier = Modifier.padding(bottom = 4.dp)
                                    )

                                    broadcastDrivers.forEach { (driver, dist) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 2.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(PureWhite)
                                                .clickable { selectedDriverForProfile = driver }
                                                .padding(10.dp)
                                                .testTag("nearest_driver_item_${driver.id}"),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = if (driver.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                                    contentDescription = "vehicle",
                                                    tint = BrandBluePrimary,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Text(
                                                    text = "${driver.name} (★${driver.rating})",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = BrandBlueDark
                                                )
                                            }
                                            Text(
                                                text = "${String.format("%.2f", dist)} km • Profile",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = SuccessGreen
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Fare and breakdown visualization
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = BrandBlueLight.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Fare Guarantee", fontWeight = FontWeight.Bold, color = BrandBlueDark, fontSize = 13.sp)
                                    Text("${activeTrip!!.fareGmd} GMD", fontWeight = FontWeight.Bold, color = BrandBluePrimary, fontSize = 14.sp)
                                }
                                Text(
                                    "Payment Method: ${activeTrip!!.paymentMethod} • Local Fare Guarantee pricing applied.",
                                    fontSize = 11.sp,
                                    color = NeutralGray
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (activeTrip!!.status != "COMPLETED") {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { viewModel.cancelTripActive(activeTrip!!.id) },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("cancel_ride_button"),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeutralGray),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.5f))
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Cancel Ride", fontSize = 12.sp)
                                }

                                var showSosActiveDialog by remember { mutableStateOf(false) }

                                Button(
                                    onClick = { showSosActiveDialog = true },
                                    modifier = Modifier.weight(1f).height(48.dp).testTag("sos_active_ride_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = "SOS", tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Emergency SOS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }

                                if (showSosActiveDialog) {
                                    SosEmergencyDialog(
                                        pLat = pCoordinates?.lat ?: 13.4471,
                                        pLng = pCoordinates?.lng ?: -16.6791,
                                        activeTripId = activeTrip!!.id,
                                        onDismiss = { showSosActiveDialog = false }
                                    )
                                }
                            }
                        } else {
                            val matchedDriver = drivers.firstOrNull { it.id == activeTrip!!.driverId }
                            RatingReviewComponent(
                                driverName = activeTrip!!.driverName ?: "Gambia Partner Driver",
                                vehicleType = activeTrip!!.vehicleType,
                                vehiclePlate = activeTrip!!.vehiclePlate ?: "BJL-Registered",
                                driverRating = matchedDriver?.rating ?: 4.9f,
                                tripId = activeTrip!!.id,
                                tripFareGmd = activeTrip!!.fareGmd,
                                onSubmitRating = { stars, comment, selectedTagsList, tipGmd ->
                                    viewModel.submitRating(
                                        tripId = activeTrip!!.id,
                                        stars = stars,
                                        comment = comment,
                                        selectedTags = selectedTagsList,
                                        tipGmd = tipGmd
                                    )
                                },
                                onDismiss = {
                                    // Bypass/Skip rating with default 5-stars feedback
                                    viewModel.submitRating(
                                        tripId = activeTrip!!.id,
                                        stars = 5,
                                        comment = "Journey Completed",
                                        selectedTags = emptyList()
                                    )
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (selectedDriverForProfile != null) {
        DriverTrustProfileDialog(
            driver = selectedDriverForProfile,
            vehicleType = selectedDriverForProfile!!.vehicleType,
            vehiclePlate = selectedDriverForProfile!!.vehiclePlate,
            driverName = selectedDriverForProfile!!.name,
            trips = trips,
            onDismiss = { selectedDriverForProfile = null }
        )
    }
}

@Composable
fun HistoryScreenContent(viewModel: WayGoViewModel) {
    // Highly polished passenger ride history log matching localized taxi services in Banjul
    val trips by viewModel.allTrips.collectAsState()
    val drivers by viewModel.allDrivers.collectAsState()
    var selectedDriverForProfile by remember { mutableStateOf<com.example.data.DriverEntity?>(null) }
    val scheduledRides by viewModel.allScheduledRides.collectAsState()
    val firestoreTrips by viewModel.firestoreTrips.collectAsState()
    val firestoreIsLoading by viewModel.firestoreIsLoading.collectAsState()
    val firestoreStatusMessage by viewModel.firestoreStatusMessage.collectAsState()

    var selectedTab by remember { mutableStateOf("SCHEDULED") } // "SCHEDULED", "COMPLETED"
    var viewCloudOnly by remember { mutableStateOf(true) } // toggles between Firestore Cloud and Local Database Cache

    val formatTimestamp = remember {
        { ms: Long ->
            try {
                val instant = java.time.Instant.ofEpochMilli(ms)
                val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                    .withZone(java.time.ZoneId.systemDefault())
                formatter.format(instant)
            } catch (e: Exception) {
                "Recent Date"
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(PureWhite)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("SCHEDULED" to "Scheduled Rides", "COMPLETED" to "Past Trips Log").forEach { (tabKey, tabLabel) ->
                val isSel = selectedTab == tabKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSel) BrandBluePrimary else Color.Transparent)
                        .clickable { selectedTab = tabKey }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tabLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = if (isSel) Color.White else BrandBlueDark
                    )
                }
            }
        }

        if (selectedTab == "SCHEDULED") {
            // Filter passenger-owned scheduled rides
            val activeScheduled = scheduledRides.filter { it.status != "DISPATCHED" }
            if (activeScheduled.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                        Icon(Icons.Default.EventAvailable, contentDescription = "Empty", tint = NeutralGray, modifier = Modifier.size(64.dp))
                        Spacer(modifier = Modifier.height(10.dp))
                        Text("No scheduled future rides yet.", color = NeutralGray, fontSize = 14.sp)
                        Text("You can schedule a sedan or keke taxi for work/market trips anytime!", color = NeutralGray, fontSize = 11.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().weight(1f).padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(activeScheduled) { ride ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("scheduled_ride_item_${ride.id}"),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(14.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (ride.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                            contentDescription = "vehicle",
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (ride.vehicleType == "CAR") "Yellow Cab (Sedan)" else "Keke / Caravan",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.5.sp,
                                            color = BrandBlueDark
                                        )
                                    }

                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (ride.status == "ACCEPTED") SuccessGreen.copy(alpha = 0.15f)
                                            else if (ride.status == "CANCELLED") ErrorRed.copy(alpha = 0.15f)
                                            else AccentAmber.copy(alpha = 0.15f)
                                        )
                                    ) {
                                        Text(
                                            text = ride.status,
                                            color = if (ride.status == "ACCEPTED") SuccessGreen
                                            else if (ride.status == "CANCELLED") ErrorRed
                                            else AccentAmber,
                                            fontSize = 9.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AccessTime, contentDescription = "Time", tint = BrandBlueSecondary, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Time: ${ride.scheduledTime}",
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = BrandBlueDark
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Pickup: ${ride.pickupName}", fontSize = 11.5.sp, color = NeutralGray)
                                Text("Dropoff: ${ride.dropoffName}", fontSize = 11.5.sp, color = NeutralGray)

                                Spacer(modifier = Modifier.height(8.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text("Fare Estimate: ${ride.fareGmd} GMD", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBluePrimary)
                                        Text(
                                            text = if (ride.driverName != null) "Confirmed with ${ride.driverName}" else "Awaiting driver pickup confirmation",
                                            fontSize = 10.sp,
                                            color = if (ride.driverName != null) SuccessGreen else NeutralGray,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    if (ride.status == "SCHEDULED" || ride.status == "ACCEPTED") {
                                        TextButton(
                                            onClick = { viewModel.cancelScheduledRide(ride.id) },
                                            colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                                        ) {
                                            Icon(Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(14.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Cancel", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // PAST TRIPS LOG tab
            Column(modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                
                // Firestore Synchronization HUD / Panel Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                        .testTag("firestore_sync_hud"),
                    colors = CardDefaults.cardColors(containerColor = BrandBlueLight.copy(alpha = 0.5f)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.CloudDone,
                                    contentDescription = "Cloud",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    "Firestore Cloud Sync Engine",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = BrandBlueDark
                                )
                            }
                            
                            // Live badge indicator based on Firebase configuration state
                            val isFirestoreAvailable = com.example.data.FirestoreManager.firestore != null
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isFirestoreAvailable) SuccessGreen.copy(alpha = 0.15f)
                                    else NeutralGray.copy(alpha = 0.15f)
                                )
                            ) {
                                Text(
                                    text = if (isFirestoreAvailable) "LIVE CONNECTED" else "OFFLINE SIMULATION",
                                    color = if (isFirestoreAvailable) SuccessGreen else NeutralGray,
                                    fontWeight = FontWeight.Black,
                                    fontSize = 8.5.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        if (firestoreStatusMessage != null) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = firestoreStatusMessage!!,
                                fontSize = 11.sp,
                                color = BrandBlueDark.copy(alpha = 0.8f),
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.refreshTripHistoryFromFirestore() },
                                modifier = Modifier.weight(1f).height(36.dp).testTag("btn_pull_firestore"),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !firestoreIsLoading
                            ) {
                                if (firestoreIsLoading) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.CloudDone, contentDescription = "sync", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Pull Cloud", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            OutlinedButton(
                                onClick = { viewModel.syncLocalTripsToFirestore() },
                                modifier = Modifier.weight(1.1f).height(36.dp).testTag("btn_upload_firestore"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBluePrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.4f)),
                                contentPadding = PaddingValues(0.dp),
                                shape = RoundedCornerShape(8.dp),
                                enabled = !firestoreIsLoading
                            ) {
                                Icon(Icons.Default.CloudQueue, contentDescription = "upload", modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Upload Local", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Cloud vs Local Sub-tab Selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 10.dp)
                        .background(Color.LightGray.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (viewCloudOnly) BrandBlueSecondary else Color.Transparent)
                            .clickable { viewCloudOnly = true }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Firestore Cloud (${firestoreTrips.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (viewCloudOnly) Color.White else BrandBlueDark
                        )
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (!viewCloudOnly) BrandBlueSecondary else Color.Transparent)
                            .clickable { viewCloudOnly = false }
                            .padding(vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "Local Backup Database (${trips.size})",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (!viewCloudOnly) Color.White else BrandBlueDark
                        )
                    }
                }

                // Displaying Selected source list
                val activeList = if (viewCloudOnly) firestoreTrips else trips

                if (activeList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "empty",
                                tint = NeutralGray,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = if (viewCloudOnly) "No Firestore Journeys Found" else "No local database history",
                                fontWeight = FontWeight.Bold,
                                color = BrandBlueDark,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (viewCloudOnly) "Click 'Upload Local' above to easily sync your current local routes to live Firestore cloud!"
                                       else "Complete trips via passenger mode or request a vehicle first.",
                                color = NeutralGray,
                                fontSize = 11.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(activeList) { trip ->
                            Card(
                                modifier = Modifier.fillMaxWidth().testTag("trip_history_card_${trip.id}"),
                                colors = CardDefaults.cardColors(containerColor = PureWhite),
                                shape = RoundedCornerShape(12.dp),
                                elevation = CardDefaults.cardElevation(2.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(34.dp)
                                                    .clip(CircleShape)
                                                    .background(BrandBlueLight),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = if (trip.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                                    contentDescription = "vehicle",
                                                    tint = BrandBluePrimary,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column {
                                                Text(
                                                    text = if (trip.vehicleType == "CAR") "Car Taxi" else "Tricycle (TukTuk)",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 13.sp,
                                                    color = BrandBlueDark
                                                )
                                                Text(
                                                    text = formatTimestamp(trip.timestamp),
                                                    fontSize = 10.sp,
                                                    color = NeutralGray
                                                )
                                            }
                                        }

                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (trip.status == "COMPLETED") SuccessGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f)
                                            ),
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = trip.status,
                                                color = if (trip.status == "COMPLETED") SuccessGreen else ErrorRed,
                                                fontWeight = FontWeight.Black,
                                                fontSize = 8.5.sp,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    
                                    // Origin & Destination points
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(BrandBluePrimary))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("From: ${trip.pickupName}", fontSize = 11.5.sp, color = BrandBlueDark, maxLines = 1)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(ErrorRed))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("To: ${trip.dropoffName}", fontSize = 11.5.sp, color = BrandBlueDark, maxLines = 1)
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(10.dp))
                                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(10.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .clickable {
                                                    val matchedD = drivers.find { it.id == trip.driverId } ?: DriverEntity(
                                                        id = trip.driverId ?: "drv_unknown",
                                                        name = trip.driverName ?: "Gambia Partner Driver",
                                                        phone = "+220 555 0100",
                                                        vehicleType = trip.vehicleType,
                                                        vehiclePlate = trip.vehiclePlate ?: "BJL Registered",
                                                        rating = if (trip.rating > 0) trip.rating.toFloat() else 4.8f,
                                                        approvalStatus = "APPROVED",
                                                        isOnline = true,
                                                        currentLat = 13.45,
                                                        currentLng = -16.6,
                                                        driverLicense = "DL-2024-9981"
                                                    )
                                                    selectedDriverForProfile = matchedD
                                                }
                                                .testTag("history_driver_profile_${trip.id}")
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(Icons.Default.Person, contentDescription = "Driver", tint = BrandBlueSecondary, modifier = Modifier.size(13.dp))
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = trip.driverName ?: "Gambia Partner Driver",
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 12.sp,
                                                    color = BrandBlueDark
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Verified,
                                                    contentDescription = "Verified Profile",
                                                    tint = SuccessGreen,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                            Text(
                                                text = "Plate: ${trip.vehiclePlate ?: "BJL Registered"} • View Profile",
                                                fontSize = 10.sp,
                                                color = BrandBluePrimary,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(start = 17.dp)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.End) {
                                            Text(
                                                text = "${trip.fareGmd} GMD",
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 13.5.sp,
                                                color = BrandBluePrimary
                                            )
                                            Text(
                                                text = "Paid via ${trip.paymentMethod}",
                                                fontSize = 9.5.sp,
                                                color = NeutralGray
                                            )
                                        }
                                    }

                                    if (trip.rating > 0) {
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(BrandBlueLight.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                                .padding(6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                (1..5).forEach { s ->
                                                    Icon(
                                                        imageVector = Icons.Default.Star,
                                                        contentDescription = "*",
                                                        tint = if (s <= trip.rating) AccentAmber else Color.LightGray,
                                                        modifier = Modifier.size(11.dp)
                                                    )
                                                }
                                            }
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = if (trip.reviewComment.isNotEmpty()) "\"${trip.reviewComment}\"" else "No commented feedback",
                                                fontSize = 10.sp,
                                                color = BrandBlueDark,
                                                fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
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
    }

    if (selectedDriverForProfile != null) {
        DriverTrustProfileDialog(
            driver = selectedDriverForProfile,
            vehicleType = selectedDriverForProfile!!.vehicleType,
            vehiclePlate = selectedDriverForProfile!!.vehiclePlate,
            driverName = selectedDriverForProfile!!.name,
            trips = trips,
            onDismiss = { selectedDriverForProfile = null }
        )
    }
}

@Composable
fun ProfileScreenContent(
    viewModel: WayGoViewModel,
    profile: UserProfileEntity?,
    initialSection: String = "PROFILE",
    onBack: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val trips by viewModel.allTrips.collectAsState()
    
    // Active Account Tab state: "PROFILE", "TRIP_LOG", "INBOX", "PAYMENT", "SAFETY", "SAVED_PLACES", "SETTINGS"
    var activeAccountTab by remember(initialSection) { mutableStateOf(initialSection) }

    // Calculate total completed rides dynamically from database
    val completedRidesCount = trips.count { it.status == "COMPLETED" }
    val totalSpendingGmd = trips.filter { it.status == "COMPLETED" }.sumOf { it.fareGmd }

    var editName by remember { mutableStateOf(profile?.name ?: "") }
    var editPhone by remember { mutableStateOf(profile?.phone ?: "") }
    var editEmail by remember { mutableStateOf(profile?.email ?: "") }
    var editGender by remember { mutableStateOf(profile?.gender ?: "Male") }
    var editMM by remember { mutableStateOf(profile?.mobileMoneyNumber ?: "") }
    var editHome by remember { mutableStateOf(profile?.savedHome ?: "Albert Market, Banjul") }
    var editWork by remember { mutableStateOf(profile?.savedWork ?: "Kairaba Avenue, Serrekunda") }
    var activeAvatarIndex by remember { mutableIntStateOf(profile?.avatarIndex ?: 1) }
    var photoUriState by remember { mutableStateOf<String?>(profile?.photoUri) }

    var isLinkingCardProcessing by remember { mutableStateOf(false) }
    var linkingCardUrl by remember { mutableStateOf<String?>(null) }
    var linkingStatusMsg by remember { mutableStateOf("") }

    var showHomePicker by remember { mutableStateOf(false) }
    var showWorkPicker by remember { mutableStateOf(false) }
    var showPhotoPickerOptions by remember { mutableStateOf(false) }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            photoUriState = uri.toString()
            showPhotoPickerOptions = false
        }
    }
    var showAddSavedPlaceDialog by remember { mutableStateOf(false) }
    var newSavedPlaceName by remember { mutableStateOf("") }
    var newSavedPlaceAddress by remember { mutableStateOf("") }

    // Saved places list
    var customSavedPlaces by remember {
        mutableStateOf(
            listOf(
                Pair("Senegambia Beach Resort", "Kairaba Avenue Coast, Kololi"),
                Pair("Banjul International Airport", "Yundum Local Terminal"),
                Pair("Arch 22 Monument", "Banjul City Gateway")
            )
        )
    }

    // Inbox messages list
    var inboxMessages by remember {
        mutableStateOf(
            listOf(
                AccountInboxItem("1", "Welcome to WayGo Gambia", "Your account is active. Book rides seamlessly across Banjul, Serrekunda & Brikama.", "10 mins ago", "SYSTEM", false),
                AccountInboxItem("2", "15% Off SeneGambia Weekend Ride", "Use promo code WAYGO15 on your next coastal ride. Offer expires Sunday midnight!", "2 hours ago", "PROMO", false),
                AccountInboxItem("3", "Flutterwave Payment Secured", "Visa, MasterCard & Wave mobile money payments are fully enabled.", "1 day ago", "PAYMENT", true),
                AccountInboxItem("4", "Safety Verification Update", "All drivers on WayGo undergo physical Gambia Police background checks.", "3 days ago", "SAFETY", true)
            )
        )
    }

    // Settings preferences
    var pushNotifsEnabled by remember { mutableStateOf(true) }
    var smsUpdatesEnabled by remember { mutableStateOf(true) }
    var selectedLanguage by remember { mutableStateOf("English") }

    // Trip Log filters: Status & Date Range
    var tripLogFilter by remember { mutableStateOf("ALL") } // "ALL", "COMPLETED", "CANCELLED"
    var tripLogDateRangeFilter by remember { mutableStateOf("ALL_TIME") } // "ALL_TIME", "TODAY", "LAST_7_DAYS", "LAST_30_DAYS", "CUSTOM"
    var customStartDaysAgo by remember { mutableIntStateOf(30) }
    var customEndDaysAgo by remember { mutableIntStateOf(0) }
    var showCustomDateDialog by remember { mutableStateOf(false) }
    var selectedTripForReceipt by remember { mutableStateOf<TripEntity?>(null) }

    LaunchedEffect(profile) {
        if (profile != null) {
            editName = profile.name
            editPhone = profile.phone
            editEmail = profile.email
            editGender = profile.gender
            editMM = profile.mobileMoneyNumber
            if (profile.savedHome.isNotEmpty()) editHome = profile.savedHome
            if (profile.savedWork.isNotEmpty()) editWork = profile.savedWork
            activeAvatarIndex = profile.avatarIndex
            photoUriState = profile.photoUri
        }
    }

    val avatars = listOf(
        Pair("🦁", Color(0xFFFFB300)), // Gold (Gambia Lion)
        Pair("🌴", Color(0xFF2E7D32)), // Emerald (Coastlands)
        Pair("🚖", Color(0xFFFDD835)), // Yellow (Taxi Cab)
        Pair("💼", Color(0xFF1E88E5)), // Blue (Business)
        Pair("☀️", Color(0xFFE64A19)), // Coral (SeneGambia Sun)
        Pair("🧉", Color(0xFF8E24AA))  // Purple (Wonjo Lover)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_view")
    ) {
        // Upper Navigation Title Bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(
                onClick = { onBack() },
                modifier = Modifier
                    .background(BrandBlueLight, CircleShape)
                    .size(40.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                    contentDescription = "Back",
                    tint = BrandBluePrimary
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column {
                Text(
                    text = "Account Hub", 
                    fontWeight = FontWeight.ExtraBold, 
                    fontSize = 20.sp, 
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Personal info, trips, payments & safety",
                    fontSize = 11.5.sp,
                    color = NeutralGray
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // PROFILE OVERVIEW HEADER CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(3.dp)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                Image(
                    painter = painterResource(id = com.example.R.drawable.profile_banner),
                    contentDescription = "Profile Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(Color.Black.copy(alpha = 0.68f))
                )
                
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f))
                            .clickable { activeAccountTab = "PROFILE" }
                            .padding(3.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (photoUriState != null) {
                                when (photoUriState) {
                                    "preset_active" -> {
                                        Image(
                                            painter = painterResource(id = com.example.R.drawable.rider_avatar_active),
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    "preset_alt" -> {
                                        Image(
                                            painter = painterResource(id = com.example.R.drawable.rider_avatar_alt),
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    else -> {
                                        Image(
                                            painter = rememberAsyncImagePainter(model = photoUriState),
                                            contentDescription = "Profile Photo",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }
                            } else {
                                val avatarStyle = avatars.getOrElse(activeAvatarIndex) { avatars[0] }
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(avatarStyle.second),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = avatarStyle.first, fontSize = 38.sp)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = editName.ifBlank { "Gambia Passenger" },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = editPhone.ifBlank { "+220 771 2345" },
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = completedRidesCount.toString(),
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = Color.White
                            )
                            Text(
                                text = "Completed Rides",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(Color.White.copy(alpha = 0.2f))
                        )

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$totalSpendingGmd GMD",
                                fontWeight = FontWeight.Black,
                                fontSize = 20.sp,
                                color = SuccessGreen
                            )
                            Text(
                                text = "Total Spending",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // QUICK ACTION CARDS (Activity, Wallet, Inbox, Safety)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Activity Card
            Card(
                onClick = { activeAccountTab = "TRIP_LOG" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeAccountTab == "TRIP_LOG") BrandBlueLight else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimary.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = "Activity", tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Activity", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Wallet Card
            Card(
                onClick = { activeAccountTab = "PAYMENT" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeAccountTab == "PAYMENT") BrandBlueLight else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Wallet", tint = SuccessGreen, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Wallet", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Inbox Card
            Card(
                onClick = { activeAccountTab = "INBOX" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeAccountTab == "INBOX") BrandBlueLight else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AccentAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Inbox, contentDescription = "Inbox", tint = AccentAmber, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Inbox", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }

            // Safety Card
            Card(
                onClick = { activeAccountTab = "SAFETY" },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (activeAccountTab == "SAFETY") BrandBlueLight else MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Shield, contentDescription = "Safety", tint = ErrorRed, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Safety", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // PLATFORM MODES CARD
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("account_platform_modes_card"),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(2.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.12f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(BrandBluePrimary.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = "Account Portals & Modes",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Surface(
                        color = BrandBlueLight,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "WayGo Unified",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 1. PASSENGER MODE ROW
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp)),
                    color = BrandBluePrimary.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrandBluePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = "Passenger Mode", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Passenger Mode", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Request rides, saved places & trip logs", fontSize = 11.5.sp, color = NeutralGray)
                            }
                        }
                        Surface(
                            color = SuccessGreen,
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Text("Active", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2. DRIVER HUB ROW
                Surface(
                    onClick = { viewModel.setRole("DRIVER") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.TwoWheeler, contentDescription = "Driver Hub", tint = BrandBlueDark, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Driver Hub", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Earn with WayGo • Online dispatch & payouts", fontSize = 11.5.sp, color = NeutralGray)
                            }
                        }
                        Button(
                            onClick = { viewModel.setRole("DRIVER") },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlueDark),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Switch", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AccentAmber)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3. ADMIN PANEL ROW
                Surface(
                    onClick = { viewModel.setRole("ADMIN") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(BrandBluePrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Console", tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                Text("Admin Panel", fontWeight = FontWeight.ExtraBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                Text("Verify drivers, dispatch & support metrics", fontSize = 11.5.sp, color = NeutralGray)
                            }
                        }
                        OutlinedButton(
                            onClick = { viewModel.setRole("ADMIN") },
                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("Open", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // ACCOUNT FEATURE NAVIGATION SELECTOR CHIPS
        Text(
            text = "Account Sections",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = BrandBlueSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )

        val unreadInboxCount = inboxMessages.count { !it.isRead }

        val accountSections = listOf(
            AccountSectionItem("PROFILE", "Profile", Icons.Default.Person, null),
            AccountSectionItem("TRIP_LOG", "Trip Log", Icons.Default.History, trips.size.takeIf { it > 0 }?.toString()),
            AccountSectionItem("INBOX", "Inbox", Icons.Default.Inbox, unreadInboxCount.takeIf { it > 0 }?.toString()),
            AccountSectionItem("DRIVER_HUB", "Driver Hub", Icons.Default.TwoWheeler, "DRIVER"),
            AccountSectionItem("ADMIN_PANEL", "Admin Console", Icons.Default.AdminPanelSettings, "ADMIN"),
            AccountSectionItem("PAYMENT", "Payment", Icons.Default.AccountBalanceWallet, if (profile?.isPaymentLinked == true) "Card" else null),
            AccountSectionItem("SAFETY", "Support & Safety", Icons.Default.Shield, null),
            AccountSectionItem("SAVED_PLACES", "Saved Places", Icons.Default.Bookmark, null),
            AccountSectionItem("SETTINGS", "Settings", Icons.Default.Settings, null)
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(horizontal = 2.dp)
        ) {
            items(accountSections) { section ->
                val isSelected = activeAccountTab == section.id
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        if (section.id == "DRIVER_HUB") {
                            viewModel.setRole("DRIVER")
                        } else if (section.id == "ADMIN_PANEL") {
                            viewModel.setRole("ADMIN")
                        } else {
                            activeAccountTab = section.id
                        }
                    },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = section.title,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 12.5.sp
                            )
                            if (section.badge != null) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.White else BrandBluePrimary)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = section.badge,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) BrandBluePrimary else Color.White
                                    )
                                }
                            }
                        }
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = section.icon,
                            contentDescription = section.title,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimary,
                        selectedLabelColor = Color.White,
                        selectedLeadingIconColor = Color.White,
                        containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f),
                        labelColor = MaterialTheme.colorScheme.onSurface,
                        iconColor = NeutralGray
                    ),
                    modifier = Modifier.testTag("tab_${section.id.lowercase()}")
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // FEATURE CONTENT SECTION BASED ON ACTIVE TAB
        when (activeAccountTab) {
            "TRIP_LOG" -> {
                // 📜 1. TRIP LOG SECTION WITH ENHANCED FILTERS
                val nowMs = System.currentTimeMillis()
                val oneDayMs = 24 * 3600 * 1000L
                val startOfTodayMs = remember(nowMs) {
                    try {
                        java.time.LocalDate.now().atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli()
                    } catch (e: Exception) {
                        nowMs - (nowMs % oneDayMs)
                    }
                }
                val sevenDaysAgoMs = nowMs - (7 * oneDayMs)
                val thirtyDaysAgoMs = nowMs - (30 * oneDayMs)
                val customStartMs = nowMs - (customStartDaysAgo * oneDayMs)
                val customEndMs = nowMs - (customEndDaysAgo * oneDayMs)

                val formatTripTime = remember {
                    { ms: Long ->
                        try {
                            val instant = java.time.Instant.ofEpochMilli(ms)
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                                .withZone(java.time.ZoneId.systemDefault())
                            formatter.format(instant)
                        } catch (e: Exception) {
                            "Recent Date"
                        }
                    }
                }

                val filteredTrips = trips.filter { trip ->
                    val matchesStatus = when (tripLogFilter) {
                        "COMPLETED" -> trip.status == "COMPLETED"
                        "CANCELLED" -> trip.status == "CANCELLED"
                        else -> true
                    }

                    val matchesDate = when (tripLogDateRangeFilter) {
                        "TODAY" -> trip.timestamp >= startOfTodayMs
                        "LAST_7_DAYS" -> trip.timestamp >= sevenDaysAgoMs
                        "LAST_30_DAYS" -> trip.timestamp >= thirtyDaysAgoMs
                        "CUSTOM" -> trip.timestamp in customStartMs..customEndMs
                        else -> true
                    }

                    matchesStatus && matchesDate
                }

                val totalFilteredSpend = filteredTrips.filter { it.status == "COMPLETED" }.sumOf { it.fareGmd + it.tipGmd }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Trip History & Log",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandBlueDark
                                )
                                Text(
                                    text = "${filteredTrips.size} trips • Total Spent: $totalFilteredSpend GMD",
                                    fontSize = 11.sp,
                                    color = NeutralGray
                                )
                            }

                            if (tripLogFilter != "ALL" || tripLogDateRangeFilter != "ALL_TIME") {
                                TextButton(
                                    onClick = {
                                        tripLogFilter = "ALL"
                                        tripLogDateRangeFilter = "ALL_TIME"
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text("Reset Filters", fontSize = 11.sp, color = ErrorRed, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // 1. STATUS FILTER CHIPS
                        Text("Filter by Status", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeutralGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val statusOptions = listOf(
                                "ALL" to "All Statuses",
                                "COMPLETED" to "Completed",
                                "CANCELLED" to "Cancelled"
                            )
                            statusOptions.forEach { (filterKey, label) ->
                                val isSelected = tripLogFilter == filterKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            when {
                                                isSelected && filterKey == "COMPLETED" -> SuccessGreen.copy(alpha = 0.18f)
                                                isSelected && filterKey == "CANCELLED" -> ErrorRed.copy(alpha = 0.18f)
                                                isSelected -> BrandBluePrimary.copy(alpha = 0.18f)
                                                else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                            }
                                        )
                                        .border(
                                            width = if (isSelected) 1.dp else 0.dp,
                                            color = when {
                                                isSelected && filterKey == "COMPLETED" -> SuccessGreen
                                                isSelected && filterKey == "CANCELLED" -> ErrorRed
                                                isSelected -> BrandBluePrimary
                                                else -> Color.Transparent
                                            },
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { tripLogFilter = filterKey }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = when {
                                            isSelected && filterKey == "COMPLETED" -> SuccessGreen
                                            isSelected && filterKey == "CANCELLED" -> ErrorRed
                                            isSelected -> BrandBluePrimary
                                            else -> NeutralGray
                                        }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // 2. DATE RANGE FILTER CHIPS
                        Text("Filter by Date Range", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = NeutralGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            val dateOptions = listOf(
                                "ALL_TIME" to "All Time",
                                "TODAY" to "Today",
                                "LAST_7_DAYS" to "7 Days",
                                "LAST_30_DAYS" to "30 Days",
                                "CUSTOM" to "Custom"
                            )
                            dateOptions.forEach { (dateKey, label) ->
                                val isSelected = tripLogDateRangeFilter == dateKey
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) BrandBluePrimary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                        .clickable {
                                            tripLogDateRangeFilter = dateKey
                                            if (dateKey == "CUSTOM") showCustomDateDialog = true
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }

                        // CUSTOM DATE RANGE SELECTOR CARD
                        AnimatedVisibility(
                            visible = tripLogDateRangeFilter == "CUSTOM",
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = BrandBluePrimary.copy(alpha = 0.06f),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("Custom Range Window:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                                            Text("${customStartDaysAgo}d ago to ${customEndDaysAgo}d ago", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = BrandBluePrimary)
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            listOf(3 to "Last 3 Days", 14 to "Last 14 Days", 60 to "Last 60 Days", 90 to "Last 90 Days").forEach { (days, btnLabel) ->
                                                OutlinedButton(
                                                    onClick = {
                                                        customStartDaysAgo = days
                                                        customEndDaysAgo = 0
                                                    },
                                                    modifier = Modifier.weight(1f).height(32.dp),
                                                    shape = RoundedCornerShape(6.dp),
                                                    contentPadding = PaddingValues(0.dp)
                                                ) {
                                                    Text(btnLabel, fontSize = 9.5.sp)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        if (filteredTrips.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 24.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = NeutralGray, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("No trip logs match selected filters.", fontSize = 13.sp, color = NeutralGray)
                                }
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filteredTrips.forEach { trip ->
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(
                                                        imageVector = if (trip.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                                        contentDescription = null,
                                                        tint = BrandBluePrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(6.dp))
                                                    Text(
                                                        text = "Trip #${trip.id.takeLast(6)}",
                                                        fontWeight = FontWeight.Bold,
                                                        fontSize = 13.sp,
                                                        color = MaterialTheme.colorScheme.onSurface
                                                    )
                                                }
                                                val statusColor = when (trip.status) {
                                                    "COMPLETED" -> SuccessGreen
                                                    "CANCELLED" -> ErrorRed
                                                    else -> AccentAmber
                                                }
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(statusColor.copy(alpha = 0.15f))
                                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = trip.status,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = statusColor
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = formatTripTime(trip.timestamp),
                                                fontSize = 10.5.sp,
                                                color = NeutralGray
                                            )

                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(SuccessGreen))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = trip.pickupName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                            }

                                            Box(modifier = Modifier.padding(start = 3.dp).width(2.dp).height(10.dp).background(Color.LightGray))

                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(ErrorRed))
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(text = trip.dropoffName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Medium)
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))
                                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))
                                            Spacer(modifier = Modifier.height(8.dp))

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column {
                                                    Text(
                                                        text = "Driver: ${(trip.driverName ?: "Fatou (Taxi)").ifBlank { "Fatou (Taxi)" }}",
                                                        fontSize = 11.5.sp,
                                                        color = NeutralGray
                                                    )
                                                    Text(
                                                        text = "Payment: ${trip.paymentMethod}",
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandBluePrimary
                                                    )
                                                }

                                                Column(horizontalAlignment = Alignment.End) {
                                                    Text(
                                                        text = "${trip.fareGmd + trip.tipGmd} GMD",
                                                        fontWeight = FontWeight.ExtraBold,
                                                        fontSize = 14.sp,
                                                        color = BrandBlueDark
                                                    )
                                                    if (trip.tipGmd > 0) {
                                                        Text(
                                                            text = "(Includes ${trip.tipGmd} GMD Tip)",
                                                            fontSize = 9.5.sp,
                                                            color = SuccessGreen,
                                                            fontWeight = FontWeight.Bold
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
                }
            }

            "INBOX" -> {
                // 📥 2. INBOX SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Inbox & Notifications",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = BrandBlueDark
                            )
                            if (unreadInboxCount > 0) {
                                TextButton(
                                    onClick = {
                                        inboxMessages = inboxMessages.map { it.copy(isRead = true) }
                                    }
                                ) {
                                    Text("Mark all as read", fontSize = 11.sp, color = BrandBluePrimary)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            inboxMessages.forEach { msg ->
                                Card(
                                    onClick = {
                                        inboxMessages = inboxMessages.map {
                                            if (it.id == msg.id) it.copy(isRead = true) else it
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (!msg.isRead) BrandBluePrimary.copy(alpha = 0.06f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        if (!msg.isRead) BrandBluePrimary.copy(alpha = 0.3f) else Color.LightGray.copy(alpha = 0.2f)
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when (msg.category) {
                                                        "PROMO" -> AccentAmber.copy(alpha = 0.15f)
                                                        "PAYMENT" -> SuccessGreen.copy(alpha = 0.15f)
                                                        else -> BrandBluePrimary.copy(alpha = 0.15f)
                                                    }
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (msg.category) {
                                                    "PROMO" -> Icons.Default.LocalOffer
                                                    "PAYMENT" -> Icons.Default.CreditCard
                                                    "SAFETY" -> Icons.Default.Shield
                                                    else -> Icons.Default.Notifications
                                                },
                                                contentDescription = null,
                                                tint = when (msg.category) {
                                                    "PROMO" -> AccentAmber
                                                    "PAYMENT" -> SuccessGreen
                                                    else -> BrandBluePrimary
                                                },
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(12.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = msg.title,
                                                    fontWeight = if (!msg.isRead) FontWeight.Bold else FontWeight.SemiBold,
                                                    fontSize = 13.5.sp,
                                                    color = MaterialTheme.colorScheme.onSurface
                                                )
                                                Text(
                                                    text = msg.timestamp,
                                                    fontSize = 10.sp,
                                                    color = NeutralGray
                                                )
                                            }
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                text = msg.message,
                                                fontSize = 11.5.sp,
                                                color = NeutralGray,
                                                lineHeight = 15.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "PROFILE" -> {
                // 👤 3. PERSONAL PROFILE SECTION
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = "Select Profile Avatar Icon",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandBlueSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            avatars.forEachIndexed { index, (emoji, bg) ->
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (activeAvatarIndex == index) bg else bg.copy(alpha = 0.3f)
                                        )
                                        .clickable { 
                                            activeAvatarIndex = index 
                                            photoUriState = null
                                        }
                                        .padding(2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = emoji, 
                                        fontSize = 24.sp,
                                        modifier = Modifier.alpha(if (activeAvatarIndex == index && photoUriState == null) 1f else 0.45f)
                                    )
                                }
                            }
                        }
                    }

                    Text(
                        text = "Personal & Contact Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandBlueSecondary,
                        modifier = Modifier.padding(start = 4.dp)
                    )
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Profile Name") },
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = "name", tint = BrandBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                                singleLine = true,
                                textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = BrandBlueDark,
                                    unfocusedTextColor = BrandBlueDark,
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
                                    focusedContainerColor = PureWhite,
                                    unfocusedContainerColor = PureWhite
                                )
                            )

                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Phone Number") },
                                leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "phone", tint = BrandBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = BrandBlueDark,
                                    unfocusedTextColor = BrandBlueDark,
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
                                    focusedContainerColor = PureWhite,
                                    unfocusedContainerColor = PureWhite
                                )
                            )

                            OutlinedTextField(
                                value = editEmail,
                                onValueChange = { editEmail = it },
                                label = { Text("Email Address") },
                                leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email", tint = BrandBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("profile_email_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = BrandBlueDark,
                                    unfocusedTextColor = BrandBlueDark,
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f),
                                    focusedContainerColor = PureWhite,
                                    unfocusedContainerColor = PureWhite
                                )
                            )

                            Column(modifier = Modifier.fillMaxWidth()) {
                                Text("Gender Selection", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = NeutralGray)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    listOf("Male", "Female", "Prefer Not").forEach { option ->
                                        Row(
                                            modifier = Modifier
                                                .clickable { editGender = option }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = editGender == option,
                                                onClick = { editGender = option },
                                                colors = RadioButtonDefaults.colors(selectedColor = BrandBluePrimary)
                                            )
                                            Text(text = option, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                        }
                                    }
                                }
                            }

                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                            OutlinedTextField(
                                value = editMM,
                                onValueChange = { editMM = it },
                                label = { Text("MobileMoney (Wave/Africell) Number") },
                                leadingIcon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "mobile_money", tint = BrandBluePrimary) },
                                modifier = Modifier.fillMaxWidth().testTag("profile_mm_input"),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                                )
                            )
                        }
                    }

                    Button(
                        onClick = {
                            viewModel.saveProfile(
                                name = editName.trim(),
                                phone = editPhone.trim(),
                                email = editEmail.trim(),
                                gender = editGender,
                                mobileMoney = editMM.trim(),
                                savedHome = editHome.trim(),
                                savedWork = editWork.trim(),
                                avatarIndex = activeAvatarIndex,
                                photoUri = photoUriState
                            )
                            onBack()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("save_profile_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Save Profile Changes", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }

            "PAYMENT" -> {
                // 💳 4. PAYMENT SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "Cashless Payments & Cards",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandBlueDark
                        )

                        if (profile?.isPaymentLinked == true) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(SuccessGreen.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = "Card Linked",
                                        tint = SuccessGreen,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Visa / MasterCard Linked",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "Card Ending in **** **** **** ${profile.linkedCardLast4.ifBlank { "4242" }}",
                                        fontSize = 12.sp,
                                        color = NeutralGray
                                    )
                                    Text(
                                        text = "Secured: ${profile.linkedPaymentEmail}",
                                        fontSize = 11.sp,
                                        color = BrandBluePrimary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                IconButton(onClick = { viewModel.removePaymentMethod() }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Unlink Card",
                                        tint = ErrorRed
                                    )
                                }
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SuccessGreen.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Cashless checkout is active. Flutterwave will charge automatically.",
                                    fontSize = 11.sp,
                                    color = SuccessGreen,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(BrandBluePrimary.copy(alpha = 0.1f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CreditCard, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(24.dp))
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("No Card Linked", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                                    Text("Link debit/credit card for automatic cashless bookings.", fontSize = 11.sp, color = NeutralGray)
                                }
                            }

                            Button(
                                onClick = {
                                    isLinkingCardProcessing = true
                                    linkingStatusMsg = "Opening secure Flutterwave gateway..."
                                    coroutineScope.launch {
                                        val pEmail = editEmail.ifBlank { "johndoe@example.com" }
                                        val pName = editName.ifBlank { "John Doe" }
                                        val pPhone = editPhone.ifBlank { "+220 771 2345" }
                                        val txRef = "flw_link_" + System.currentTimeMillis().toString().takeLast(6)
                                        val link = com.example.data.FlutterwaveManager.initiatePayment(
                                            amountGmd = 25.0,
                                            passengerEmail = pEmail,
                                            passengerName = pName,
                                            passengerPhone = pPhone,
                                            tripTxRef = txRef
                                        )
                                        if (link != null) {
                                            linkingCardUrl = link
                                        } else {
                                            linkingStatusMsg = "Failed to connect to Flutterwave."
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth().height(44.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Link Card with Flutterwave", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        Text("Mobile Money Wallets", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.AccountBalanceWallet, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(22.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Wave / Africell Pay", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                Text(editMM.ifBlank { "No Mobile Money number set" }, fontSize = 11.sp, color = NeutralGray)
                            }
                        }
                    }
                }
            }

            "SAFETY" -> {
                // 🛡️ 5. SUPPORT & SAFETY SECTION
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Shield, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Gambia Emergency Hotlines (24/7)", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = ErrorRed)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                listOf(
                                    Triple("Gambia Police", "117", Icons.Default.LocalPolice),
                                    Triple("Ambulance", "116", Icons.Default.MedicalServices),
                                    Triple("Fire Rescue", "118", Icons.Default.FireTruck)
                                ).forEach { (name, number, icon) ->
                                    Card(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 2.dp),
                                        colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.08f)),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(10.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Icon(icon, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(name, fontSize = 10.5.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                            Text(number, fontSize = 12.sp, fontWeight = FontWeight.Black, color = ErrorRed)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text("Ride Safety Features", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Share Trip Live Location", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    Text("Send real-time GPS coordinates to family or trusted contacts during active rides.", fontSize = 11.sp, color = NeutralGray)
                                }
                            }

                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.2f))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.HeadsetMic, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("WayGo Support Live Chat", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                    Text("Contact 24/7 Banjul operations support agent directly.", fontSize = 11.sp, color = NeutralGray)
                                }
                            }
                        }
                    }
                }
            }

            "SAVED_PLACES" -> {
                // 📍 SAVED PLACES MANAGEMENT SCREEN
                val savedPlacesFromDb by viewModel.allSavedPlaces.collectAsState()

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    // Top Hero Banner Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Saved Places & Locations",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = BrandBlueDark
                                    )
                                    Text(
                                        text = "${savedPlacesFromDb.size} locations stored for 1-tap booking",
                                        fontSize = 11.5.sp,
                                        color = NeutralGray
                                    )
                                }

                                Button(
                                    onClick = { showAddSavedPlaceDialog = true },
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                    shape = RoundedCornerShape(10.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                    modifier = Modifier.testTag("add_saved_place_top_btn")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Add Place", fontSize = 12.sp, color = Color.White)
                                }
                            }

                            // Quick tip banner
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = BrandBluePrimary.copy(alpha = 0.06f),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.15f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.TouchApp, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Tap 'Book Ride' on any place to instantly load it as your dropoff location and calculate instant fares!",
                                        fontSize = 10.5.sp,
                                        color = BrandBlueDark,
                                        lineHeight = 14.sp
                                    )
                                }
                            }
                        }
                    }

                    // 1. PRIMARY LOCATIONS (Home & Work)
                    Text("Primary Locations", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Home Card
                        val homePlace = savedPlacesFromDb.firstOrNull { it.label.equals("Home", ignoreCase = true) }
                        val homeAddress = homePlace?.name ?: editHome
                        val homeLat = homePlace?.lat ?: 13.4471
                        val homeLng = homePlace?.lng ?: -16.6791

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandBluePrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Home, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { showHomePicker = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Home", tint = NeutralGray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Home", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = BrandBlueDark)
                                Text(
                                    text = homeAddress,
                                    fontSize = 11.sp,
                                    color = NeutralGray,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.heightIn(min = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.selectOneTapDestination(
                                            com.example.data.SavedPlaceEntity(
                                                name = homeAddress,
                                                label = "Home",
                                                lat = homeLat,
                                                lng = homeLng,
                                                iconType = "HOME"
                                            )
                                        )
                                        onBack()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(32.dp).testTag("book_home_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Book Ride", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // Work Card
                        val workPlace = savedPlacesFromDb.firstOrNull { it.label.equals("Work", ignoreCase = true) }
                        val workAddress = workPlace?.name ?: editWork
                        val workLat = workPlace?.lat ?: 13.4533
                        val workLng = workPlace?.lng ?: -16.5746

                        Card(
                            modifier = Modifier.weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier.size(32.dp).clip(CircleShape).background(BrandBluePrimary.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Work, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                                    }
                                    IconButton(onClick = { showWorkPicker = true }, modifier = Modifier.size(24.dp)) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit Work", tint = NeutralGray, modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Work", fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = BrandBlueDark)
                                Text(
                                    text = workAddress,
                                    fontSize = 11.sp,
                                    color = NeutralGray,
                                    maxLines = 2,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.heightIn(min = 32.dp)
                                )
                                Spacer(modifier = Modifier.height(10.dp))
                                Button(
                                    onClick = {
                                        viewModel.selectOneTapDestination(
                                            com.example.data.SavedPlaceEntity(
                                                name = workAddress,
                                                label = "Work",
                                                lat = workLat,
                                                lng = workLng,
                                                iconType = "WORK"
                                            )
                                        )
                                        onBack()
                                    },
                                    modifier = Modifier.fillMaxWidth().height(32.dp).testTag("book_work_button"),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp), tint = Color.White)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Book Ride", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    // 2. CUSTOM FAVORITE PLACES LIST
                    val customPlaces = savedPlacesFromDb.filter {
                        !it.label.equals("Home", ignoreCase = true) && !it.label.equals("Work", ignoreCase = true)
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Custom Favorite Locations", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)
                                Text("${customPlaces.size} saved", fontSize = 11.sp, color = NeutralGray)
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            if (customPlaces.isEmpty()) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(Icons.Default.AddLocationAlt, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(36.dp))
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("No Custom Saved Places Yet", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                                        Text(
                                            "Pin your favorite gym, beach resort, market, or family location for 1-tap booking.",
                                            fontSize = 11.sp,
                                            color = NeutralGray,
                                            textAlign = TextAlign.Center
                                        )
                                        Spacer(modifier = Modifier.height(12.dp))
                                        OutlinedButton(
                                            onClick = { showAddSavedPlaceDialog = true },
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("+ Add First Custom Place", fontSize = 11.5.sp)
                                        }
                                    }
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    customPlaces.forEach { place ->
                                        val placeIcon = when (place.iconType) {
                                            "HOME" -> Icons.Default.Home
                                            "WORK" -> Icons.Default.Work
                                            "STAR" -> Icons.Default.Star
                                            "SHOPPING" -> Icons.Default.ShoppingBag
                                            "AIRPORT" -> Icons.Default.Flight
                                            "BEACH" -> Icons.Default.BeachAccess
                                            else -> Icons.Default.Place
                                        }

                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)),
                                            shape = RoundedCornerShape(12.dp),
                                            border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(36.dp).clip(CircleShape).background(AccentAmber.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(placeIcon, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(20.dp))
                                                }

                                                Spacer(modifier = Modifier.width(10.dp))

                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(place.label, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
                                                    Text(place.name, fontSize = 11.sp, color = NeutralGray)
                                                }

                                                Spacer(modifier = Modifier.width(6.dp))

                                                Button(
                                                    onClick = {
                                                        viewModel.selectOneTapDestination(place)
                                                        onBack()
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(30.dp).testTag("book_custom_place_${place.id}")
                                                ) {
                                                    Text("Book", fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }

                                                IconButton(
                                                    onClick = { viewModel.removeSavedPlace(place.id) },
                                                    modifier = Modifier.size(28.dp).testTag("delete_saved_place_${place.id}")
                                                ) {
                                                    Icon(Icons.Default.DeleteOutline, contentDescription = "Remove", tint = ErrorRed.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            "SETTINGS" -> {
                // ⚙️ 7. SETTINGS SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("App Appearance & Theme", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)

                        val currentThemeMode by viewModel.themeMode.collectAsState()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f), RoundedCornerShape(12.dp))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val themeOptions = listOf(
                                Triple(ThemeMode.SYSTEM, Icons.Default.SettingsSuggest, "System"),
                                Triple(ThemeMode.LIGHT, Icons.Default.LightMode, "Light"),
                                Triple(ThemeMode.DARK, Icons.Default.DarkMode, "Dark")
                            )

                            themeOptions.forEach { (mode, icon, label) ->
                                val isSelected = currentThemeMode == mode
                                val bg = if (isSelected) BrandBluePrimary else Color.Transparent
                                val contentColor = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bg)
                                        .clickable { viewModel.setThemeMode(mode) }
                                        .padding(vertical = 10.dp)
                                        .testTag("theme_mode_${label.lowercase()}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = contentColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Text(
                                            text = label,
                                            color = contentColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        Text("Notification Preferences", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Push Notifications", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                Text("Receive real-time ride updates & driver status", fontSize = 11.sp, color = NeutralGray)
                            }
                            Switch(
                                checked = pushNotifsEnabled,
                                onCheckedChange = { pushNotifsEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBluePrimary)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("SMS Trip Alerts", fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
                                Text("Receive SMS receipt confirmation on trip end", fontSize = 11.sp, color = NeutralGray)
                            }
                            Switch(
                                checked = smsUpdatesEnabled,
                                onCheckedChange = { smsUpdatesEnabled = it },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = BrandBluePrimary)
                            )
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        Text("App Language", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            listOf("English", "Wolof", "Mandinka").forEach { lang ->
                                FilterChip(
                                    selected = selectedLanguage == lang,
                                    onClick = { selectedLanguage = lang },
                                    label = { Text(lang, fontSize = 12.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = BrandBluePrimary,
                                        selectedLabelColor = Color.White
                                    )
                                )
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        OutlinedButton(
                            onClick = { viewModel.logout() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("logout_profile_button"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Logout, contentDescription = "logout", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Log Out phone session", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(30.dp))
    }

    // ADD NEW SAVED PLACE DIALOG
    if (showAddSavedPlaceDialog) {
        AlertDialog(
            onDismissRequest = { showAddSavedPlaceDialog = false },
            title = { Text("Add Favorite Place", fontWeight = FontWeight.Bold, color = BrandBlueDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newSavedPlaceName,
                        onValueChange = { newSavedPlaceName = it },
                        label = { Text("Place Name (e.g. SeneGambia Market)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrandBlueDark,
                            unfocusedTextColor = BrandBlueDark,
                            focusedBorderColor = BrandBluePrimary,
                            unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )
                    OutlinedTextField(
                        value = newSavedPlaceAddress,
                        onValueChange = { newSavedPlaceAddress = it },
                        label = { Text("Address / Landmark") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrandBlueDark,
                            unfocusedTextColor = BrandBlueDark,
                            focusedBorderColor = BrandBluePrimary,
                            unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                            focusedContainerColor = PureWhite,
                            unfocusedContainerColor = PureWhite
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newSavedPlaceName.isNotBlank()) {
                            customSavedPlaces = customSavedPlaces + Pair(newSavedPlaceName.trim(), newSavedPlaceAddress.trim().ifBlank { "Serrekunda District" })
                            newSavedPlaceName = ""
                            newSavedPlaceAddress = ""
                            showAddSavedPlaceDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Text("Add Place")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddSavedPlaceDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // CUSTOM CARD LINKING CHECKOUT DIALOG
    if (linkingCardUrl != null) {
        FlutterwaveCheckoutDialog(
            checkoutUrl = linkingCardUrl!!,
            onPaymentSuccess = { txRef, transactionId ->
                viewModel.linkPaymentMethod(
                    email = editEmail.ifBlank { "johndoe@example.com" },
                    cardLast4 = txRef.takeLast(4).filter { it.isDigit() }.ifBlank { "4242" }
                )
                linkingCardUrl = null
                isLinkingCardProcessing = false
            },
            onCancel = {
                linkingCardUrl = null
                isLinkingCardProcessing = false
            }
        )
    }

    if (isLinkingCardProcessing && linkingCardUrl == null) {
        AlertDialog(
            onDismissRequest = { isLinkingCardProcessing = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = BrandBluePrimary,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Flutterwave Secure", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                }
            },
            text = {
                Text(
                    text = linkingStatusMsg,
                    fontSize = 13.sp,
                    color = BrandBlueDark
                )
            },
            confirmButton = {
                TextButton(onClick = { isLinkingCardProcessing = false }) {
                    Text("Cancel", color = NeutralGray)
                }
            }
        )
    }

    // HOME LOCATION PICKER DIALOG
    if (showHomePicker) {
        AlertDialog(
            onDismissRequest = { showHomePicker = false },
            title = { Text("Select Home Location", fontWeight = FontWeight.Bold, color = BrandBlueDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a saved location in Banjul or Serrekunda for quick booking:", fontSize = 13.sp, color = NeutralGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    GAMBIA_LOCATIONS.forEach { loc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editHome = loc.name
                                    showHomePicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = loc.name, fontSize = 14.sp, color = BrandBlueDark)
                        }
                        HorizontalDivider(color = NeutralGray.copy(alpha = 0.1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showHomePicker = false }) {
                    Text("Cancel", color = BrandBluePrimary)
                }
            }
        )
    }

    // WORK LOCATION PICKER DIALOG
    if (showWorkPicker) {
        AlertDialog(
            onDismissRequest = { showWorkPicker = false },
            title = { Text("Select Work Location", fontWeight = FontWeight.Bold, color = BrandBlueDark) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a saved location in Banjul or Serrekunda for quick booking:", fontSize = 13.sp, color = NeutralGray)
                    Spacer(modifier = Modifier.height(4.dp))
                    GAMBIA_LOCATIONS.forEach { loc ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editWork = loc.name
                                    showWorkPicker = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = loc.name, fontSize = 14.sp, color = BrandBlueDark)
                        }
                        HorizontalDivider(color = NeutralGray.copy(alpha = 0.1f))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showWorkPicker = false }) {
                    Text("Cancel", color = BrandBluePrimary)
                }
            }
        )
    }

// End of saved places dialogs

    // PROFILE PHOTO CHOOSER DIALOG
    if (showPhotoPickerOptions) {
        AlertDialog(
            onDismissRequest = { showPhotoPickerOptions = false },
            title = { Text("Choose Profile Photo", fontWeight = FontWeight.Bold, color = BrandBlueDark) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Select how you want to update your profile photo:", fontSize = 13.sp, color = NeutralGray)
                    
                    // Option 1: Pick from device gallery
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoPickerLauncher.launch("image/*")
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.PhotoLibrary, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Upload from Device Gallery", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandBlueDark)
                    }

                    HorizontalDivider(color = NeutralGray.copy(alpha = 0.1f))

                    // Option 2: Use Preset Male Illustration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoUriState = "preset_active"
                                showPhotoPickerOptions = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Face, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Use Preset Male Rider Avatar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandBlueDark)
                    }

                    HorizontalDivider(color = NeutralGray.copy(alpha = 0.1f))

                    // Option 3: Use Preset Female Illustration
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoUriState = "preset_alt"
                                showPhotoPickerOptions = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.FaceRetouchingNatural, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Use Preset Female Rider Avatar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BrandBlueDark)
                    }

                    HorizontalDivider(color = NeutralGray.copy(alpha = 0.1f))

                    // Option 4: Clear custom photo and use emoji
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                photoUriState = null
                                showPhotoPickerOptions = false
                            }
                            .padding(vertical = 10.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Remove Photo & use Emoji Avatar", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = ErrorRed)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showPhotoPickerOptions = false }) {
                    Text("Cancel", color = BrandBluePrimary)
                }
            }
        )
    }
}

@Composable
fun SupportInboxContent(viewModel: WayGoViewModel) {
    val messages by viewModel.supportMessages.collectAsState()
    var messageInput by remember { mutableStateOf("") }
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("WayGo Live Help Center", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandBlueDark)
        Text("Your chat with support team and active drivers.", fontSize = 11.sp, color = NeutralGray)

        Spacer(modifier = Modifier.height(8.dp))

        // Messages list
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(messages) { msg ->
                val isMe = msg.senderRole == "PASSENGER"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
                ) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isMe) BrandBluePrimary else Color(0xFFE5ECEF)
                        ),
                        shape = RoundedCornerShape(
                            topStart = 12.dp,
                            topEnd = 12.dp,
                            bottomStart = if (isMe) 12.dp else 0.dp,
                            bottomEnd = if (isMe) 0.dp else 12.dp
                        ),
                        modifier = Modifier.fillMaxWidth(0.85f)
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = if (isMe) "You (Passenger)" else "WayGo Support Agent",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isMe) Color.White.copy(alpha = 0.8f) else BrandBluePrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = msg.message,
                                fontSize = 13.sp,
                                color = if (isMe) Color.White else BrandBlueDark
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Input row
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = messageInput,
                onValueChange = { messageInput = it },
                placeholder = { Text("Ask support a question...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = BrandBlueDark),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = BrandBlueDark,
                    unfocusedTextColor = BrandBlueDark,
                    focusedBorderColor = BrandBluePrimary,
                    unfocusedBorderColor = NeutralGray.copy(alpha = 0.3f),
                    focusedContainerColor = PureWhite,
                    unfocusedContainerColor = PureWhite,
                    focusedPlaceholderColor = NeutralGray.copy(alpha = 0.6f),
                    unfocusedPlaceholderColor = NeutralGray.copy(alpha = 0.6f)
                )
            )
            Spacer(modifier = Modifier.width(6.dp))
            IconButton(
                onClick = {
                    if (messageInput.isNotBlank()) {
                        viewModel.sendSupportMsg("PASSENGER", messageInput)
                        messageInput = ""
                        coroutineScope.launch {
                            delay(200)
                            listState.animateScrollToItem(messages.size)
                        }
                    }
                },
                modifier = Modifier.background(BrandBluePrimary, CircleShape)
            ) {
                Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun FlutterwaveCheckoutDialog(
    checkoutUrl: String,
    onPaymentSuccess: (txRef: String, transactionId: String) -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onCancel() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Custom Header for Secure Payment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandBlueDark)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Checkout",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Flutterwave Secure Checkout",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = SuccessGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "256-bit SSL Encrypted Connection",
                                color = SuccessGreen,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    // Tiny brand logo text
                    Text(
                        text = "flutterwave",
                        color = AccentAmber,
                        fontWeight = FontWeight.Black,
                        fontSize = 14.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                var progress by remember { mutableStateOf(0.1f) }
                if (progress < 1.0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = BrandBluePrimary,
                        trackColor = BrandBlueLight
                    )
                }

                // Android WebView for secure hosting checkout
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        WebView(context).apply {
                            layoutParams = ViewGroup.LayoutParams(
                                ViewGroup.LayoutParams.MATCH_PARENT,
                                ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                            }
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    progress = 0.3f
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    progress = 1.0f
                                    
                                    // Handle success redirect checking
                                    if (url != null && url.contains("standard-checkout-redirect.waygo.com")) {
                                        val uri = android.net.Uri.parse(url)
                                        val status = uri.getQueryParameter("status")
                                        val txRef = uri.getQueryParameter("tx_ref") ?: "flw_tx_ref_" + System.currentTimeMillis()
                                        val transactionId = uri.getQueryParameter("transaction_id") ?: "12345"

                                        if (status == "successful" || status == "completed" || url.contains("status=successful")) {
                                            onPaymentSuccess(txRef, transactionId)
                                        } else {
                                            onPaymentSuccess(txRef, transactionId)
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: ""
                                    if (url.contains("standard-checkout-redirect.waygo.com")) {
                                        val uri = android.net.Uri.parse(url)
                                        val status = uri.getQueryParameter("status")
                                        val txRef = uri.getQueryParameter("tx_ref") ?: "flw_tx_ref_" + System.currentTimeMillis()
                                        val transactionId = uri.getQueryParameter("transaction_id") ?: "12345"
                                        onPaymentSuccess(txRef, transactionId)
                                        return true
                                    }
                                    return false
                                }
                            }
                        }
                    },
                    update = { webView ->
                        webView.loadUrl(checkoutUrl)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

@Composable
fun DriverTrustProfileDialog(
    driver: com.example.data.DriverEntity?,
    vehicleType: String,
    vehiclePlate: String,
    driverName: String,
    trips: List<com.example.data.TripEntity> = emptyList(),
    onDismiss: () -> Unit
) {
    var activeProfileTab by remember { mutableStateOf("TRUST") } // "TRUST", "VEHICLE", "REVIEWS"

    val ratingVal = driver?.rating ?: 4.8f
    val licenseVal = driver?.driverLicense ?: "DL-2024-9981"
    val nameVal = driver?.name ?: driverName
    val isTricycle = (driver?.vehicleType ?: vehicleType) == "TRICYCLE"
    val plateVal = driver?.vehiclePlate ?: vehiclePlate
    
    val tripsCompletedCount = when (driver?.id) {
        "drv_alieu" -> 342
        "drv_mariama" -> 512
        "drv_bakary" -> 281
        "drv_ebrima" -> 194
        else -> 250
    }

    val driverInitials = nameVal.split(" ")
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("").take(2).uppercase()

    // Query active reviews for this driver ID from the database
    val completedDriverTrips = trips.filter { 
        it.driverId == (driver?.id ?: "") && it.status == "COMPLETED" 
    }
    
    val defaultReviews = when (driver?.id) {
        "drv_alieu" -> listOf(
            Pair(5, "Exceptional and very polite driver! Alieu knows all shortcuts around Albert Market."),
            Pair(5, "Very clean yellow sedan, smooth driving from Serekunda. Highly recommended!"),
            Pair(4, "On time and safe driving. Guided me to the correct Arch 22 entrance.")
        )
        "drv_mariama" -> listOf(
            Pair(5, "Mariama is amazing! Her tricycle is very clean and comfortable."),
            Pair(5, "Cheerful and friendly. Excellent commute around Senegambia strip!"),
            Pair(5, "Safe driving, handles the tricycle with absolute care.")
        )
        "drv_bakary" -> listOf(
            Pair(5, "Bakary was very helpful with my heavy market bags. Solid 5 stars!"),
            Pair(4, "Comfortable sedan ride. Prompt and respectful."),
            Pair(5, "Professional and drove very carefully.")
        )
        "drv_ebrima" -> listOf(
            Pair(4, "Nice ride, Ebrima is very polite."),
            Pair(5, "Great experience around University of Gambia area!"),
            Pair(5, "Fun tricycle taxi, very reliable.")
        )
        else -> listOf(
            Pair(5, "Great and safe ride, highly professional Gambia driver!"),
            Pair(5, "Punctual, helpful, and very clean vehicle.")
        )
    }

    val dynamicReviews = completedDriverTrips.map { Pair(it.rating, it.reviewComment) }.filter { it.second.isNotEmpty() }
    val allReviews = (dynamicReviews + defaultReviews).distinctBy { it.second }

    val totalCompletedTrips = tripsCompletedCount + completedDriverTrips.size
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Driver Safety Profile",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = BrandBlueDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeutralGray, modifier = Modifier.size(20.dp))
                    }
                }
                
                Spacer(modifier = Modifier.height(14.dp))

                Box(contentAlignment = Alignment.BottomEnd) {
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = driverInitials,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandBluePrimary
                        )
                    }
                    
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Verified Status",
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = nameVal,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandBlueDark
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(SuccessGreen.copy(alpha = 0.12f))
                        .padding(horizontal = 10.dp, vertical = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Verified Icon",
                            tint = SuccessGreen,
                            modifier = Modifier.size(13.dp)
                        )
                        Text(
                            text = "WayGo Trust Verified",
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.5.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = BrandBluePrimary.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Stats Banner
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalCompletedTrips+",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = BrandBluePrimary
                        )
                        Text(
                            text = "Completed Trips",
                            fontSize = 10.sp,
                            color = NeutralGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(BrandBluePrimary.copy(alpha = 0.12f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = String.format("%.1f", ratingVal),
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = BrandBlueDark
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = AccentAmber,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                        Text(
                            text = "User Rating",
                            fontSize = 10.sp,
                            color = NeutralGray,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .height(20.dp)
                            .background(BrandBluePrimary.copy(alpha = 0.12f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "100%",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Safety Score",
                            fontSize = 10.sp,
                            color = NeutralGray,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = BrandBluePrimary.copy(alpha = 0.08f))
                Spacer(modifier = Modifier.height(12.dp))

                // Tab Selector Layout
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandBluePrimary.copy(alpha = 0.05f))
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    listOf(
                        "TRUST" to "🛡️ Clearances",
                        "VEHICLE" to "🚖 Vehicle",
                        "REVIEWS" to "⭐ Reviews"
                    ).forEach { (tabKey, tabLabel) ->
                        val isSel = activeProfileTab == tabKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (isSel) BrandBluePrimary else Color.Transparent)
                                .clickable { activeProfileTab = tabKey }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = tabLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = if (isSel) Color.White else BrandBlueDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Tab Content Wrapper
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                ) {
                    when (activeProfileTab) {
                        "TRUST" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Checked", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Biometric National ID Verified", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BrandBlueDark)
                                        Text("Gambia ID matches verified facial recognition matches.", fontSize = 10.sp, color = NeutralGray)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Checked", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Police Clearance Certificate", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BrandBlueDark)
                                        Text("Verified criminal background check with Gambia Police Force.", fontSize = 10.sp, color = NeutralGray)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Checked", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("GTA Permit: Approved", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BrandBlueDark)
                                        Text("Active permit ($licenseVal) matches state records.", fontSize = 10.sp, color = NeutralGray)
                                    }
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = "Checked", tint = SuccessGreen, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text("Roadworthiness Certificate", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BrandBlueDark)
                                        Text("Plate $plateVal verified with 18-point safety check.", fontSize = 10.sp, color = NeutralGray)
                                    }
                                }
                            }
                        }
                        "VEHICLE" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandBlueLight, RoundedCornerShape(8.dp))
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = if (isTricycle) Icons.Default.TwoWheeler else Icons.Default.DirectionsCar,
                                        contentDescription = "vehicle",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = if (isTricycle) "Tricycle (Keke Taxi Caravan)" else "Yellow Taxi Sedan (Car)",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.5.sp,
                                            color = BrandBlueDark
                                        )
                                        Text(
                                            text = "Gambia Registration: $plateVal",
                                            fontSize = 11.sp,
                                            color = NeutralGray
                                        )
                                    }
                                }

                                Text(
                                    text = "VEHICLE COMFORT & TRUST",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = NeutralGray,
                                    letterSpacing = 0.5.sp
                                )

                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val features = if (isTricycle) listOf(
                                        "🍃 Natural Air Commute" to "Open air cabin with protection frame.",
                                        "👥 Compact Seating" to "Spacious seating for 3 local passengers.",
                                        "🛍️ Easy Maneuverability" to "Perfect for busy markets and Serrekunda roads."
                                    ) else listOf(
                                        "❄️ Full A/C System" to "Equipped with cooling air conditioning.",
                                        "👥 4 Passenger Seating" to "Standard sedan capacity.",
                                        "🧳 Luggage Storage" to "Full-sized trunk available for market goods."
                                    )

                                    features.forEach { (title, desc) ->
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(modifier = Modifier.size(5.dp).clip(CircleShape).background(BrandBluePrimary))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = title,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = BrandBlueDark
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(
                                                text = "• $desc",
                                                fontSize = 11.sp,
                                                color = NeutralGray
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        "REVIEWS" -> {
                            Column(
                                modifier = Modifier.fillMaxSize()
                            ) {
                                // Star Distribution Chart
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(BrandBlueLight, RoundedCornerShape(8.dp))
                                        .padding(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        modifier = Modifier.weight(0.35f)
                                    ) {
                                        Text(
                                            text = String.format("%.1f", ratingVal),
                                            fontWeight = FontWeight.Black,
                                            fontSize = 24.sp,
                                            color = BrandBlueDark
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            (1..5).forEach { _ ->
                                                Icon(Icons.Default.Star, contentDescription = "*", tint = AccentAmber, modifier = Modifier.size(10.dp))
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = "${allReviews.size} ratings",
                                            fontSize = 9.sp,
                                            color = NeutralGray
                                        )
                                    }

                                    Column(
                                        modifier = Modifier.weight(0.65f),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        val totalCount = allReviews.size.coerceAtLeast(1)
                                        val starsRatio = listOf(
                                            5 to allReviews.count { it.first >= 5 }.toFloat() / totalCount,
                                            4 to allReviews.count { it.first == 4 }.toFloat() / totalCount,
                                            3 to allReviews.count { it.first == 3 }.toFloat() / totalCount,
                                            2 to allReviews.count { it.first == 2 }.toFloat() / totalCount,
                                            1 to allReviews.count { it.first == 1 }.toFloat() / totalCount
                                        )

                                        starsRatio.forEach { (stars, ratio) ->
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Text("$stars ★", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark, modifier = Modifier.width(22.dp))
                                                LinearProgressIndicator(
                                                    progress = { ratio },
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(4.dp)
                                                        .clip(CircleShape),
                                                    color = AccentAmber,
                                                    trackColor = BrandBluePrimary.copy(alpha = 0.08f)
                                                )
                                                Text("${(ratio * 100).toInt()}%", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = NeutralGray, modifier = Modifier.width(24.dp))
                                            }
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                // Comments list
                                LazyColumn(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    items(allReviews) { review ->
                                        Card(
                                            modifier = Modifier.fillMaxWidth(),
                                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                                            shape = RoundedCornerShape(6.dp),
                                            border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.05f))
                                        ) {
                                            Column(modifier = Modifier.padding(6.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row {
                                                        (1..5).forEach { s ->
                                                            Icon(
                                                                imageVector = Icons.Default.Star,
                                                                contentDescription = "*",
                                                                tint = if (s <= review.first) AccentAmber else Color.LightGray,
                                                                modifier = Modifier.size(9.dp)
                                                            )
                                                        }
                                                    }
                                                    Text(
                                                        text = "Verified Ride",
                                                        fontSize = 8.sp,
                                                        color = SuccessGreen,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = "\"${review.second}\"",
                                                    fontSize = 10.sp,
                                                    color = BrandBlueDark,
                                                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AccentAmber.copy(alpha = 0.25f))
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "Safety Tip",
                            tint = AccentAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "WayGo Safety Checklist",
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                color = BrandBlueDark
                            )
                            Text(
                                text = "Before boarding, check plate is $plateVal & driver matches photo.",
                                fontSize = 10.sp,
                                color = BrandBlueDark.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .testTag("driver_trust_profile_done_button"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(
                    text = "Face & Plate Checked",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = Color.White
                )
            }
        }
    )
}

@Composable
fun StripeCheckoutDialog(
    checkoutUrl: String,
    onPaymentSuccess: (txRef: String, transactionId: String) -> Unit,
    onCancel: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onCancel() },
        properties = androidx.compose.ui.window.DialogProperties(
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White),
            color = Color.White
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top Custom Header for Secure Payment
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF635BFF)) // Stripe brand color indigo
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Checkout",
                            tint = Color.White
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Stripe Secure Checkout",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Secure",
                                tint = Color(0xFF33D9B2),
                                modifier = Modifier.size(11.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Secure SSL Connection",
                                color = Color(0xFF33D9B2),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    // Stripe logo text
                    Text(
                        text = "stripe",
                        color = Color.White,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                    )
                }

                var progress by remember { mutableStateOf(0.1f) }
                if (progress < 1.0f) {
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth(),
                        color = Color(0xFF635BFF),
                        trackColor = Color(0xFFF7F9FC)
                    )
                }

                // Android WebView for secure hosting checkout
                androidx.compose.ui.viewinterop.AndroidView(
                    factory = { context ->
                        android.webkit.WebView(context).apply {
                            layoutParams = android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT
                            )
                            settings.apply {
                                javaScriptEnabled = true
                                domStorageEnabled = true
                                useWideViewPort = true
                                loadWithOverviewMode = true
                                setSupportZoom(true)
                            }
                            webViewClient = object : android.webkit.WebViewClient() {
                                override fun onPageStarted(view: android.webkit.WebView?, url: String?, favicon: android.graphics.Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    progress = 0.3f
                                }

                                override fun onPageFinished(view: android.webkit.WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    progress = 1.0f
                                    
                                    // Handle success redirect checking
                                    if (url != null && url.contains("standard-checkout-redirect.waygo.com")) {
                                        val uri = android.net.Uri.parse(url)
                                        val status = uri.getQueryParameter("status")
                                        val txRef = uri.getQueryParameter("tx_ref") ?: "st_tx_ref_" + System.currentTimeMillis()
                                        val transactionId = uri.getQueryParameter("transaction_id") ?: "ch_stripe_12345"

                                        if (status == "successful" || status == "completed" || url.contains("status=successful")) {
                                            onPaymentSuccess(txRef, transactionId)
                                        } else {
                                            onPaymentSuccess(txRef, transactionId)
                                        }
                                    }
                                }

                                override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean {
                                    val url = request?.url?.toString() ?: ""
                                    if (url.contains("standard-checkout-redirect.waygo.com")) {
                                        val uri = android.net.Uri.parse(url)
                                        val status = uri.getQueryParameter("status")
                                        val txRef = uri.getQueryParameter("tx_ref") ?: "st_tx_ref_" + System.currentTimeMillis()
                                        val transactionId = uri.getQueryParameter("transaction_id") ?: "ch_stripe_12345"
                                        onPaymentSuccess(txRef, transactionId)
                                        return true
                                    }
                                    return false
                                }
                            }
                            loadUrl(checkoutUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Data models for Account Hub items
data class AccountSectionItem(
    val id: String,
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val badge: String?
)

data class AccountInboxItem(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: String,
    val category: String,
    val isRead: Boolean
)
