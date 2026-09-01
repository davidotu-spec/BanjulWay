package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.PastRideHistoryEntity
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PastRideHistoryView(
    viewModel: WayGoViewModel,
    modifier: Modifier = Modifier,
    onRebookRide: ((pickup: String, destination: String, vehicleType: String) -> Unit)? = null
) {
    val pastRides by viewModel.allPastRides.collectAsStateWithLifecycle()

    var searchQuery by remember { mutableStateOf("") }
    var selectedVehicleFilter by remember { mutableStateOf("ALL") } // "ALL", "CAR", "TRICYCLE"
    var selectedDateRangeFilter by remember { mutableStateOf("ALL") } // "ALL", "WEEK", "MONTH"
    var selectedRideDetail by remember { mutableStateOf<PastRideHistoryEntity?>(null) }
    var showAddTestRideModal by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val oneDayMs = 24 * 60 * 60 * 1000L
    val oneWeekMs = 7 * oneDayMs
    val oneMonthMs = 30 * oneDayMs

    val filteredRides = remember(pastRides, searchQuery, selectedVehicleFilter, selectedDateRangeFilter) {
        pastRides.filter { ride ->
            val matchesSearch = searchQuery.isBlank() ||
                    ride.destination.contains(searchQuery, ignoreCase = true) ||
                    ride.pickupLocation.contains(searchQuery, ignoreCase = true) ||
                    ride.driverName.contains(searchQuery, ignoreCase = true)

            val matchesVehicle = when (selectedVehicleFilter) {
                "CAR" -> ride.vehicleType == "CAR"
                "TRICYCLE" -> ride.vehicleType == "TRICYCLE"
                else -> true
            }

            val matchesDate = when (selectedDateRangeFilter) {
                "WEEK" -> (now - ride.timestamp) <= oneWeekMs
                "MONTH" -> (now - ride.timestamp) <= oneMonthMs
                else -> true
            }

            matchesSearch && matchesVehicle && matchesDate
        }
    }

    val totalSpentGmd = remember(pastRides) {
        pastRides.filter { it.status == "COMPLETED" }.sumOf { it.fareGmd + it.tipGmd }
    }
    val totalDistanceKm = remember(pastRides) {
        pastRides.sumOf { it.distanceKm }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BrandBlueLight)
            .padding(14.dp)
            .testTag("past_ride_history_screen")
    ) {
        // Room Database Table Header Banner
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp)
                .testTag("room_past_ride_history_banner"),
            colors = CardDefaults.cardColors(containerColor = PureWhite),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = BrandBluePrimary.copy(alpha = 0.12f),
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Room History",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Past Ride History",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = BrandBlueDark
                            )
                            Text(
                                text = "Stored locally in Room Database (past_ride_history table)",
                                fontSize = 10.5.sp,
                                color = NeutralGray
                            )
                        }
                    }

                    // Room DB Live Status Pill
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = SuccessGreen.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.3f))
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Room SQLite",
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = SuccessGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Stats Row: Total Rides, Spent GMD, Distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HistoryStatMetricCard(
                        title = "Past Rides",
                        value = "${pastRides.size}",
                        icon = Icons.Default.DirectionsCar,
                        color = BrandBluePrimary,
                        modifier = Modifier.weight(1f)
                    )
                    HistoryStatMetricCard(
                        title = "Total Spent",
                        value = "$totalSpentGmd GMD",
                        icon = Icons.Default.Payments,
                        color = SuccessGreen,
                        modifier = Modifier.weight(1.2f)
                    )
                    HistoryStatMetricCard(
                        title = "Total Distance",
                        value = "${String.format(java.util.Locale.US, "%.1f", totalDistanceKm)} km",
                        icon = Icons.Default.Route,
                        color = BrandBlueSecondary,
                        modifier = Modifier.weight(1.1f)
                    )
                }
            }
        }

        // Search Bar for Destinations and Pickup Locations
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
                .testTag("past_rides_search_input"),
            placeholder = { Text("Search past destinations (e.g. Senegambia, Airport)...", fontSize = 12.sp, color = NeutralGray) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = BrandBluePrimary) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = NeutralGray)
                    }
                }
            },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = PureWhite,
                unfocusedContainerColor = PureWhite,
                focusedBorderColor = BrandBluePrimary,
                unfocusedBorderColor = Color.LightGray.copy(alpha = 0.5f)
            ),
            singleLine = true
        )

        // Filter Chips (Vehicle Types & Date Ranges)
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                FilterChip(
                    selected = selectedVehicleFilter == "ALL" && selectedDateRangeFilter == "ALL",
                    onClick = {
                        selectedVehicleFilter = "ALL"
                        selectedDateRangeFilter = "ALL"
                    },
                    label = { Text("All Rides (${pastRides.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedVehicleFilter == "CAR",
                    onClick = { selectedVehicleFilter = if (selectedVehicleFilter == "CAR") "ALL" else "CAR" },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = "Car", modifier = Modifier.size(14.dp)) },
                    label = { Text("Yellow Cabs", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedVehicleFilter == "TRICYCLE",
                    onClick = { selectedVehicleFilter = if (selectedVehicleFilter == "TRICYCLE") "ALL" else "TRICYCLE" },
                    leadingIcon = { Icon(Icons.Default.TwoWheeler, contentDescription = "Tricycle", modifier = Modifier.size(14.dp)) },
                    label = { Text("Tricycle / Keke", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedDateRangeFilter == "WEEK",
                    onClick = { selectedDateRangeFilter = if (selectedDateRangeFilter == "WEEK") "ALL" else "WEEK" },
                    leadingIcon = { Icon(Icons.Default.DateRange, contentDescription = "Week", modifier = Modifier.size(14.dp)) },
                    label = { Text("Past 7 Days", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
            item {
                FilterChip(
                    selected = selectedDateRangeFilter == "MONTH",
                    onClick = { selectedDateRangeFilter = if (selectedDateRangeFilter == "MONTH") "ALL" else "MONTH" },
                    leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = "Month", modifier = Modifier.size(14.dp)) },
                    label = { Text("This Month", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = BrandBluePrimary,
                        selectedLabelColor = Color.White
                    )
                )
            }
        }

        // List of Past Rides stored in Room Database Table
        if (filteredRides.isEmpty()) {
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
                    Surface(
                        shape = CircleShape,
                        color = Color.LightGray.copy(alpha = 0.2f),
                        modifier = Modifier.size(60.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = "No rides",
                                tint = NeutralGray,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (searchQuery.isNotEmpty()) "No destinations match \"$searchQuery\"" else "No past rides in Room database",
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Your completed journeys across Banjul & Serrekunda will be saved here automatically.",
                        color = NeutralGray,
                        fontSize = 11.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { showAddTestRideModal = true },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandBluePrimary)
                    ) {
                        Icon(Icons.Default.AddLocationAlt, contentDescription = "Add Test Ride", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Custom Past Ride to Room", fontSize = 12.sp)
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .testTag("past_rides_list"),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredRides, key = { it.id }) { ride ->
                    PastRideHistoryItemCard(
                        ride = ride,
                        onClick = { selectedRideDetail = ride },
                        onRebook = {
                            onRebookRide?.invoke(ride.pickupLocation, ride.destination, ride.vehicleType)
                        },
                        onDelete = {
                            viewModel.deletePastRide(ride.id)
                        }
                    )
                }
            }
        }
    }

    // Modal: Detailed Past Ride & Receipt Breakdown
    selectedRideDetail?.let { ride ->
        PastRideDetailModal(
            ride = ride,
            onDismiss = { selectedRideDetail = null },
            onRebook = {
                selectedRideDetail = null
                onRebookRide?.invoke(ride.pickupLocation, ride.destination, ride.vehicleType)
            },
            onDelete = {
                viewModel.deletePastRide(ride.id)
                selectedRideDetail = null
            }
        )
    }

    // Modal: Add Custom Past Ride to Room Database
    if (showAddTestRideModal) {
        AddCustomPastRideModal(
            onDismiss = { showAddTestRideModal = false },
            onSave = { newRide ->
                viewModel.recordPastRide(newRide)
                showAddTestRideModal = false
            }
        )
    }
}

@Composable
fun HistoryStatMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = color.copy(alpha = 0.08f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, tint = color, modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text(text = title, fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, color = NeutralGray)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlueDark)
        }
    }
}

