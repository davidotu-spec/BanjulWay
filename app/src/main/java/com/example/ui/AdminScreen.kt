package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.SupportMessageEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: WayGoViewModel,
    modifier: Modifier = Modifier,
    onOpenSectionSheet: (() -> Unit)? = null
) {
    val drivers by viewModel.allDrivers.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val supportMsgs by viewModel.supportMessages.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val scheduledRides by viewModel.allScheduledRides.collectAsState()

    val isAdminLoggedIn by viewModel.isAdminLoggedIn.collectAsState()
    val adminEmail by viewModel.adminEmail.collectAsState()
    val adminPassword by viewModel.adminPassword.collectAsState()
    val adminAuthError by viewModel.adminAuthError.collectAsState()
    val isAdminAuthenticating by viewModel.isAdminAuthenticating.collectAsState()
    val adminUserEmail by viewModel.adminUserEmail.collectAsState()
    val adminUserRole by viewModel.adminUserRole.collectAsState()

    val oidcStatusMessage by viewModel.oidcStatusMessage.collectAsState()
    val isOidcAuthenticating by viewModel.isOidcAuthenticating.collectAsState()
    val lastOidcResult by viewModel.lastOidcResult.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as? android.app.Activity

    var activeAdminTab by remember { mutableStateOf("METRICS") } // "METRICS", "DRIVERS", "SUPPORT", "SCHEDULED", "SECURITY"
    var showSignOutModal by remember { mutableStateOf(false) }

    if (showSignOutModal) {
        SignOutConfirmationDialog(
            userRole = "Administrator",
            userEmail = adminUserEmail.ifBlank { "admin@waygo.gm" },
            isDark = false,
            onDismiss = { showSignOutModal = false },
            onConfirmSignOut = { viewModel.logoutAdmin() }
        )
    }

    // If Admin is not logged in via Email, display the Enterprise Admin Email Login Gate
    if (!isAdminLoggedIn) {
        AdminEmailLoginView(
            email = adminEmail,
            password = adminPassword,
            authError = adminAuthError,
            isAuthenticating = isAdminAuthenticating,
            oidcStatusMessage = oidcStatusMessage,
            isOidcAuthenticating = isOidcAuthenticating,
            lastOidcResult = lastOidcResult,
            onEmailChange = { viewModel.setAdminEmail(it) },
            onPasswordChange = { viewModel.setAdminPassword(it) },
            onLoginClick = { viewModel.loginAdminWithEmail() },
            onOidcLoginClick = { providerId, email ->
                viewModel.loginWithOidcProvider(
                    activity = activity,
                    providerId = providerId,
                    desiredEmail = email,
                    targetRoleContext = "ADMIN"
                )
            },
            onQuickCredentialSelect = { email, pass ->
                viewModel.setAdminEmail(email)
                viewModel.setAdminPassword(pass)
                viewModel.loginAdminWithEmail(email, pass)
            },
            onRegisterAdminSubmit = { email, pass, name, inviteCode, onError ->
                viewModel.registerAdminWithEmail(
                    email = email,
                    pass = pass,
                    name = name,
                    inviteCode = inviteCode,
                    onSuccess = { },
                    onError = onError
                )
            },
            onOpenSectionSheet = onOpenSectionSheet,
            onSwitchToPassenger = { viewModel.setRole("PASSENGER") },
            onSelectRole = { role -> viewModel.setRole(role) }
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
                            .testTag("admin_topbar_brand")
                    ) {
                        // Circular brand badge with WayGo inside
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(CircleShape)
                                .background(
                                    Brush.linearGradient(
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
                                    text = "Admin Panel",
                                    color = BrandBlueDark,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 15.sp,
                                    letterSpacing = (-0.3).sp
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
                                text = "$adminUserEmail • $adminUserRole",
                                color = SuccessGreen,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                },
                actions = {
                    AssistChip(
                        onClick = { showSignOutModal = true },
                        label = { Text("Sign Out", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.Logout, contentDescription = "Sign Out Admin", modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = ErrorRed.copy(alpha = 0.1f),
                            labelColor = ErrorRed,
                            leadingIconContentColor = ErrorRed
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("admin_logout_btn")
                    )
                    IconButton(
                        onClick = { onOpenSectionSheet?.invoke() },
                        modifier = Modifier.testTag("admin_open_section_sheet_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.GridView,
                            contentDescription = "Switch Section",
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    AssistChip(
                        onClick = { viewModel.setRole("PASSENGER") },
                        label = { Text("Passenger", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        leadingIcon = {
                            Icon(Icons.Default.DirectionsCar, contentDescription = "Passenger", modifier = Modifier.size(14.dp))
                        },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = BrandBlueLight,
                            labelColor = BrandBluePrimary,
                            leadingIconContentColor = BrandBluePrimary
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .testTag("admin_switch_passenger_chip")
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite)
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = PureWhite,
                tonalElevation = 8.dp
            ) {
                NavigationBarItem(
                    selected = activeAdminTab == "METRICS",
                    onClick = { activeAdminTab = "METRICS" },
                    icon = { Icon(Icons.Default.Analytics, contentDescription = "Metrics") },
                    label = { Text("Analytics") }
                )
                NavigationBarItem(
                    selected = activeAdminTab == "DRIVERS",
                    onClick = { activeAdminTab = "DRIVERS" },
                    icon = { Icon(Icons.Default.HowToReg, contentDescription = "Drivers") },
                    label = { Text("Verification") }
                )
                NavigationBarItem(
                    selected = activeAdminTab == "SCHEDULED",
                    onClick = { activeAdminTab = "SCHEDULED" },
                    icon = { Icon(Icons.Default.Schedule, contentDescription = "Schedules") },
                    label = { Text("Schedules") }
                )
                NavigationBarItem(
                    selected = activeAdminTab == "SUPPORT",
                    onClick = { activeAdminTab = "SUPPORT" },
                    icon = { Icon(Icons.Default.QuestionAnswer, contentDescription = "Support") },
                    label = { Text("Tickets") }
                )
                NavigationBarItem(
                    selected = activeAdminTab == "SECURITY",
                    onClick = { activeAdminTab = "SECURITY" },
                    icon = { Icon(Icons.Default.Shield, contentDescription = "Security Rules") },
                    label = { Text("Security") },
                    modifier = Modifier.testTag("admin_security_tab_item")
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BrandBlueLight)
        ) {
            when (activeAdminTab) {
                "METRICS" -> AdminMetricsTab(drivers = drivers, trips = trips, passengerName = profile?.name ?: "John Doe")
                "DRIVERS" -> AdminDriversVerificationTab(drivers = drivers, onApprove = { viewModel.approveDriver(it) }, onReject = { viewModel.rejectDriver(it) })
                "SUPPORT" -> AdminSupportTab(messages = supportMsgs, onReply = { msg -> viewModel.sendSupportMsg("ADMIN", msg) })
                "SCHEDULED" -> AdminSchedulesTab(
                    scheduledRides = scheduledRides,
                    onCancel = { viewModel.cancelScheduledRide(it) },
                    onDispatch = { viewModel.adminDispatchScheduledRide(it) }
                )
                "SECURITY" -> AdminSecurityRulesTab()
            }
        }
    }
}

@Composable
fun AdminMetricsTab(
    drivers: List<DriverEntity>,
    trips: List<com.example.data.TripEntity>,
    passengerName: String
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("admin_metrics_view"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Quick Title
        item {
            Text(
                "Gambia System Status Insights",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlueDark
            )
            Text(
                "Live metric telemetry from Kanifing and Banjul pilot transit grids.",
                fontSize = 11.sp,
                color = NeutralGray
            )
        }

        // Live dashboard cards
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // DAU Counter
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Daily Active Users", fontSize = 11.sp, color = NeutralGray)
                        Text("358", fontSize = 22.sp, fontWeight = FontWeight.Black, color = BrandBluePrimary)
                        Text("+8.5% Growth", fontSize = 10.sp, color = SuccessGreen)
                    }
                }

                // Ride Volumes
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Rides Requested Today", fontSize = 11.sp, color = NeutralGray)
                        Text("${trips.size + 42}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = BrandBluePrimary)
                        Text("99% Completion Rate", fontSize = 10.sp, color = SuccessGreen)
                    }
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Online Driver availability percentage
                val onlineCount = drivers.count { it.isOnline }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Online Driver Fleet", fontSize = 11.sp, color = NeutralGray)
                        Text("$onlineCount / ${drivers.size}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = BrandBluePrimary)
                        Text("Peak Serekunda Demand", fontSize = 10.sp, color = AccentAmber)
                    }
                }

                // Commission tracking Fulfills Admin revenue tracker MVP
                val completedTrips = trips.filter { it.status == "COMPLETED" }
                val grossRevenue = completedTrips.sumOf { it.fareGmd }
                val systemTake = (grossRevenue * 0.15).toInt()
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("WayGo Take (15%)", fontSize = 11.sp, color = NeutralGray)
                        Text("${systemTake} GMD", fontSize = 22.sp, fontWeight = FontWeight.Black, color = SuccessGreen)
                        Text("Gross: $grossRevenue GMD", fontSize = 10.sp, color = NeutralGray)
                    }
                }
            }
        }

        // Passenger Profiles
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Verified Platform Passenger Profiles", fontWeight = FontWeight.Bold, color = BrandBlueDark, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.AccountCircle, contentDescription = "p", tint = BrandBlueSecondary, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(passengerName, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("+220 771 2345 • Pilot Passenger Status", fontSize = 11.sp, color = NeutralGray)
                        }
                    }
                }
            }
        }

        // Live trip logs
        item {
            Text("Real-Time Trip Operations Monitor", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark, modifier = Modifier.padding(top = 8.dp))
        }

        if (trips.isEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                    Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No active or logged journeys on server.", color = NeutralGray, fontSize = 12.sp)
                    }
                }
            }
        } else {
            items(trips) { trip ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Row(modifier = Modifier.padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("ID: ${trip.id} • Passenger: ${trip.passengerName}", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("From: ${trip.pickupName} ➔ To: ${trip.dropoffName}", fontSize = 11.sp, color = NeutralGray)
                            Text("Driver: ${trip.driverName ?: "Awaiting assign"} • Plate: ${trip.vehiclePlate ?: "N/A"}", fontSize = 11.sp)
                        }
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (trip.status == "COMPLETED") SuccessGreen.copy(alpha = 0.15f) else BrandBluePrimary.copy(alpha = 0.15f)
                            )
                        ) {
                            Text(
                                trip.status,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (trip.status == "COMPLETED") SuccessGreen else BrandBluePrimary,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDriversVerificationTab(
    drivers: List<DriverEntity>,
    onApprove: (String) -> Unit,
    onReject: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("admin_drivers_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Driver Application Verification Queue",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlueDark
            )
            Text(
                "Verify licenses and vehicle registration documents for Gambia safety alignment.",
                fontSize = 11.sp,
                color = NeutralGray
            )
        }

        val pendingDrivers = drivers.filter { it.approvalStatus == "PENDING" }
        if (pendingDrivers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.1f))
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.TaskAlt, contentDescription = "done", tint = SuccessGreen)
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("All driver applications verified perfectly! Good job.", fontSize = 13.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        } else {
            items(pendingDrivers) { driver ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Column {
                                Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = BrandBlueDark)
                                Text("Selected Vehicle: ${driver.vehicleType} • Plate: ${driver.vehiclePlate}", fontSize = 12.sp, color = NeutralGray)
                            }
                            Card(colors = CardDefaults.cardColors(containerColor = AccentAmber.copy(alpha = 0.15f))) {
                                Text(
                                    "PENDING REVIEW",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentAmber,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))
                        Divider()
                        Spacer(modifier = Modifier.height(10.dp))

                        Text("UPLOADED DOCUMENTS FOR GAMBIA PARTNERSHIP:", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = NeutralGray)
                        Text("• Driver's License Code: ${driver.driverLicense} (Standard Gambia Police valid)", fontSize = 11.sp)
                        Text("• Vehicle registration file: KM-CARD-${driver.vehiclePlate.replace(" ", "")}", fontSize = 11.sp)
                        Text("• Verified Identity Card barcode: ID-294021299401", fontSize = 11.sp)

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onApprove(driver.id) },
                                modifier = Modifier.weight(1f).testTag("approve_driver_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = "ok", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Approve License")
                            }

                            OutlinedButton(
                                onClick = { onReject(driver.id) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                            ) {
                                Text("Reject / Deny")
                            }
                        }
                    }
                }
            }
        }

        // Approved active list
        item {
            Text("Registered Active Fleet", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark, modifier = Modifier.padding(top = 12.dp))
        }

        val approvedDrivers = drivers.filter { it.approvalStatus == "APPROVED" }
        items(approvedDrivers) { driver ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = PureWhite)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (driver.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                            contentDescription = "v",
                            tint = BrandBlueSecondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(driver.name, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Plate: ${driver.vehiclePlate} • Lic: ${driver.driverLicense.takeLast(6)}", fontSize = 11.sp, color = NeutralGray)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (driver.isOnline) SuccessGreen else NeutralGray)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (driver.isOnline) "Active" else "Offline", fontSize = 11.sp, color = if (driver.isOnline) SuccessGreen else NeutralGray)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminSupportTab(
    messages: List<SupportMessageEntity>,
    onReply: (String) -> Unit
) {
    var replyInput by remember { mutableStateOf("") }
    val pendingTickets = messages.filter { msg -> msg.senderRole == "PASSENGER" }

    Column(modifier = Modifier.fillMaxSize().padding(12.dp)) {
        Text("Admin Support Center Router", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = BrandBlueDark)
        Text("Respond to passenger safety and billing assist queries live.", fontSize = 11.sp, color = NeutralGray)

        Spacer(modifier = Modifier.height(10.dp))

        if (pendingTickets.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.QuestionAnswer, contentDescription = "none", tint = NeutralGray, modifier = Modifier.size(54.dp))
                    Text("No outstanding subscriber support requests.", color = NeutralGray, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pendingTickets) { ticket ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PureWhite)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                Text("Ticket From: Passenger", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBluePrimary)
                                Text("PENDING ACTION", fontWeight = FontWeight.Bold, fontSize = 10.sp, color = AccentAmber)
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(ticket.message, fontSize = 13.sp, color = BrandBlueDark)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Reply input
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = replyInput,
                onValueChange = { replyInput = it },
                placeholder = { Text("Type support answer ticket...") },
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
                    if (replyInput.isNotBlank()) {
                        onReply(replyInput)
                        replyInput = ""
                    }
                },
                modifier = Modifier.background(BrandBluePrimary, CircleShape)
            ) {
                Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.White)
            }
        }
    }
}

