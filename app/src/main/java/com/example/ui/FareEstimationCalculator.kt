package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@Composable
fun FareEstimationCalculator(
    pickupName: String,
    dropoffName: String,
    pLat: Double,
    pLng: Double,
    dLat: Double,
    dLng: Double,
    vehicleType: String, // "CAR", "TRICYCLE"
    onFareCalculated: (Int) -> Unit, // Callback to update the final parent state if desired
    modifier: Modifier = Modifier
) {
    // 1. Calculate haversine distance
    val distanceKm = remember(pLat, pLng, dLat, dLng) {
        val r = 6371.0 // Earth radius
        val dLatRad = Math.toRadians(dLat - pLat)
        val dLonRad = Math.toRadians(dLng - pLng)
        val a = Math.sin(dLatRad / 2) * Math.sin(dLatRad / 2) +
                Math.cos(Math.toRadians(pLat)) * Math.cos(Math.toRadians(dLat)) *
                Math.sin(dLonRad / 2) * Math.sin(dLonRad / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val raw = r * c
        // Prevent 0.0 or excessive coordinates
        if (raw < 0.2) 2.4 else Math.round(raw * 10) / 10.0
    }

    // 2. Interactive Surcharge selection
    var selectedSurchargeTier by remember { mutableStateOf("STANDARD") } // "OFF_PEAK", "STANDARD", "RUSH_HOUR"

    val multiplier = when (selectedSurchargeTier) {
        "OFF_PEAK" -> 0.85
        "RUSH_HOUR" -> 1.30
        else -> 1.00
    }

    // 3. Estimate Duration based on average Gambia traffic speed (say 25 km/h) plus 2 mins min buffer
    val durationMinutes = remember(distanceKm) {
        val calculated = (distanceKm / 25.0 * 60).toInt() + 2
        if (calculated < 3) 5 else calculated
    }

    // Cost parameters config
    val baseFare = if (vehicleType == "CAR") 60.0 else 30.0
    val perKmRate = if (vehicleType == "CAR") 22.0 else 11.0
    val perMinRate = if (vehicleType == "CAR") 3.0 else 1.5

    // Detailed math breakdown
    val calculatedBasePart = baseFare
    val calculatedDistancePart = distanceKm * perKmRate
    val calculatedTimePart = durationMinutes * perMinRate

    val rawSubTotal = calculatedBasePart + calculatedDistancePart + calculatedTimePart
    val rawGrandTotal = rawSubTotal * multiplier

    // Round off to nearest 5 GMD to match Gambian market change system
    val finalEstimatedTotal = remember(rawGrandTotal) {
        val rounded = (Math.round(rawGrandTotal / 5.0) * 5).toInt()
        // Ensure some reasonable minimum bounds
        val minLimit = if (vehicleType == "CAR") 100 else 50
        if (rounded < minLimit) minLimit else rounded
    }

    // Keep parent informed of estimated fare
    LaunchedEffect(finalEstimatedTotal) {
        onFareCalculated(finalEstimatedTotal)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("fare_calc_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandBlueLight),
        border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Calculate,
                            contentDescription = "Calculator",
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Detailed Fare Computation",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                }

                Text(
                    text = if (vehicleType == "CAR") "Car Sedan Tariff" else "Keke/TukTuk Tariff",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandBlueSecondary
                )
            }

            Divider(color = Color.LightGray.copy(alpha = 0.3f))

            // Main variables readouts
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = "Dist", tint = NeutralGray, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Est. Distance", fontSize = 11.sp, color = NeutralGray)
                    }
                    Text(
                        text = "$distanceKm km",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = "Time", tint = NeutralGray, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Est. Duration", fontSize = 11.sp, color = NeutralGray)
                    }
                    Text(
                        text = "$durationMinutes mins",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                }

                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.End) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Payments, contentDescription = "Change", tint = NeutralGray, modifier = Modifier.size(13.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Rounded", fontSize = 11.sp, color = NeutralGray)
                    }
                    Text(
                        text = "To nearest 5 GMD",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SuccessGreen
                    )
                }
            }

            // Breakdown list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(10.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Item 1: Base Fee
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("breakdown_base_fee"),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Base Fee", fontSize = 11.sp, color = NeutralGray)
                    Text("${baseFare.toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark, fontWeight = FontWeight.Medium)
                }

                // Item 2: Distance
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("breakdown_distance"),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Distance ($distanceKm km × ${perKmRate.toInt()} GMD)", fontSize = 11.sp, color = NeutralGray)
                    Text("${calculatedDistancePart.toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark, fontWeight = FontWeight.Medium)
                }

                // Item 3: Time Charge
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("breakdown_time"),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Traffic/Time ($durationMinutes mins × $perMinRate GMD)", fontSize = 11.sp, color = NeutralGray)
                    Text("${calculatedTimePart.toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark, fontWeight = FontWeight.Medium)
                }

                Divider(color = Color.LightGray.copy(alpha = 0.2f))

                // Item 4: Current Demand Multiplier
                Row(
                    modifier = Modifier.fillMaxWidth().testTag("breakdown_multiplier"),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Current Demand Multiplier", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                    Text(
                        "${multiplier}x",
                        fontSize = 11.sp,
                        color = if (multiplier > 1.0) ErrorRed else if (multiplier < 1.0) SuccessGreen else BrandBluePrimary,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Interactive Multi-Tier Surge selection widgets
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    Triple("OFF_PEAK", "Off-Peak (0.85x)", SuccessGreen),
                    Triple("STANDARD", "Standard (1.0x)", BrandBluePrimary),
                    Triple("RUSH_HOUR", "Rush Surcharge (1.3x)", ErrorRed)
                ).forEach { (tier, txt, color) ->
                    val isSelected = selectedSurchargeTier == tier
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedSurchargeTier = tier },
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) color.copy(alpha = 0.12f) else Color.White
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isSelected) color else Color.LightGray.copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Box(
                            modifier = Modifier.padding(6.dp).fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = txt,
                                fontSize = 9.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) color else NeutralGray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Live estimated total readout
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandBlueSecondary.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Total Guaranteed Fare", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                    Text("Includes all local tolls & traffic premiums", fontSize = 9.sp, color = NeutralGray)
                }
                Text(
                    text = "$finalEstimatedTotal GMD",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = BrandBluePrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WayGoFareEstimatorDialog(
    onDismiss: () -> Unit,
    onApplyRoute: (GLocation, GLocation, String) -> Unit,
    modifier: Modifier = Modifier
) {
    // 1. Available locations list in Banjul and Kanifing/Serrekunda
    val locations = remember { GAMBIA_LOCATIONS }

    // 2. Selectable state
    var pickupLoc by remember { mutableStateOf(locations[2]) } // Default Kairaba Avenue
    var dropoffLoc by remember { mutableStateOf(locations[0]) } // Default Albert Market
    var selectedVehicleType by remember { mutableStateOf("CAR") } // "CAR", "TRICYCLE"
    var selectedSurchargeTier by remember { mutableStateOf("STANDARD") } // "OFF_PEAK", "STANDARD", "RUSH_HOUR"

    var showPickupDropdown by remember { mutableStateOf(false) }
    var showDropoffDropdown by remember { mutableStateOf(false) }
    var showFormulaDetails by remember { mutableStateOf(false) }

    // 3. Distance calculation
    val distanceKm = remember(pickupLoc, dropoffLoc) {
        val r = 6371.0 // Earth radius
        val dLatRad = Math.toRadians(dropoffLoc.lat - pickupLoc.lat)
        val dLonRad = Math.toRadians(dropoffLoc.lng - pickupLoc.lng)
        val a = Math.sin(dLatRad / 2) * Math.sin(dLatRad / 2) +
                Math.cos(Math.toRadians(pickupLoc.lat)) * Math.cos(Math.toRadians(dropoffLoc.lat)) *
                Math.sin(dLonRad / 2) * Math.sin(dLonRad / 2)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        val raw = r * c
        if (raw < 0.1) 0.5 else Math.round(raw * 10) / 10.0
    }

    // 4. Multipliers
    val multiplier = when (selectedSurchargeTier) {
        "OFF_PEAK" -> 0.85
        "RUSH_HOUR" -> 1.30
        else -> 1.00
    }

    // 5. Travel duration estimate
    val durationMinutes = remember(distanceKm) {
        val calculated = (distanceKm / 25.0 * 60).toInt() + 2
        if (calculated < 3) 5 else calculated
    }

    // 6. Base rate calculations
    val baseFare = if (selectedVehicleType == "CAR") 60.0 else 30.0
    val perKmRate = if (selectedVehicleType == "CAR") 22.0 else 11.0
    val perMinRate = if (selectedVehicleType == "CAR") 3.0 else 1.5

    val distanceCharge = distanceKm * perKmRate
    val timeCharge = durationMinutes * perMinRate
    val subTotal = baseFare + distanceCharge + timeCharge
    val surgeTotal = subTotal * multiplier

    // Round to nearest 5 GMD (Gambia local currency convention)
    val grandTotal = remember(surgeTotal, selectedVehicleType) {
        val rounded = (Math.round(surgeTotal / 5.0) * 5).toInt()
        val minLimit = if (selectedVehicleType == "CAR") 100 else 50
        if (rounded < minLimit) minLimit else rounded
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(BrandBluePrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Calculate,
                                contentDescription = "Estimator Icon",
                                tint = BrandBluePrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "WayGo Fare Estimator",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = BrandBlueDark
                            )
                            Text(
                                text = "Regulated Banjul & Kanifing Tariff Guide",
                                fontSize = 11.sp,
                                color = NeutralGray
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = NeutralGray,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Divider(color = BrandBluePrimary.copy(alpha = 0.1f))

                // Interactive Route Pickers
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandBlueLight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pickup Location Dropdown Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PureWhite)
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { showPickupDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MyLocation,
                                    contentDescription = "From",
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("From (Pickup Location)", fontSize = 9.sp, color = NeutralGray)
                                    Text(
                                        text = pickupLoc.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlueDark,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Open", tint = NeutralGray)
                            }

                            DropdownMenu(
                                expanded = showPickupDropdown,
                                onDismissRequest = { showPickupDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                locations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc.name, fontSize = 13.sp) },
                                        onClick = {
                                            pickupLoc = loc
                                            showPickupDropdown = false
                                        }
                                    )
                                }
                            }
                        }

                        // Swap Button Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = {
                                    val temp = pickupLoc
                                    pickupLoc = dropoffLoc
                                    dropoffLoc = temp
                                },
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(PureWhite, CircleShape)
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Swap Locations",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        // Dropoff Location Dropdown Selector
                        Box(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(PureWhite)
                                    .border(1.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                    .clickable { showDropoffDropdown = true }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Place,
                                    contentDescription = "To",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("To (Destination)", fontSize = 9.sp, color = NeutralGray)
                                    Text(
                                        text = dropoffLoc.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBlueDark,
                                        maxLines = 1,
                                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                    )
                                }
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Open", tint = NeutralGray)
                            }

                            DropdownMenu(
                                expanded = showDropoffDropdown,
                                onDismissRequest = { showDropoffDropdown = false },
                                modifier = Modifier.fillMaxWidth(0.85f)
                            ) {
                                locations.forEach { loc ->
                                    DropdownMenuItem(
                                        text = { Text(loc.name, fontSize = 13.sp) },
                                        onClick = {
                                            dropoffLoc = loc
                                            showDropoffDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Route Indicators
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Route, contentDescription = "Dist", tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Est. Route Distance:",
                            fontSize = 12.sp,
                            color = NeutralGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$distanceKm km",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueDark
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, contentDescription = "Time", tint = NeutralGray, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Duration:",
                            fontSize = 12.sp,
                            color = NeutralGray
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$durationMinutes mins",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBlueDark
                        )
                    }
                }

                // Side-by-Side Vehicle Class Compare
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "COMPARE REGULATED RIDE CLASSES",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeutralGray,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Vehicle 1: Sedan
                        val sedanEst = remember(distanceKm, multiplier) {
                            val raw = (60.0 + (distanceKm * 22.0) + (durationMinutes * 3.0)) * multiplier
                            val r = (Math.round(raw / 5.0) * 5).toInt()
                            if (r < 100) 100 else r
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedVehicleType = "CAR" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedVehicleType == "CAR") BrandBluePrimary.copy(alpha = 0.08f) else PureWhite
                            ),
                            border = BorderStroke(
                                width = if (selectedVehicleType == "CAR") 2.dp else 1.dp,
                                color = if (selectedVehicleType == "CAR") BrandBluePrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.DirectionsCar,
                                    contentDescription = "Sedan",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Yellow Sedan", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandBlueDark)
                                Text("Regulated Base: 60 GMD", fontSize = 9.sp, color = NeutralGray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$sedanEst GMD",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = BrandBluePrimary
                                )
                            }
                        }

                        // Vehicle 2: Tricycle
                        val tricycleEst = remember(distanceKm, multiplier) {
                            val raw = (30.0 + (distanceKm * 11.0) + (durationMinutes * 1.5)) * multiplier
                            val r = (Math.round(raw / 5.0) * 5).toInt()
                            if (r < 50) 50 else r
                        }
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedVehicleType = "TRICYCLE" },
                            colors = CardDefaults.cardColors(
                                containerColor = if (selectedVehicleType == "TRICYCLE") BrandBluePrimary.copy(alpha = 0.08f) else PureWhite
                            ),
                            border = BorderStroke(
                                width = if (selectedVehicleType == "TRICYCLE") 2.dp else 1.dp,
                                color = if (selectedVehicleType == "TRICYCLE") BrandBluePrimary else Color.LightGray.copy(alpha = 0.5f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TwoWheeler,
                                    contentDescription = "Tricycle",
                                    tint = AccentAmber,
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Keke / Tricycle", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = BrandBlueDark)
                                Text("Regulated Base: 30 GMD", fontSize = 9.sp, color = NeutralGray)
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "$tricycleEst GMD",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 15.sp,
                                    color = AccentAmber
                                )
                            }
                        }
                    }
                }

                // Surcharge Simulator Toggles
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "HOURLY TRAFFIC SURCHARGE SURGE SIMULATION",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = NeutralGray,
                        letterSpacing = 1.sp
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf(
                            Triple("OFF_PEAK", "Off-Peak (0.85x)", SuccessGreen),
                            Triple("STANDARD", "Standard (1.0x)", BrandBluePrimary),
                            Triple("RUSH_HOUR", "Rush-Hour (1.30x)", ErrorRed)
                        ).forEach { (tier, name, color) ->
                            val isSel = selectedSurchargeTier == tier
                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { selectedSurchargeTier = tier },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSel) color.copy(alpha = 0.1f) else PureWhite
                                ),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSel) color else Color.LightGray.copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        fontSize = 10.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) color else NeutralGray
                                    )
                                }
                            }
                        }
                    }
                }

                // Detailed math breakdown accordion
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandBlueLight),
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.1f))
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showFormulaDetails = !showFormulaDetails }
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = "Info", tint = BrandBluePrimary, modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Show Tariff Calculation Breakdown", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandBlueDark)
                            }
                            Icon(
                                imageVector = if (showFormulaDetails) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                                contentDescription = "Expand",
                                tint = BrandBluePrimary
                            )
                        }

                        if (showFormulaDetails) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(PureWhite)
                                    .padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().testTag("dialog_breakdown_base_fee"),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Base Fee", fontSize = 11.sp, color = NeutralGray)
                                    Text("${baseFare.toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().testTag("dialog_breakdown_distance"),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Distance ($distanceKm km × ${perKmRate.toInt()} GMD)", fontSize = 11.sp, color = NeutralGray)
                                    Text("${distanceCharge.toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().testTag("dialog_breakdown_time"),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Time/Traffic Buffer ($durationMinutes mins × $perMinRate GMD)", fontSize = 11.sp, color = NeutralGray)
                                    Text("${timeCharge.toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark)
                                }
                                Divider(modifier = Modifier.padding(vertical = 4.dp), color = Color.LightGray.copy(alpha = 0.3f))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Subtotal", fontSize = 11.sp, color = NeutralGray)
                                    Text("${subTotal.toInt()} GMD", fontSize = 11.sp, color = BrandBlueDark)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth().testTag("dialog_breakdown_multiplier"),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Current Demand Multiplier", fontSize = 11.sp, color = NeutralGray)
                                    Text("${multiplier}x", fontSize = 11.sp, color = if (multiplier > 1.0) ErrorRed else if (multiplier < 1.0) SuccessGreen else BrandBluePrimary, fontWeight = FontWeight.Bold)
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Banjul Cash Rounding", fontSize = 11.sp, color = NeutralGray)
                                    Text("Nearest 5 GMD", fontSize = 11.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                // Final live total
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(BrandBlueDark)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Live Projected Cost", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
                        Text("Guaranteed Local Fare", fontSize = 9.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        text = "$grandTotal GMD",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onApplyRoute(pickupLoc, dropoffLoc, selectedVehicleType)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("apply_fare_estimate_btn"),
                colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Apply", tint = Color.White, modifier = Modifier.size(16.dp))
                    Text(
                        text = "Apply Route to Booking",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                }
            }
        }
    )
}

