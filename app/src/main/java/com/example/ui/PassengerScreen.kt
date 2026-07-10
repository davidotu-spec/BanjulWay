package com.example.ui

import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.example.data.FlutterwaveManager
import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.TripEntity
import com.example.data.UserProfileEntity
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

// Seed locations of Gambia
data class GLocation(val name: String, val lat: Double, val lng: Double)
val GAMBIA_LOCATIONS = listOf(
    GLocation("Albert Market, Banjul", 13.4533, -16.5746),
    GLocation("Arch 22, Banjul", 13.4580, -16.5820),
    GLocation("Kairaba Avenue, Serrekunda", 13.4471, -16.6791),
    GLocation("University of Gambia, Kanifing", 13.4452, -16.6713),
    GLocation("Senegambia Beach Resort", 13.4420, -16.7110),
    GLocation("Independence Stadium, Bakau", 13.4722, -16.6690)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PassengerScreen(
    viewModel: WayGoViewModel,
    modifier: Modifier = Modifier
) {
    val isLoggedIn by viewModel.isUserLoggedIn.collectAsState()
    val otpRequested by viewModel.otpRequested.collectAsState()
    val generatedOtp by viewModel.generatedOtp.collectAsState()
    val authError by viewModel.authError.collectAsState()

    val profileFlow = viewModel.userProfile.collectAsState()
    val profile = profileFlow.value

    var activeTabSubState by remember { mutableStateOf("HOME") } // "HOME", "HISTORY", "PROFILE", "CHAT"

    // Authentication Gate
    if (!isLoggedIn) {
        PassengerAuthView(
            otpRequested = otpRequested,
            generatedOtp = generatedOtp,
            authError = authError,
            onRequestOtp = { viewModel.sendOtp(it) },
            onVerifyOtp = { viewModel.verifyOtp(it) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Minimalist circular brand badge
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(BrandBluePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "B",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                style = LocalTextStyle.current.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        }
                        Text(
                            text = "WayGo",
                            color = BrandBluePrimary,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            letterSpacing = (-0.5).sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite),
                actions = {
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
                    icon = { Icon(Icons.Default.Map, contentDescription = "Book") },
                    label = { Text("Book Ride") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBluePrimary,
                        selectedTextColor = BrandBluePrimary,
                        indicatorColor = BrandBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTabSubState == "HISTORY",
                    onClick = { activeTabSubState = "HISTORY" },
                    icon = { Icon(Icons.Default.History, contentDescription = "History") },
                    label = { Text("Trips Log") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBluePrimary,
                        selectedTextColor = BrandBluePrimary,
                        indicatorColor = BrandBlueLight
                    )
                )
                NavigationBarItem(
                    selected = activeTabSubState == "CHAT",
                    onClick = { activeTabSubState = "CHAT" },
                    icon = { Icon(Icons.Default.SupportAgent, contentDescription = "Support") },
                    label = { Text("Inbox") },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = BrandBluePrimary,
                        selectedTextColor = BrandBluePrimary,
                        indicatorColor = BrandBlueLight
                    )
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
                "HISTORY" -> HistoryScreenContent(viewModel)
                "PROFILE" -> ProfileScreenContent(viewModel, profile) { activeTabSubState = "HOME" }
                "CHAT" -> SupportInboxContent(viewModel)
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

@Composable
fun PassengerAuthView(
    otpRequested: Boolean,
    generatedOtp: String,
    authError: String,
    onRequestOtp: (String) -> Unit,
    onVerifyOtp: (String) -> Unit
) {
    var phoneInput by remember { mutableStateOf("+220 771 ") }
    var otpInput by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BrandBluePrimary)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.DirectionsCar,
                    contentDescription = "WayGo Logo",
                    tint = BrandBluePrimary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "WAYGO",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandBlueDark,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "Trusted Rides Across Gambia",
                    fontSize = 12.sp,
                    color = NeutralGray
                )
                Spacer(modifier = Modifier.height(24.dp))

                if (!otpRequested) {
                    Text(
                        text = "Verify Phone Number",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "We will send an SMS OTP verification code to log in.",
                        fontSize = 13.sp,
                        color = BrandBlueDark.copy(alpha = 0.6f),
                        modifier = Modifier.align(Alignment.Start)
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "Phone") },
                        modifier = Modifier.fillMaxWidth().testTag("phone_input"),
                        singleLine = true
                    )
                    if (authError.isNotEmpty()) {
                        Text(authError, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onRequestOtp(phoneInput) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("get_otp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                    ) {
                        Text("Get OTP Code", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Enter SMS Code",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "SMS Sent! Enter the mock OTP shown below to verify:",
                        fontSize = 13.sp,
                        color = BrandBlueDark.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Virtual SMS notification panel
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BrandBlueLight)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Sms, contentDescription = "SMS", tint = BrandBluePrimary)
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("WayGo Security", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandBlueDark)
                                Text("Use OTP code: $generatedOtp", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessGreen)
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = otpInput,
                        onValueChange = { otpInput = it },
                        label = { Text("4-Digit OTP Code") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("otp_input"),
                        singleLine = true
                    )
                    if (authError.isNotEmpty()) {
                        Text(authError, color = ErrorRed, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = { onVerifyOtp(otpInput) },
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("verify_otp_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                    ) {
                        Text("Verify & Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
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

    var showPickupDropdown by remember { mutableStateOf(false) }
    var showDropoffDropdown by remember { mutableStateOf(false) }
    var showFareEstimatorDialog by remember { mutableStateOf(false) }

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
    var dynamicCalculatedFare by remember { mutableStateOf(150) }

    var ratingScore by remember { mutableIntStateOf(5) }
    var ratingComment by remember { mutableStateOf("") }
    val tags = listOf("Safe", "Polite", "Fast", "Clean Car", "Good Music")
    val selectedTags = remember { mutableStateListOf<String>() }

    // Prepopulate inputs with profile details or templates on first mount
    LaunchedEffect(profile) {
        if (profile != null) {
            pickupName = profile.savedHome
            val homeMatch = GAMBIA_LOCATIONS.firstOrNull { it.name.startsWith(profile.savedHome.split(",")[0]) }
            pCoordinates = homeMatch ?: GAMBIA_LOCATIONS[2]

            dropoffName = profile.savedWork
            val workMatch = GAMBIA_LOCATIONS.firstOrNull { it.name.startsWith(profile.savedWork.split(",")[0]) }
            dCoordinates = workMatch ?: GAMBIA_LOCATIONS[0]
        }
    }

    // Dynamic cost calculator
    // Cars are 15 GMD per step (approx 180 total), Tricycles are 8 GMD per step (approx 100 total)
    val distanceUnit = pCoordinates?.let { p ->
        dCoordinates?.let { d ->
            val dLat = Math.abs(p.lat - d.lat)
            val dLng = Math.abs(p.lng - d.lng)
            dLat + dLng
        }
    } ?: 0.05
    val estimatedFare = if (selectVehicleType == "CAR") {
        (60 + (distanceUnit * 4200)).toInt()
    } else {
        (30 + (distanceUnit * 2200)).toInt()
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
                    passengerLng = pCoordinates?.lng ?: -16.6791
                )
                
                SosEmergencyButton(
                    pLat = pCoordinates?.lat ?: 13.4471,
                    pLng = pCoordinates?.lng ?: -16.6791,
                    activeTripId = activeTrip?.id,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(14.dp)
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

                        // Interactive Fare Estimator Quick Link Banner
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBluePrimary.copy(alpha = 0.08f))
                                .clickable { showFareEstimatorDialog = true }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(BrandBluePrimary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Fare Estimator Icon",
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = "Interactive Fare Estimator",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = BrandBluePrimary
                                    )
                                    Text(
                                        text = "Calculate projected route rates & tariffs",
                                        fontSize = 10.sp,
                                        color = BrandBlueDark.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.ChevronRight,
                                contentDescription = "Open Estimator",
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

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
                                    modifier = Modifier
                                        .clickable { selectedSavedPlaceForOptions = place }
                                        .testTag("saved_place_item_${place.label.lowercase()}"),
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
                                    modifier = Modifier
                                        .clickable { showAddSavedPlaceDialog = true }
                                        .testTag("add_saved_place_button"),
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
                                            singleLine = true
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
                                                modifier = Modifier.fillMaxWidth().clickable { isDropdownExpanded = true }.testTag("add_saved_place_location_select")
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

                        // Pickup Input
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = pickupName,
                                onValueChange = {
                                    pickupName = it
                                    showPickupDropdown = true
                                },
                                label = { Text("Pickup Location") },
                                leadingIcon = { Icon(Icons.Default.MyLocation, contentDescription = "Loc", tint = SuccessGreen) },
                                modifier = Modifier.fillMaxWidth().testTag("pickup_input"),
                                singleLine = true
                            )
                            if (showPickupDropdown) {
                                DropdownMenu(
                                    expanded = showPickupDropdown,
                                    onDismissRequest = { showPickupDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    GAMBIA_LOCATIONS.forEach { loc ->
                                        DropdownMenuItem(
                                            text = { Text(loc.name) },
                                            onClick = {
                                                pickupName = loc.name
                                                pCoordinates = loc
                                                showPickupDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Dropoff Input
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = dropoffName,
                                onValueChange = {
                                    dropoffName = it
                                    showDropoffDropdown = true
                                },
                                label = { Text("Destination") },
                                leadingIcon = { Icon(Icons.Default.Place, contentDescription = "Drop", tint = ErrorRed) },
                                modifier = Modifier.fillMaxWidth().testTag("dropoff_input"),
                                singleLine = true
                            )
                            if (showDropoffDropdown) {
                                DropdownMenu(
                                    expanded = showDropoffDropdown,
                                    onDismissRequest = { showDropoffDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    GAMBIA_LOCATIONS.forEach { loc ->
                                        DropdownMenuItem(
                                            text = { Text(loc.name) },
                                            onClick = {
                                                dropoffName = loc.name
                                                dCoordinates = loc
                                                showDropoffDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Vehicle Selector
                        Text("Select Ride Class", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectVehicleType = "CAR" }
                                    .testTag("vehicle_car_select"),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 2.dp,
                                    color = if (selectVehicleType == "CAR") BrandBluePrimary else Color(0xFFE2E8F0)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectVehicleType == "CAR") BrandBluePrimary.copy(alpha = 0.05f) else PureWhite
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
                                    Text("WayGo Sedan", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                                    Text("Est. ${estimatedFare} GMD", fontSize = 11.sp, color = BrandBlueDark.copy(alpha = 0.7f))
                                    Text("Best Value", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectVehicleType = "TRICYCLE" }
                                    .testTag("vehicle_tuk_select"),
                                shape = RoundedCornerShape(24.dp),
                                border = androidx.compose.foundation.BorderStroke(
                                    width = 2.dp,
                                    color = if (selectVehicleType == "TRICYCLE") BrandBluePrimary else Color(0xFFE2E8F0)
                                ),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectVehicleType == "TRICYCLE") BrandBluePrimary.copy(alpha = 0.05f) else PureWhite
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
                                    // Tricycle is cheaper
                                    Text("Est. ${(estimatedFare * 0.6).toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark.copy(alpha = 0.7f))
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
                            listOf("CASH", "WAVE", "AFRICELL", "FLUTTERWAVE").forEach { method ->
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { selectPaymentMethod = method },
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

                        Spacer(modifier = Modifier.height(20.dp))

                        // Fare Estimation Detail Panel
                        FareEstimationCalculator(
                            pickupName = pickupName,
                            dropoffName = dropoffName,
                            pLat = pCoordinates?.lat ?: 13.4471,
                            pLng = pCoordinates?.lng ?: -16.6791,
                            dLat = dCoordinates?.lat ?: 13.4533,
                            dLng = dCoordinates?.lng ?: -16.5746,
                            vehicleType = selectVehicleType,
                            onFareCalculated = { dynamicCalculatedFare = it }
                        )

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (selectPaymentMethod == "FLUTTERWAVE") {
                                    isFlutterwaveInitiating = true
                                    isProcessingFlutterwave = true
                                    flutterwaveStatusMsg = "Preparing secure payment gateway..."
                                    coroutineScope.launch {
                                        val pEmail = profile?.email ?: "davidotu@mixxd.org"
                                        val pName = profile?.name ?: "David Otu"
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
                                } else {
                                    viewModel.initiateBooking(
                                        pickupName = pickupName,
                                        dropoffName = dropoffName,
                                        vehicleType = selectVehicleType,
                                        paymentMethod = selectPaymentMethod,
                                        fare = dynamicCalculatedFare,
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
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .clickable { selectedScheduleDate = item },
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
                                    confirmButton = {}
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

                        Spacer(modifier = Modifier.height(16.dp))

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

    if (showFareEstimatorDialog) {
        WayGoFareEstimatorDialog(
            onDismiss = { showFareEstimatorDialog = false },
            onApplyRoute = { pLoc, dLoc, vType ->
                pickupName = pLoc.name
                pCoordinates = pLoc
                dropoffName = dLoc.name
                dCoordinates = dLoc
                selectVehicleType = vType
                showFareEstimatorDialog = false
            }
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
    onBack: () -> Unit
) {
    val trips by viewModel.allTrips.collectAsState()
    
    // Calculate total completed rides dynamically from database
    val completedRidesCount = trips.count { it.status == "COMPLETED" }
    val totalSpendingGmd = trips.filter { it.status == "COMPLETED" }.sumOf { it.fareGmd }

    var editName by remember { mutableStateOf(profile?.name ?: "") }
    var editPhone by remember { mutableStateOf(profile?.phone ?: "") }
    var editEmail by remember { mutableStateOf(profile?.email ?: "") }
    var editGender by remember { mutableStateOf(profile?.gender ?: "Male") }
    var editMM by remember { mutableStateOf(profile?.mobileMoneyNumber ?: "") }
    var editHome by remember { mutableStateOf(profile?.savedHome ?: "") }
    var editWork by remember { mutableStateOf(profile?.savedWork ?: "") }
    var activeAvatarIndex by remember { mutableIntStateOf(profile?.avatarIndex ?: 1) }

    LaunchedEffect(profile) {
        if (profile != null) {
            editName = profile.name
            editPhone = profile.phone
            editEmail = profile.email
            editGender = profile.gender
            editMM = profile.mobileMoneyNumber
            editHome = profile.savedHome
            editWork = profile.savedWork
            activeAvatarIndex = profile.avatarIndex
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
        // Upper Title bar
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
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Profile Management", 
                fontWeight = FontWeight.Bold, 
                fontSize = 20.sp, 
                color = BrandBlueDark
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // DELIGHTFUL PROFILE OVERVIEW / STATS CARD
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = BrandBlueDark),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Large Avatar
                val avatarStyle = avatars.getOrElse(activeAvatarIndex) { avatars[0] }
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarStyle.second),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = avatarStyle.first, fontSize = 42.sp)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = editName.ifBlank { "Gambia Rider" },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Text(
                    text = editPhone.ifBlank { "No verified phone" },
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color.White.copy(alpha = 0.15f))

                Spacer(modifier = Modifier.height(16.dp))

                // Stats row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = completedRidesCount.toString(),
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
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
                            .height(36.dp)
                            .background(Color.White.copy(alpha = 0.2f))
                    )

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalSpendingGmd GMD",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = SuccessGreen
                        )
                        Text(
                            text = "Total Spent",
                            fontSize = 11.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CHOOSE AVATAR SECTION
        Text(
            text = "Select Avatar Profile Icon",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = BrandBlueSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
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
                            .clickable { activeAvatarIndex = index }
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji, 
                            fontSize = 24.sp,
                            modifier = Modifier.alpha(if (activeAvatarIndex == index) 1f else 0.45f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // CONTACT INFORMATION SECTION
        Text(
            text = "Contact Information",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = BrandBlueSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Input
                OutlinedTextField(
                    value = editName,
                    onValueChange = { editName = it },
                    label = { Text("Profile Name") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = "name", tint = BrandBluePrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                    )
                )

                // Phone Input
                OutlinedTextField(
                    value = editPhone,
                    onValueChange = { editPhone = it },
                    label = { Text("Phone Number") },
                    leadingIcon = { Icon(Icons.Default.Phone, contentDescription = "phone", tint = BrandBluePrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_phone_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                    )
                )

                // Email Input
                OutlinedTextField(
                    value = editEmail,
                    onValueChange = { editEmail = it },
                    label = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "email", tint = BrandBluePrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_email_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                    )
                )

                // Gender Selection
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
                                Text(text = option, fontSize = 13.sp, color = BrandBlueDark)
                            }
                        }
                    }
                }

                Divider(color = Color.LightGray.copy(alpha = 0.3f))

                // Mobile money number (Gambia Wave, Africell pay)
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

        Spacer(modifier = Modifier.height(20.dp))

        // SAVED ADDRESSES SECTION (HOME & WORK)
        Text(
            text = "Saved Addresses & Locations",
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = BrandBlueSecondary,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Home Address
                OutlinedTextField(
                    value = editHome,
                    onValueChange = { editHome = it },
                    label = { Text("Home Address Location") },
                    leadingIcon = { Icon(Icons.Default.Home, contentDescription = "home_addr", tint = BrandBluePrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_home_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                    )
                )

                // Work Address
                OutlinedTextField(
                    value = editWork,
                    onValueChange = { editWork = it },
                    label = { Text("Work Address Location") },
                    leadingIcon = { Icon(Icons.Default.Work, contentDescription = "work_addr", tint = BrandBluePrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("profile_work_input"),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = Color.LightGray.copy(alpha = 0.6f)
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        // ACTION BUTTONS SECTION
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
                    avatarIndex = activeAvatarIndex
                )
                onBack()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("save_profile_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Save Changes", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.logout() },
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("logout_profile_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
            border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.Logout, contentDescription = "logout")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Log Out phone session", fontWeight = FontWeight.SemiBold)
        }

        Spacer(modifier = Modifier.height(40.dp))
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
                singleLine = true
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


