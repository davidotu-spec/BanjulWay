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
import com.example.data.TripEntity
import com.example.ui.theme.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverScreen(
    viewModel: WayGoViewModel,
    modifier: Modifier = Modifier
) {
    val drivers by viewModel.allDrivers.collectAsState()
    val activeDriverId by viewModel.activeDriverId.collectAsState()
    val trips by viewModel.allTrips.collectAsState()
    val scheduledRides by viewModel.allScheduledRides.collectAsState()
    val notifications by viewModel.driverNotifications.collectAsState()

    var showOnboardingForm by remember { mutableStateOf(false) }

    // Find the currently active driver object
    val currentDriver = drivers.firstOrNull { it.id == activeDriverId } ?: drivers.firstOrNull()

    // Local subscription state
    var hasActiveSubscription by remember { mutableStateOf(true) }
    var showSubscriptionDialog by remember { mutableStateOf(false) }

    // Wallet Cash-out dialog state
    var showCashOutDialog by remember { mutableStateOf(false) }
    var isCashingOutDone by remember { mutableStateOf(false) }

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
                        Column {
                            Text(
                                text = "WayGo",
                                color = BrandBluePrimary,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp,
                                letterSpacing = (-0.5).sp
                            )
                            Text(
                                text = "Driver Hub",
                                color = NeutralGray,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PureWhite),
                actions = {
                    // Let user switch driver perspective dynamically to inspect different vehicles!
                    var showDriverSwitchMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showDriverSwitchMenu = true }) {
                        Icon(Icons.Default.Cached, contentDescription = "Switch Driver", tint = BrandBlueDark)
                    }
                    DropdownMenu(
                        expanded = showDriverSwitchMenu,
                        onDismissRequest = { showDriverSwitchMenu = false }
                    ) {
                        drivers.forEach { driver ->
                            DropdownMenuItem(
                                text = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(driver.name)
                                        Card(
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (driver.approvalStatus == "APPROVED") SuccessGreen.copy(alpha = 0.15f) else Color.LightGray
                                            )
                                        ) {
                                            Text(
                                                driver.approvalStatus,
                                                fontSize = 9.sp,
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    viewModel.setActiveDriver(driver.id)
                                    showDriverSwitchMenu = false
                                }
                            )
                        }
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                        DropdownMenuItem(
                            text = {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.PersonAdd,
                                        contentDescription = "register",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        "Onboard New Driver",
                                        color = BrandBluePrimary,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                showOnboardingForm = true
                                showDriverSwitchMenu = false
                            },
                            modifier = Modifier.testTag("menu_onboard_driver")
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (showOnboardingForm) {
            Box(
                modifier = modifier
                    .padding(innerPadding)
                    .fillMaxSize()
            ) {
                DriverOnboardingForm(
                    viewModel = viewModel,
                    onDismiss = { showOnboardingForm = false },
                    modifier = Modifier.fillMaxSize()
                )
            }
            return@Scaffold
        }

        if (currentDriver == null) {
            Box(
                modifier = modifier.padding(innerPadding).fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandBluePrimary)
            }
            return@Scaffold
        }

        Box(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(BrandBlueLight)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
                    .testTag("driver_screen_col"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // DRIVER HEAD CARD & STATUS TOGGLE
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = currentDriver.name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = BrandBlueDark
                                    )
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (currentDriver.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                            contentDescription = "vehicle",
                                            tint = BrandBlueSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "${if (currentDriver.vehicleType == "CAR") "Yellow Cab" else "Tricycle (Tuk)"} • ${currentDriver.vehiclePlate}",
                                            fontSize = 12.sp,
                                            color = NeutralGray
                                        )
                                    }
                                }

                                // Interactive Online toggle
                                val onlineColor = if (currentDriver.isOnline) SuccessGreen else NeutralGray
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(20.dp))
                                        .background(onlineColor.copy(alpha = 0.15f))
                                        .clickable {
                                            viewModel.toggleDriverOnlineState(currentDriver.id, !currentDriver.isOnline)
                                        }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(onlineColor)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (currentDriver.isOnline) "ONLINE" else "OFFLINE",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 11.sp,
                                        color = onlineColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            Divider()
                            Spacer(modifier = Modifier.height(12.dp))

                            // Verification / Document Summary Fulfills MVP verification status
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Verification Badge", fontWeight = FontWeight.SemiBold, fontSize = 11.sp, color = NeutralGray)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (currentDriver.approvalStatus == "APPROVED") Icons.Default.Verified else Icons.Default.NewReleases,
                                            contentDescription = "verified",
                                            tint = if (currentDriver.approvalStatus == "APPROVED") SuccessGreen else ErrorRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (currentDriver.approvalStatus == "APPROVED") "Verified WayGo Partner" else "Under Review by Admin",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (currentDriver.approvalStatus == "APPROVED") SuccessGreen else ErrorRed
                                        )
                                    }
                                }

                                Text(
                                    text = "Lic: ${currentDriver.driverLicense.takeLast(6)}",
                                    fontSize = 11.sp,
                                    color = NeutralGray
                                )
                            }
                        }
                    }
                }

                // SUB STATUS / PENDING ADMIN REVIEW NOTICE
                if (currentDriver.approvalStatus != "APPROVED") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = ErrorRed.copy(alpha = 0.1f)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = "Pend", tint = ErrorRed)
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text("Application Standard Review", fontWeight = FontWeight.Bold, color = ErrorRed, fontSize = 13.sp)
                                    Text("Your license & tricycle/car registration files are awaiting approval. Switch to Admin Portal to approve this driver instantly!", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }

                // DRIVER ALERTS & PUSH NOTIFICATIONS CENTER
                val driverNotificationsFiltered = notifications.filter { it.driverId == currentDriver.id }
                if (currentDriver.approvalStatus == "APPROVED" && currentDriver.isOnline) {
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("driver_alerts_center_card"),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
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
                                        Icon(
                                            imageVector = Icons.Default.NotificationsActive,
                                            contentDescription = "Alerts",
                                            tint = BrandBluePrimary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Text(
                                            text = "Push Notifications Inbox",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = BrandBlueDark
                                        )
                                    }

                                    if (driverNotificationsFiltered.isNotEmpty()) {
                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = BrandBluePrimary.copy(alpha = 0.12f))
                                        ) {
                                            Text(
                                                text = "${driverNotificationsFiltered.size} Alerts",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 11.sp,
                                                color = BrandBluePrimary,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                if (driverNotificationsFiltered.isEmpty()) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.MarkChatRead,
                                            contentDescription = "No alerts",
                                            tint = Color.LightGray,
                                            modifier = Modifier.size(36.dp)
                                        )
                                        Text(
                                            text = "Your Push Inbox is empty",
                                            color = NeutralGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "New passenger requests will appear here in real-time as push notifications.",
                                            color = NeutralGray.copy(alpha = 0.7f),
                                            fontSize = 10.sp,
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                    }
                                } else {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        driverNotificationsFiltered.take(4).forEach { notif ->
                                            val associatedTrip = notif.payload?.id?.let { tid ->
                                                trips.firstOrNull { it.id == tid }
                                            }
                                            val isAvailable = associatedTrip?.status == "REQUESTED"

                                            Card(
                                                colors = CardDefaults.cardColors(
                                                    containerColor = if (isAvailable) BrandBlueLight.copy(alpha = 0.6f) else Color(0xFFF1F5F9)
                                                ),
                                                shape = RoundedCornerShape(10.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                Column(modifier = Modifier.padding(12.dp)) {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        horizontalArrangement = Arrangement.SpaceBetween,
                                                        verticalAlignment = Alignment.Top
                                                    ) {
                                                        Column(modifier = Modifier.weight(1f)) {
                                                            Text(
                                                                text = notif.title,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp,
                                                                color = BrandBlueDark
                                                            )
                                                            Text(
                                                                text = notif.message,
                                                                fontSize = 11.sp,
                                                                color = Color.DarkGray,
                                                                lineHeight = 15.sp,
                                                                modifier = Modifier.padding(vertical = 4.dp)
                                                            )
                                                        }

                                                        val sdf = java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault())
                                                        val timeStr = sdf.format(java.util.Date(notif.timestamp))
                                                        Text(
                                                            text = timeStr,
                                                            fontSize = 9.sp,
                                                            color = NeutralGray,
                                                            fontWeight = FontWeight.Medium,
                                                            modifier = Modifier.padding(start = 6.dp)
                                                        )
                                                    }

                                                    Spacer(modifier = Modifier.height(6.dp))

                                                    if (isAvailable && associatedTrip != null) {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.End
                                                        ) {
                                                            Button(
                                                                onClick = {
                                                                    viewModel.acceptBooking(associatedTrip.id, currentDriver.id)
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier.height(28.dp),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp)
                                                            ) {
                                                                Text("Accept Request", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                                            }
                                                        }
                                                    } else {
                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.SpaceBetween,
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            val statusLabel = if (associatedTrip?.driverId == currentDriver.id) "Assigned to You" else "No longer available"
                                                            val statusColor = if (associatedTrip?.driverId == currentDriver.id) SuccessGreen else NeutralGray
                                                            Row(
                                                                verticalAlignment = Alignment.CenterVertically,
                                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                            ) {
                                                                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(statusColor))
                                                                Text(text = statusLabel, fontSize = 9.sp, color = statusColor, fontWeight = FontWeight.Bold)
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
                }

                // WALLET & EARNINGS CARD (Fulfills Earnings & Wallet Tracker MVP & rich Driver Earnings Dashboard)
                item {
                    var selectedTimeframe by remember { mutableStateOf("DAILY") }

                    val driverCompletedTrips = trips.filter { it.driverId == currentDriver.id && it.status == "COMPLETED" }

                    // Time Calculations using Java Calendar
                    val calendar = java.util.Calendar.getInstance()
                    calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
                    calendar.set(java.util.Calendar.MINUTE, 0)
                    calendar.set(java.util.Calendar.SECOND, 0)
                    calendar.set(java.util.Calendar.MILLISECOND, 0)
                    val beginningOfToday = calendar.timeInMillis

                    // 7 days ago (weekly)
                    calendar.add(java.util.Calendar.DAY_OF_YEAR, -6)
                    val beginningOfWeek = calendar.timeInMillis

                    // Filter subsets
                    val todayTrips = driverCompletedTrips.filter { it.timestamp >= beginningOfToday }
                    val weeklyTrips = driverCompletedTrips.filter { it.timestamp >= beginningOfWeek }

                    val activeTripsSubset = if (selectedTimeframe == "DAILY") todayTrips else weeklyTrips

                    // Earnings for active timeframe
                    val totalTips = activeTripsSubset.sumOf { it.tipGmd }
                    val totalFares = activeTripsSubset.sumOf { it.fareGmd }
                    val totalGross = totalFares + totalTips
                    val platformCommission = (totalFares * 0.15).toInt()
                    val netEarningsForTimeframe = (totalFares - platformCommission) + totalTips
                    val successfulTripsCount = activeTripsSubset.size

                    // Total available balance to Cash Out (All-time net completed trips)
                    val totalFaresAllTime = driverCompletedTrips.sumOf { it.fareGmd }
                    val totalTipsAllTime = driverCompletedTrips.sumOf { it.tipGmd }
                    val totalGrossAllTime = totalFaresAllTime + totalTipsAllTime
                    val totalCommissionAllTime = (totalFaresAllTime * 0.15).toInt()
                    val totalNetAllTime = (totalFaresAllTime - totalCommissionAllTime) + totalTipsAllTime

                    // Helper to identify regions
                    fun getTripRegionName(trip: TripEntity): String {
                        val pickup = trip.pickupName.lowercase()
                        val dropoff = trip.dropoffName.lowercase()
                        return when {
                            pickup.contains("banjul") || dropoff.contains("banjul") -> "BANJUL"
                            pickup.contains("kanifing") || dropoff.contains("kanifing") ||
                            pickup.contains("serrekunda") || dropoff.contains("serrekunda") ||
                            pickup.contains("senegambia") || dropoff.contains("senegambia") ||
                            pickup.contains("bakau") || dropoff.contains("bakau") ||
                            pickup.contains("kololi") || dropoff.contains("kololi") ||
                            pickup.contains("kairaba") || dropoff.contains("kairaba") ||
                            pickup.contains("stadium") || dropoff.contains("stadium") ||
                            pickup.contains("university") || dropoff.contains("university") -> "KANIFING"
                            else -> "KANIFING" // default fallback to Kanifing
                        }
                    }

                    // Regional counts for active timeframe
                    val banjulTrips = activeTripsSubset.filter { getTripRegionName(it) == "BANJUL" }
                    val banjulGross = banjulTrips.sumOf { it.fareGmd } + banjulTrips.sumOf { it.tipGmd }
                    val banjulCount = banjulTrips.size

                    val kanifingTrips = activeTripsSubset.filter { getTripRegionName(it) == "KANIFING" }
                    val kanifingGross = kanifingTrips.sumOf { it.fareGmd } + kanifingTrips.sumOf { it.tipGmd }
                    val kanifingCount = kanifingTrips.size

                    Card(
                        modifier = Modifier.fillMaxWidth().testTag("driver_earnings_dashboard_card"),
                        colors = CardDefaults.cardColors(containerColor = PureWhite),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            // Header with Icon
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.TrendingUp,
                                        contentDescription = "Earnings Hub",
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Driver Earnings Dashboard",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 15.sp,
                                        color = BrandBlueDark
                                    )
                                }
                                
                                Badge(
                                    containerColor = BrandBlueLight,
                                    contentColor = BrandBluePrimary
                                ) {
                                    Text(
                                        text = if (selectedTimeframe == "DAILY") "Today" else "7 Days",
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Timeframe Segmented Control
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(BrandBlueLight)
                                    .padding(2.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedTimeframe == "DAILY") PureWhite else Color.Transparent)
                                        .clickable { selectedTimeframe = "DAILY" }
                                        .padding(vertical = 8.dp)
                                        .testTag("earnings_timeframe_daily"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Daily Summary",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selectedTimeframe == "DAILY") BrandBluePrimary else NeutralGray
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (selectedTimeframe == "WEEKLY") PureWhite else Color.Transparent)
                                        .clickable { selectedTimeframe = "WEEKLY" }
                                        .padding(vertical = 8.dp)
                                        .testTag("earnings_timeframe_weekly"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        "Weekly Summary",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = if (selectedTimeframe == "WEEKLY") BrandBluePrimary else NeutralGray
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Statistics Cards Grid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Net Payout Card (Primary Focus)
                                Card(
                                    modifier = Modifier.weight(1.2f),
                                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.05f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.15f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Net Payout", fontSize = 10.sp, color = NeutralGray)
                                        Text(
                                            text = "${netEarningsForTimeframe} GMD",
                                            fontWeight = FontWeight.Black,
                                            fontSize = 16.sp,
                                            color = SuccessGreen,
                                            modifier = Modifier.testTag("total_earnings_val")
                                        )
                                        Text("Take-home", fontSize = 9.sp, color = NeutralGray.copy(alpha = 0.8f))
                                    }
                                }

                                // Gross Earnings
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = BrandBluePrimary.copy(alpha = 0.03f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Gross Fares", fontSize = 10.sp, color = NeutralGray)
                                        Text("${totalGross} GMD", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)
                                        Text("Before platform fee", fontSize = 9.sp, color = NeutralGray.copy(alpha = 0.8f))
                                    }
                                }

                                // Successful Trips Count
                                Card(
                                    modifier = Modifier.weight(1f),
                                    colors = CardDefaults.cardColors(containerColor = BrandBluePrimary.copy(alpha = 0.03f)),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.1f))
                                ) {
                                    Column(modifier = Modifier.padding(10.dp)) {
                                        Text("Success Trips", fontSize = 10.sp, color = NeutralGray)
                                        Text("${successfulTripsCount}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)
                                        Text("Completed", fontSize = 9.sp, color = NeutralGray.copy(alpha = 0.8f))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))

                            // Regional breakdown
                            Text(
                                text = "Regional Tracking (Banjul & Kanifing)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandBlueDark,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )

                            Column(
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                // Banjul Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Banjul",
                                            tint = BrandBlueSecondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Banjul City", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrandBlueDark)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "$banjulCount trips",
                                            fontSize = 11.sp,
                                            color = NeutralGray,
                                            modifier = Modifier.testTag("banjul_trips_count")
                                        )
                                        Text(
                                            text = "$banjulGross GMD",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = BrandBluePrimary,
                                            modifier = Modifier.testTag("banjul_earnings_val")
                                        )
                                    }
                                }

                                // Kanifing Row
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.LocationOn,
                                            contentDescription = "Kanifing",
                                            tint = SuccessGreen,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Kanifing Area", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrandBlueDark)
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            text = "$kanifingCount trips",
                                            fontSize = 11.sp,
                                            color = NeutralGray,
                                            modifier = Modifier.testTag("kanifing_trips_count")
                                        )
                                        Text(
                                            text = "$kanifingGross GMD",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = BrandBluePrimary,
                                            modifier = Modifier.testTag("kanifing_earnings_val")
                                        )
                                    }
                                }

                                // Dynamic ratio calculation
                                val totalGmd = (banjulGross + kanifingGross).toFloat()
                                val banjulRatio = if (totalGmd > 0f) banjulGross / totalGmd else 0.5f
                                val kanifingRatio = if (totalGmd > 0f) kanifingGross / totalGmd else 0.5f

                                // Duo-colored visual bar
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color.LightGray.copy(alpha = 0.3f))
                                ) {
                                    if (totalGmd > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(banjulRatio)
                                                .background(BrandBlueSecondary)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(kanifingRatio)
                                                .background(SuccessGreen)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxHeight()
                                                .weight(1f)
                                                .background(Color.LightGray.copy(alpha = 0.4f))
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Banjul City (${(banjulRatio * 100).toInt()}%)",
                                        fontSize = 9.sp,
                                        color = BrandBlueSecondary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        text = "Kanifing Area (${(kanifingRatio * 100).toInt()}%)",
                                        fontSize = 9.sp,
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(14.dp))

                            // Action buttons: Cash Out & Weekly Status
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        showCashOutDialog = true
                                        isCashingOutDone = false
                                    },
                                    enabled = totalNetAllTime > 0,
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Payment, contentDescription = "Pay", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Cash Out (${totalNetAllTime} GMD)", fontSize = 11.sp, maxLines = 1)
                                }

                                OutlinedButton(
                                    onClick = { showSubscriptionDialog = true },
                                    modifier = Modifier.weight(1f),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBluePrimary)
                                ) {
                                    Icon(
                                        imageVector = if (hasActiveSubscription) Icons.Default.CardMembership else Icons.Default.AddCard,
                                        contentDescription = "Sub",
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasActiveSubscription) "Weekly Active" else "Renew License",
                                        fontSize = 11.sp,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }

                // INCOMING RIDE REQUEST OVERLAY (Real-time Closed Loop!)
                val incomingRequest = trips.firstOrNull { it.status == "REQUESTED" && it.vehicleType == currentDriver.vehicleType }
                if (incomingRequest != null && currentDriver.isOnline && currentDriver.approvalStatus == "APPROVED") {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("incoming_request_card"),
                            colors = CardDefaults.cardColors(containerColor = BrandBlueDark),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(8.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NotificationsActive, contentDescription = "bell", tint = AccentAmber)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("NEW TRIP REQUEST AVAILABLE!", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }

                                    Text(
                                        text = "${incomingRequest.fareGmd} GMD",
                                        color = SuccessGreen,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 18.sp
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text("Passenger: ${incomingRequest.passengerName}", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                                Text("Pickup: ${incomingRequest.pickupName}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text("Dropoff: ${incomingRequest.dropoffName}", color = Color.White.copy(alpha = 0.8f), fontSize = 12.sp)
                                Text("Prefers: ${incomingRequest.paymentMethod} • Cashless Preferred", color = AccentAmber, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)

                                Spacer(modifier = Modifier.height(16.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { viewModel.acceptBooking(incomingRequest.id, currentDriver.id) },
                                        modifier = Modifier.weight(1.5f).testTag("accept_button"),
                                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                    ) {
                                        Text("Accept Booking Flat")
                                    }

                                    OutlinedButton(
                                        onClick = { viewModel.declineBooking(incomingRequest.id) },
                                        modifier = Modifier.weight(1.0f).testTag("decline_button"),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed)
                                    ) {
                                        Text("Ignore")
                                    }
                                }
                            }
                        }
                    }
                }

                // TRIP EXECUTION NAVIGATION SHEET (Visual control from driver App side)
                val activeExecution = trips.firstOrNull {
                    it.driverId == currentDriver.id && it.status in listOf("ACCEPTED", "ARRIVED", "EN_ROUTE")
                }
                if (activeExecution != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("active_execution_card"),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(16.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandBluePrimary)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Active Journey Guidance",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = BrandBlueDark
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Column {
                                        Text("Current Passenger: ${activeExecution.passengerName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text("Heading To: ${activeExecution.dropoffName.split(",")[0]}", fontSize = 12.sp, color = NeutralGray)
                                    }

                                    Card(colors = CardDefaults.cardColors(containerColor = BrandBlueLight)) {
                                        Text(
                                            text = activeExecution.status,
                                            fontWeight = FontWeight.Black,
                                            fontSize = 11.sp,
                                            color = BrandBluePrimary,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    var showDriverChatDialog by remember { mutableStateOf(false) }

                                    AssistChip(
                                        onClick = { showDriverChatDialog = true },
                                        label = { Text("Chat with Passenger", fontSize = 11.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Chat,
                                                contentDescription = "Chat",
                                                modifier = Modifier.size(14.dp),
                                                tint = BrandBluePrimary
                                            )
                                        },
                                        modifier = Modifier.testTag("driver_chat_launcher")
                                    )

                                    if (showDriverChatDialog) {
                                        WayGoChatDialog(
                                            tripId = activeExecution.id,
                                            currentRole = "DRIVER",
                                            currentUserId = currentDriver.id,
                                            currentUserName = "${currentDriver.name} (Driver)",
                                            viewModel = viewModel,
                                            onDismiss = {
                                                showDriverChatDialog = false
                                                viewModel.endChatSession()
                                            }
                                        )
                                    }

                                    var showCallMaskDialog by remember { mutableStateOf(false) }

                                    AssistChip(
                                        onClick = { showCallMaskDialog = true },
                                        label = { Text("Call Support Masked", fontSize = 11.sp) },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Default.Phone,
                                                contentDescription = "Call",
                                                modifier = Modifier.size(14.dp),
                                                tint = BrandBluePrimary
                                            )
                                        }
                                    )

                                    if (showCallMaskDialog) {
                                        AlertDialog(
                                            onDismissRequest = { showCallMaskDialog = false },
                                            title = { Text("Encrypted Call Masking", fontSize = 16.sp, fontWeight = FontWeight.Bold) },
                                            text = {
                                                Text(
                                                    "Calling passenger ${activeExecution.passengerName} via secure WayGo masked gateway. Phone: +220 330-WAY-MASK",
                                                    fontSize = 12.sp
                                                )
                                            },
                                            confirmButton = {
                                                Button(onClick = { showCallMaskDialog = false }) { Text("Dial") }
                                            },
                                            dismissButton = {
                                                TextButton(onClick = { showCallMaskDialog = false }) { Text("Cancel") }
                                            }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(16.dp))

                                // Driver manual steps to match real ride handling
                                when (activeExecution.status) {
                                    "ACCEPTED" -> {
                                        Button(
                                            onClick = { viewModel.acceptBooking(activeExecution.id, currentDriver.id) }, // triggers update to arrived state next
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlueSecondary)
                                        ) {
                                            Text("Set: Arrived at Pickup")
                                        }
                                    }
                                    "ARRIVED" -> {
                                        Button(
                                            onClick = { viewModel.acceptBooking(activeExecution.id, currentDriver.id) }, // shifts into active EN ROUTE
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                        ) {
                                            Text("Begin Transit & Pick Up Passenger")
                                        }
                                    }
                                    "EN_ROUTE" -> {
                                        Button(
                                            onClick = { viewModel.acceptBooking(activeExecution.id, currentDriver.id) }, // finalize as completed
                                            modifier = Modifier.fillMaxWidth().height(44.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                                        ) {
                                            Text("Complete Drive (Confirm & End)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ADVANCE BOOKINGS QUEUE CARD SECTION
                item {
                    Text(
                        "Available Advance Bookings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandBlueDark,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                val availableSchedules = scheduledRides.filter { 
                    it.status == "SCHEDULED" && it.vehicleType == currentDriver.vehicleType 
                }

                if (availableSchedules.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureWhite)
                        ) {
                            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text(
                                    "No upcoming advance bookings listed for your vehicle class right now.",
                                    color = NeutralGray,
                                    fontSize = 11.5.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    items(availableSchedules) { ride ->
                        Card(
                            modifier = Modifier.fillMaxWidth().testTag("driver_advance_ride_${ride.id}"),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.EventNote, contentDescription = "Schedule", tint = SuccessGreen, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Time: ${ride.scheduledTime}",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = BrandBlueDark
                                        )
                                    }

                                    Text(
                                        "${ride.fareGmd} GMD",
                                        fontWeight = FontWeight.Black,
                                        fontSize = 14.sp,
                                        color = SuccessGreen
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Customer: ${ride.passengerName}", fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrandBlueDark)
                                Text("Pickup: ${ride.pickupName}", fontSize = 11.sp, color = NeutralGray)
                                Text("Dropoff: ${ride.dropoffName}", fontSize = 11.sp, color = NeutralGray)

                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = { 
                                        viewModel.driverAcceptScheduledRide(ride.id, currentDriver.id, currentDriver.name) 
                                    },
                                    modifier = Modifier.fillMaxWidth().height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Icon(Icons.Default.Check, contentDescription = "Accept", modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Claim Scheduled Booking", fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }

                val myAcceptedSchedules = scheduledRides.filter {
                    it.driverId == currentDriver.id && it.status == "ACCEPTED"
                }
                
                if (myAcceptedSchedules.isNotEmpty()) {
                    item {
                        Text(
                            "My Confirmed Scheduled Rides",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandBlueDark,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }

                    items(myAcceptedSchedules) { ride ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.5.dp, BrandBluePrimary.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Date/Time: ${ride.scheduledTime}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.5.sp,
                                        color = BrandBluePrimary
                                    )

                                    Card(colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f))) {
                                        Text(
                                            "CONFIRMED",
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Passenger: ${ride.passengerName}", fontWeight = FontWeight.Medium, fontSize = 12.sp)
                                Text("Pickup: ${ride.pickupName}", fontSize = 11.sp, color = NeutralGray)
                                Text("Dropoff: ${ride.dropoffName}", fontSize = 11.sp, color = NeutralGray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("Fare: ${ride.fareGmd} GMD", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBlueDark)
                                    
                                    TextButton(
                                        onClick = { viewModel.cancelScheduledRide(ride.id) },
                                        colors = ButtonDefaults.textButtonColors(contentColor = ErrorRed)
                                    ) {
                                        Text("Release", fontSize = 11.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                // COMPLETED LIST
                item {
                    Text(
                        "Completed Orders",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = BrandBlueDark,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                val driverTripsList = trips.filter { it.driverId == currentDriver.id && it.status == "COMPLETED" }
                if (driverTripsList.isEmpty()) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = PureWhite)) {
                            Box(modifier = Modifier.padding(16.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                                Text("No rides completed today yet.", color = NeutralGray, fontSize = 12.sp)
                            }
                        }
                    }
                } else {
                    items(driverTripsList) { trip ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = PureWhite),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Pickup: ${trip.pickupName.split(",")[0]}", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = BrandBlueDark)
                                        Text("Dropoff: ${trip.dropoffName.split(",")[0]}", fontSize = 11.sp, color = NeutralGray)
                                        Text("Paid: ${trip.fareGmd} GMD via ${trip.paymentMethod}", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                                    }

                                    if (trip.rating > 0) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text("${trip.rating}", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = BrandBlueDark)
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Icon(Icons.Default.Star, contentDescription = "r", tint = AccentAmber, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }

                                if (trip.rating > 0 && (trip.reviewComment.isNotEmpty() || trip.reviewTags.isNotEmpty())) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Divider(color = Color.LightGray.copy(alpha = 0.3f))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    
                                    if (trip.reviewTags.isNotEmpty()) {
                                        Text(
                                            text = "Tags: ${trip.reviewTags}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BrandBluePrimary,
                                            modifier = Modifier.padding(bottom = 2.dp)
                                        )
                                    }
                                    if (trip.reviewComment.isNotEmpty()) {
                                        Text(
                                            text = "\"${trip.reviewComment}\"",
                                            fontSize = 11.sp,
                                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                            color = BrandBlueDark.copy(alpha = 0.8f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Wallet Dialog (Simulated Mobile Money Transfer)
            if (showCashOutDialog) {
                var mobileMoneyNum by remember { mutableStateOf("+220 384 5678") }
                AlertDialog(
                    onDismissRequest = { showCashOutDialog = false },
                    title = { Text("Wave / QMoney Digital Cashout") },
                    text = {
                        Column {
                            if (!isCashingOutDone) {
                                Text("Transfer net partner earnings to your mobile-money phone line in Gambia instantly.")
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = mobileMoneyNum,
                                    onValueChange = { mobileMoneyNum = it },
                                    label = { Text("Transfer Phone Line") },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text("Success! Wallet balance moved successfully to Wave. Standard network commissions computed.", color = SuccessGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                    },
                    confirmButton = {
                        if (!isCashingOutDone) {
                            Button(
                                onClick = {
                                    isCashingOutDone = true
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                            ) {
                                Text("Authorize Wave Transfer")
                            }
                        } else {
                            Button(onClick = { showCashOutDialog = false }) {
                                Text("Close")
                            }
                        }
                    }
                )
            }

            // Subscription Options Dialog (Fulfills Subscription Plans MVP)
            if (showSubscriptionDialog) {
                AlertDialog(
                    onDismissRequest = { showSubscriptionDialog = false },
                    title = { Text("Driver Hub Subscriptions") },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("WayGo works with a zero-interest subscription structure for drivers instead of taxing heavily.")
                            Spacer(modifier = Modifier.height(4.dp))

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { hasActiveSubscription = true; showSubscriptionDialog = false },
                                colors = CardDefaults.cardColors(containerColor = BrandBlueLight)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Weekly Access Plan", fontWeight = FontWeight.Bold, color = BrandBlueDark)
                                    Text("250 GMD / Week", fontSize = 13.sp, color = BrandBluePrimary, fontWeight = FontWeight.SemiBold)
                                    Text("Keep 100% of organic customer cash.", fontSize = 11.sp, color = NeutralGray)
                                }
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { hasActiveSubscription = true; showSubscriptionDialog = false },
                                colors = CardDefaults.cardColors(containerColor = BrandBlueLight)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text("Monthly Access Premium Plan", fontWeight = FontWeight.Bold, color = BrandBlueDark)
                                    Text("900 GMD / Month", fontSize = 13.sp, color = BrandBluePrimary, fontWeight = FontWeight.SemiBold)
                                    Text("Preferred queue scheduling algorithm & support priority, save 100 GMD.", fontSize = 11.sp, color = NeutralGray)
                                }
                            }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { showSubscriptionDialog = false }) {
                            Text("Dismiss", color = NeutralGray)
                        }
                    }
                )
            }
        }
    }
}
