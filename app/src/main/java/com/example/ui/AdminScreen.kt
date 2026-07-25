package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.SupportMessageEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    viewModel: WayGoViewModel,
    modifier: Modifier = Modifier
) {
    val drivers by viewModel.allDrivers.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val supportMsgs by viewModel.supportMessages.collectAsState()
    val profile by viewModel.userProfile.collectAsState()
    val scheduledRides by viewModel.allScheduledRides.collectAsState()

    var activeAdminTab by remember { mutableStateOf("METRICS") } // "METRICS", "DRIVERS", "SUPPORT", "SCHEDULED"

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
                                "W",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 18.sp,
                                style = LocalTextStyle.current.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                            )
                        }
                        Column {
                            Text(
                                text = "WayGo",
                                color = BrandBluePrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Admin Panel",
                                color = NeutralGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                actions = {
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
                "METRICS" -> AdminMetricsTab(drivers = drivers, trips = trips, passengerName = profile?.name ?: "David Otu")
                "DRIVERS" -> AdminDriversVerificationTab(drivers = drivers, onApprove = { viewModel.approveDriver(it) }, onReject = { viewModel.rejectDriver(it) })
                "SUPPORT" -> AdminSupportTab(messages = supportMsgs, onReply = { msg -> viewModel.sendSupportMsg("ADMIN", msg) })
                "SCHEDULED" -> AdminSchedulesTab(
                    scheduledRides = scheduledRides,
                    onCancel = { viewModel.cancelScheduledRide(it) },
                    onDispatch = { viewModel.adminDispatchScheduledRide(it) }
                )
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
                singleLine = true
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