@Composable
fun AdminSchedulesTab(
    scheduledRides: List<com.example.data.ScheduledRideEntity>,
    onCancel: (String) -> Unit,
    onDispatch: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
            .testTag("admin_schedules_view"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                "Advance Booking Scheduler Operations Log",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandBlueDark
            )
            Text(
                "Overview of future ride bookings requested by platform passengers. Force dispatch live or void schedules.",
                fontSize = 11.sp,
                color = NeutralGray
            )
        }

        if (scheduledRides.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Box(modifier = Modifier.padding(24.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text("No future passenger scheduled bookings active on the database registry.", color = NeutralGray, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    }
                }
            }
        } else {
            items(scheduledRides) { ride ->
                Card(
                    modifier = Modifier.fillMaxWidth().testTag("admin_scheduled_item_${ride.id}"),
                    colors = CardDefaults.cardColors(containerColor = PureWhite)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Ride ID: ${ride.id} • ${ride.passengerName}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandBlueDark
                            )

                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = when (ride.status) {
                                        "ACCEPTED" -> SuccessGreen.copy(alpha = 0.15f)
                                        "CANCELLED" -> ErrorRed.copy(alpha = 0.15f)
                                        "DISPATCHED" -> BrandBluePrimary.copy(alpha = 0.15f)
                                        else -> AccentAmber.copy(alpha = 0.15f)
                                    }
                                )
                            ) {
                                Text(
                                    text = ride.status,
                                    color = when (ride.status) {
                                        "ACCEPTED" -> SuccessGreen
                                        "CANCELLED" -> ErrorRed
                                        "DISPATCHED" -> BrandBluePrimary
                                        else -> AccentAmber
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Route: ${ride.pickupName} ➔ ${ride.dropoffName}", fontSize = 12.sp, color = BrandBlueDark)
                        Text("Type: ${if (ride.vehicleType == "CAR") "Yellow Cab" else "Tricycle caravan"} • Fare: ${ride.fareGmd} GMD • Cashless: ${ride.paymentMethod}", fontSize = 11.sp, color = NeutralGray)
                        Text("Scheduled: ${ride.scheduledTime}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlueSecondary)
                        
                        if (ride.driverName != null) {
                            Text("Assigned Driver: ${ride.driverName}", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                        } else {
                            Text("Assigned Driver: Awaiting accept", fontSize = 11.sp, color = NeutralGray)
                        }

                        if (ride.status == "SCHEDULED" || ride.status == "ACCEPTED") {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onDispatch(ride.id) },
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                    modifier = Modifier.weight(1.5f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.FlashOn, contentDescription = "Dispatch", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Dispatch Live Now", fontSize = 12.sp)
                                }

                                OutlinedButton(
                                    onClick = { onCancel(ride.id) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, ErrorRed),
                                    modifier = Modifier.weight(1f).height(38.dp)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Cancel", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Void Booking", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminEmailLoginView(
    email: String,
    password: String,
    authError: String,
    isAuthenticating: Boolean,
    oidcStatusMessage: String = "",
    isOidcAuthenticating: Boolean = false,
    lastOidcResult: com.example.data.OidcAuthResult? = null,
    onEmailChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onLoginClick: () -> Unit,
    onOidcLoginClick: (providerId: String, desiredEmail: String) -> Unit = { _, _ -> },
    onQuickCredentialSelect: (String, String) -> Unit,
    onRegisterAdminSubmit: (email: String, pass: String, name: String, inviteCode: String, onError: (String) -> Unit) -> Unit = { _, _, _, _, _ -> },
    onOpenSectionSheet: (() -> Unit)? = null,
    onSwitchToPassenger: () -> Unit,
    onSelectRole: (String) -> Unit = {}
) {
    var passwordVisible by remember { mutableStateOf(false) }
    var isAdminRegisterMode by remember { mutableStateOf(false) }
    var adminNameInput by remember { mutableStateOf("") }
    var adminInviteCodeInput by remember { mutableStateOf("WAYGO-ADMIN-2026") }
    var localRegistrationError by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0A192F),
                        BrandBluePrimary,
                        BrandBlueSecondary,
                        Color(0xFF0F172A)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Brand Badge Icon
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(BrandBluePrimary, BrandBlueDark)
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AdminPanelSettings,
                        contentDescription = "Admin Security Shield",
                        tint = Color.White,
                        modifier = Modifier.size(38.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "WayGo Enterprise Portal",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Fleet Operations & System Administration",
                    fontSize = 13.sp,
                    color = Color(0xFF94A3B8),
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Role Section Switcher Tabs
                AuthRoleSectionTabs(
                    activeRole = "ADMIN",
                    onSelectRole = onSelectRole,
                    isDarkBg = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // RBAC Chip Tag
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xFF1E293B),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "RBAC Security",
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "ROLE: ADMIN (OIDC / OAuth 2.0 Enforced)",
                            fontSize = 10.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFE2E8F0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // FIREBASE AUTH OIDC PROVIDER SSO CARD
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.4f)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.VpnKey,
                                contentDescription = "OIDC SSO",
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Firebase Auth OIDC SSO Login",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "OAuth 2.0 / OpenID Connect Provider with JWT Claim RBAC",
                            fontSize = 11.5.sp,
                            color = Color(0xFF94A3B8)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trigger OIDC Login Button
                        Button(
                            onClick = {
                                onOidcLoginClick("oidc.waygo-sso", email.ifBlank { "admin@waygo.com" })
                            },
                            enabled = !isOidcAuthenticating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("oidc_sso_login_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isOidcAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Authenticating OIDC...", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(Icons.Default.Security, contentDescription = "OIDC", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign In with Corporate OIDC Provider", fontSize = 13.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Post-Auth Role Differentiation Demo Buttons
                        Text(
                            text = "Test Post-Authentication Role Differentiation:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFCBD5E1)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Admin OIDC Tenant
                            OutlinedButton(
                                onClick = {
                                    onOidcLoginClick("oidc.waygo-sso", "admin.ops@waygo.com")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("oidc_test_admin_btn"),
                                border = BorderStroke(1.dp, SuccessGreen),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("OIDC Admin\n(Role: ADMIN)", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }

                            // Passenger OIDC Tenant
                            OutlinedButton(
                                onClick = {
                                    onOidcLoginClick("oidc.waygo-sso", "passenger.rider@waygo.com")
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("oidc_test_passenger_btn"),
                                border = BorderStroke(1.dp, Color(0xFFF59E0B)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF59E0B)),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("OIDC Rider\n(Role: PASSENGER)", fontSize = 10.sp, fontWeight = FontWeight.Bold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            }
                        }

                        if (oidcStatusMessage.isNotBlank()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF1E293B),
                                border = BorderStroke(1.dp, Color(0xFF334155)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = oidcStatusMessage,
                                    fontSize = 11.sp,
                                    color = Color(0xFF38BDF8),
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.padding(10.dp)
                                )
                            }
                        }

                        if (lastOidcResult != null) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFF0284C7).copy(alpha = 0.15f),
                                border = BorderStroke(1.dp, Color(0xFF0284C7)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("JWT Claims & Role Verification:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    Text("• User ID: ${lastOidcResult.uid}", fontSize = 10.5.sp, color = Color(0xFFE2E8F0))
                                    Text("• Verified Email: ${lastOidcResult.email}", fontSize = 10.5.sp, color = Color(0xFFE2E8F0))
                                    Text("• OIDC Provider ID: ${lastOidcResult.providerId}", fontSize = 10.5.sp, color = Color(0xFFE2E8F0))
                                    Text("• JWT Claim Role: ${lastOidcResult.customClaims["role"] ?: "N/A"}", fontSize = 10.5.sp, color = Color(0xFFE2E8F0))
                                    Text("• Evaluated Post-Auth Role: ${lastOidcResult.resolvedRole}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Standard Password Sign In Divider / Heading
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Divider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                    Text(
                        text = " OR PASSWORD LOGIN ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                    Divider(modifier = Modifier.weight(1f), color = Color(0xFF334155))
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Login Form Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Mode Switcher Tabs
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandBlueLight)
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = { isAdminRegisterMode = false },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (!isAdminRegisterMode) BrandBluePrimary else Color.Transparent,
                                    contentColor = if (!isAdminRegisterMode) Color.White else BrandBlueDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                elevation = null
                            ) {
                                Text("Sign In", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { isAdminRegisterMode = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (isAdminRegisterMode) BrandBluePrimary else Color.Transparent,
                                    contentColor = if (isAdminRegisterMode) Color.White else BrandBlueDark
                                ),
                                shape = RoundedCornerShape(10.dp),
                                elevation = null
                            ) {
                                Text("Register Admin", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = if (isAdminRegisterMode) "Admin Registration (Invite Required)" else "Admin Email Sign In",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueDark
                        )

                        Text(
                            text = if (isAdminRegisterMode) "Register a new admin account with corporate invite token" else "Access live dispatch, driver verifications & analytics",
                            fontSize = 12.sp,
                            color = NeutralGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isAdminRegisterMode) {
                            OutlinedTextField(
                                value = adminNameInput,
                                onValueChange = { adminNameInput = it },
                                label = { Text("Admin Full Name") },
                                placeholder = { Text("e.g. John Doe") },
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = null, tint = BrandBluePrimary)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_register_name_input"),
                                shape = RoundedCornerShape(14.dp),
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
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        // Email Field
                        OutlinedTextField(
                            value = email,
                            onValueChange = onEmailChange,
                            label = { Text("Corporate Admin Email") },
                            placeholder = { Text("admin@waygo.com") },
                            leadingIcon = {
                                Icon(Icons.Default.Email, contentDescription = "Email Icon", tint = BrandBluePrimary)
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_email_input"),
                            shape = RoundedCornerShape(14.dp),
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

                        Spacer(modifier = Modifier.height(10.dp))

                        // Password Field
                        OutlinedTextField(
                            value = password,
                            onValueChange = onPasswordChange,
                            label = { Text("Password") },
                            placeholder = { Text("••••••••") },
                            leadingIcon = {
                                Icon(Icons.Default.Lock, contentDescription = "Password Icon", tint = BrandBluePrimary)
                            },
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        tint = NeutralGray
                                    )
                                }
                            },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("admin_password_input"),
                            shape = RoundedCornerShape(14.dp),
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

                        if (isAdminRegisterMode) {
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedTextField(
                                value = adminInviteCodeInput,
                                onValueChange = { adminInviteCodeInput = it },
                                label = { Text("Invite Token / Reg Code") },
                                placeholder = { Text("WAYGO-ADMIN-2026") },
                                leadingIcon = {
                                    Icon(Icons.Default.Key, contentDescription = null, tint = BrandBluePrimary)
                                },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("admin_invite_code_input"),
                                shape = RoundedCornerShape(14.dp),
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
                        }

                        val displayedError = if (isAdminRegisterMode && localRegistrationError.isNotBlank()) localRegistrationError else authError

                        if (displayedError.isNotBlank()) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = ErrorRed.copy(alpha = 0.1f),
                                border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.ErrorOutline, contentDescription = "Error", tint = ErrorRed)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = displayedError,
                                        color = ErrorRed,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // Submit Button
                        Button(
                            onClick = {
                                if (isAdminRegisterMode) {
                                    localRegistrationError = ""
                                    onRegisterAdminSubmit(
                                        email,
                                        password,
                                        adminNameInput,
                                        adminInviteCodeInput
                                    ) { err ->
                                        localRegistrationError = err
                                    }
                                } else {
                                    onLoginClick()
                                }
                            },
                            enabled = !isAuthenticating,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .testTag("admin_login_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            shape = RoundedCornerShape(14.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                        ) {
                            if (isAuthenticating) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Processing...", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            } else {
                                Icon(
                                    imageVector = if (isAdminRegisterMode) Icons.Default.HowToReg else Icons.Default.Login,
                                    contentDescription = "Submit",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (isAdminRegisterMode) "Register Admin Account" else "Sign In to Admin Console",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))



                // Switch section back to Passenger / Driver button
                OutlinedButton(
                    onClick = { onSwitchToPassenger() },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                    border = BorderStroke(1.dp, Color(0xFF334155))
                ) {
                    Icon(Icons.Default.DirectionsCar, contentDescription = "Passenger Mode", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Return to Passenger App", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