@Composable
fun PastRideHistoryItemCard(
    ride: PastRideHistoryEntity,
    onClick: () -> Unit,
    onRebook: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("past_ride_item_${ride.id}"),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(2.dp),
        border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: Vehicle, Date, and Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = CircleShape,
                        color = if (ride.vehicleType == "CAR") BrandBluePrimary.copy(alpha = 0.12f) else AccentAmber.copy(alpha = 0.15f),
                        modifier = Modifier.size(32.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (ride.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                contentDescription = ride.vehicleType,
                                tint = if (ride.vehicleType == "CAR") BrandBluePrimary else AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (ride.vehicleType == "CAR") "Yellow Cab (Car)" else "Tricycle (Keke)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = BrandBlueDark
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.CalendarToday, contentDescription = "Date", tint = NeutralGray, modifier = Modifier.size(10.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ride.dateFormatted,
                                fontSize = 10.sp,
                                color = NeutralGray,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${ride.fareGmd} GMD",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        color = BrandBluePrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = if (ride.status == "COMPLETED") SuccessGreen.copy(alpha = 0.12f) else ErrorRed.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = ride.status,
                            color = if (ride.status == "COMPLETED") SuccessGreen else ErrorRed,
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Origin -> Destination Route Block
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandBlueLight)
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(BrandBlueSecondary))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "From: ${ride.pickupLocation}",
                        fontSize = 11.5.sp,
                        color = BrandBlueDark,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(7.dp).clip(CircleShape).background(ErrorRed))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "To: ${ride.destination}",
                        fontSize = 12.sp,
                        color = BrandBlueDark,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Driver details & Action Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = "Driver", tint = NeutralGray, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${ride.driverName} • ${ride.vehiclePlate}",
                        fontSize = 11.sp,
                        color = NeutralGray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Icon(Icons.Default.Star, contentDescription = "Rating", tint = AccentAmber, modifier = Modifier.size(12.dp))
                    Text(
                        text = String.format(java.util.Locale.US, "%.1f", ride.rating),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = BrandBluePrimary.copy(alpha = 0.08f),
                        modifier = Modifier.clickable { onRebook() }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(Icons.Default.Replay, contentDescription = "Rebook", tint = BrandBluePrimary, modifier = Modifier.size(12.dp))
                            Spacer(modifier = Modifier.width(3.dp))
                            Text("Rebook", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PastRideDetailModal(
    ride: PastRideHistoryEntity,
    onDismiss: () -> Unit,
    onRebook: () -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth().testTag("past_ride_detail_modal")
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ride Receipt & Details",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 16.sp,
                        color = BrandBlueDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeutralGray)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Date & Vehicle Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = "Date", tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(text = ride.dateFormatted, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = BrandBlueDark)
                    }
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = SuccessGreen.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = ride.status,
                            color = SuccessGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Destination & Pickup Breakdown
                Text("Route Traveled", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeutralGray)
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(BrandBlueSecondary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Pickup Origin", fontSize = 9.5.sp, color = NeutralGray)
                        Text(ride.pickupLocation, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = BrandBlueDark)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Box(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(ErrorRed)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text("Destination", fontSize = 9.5.sp, color = NeutralGray)
                        Text(ride.destination, fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlueDark)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Divider(color = Color.LightGray.copy(alpha = 0.3f))
                Spacer(modifier = Modifier.height(12.dp))

                // Financial Itemization Breakdown
                Text("Payment Breakdown", fontWeight = FontWeight.Bold, fontSize = 12.sp, color = NeutralGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Base Fare (${ride.vehicleType.lowercase()})", fontSize = 11.5.sp, color = BrandBlueDark)
                    Text("${ride.fareGmd} GMD", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = BrandBlueDark)
                }
                if (ride.tipGmd > 0) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Driver Tip", fontSize = 11.5.sp, color = SuccessGreen)
                        Text("+${ride.tipGmd} GMD", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = SuccessGreen)
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Payment Method", fontSize = 11.5.sp, color = NeutralGray)
                    Text(ride.paymentMethod, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandBluePrimary)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total Paid", fontSize = 13.sp, fontWeight = FontWeight.ExtraBold, color = BrandBlueDark)
                    Text("${ride.fareGmd + ride.tipGmd} GMD", fontSize = 14.sp, fontWeight = FontWeight.Black, color = BrandBluePrimary)
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Driver & Ride Note
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = BrandBlueLight,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.DriveEta, contentDescription = "Driver", tint = BrandBluePrimary, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(text = "Driver: ${ride.driverName} (${ride.vehiclePlate})", fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                            Text(text = ride.notes, fontSize = 10.sp, color = NeutralGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = ErrorRed),
                        border = BorderStroke(1.dp, ErrorRed.copy(alpha = 0.5f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Delete", fontSize = 11.5.sp)
                    }

                    Button(
                        onClick = onRebook,
                        modifier = Modifier.weight(1.5f),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = "Rebook", modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rebook Route", fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCustomPastRideModal(
    onDismiss: () -> Unit,
    onSave: (PastRideHistoryEntity) -> Unit
) {
    var destination by remember { mutableStateOf("") }
    var pickup by remember { mutableStateOf("") }
    var driverName by remember { mutableStateOf("Alieu Ceesay") }
    var fareStr by remember { mutableStateOf("250") }
    var vehicleType by remember { mutableStateOf("CAR") }
    var paymentMethod by remember { mutableStateOf("WAVE") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = PureWhite,
            shadowElevation = 8.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add Past Ride to Room",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = BrandBlueDark
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = NeutralGray)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = destination,
                    onValueChange = { destination = it },
                    label = { Text("Destination (e.g. Senegambia Beach)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = pickup,
                    onValueChange = { pickup = it },
                    label = { Text("Pickup Location (e.g. Westfield Junction)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = fareStr,
                        onValueChange = { fareStr = it },
                        label = { Text("Fare (GMD)") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = driverName,
                        onValueChange = { driverName = it },
                        label = { Text("Driver Name") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = {
                        val fare = fareStr.toIntOrNull() ?: 200
                        val dateFmt = try {
                            val instant = java.time.Instant.now()
                            val formatter = java.time.format.DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                                .withZone(java.time.ZoneId.systemDefault())
                            formatter.format(instant)
                        } catch (e: Exception) {
                            "01 Sep 2026, 14:00"
                        }
                        val ride = PastRideHistoryEntity(
                            id = "past_${System.currentTimeMillis()}",
                            pickupLocation = pickup.ifBlank { "Westfield Junction, Serrekunda" },
                            destination = destination.ifBlank { "Senegambia Strip, Kololi" },
                            dateFormatted = dateFmt,
                            timestamp = System.currentTimeMillis(),
                            fareGmd = fare,
                            driverName = driverName.ifBlank { "Alieu Ceesay" },
                            vehicleType = vehicleType,
                            vehiclePlate = if (vehicleType == "CAR") "BJL 4821 C" else "KM 9312 T",
                            paymentMethod = paymentMethod,
                            status = "COMPLETED",
                            rating = 5.0f,
                            distanceKm = 6.0,
                            durationMinutes = 15,
                            notes = "Manual ride logged into local Room database"
                        )
                        onSave(ride)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = "Save", modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save to Room Database", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
