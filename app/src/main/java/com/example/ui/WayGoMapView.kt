package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.data.DriverEntity
import com.example.data.TripEntity
import com.example.ui.theme.*

// LatLng bbox for Kanifing / Serekunda / Banjul region
// Min Lat: 13.4300, Max Lat: 13.4800
// Min Lng: -16.7200, Max Lng: -16.5600
private const val MIN_LAT = 13.4300
private const val MAX_LAT = 13.4800
private const val MIN_LNG = -16.7200
private const val MAX_LNG = -16.5600

data class MapLandmark(
    val name: String,
    val lat: Double,
    val lng: Double,
    val description: String
)

val GAMBIAN_LANDMARKS = listOf(
    MapLandmark("Albert Market", 13.4533, -16.5746, "Market hub in Banjul"),
    MapLandmark("Arch 22", 13.4580, -16.5820, "Historic Banjul gateway gate"),
    MapLandmark("University of Gambia", 13.4452, -16.6713, "Education center"),
    MapLandmark("Kairaba Business Hub", 13.4471, -16.6791, "Serrekunda shopping street"),
    MapLandmark("Senegambia Beach", 13.4420, -16.7110, "Tourist & beach hotels"),
    MapLandmark("Independence Stadium", 13.4722, -16.6690, "Bakau national stadium")
)

private fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Earth's radius in km
    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)
    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@OptIn(ExperimentalTextApi::class)
@Composable
fun WayGoMapView(
    modifier: Modifier = Modifier,
    drivers: List<DriverEntity> = emptyList(),
    activeTrip: TripEntity? = null,
    simulatedDriverLat: Double? = null,
    simulatedDriverLng: Double? = null,
    passengerLat: Double = 13.4471,
    passengerLng: Double = -16.6791,
    progress: Float = 0f,
    onSelectDriver: ((DriverEntity) -> Unit)? = null
) {
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current

    // Interactive Map Control States
    var mapType by remember { mutableStateOf("ROADMAP") } // "ROADMAP", "SATELLITE", "TERRAIN"
    var showTraffic by remember { mutableStateOf(false) }
    var vehicleFilter by remember { mutableStateOf("ALL") } // "ALL", "CAR", "TRICYCLE"
    var zoomLevel by remember { mutableFloatStateOf(1.0f) }
    var selectedDriverOnMap by remember { mutableStateOf<DriverEntity?>(null) }
    var showMapTypeMenu by remember { mutableStateOf(false) }

    val isActiveRide = activeTrip != null && activeTrip.status in listOf("ACCEPTED", "ARRIVED", "EN_ROUTE")
    val boxHeight = if (isActiveRide) 380.dp else 340.dp

    // Filter available online drivers
    val availableDrivers = remember(drivers, vehicleFilter) {
        drivers.filter { driver ->
            driver.isOnline && driver.approvalStatus == "APPROVED" &&
                    (activeTrip == null || activeTrip.driverId != driver.id) &&
                    (vehicleFilter == "ALL" || driver.vehicleType == vehicleFilter)
        }
    }

    val carCount = remember(drivers) { drivers.count { it.isOnline && it.approvalStatus == "APPROVED" && it.vehicleType == "CAR" } }
    val bikeCount = remember(drivers) { drivers.count { it.isOnline && it.approvalStatus == "APPROVED" && it.vehicleType == "TRICYCLE" } }

    // Pulsing animation for real-time driver GPS pins
    val infiniteTransition = rememberInfiniteTransition(label = "driver_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 0.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "pulse_alpha"
    )

    // Colors matching official Google Maps themes
    val (landColor, oceanColor, roadBorderColor, roadFillColor, highwayColor, parkColor) = when (mapType) {
        "SATELLITE" -> Tuple6(
            Color(0xFF0F172A), // Dark slate satellite base
            Color(0xFF0284C7), // Ocean deep blue
            Color(0xFF334155),
            Color(0xFF1E293B),
            Color(0xFF38BDF8),
            Color(0xFF065F46)
        )
        "TERRAIN" -> Tuple6(
            Color(0xFFEFEBE4), // Topographic warm paper
            Color(0xFF90CAF9),
            Color(0xFFD7CCC8),
            Color(0xFFFFFFFF),
            Color(0xFFFFB74D),
            Color(0xFFA5D6A7)
        )
        else -> Tuple6( // "ROADMAP" - Official Google Light Palette
            Color(0xFFF5F3ED), // Google Map Land warm off-white
            Color(0xFFAADAFF), // Google Map Sky Water
            Color(0xFFE6E6E6),
            Color(0xFFFFFFFF),
            Color(0xFFFFD54F), // Google Highway Yellow
            Color(0xFFC8E6C9)  // Google Park Light Green
        )
    }

    // Keep track of rendered driver screen positions for tap detection
    val driverPositions = remember { mutableStateListOf<Pair<DriverEntity, Offset>>() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clip(RoundedCornerShape(24.dp))
            .border(1.5.dp, if (mapType == "SATELLITE") Color(0xFF334155) else Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            .background(landColor)
            .testTag("google_maps_view")
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(availableDrivers, zoomLevel, mapType) {
                    detectTapGestures { tapOffset ->
                        val touchRadiusPx = with(density) { 32.dp.toPx() }
                        val tapped = driverPositions.firstOrNull { (_, pos) ->
                            val dx = tapOffset.x - pos.x
                            val dy = tapOffset.y - pos.y
                            (dx * dx + dy * dy) <= (touchRadiusPx * touchRadiusPx)
                        }?.first

                        selectedDriverOnMap = tapped
                    }
                }
        ) {
            val width = size.width
            val height = size.height

            // Clear previous positions
            driverPositions.clear()

            // Coordinate mapping with zoom scaling
            fun getX(lng: Double): Float {
                val ratio = (lng - MIN_LNG) / (MAX_LNG - MIN_LNG)
                val rawX = ratio * width
                val centerX = width / 2f
                return (centerX + (rawX - centerX) * zoomLevel).toFloat()
            }

            fun getY(lat: Double): Float {
                val ratio = (MAX_LAT - lat) / (MAX_LAT - MIN_LAT)
                val rawY = ratio * height
                val centerY = height / 2f
                return (centerY + (rawY - centerY) * zoomLevel).toFloat()
            }

            // 1. Draw Land Mass
            drawRect(color = landColor)

            // 2. Draw Parks / Greenery polygons
            val parkPath = Path().apply {
                val x1 = getX(-16.7050)
                val y1 = getY(13.4450)
                val x2 = getX(-16.6900)
                val y2 = getY(13.4600)
                moveTo(x1, y1)
                lineTo(x2, y1)
                lineTo(x2, y2)
                lineTo(x1, y2)
                close()
            }
            drawPath(parkPath, color = parkColor.copy(alpha = 0.5f))

            // 3. Draw Google Map Grid / Block Texture
            val gridSize = 36.dp.toPx() * zoomLevel
            var curX = 0f
            val gridColor = if (mapType == "SATELLITE") Color(0xFF334155).copy(alpha = 0.2f) else Color(0xFFE0E0E0).copy(alpha = 0.25f)
            while (curX < width) {
                drawLine(color = gridColor, start = Offset(curX, 0f), end = Offset(curX, height), strokeWidth = 1f)
                curX += gridSize
            }
            var curY = 0f
            while (curY < height) {
                drawLine(color = gridColor, start = Offset(0f, curY), end = Offset(width, curY), strokeWidth = 1f)
                curY += gridSize
            }

            // 4. Draw Atlantic Ocean coastline
            val oceanPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                quadraticTo(width * 0.7f, height * 0.28f, width * 0.45f, height * 0.15f)
                quadraticTo(width * 0.2f, height * 0.38f, 0f, height * 0.22f)
                close()
            }
            drawPath(oceanPath, color = oceanColor)

            // River Gambia Estuary
            val riverPath = Path().apply {
                moveTo(width, height * 0.25f)
                quadraticTo(width * 0.85f, height * 0.42f, width * 0.88f, height * 0.68f)
                quadraticTo(width * 0.8f, height * 0.85f, width * 0.84f, height)
                lineTo(width, height)
                close()
            }
            drawPath(riverPath, color = oceanColor)

            // 5. Draw Google Roads & Major Highways
            // Banjul - Serrekunda Highway
            val hStart = Offset(getX(-16.7110), getY(13.4380))
            val hMid = Offset(getX(-16.6500), getY(13.4490))
            val hEnd = Offset(getX(-16.5820), getY(13.4580))

            drawLine(roadBorderColor, hStart, hMid, strokeWidth = 10f * zoomLevel)
            drawLine(roadBorderColor, hMid, hEnd, strokeWidth = 10f * zoomLevel)
            drawLine(highwayColor, hStart, hMid, strokeWidth = 6f * zoomLevel)
            drawLine(highwayColor, hMid, hEnd, strokeWidth = 6f * zoomLevel)

            // Kairaba Avenue
            val kStart = Offset(getX(-16.6820), getY(13.4310))
            val kEnd = Offset(getX(-16.6710), getY(13.4750))
            drawLine(roadBorderColor, kStart, kEnd, strokeWidth = 8f * zoomLevel)
            drawLine(roadFillColor, kStart, kEnd, strokeWidth = 4f * zoomLevel)

            // Atlantic Boulevard
            val bStart = Offset(getX(-16.7110), getY(13.4420))
            val bEnd = Offset(getX(-16.6800), getY(13.4750))
            drawLine(roadBorderColor, bStart, bEnd, strokeWidth = 7f * zoomLevel)
            drawLine(roadFillColor, bStart, bEnd, strokeWidth = 3.5f * zoomLevel)

            // 6. Google Live Traffic Overlay Layer
            if (showTraffic) {
                // Smooth traffic (Green) along Highway
                drawLine(Color(0xFF4CAF50), hStart, hMid, strokeWidth = 4f * zoomLevel)
                // Moderate traffic (Orange) along Kairaba
                drawLine(Color(0xFFFF9800), kStart, kEnd, strokeWidth = 4f * zoomLevel)
                // Congested traffic (Red) along Banjul entry
                drawLine(Color(0xFFF44336), hMid, hEnd, strokeWidth = 5f * zoomLevel)
            }

            // 7. Draw Landmarks
            GAMBIAN_LANDMARKS.forEach { landmark ->
                val lx = getX(landmark.lng)
                val ly = getY(landmark.lat)

                if (lx in 0f..width && ly in 0f..height) {
                    drawCircle(color = BrandBlueDark.copy(alpha = 0.3f), radius = 5f * zoomLevel, center = Offset(lx, ly))
                    drawCircle(color = BrandBlueSecondary, radius = 3f * zoomLevel, center = Offset(lx, ly))

                    val textStyle = TextStyle(
                        fontSize = (8.5.sp.value * zoomLevel).sp,
                        fontWeight = FontWeight.Bold,
                        color = if (mapType == "SATELLITE") Color.White else BrandBlueDark.copy(alpha = 0.85f)
                    )
                    val textLayoutResult = textMeasurer.measure(text = landmark.name, style = textStyle)
                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = Offset(lx - textLayoutResult.size.width / 2f, ly + 5.dp.toPx())
                    )
                }
            }

            // 8. Active Trip Routing Line (if ride in progress)
            activeTrip?.let { trip ->
                val pX = getX(trip.pickupLng)
                val pY = getY(trip.pickupLat)
                val dX = getX(trip.dropoffLng)
                val dY = getY(trip.dropoffLat)

                // Pickup Pin
                drawCircle(color = SuccessGreen.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(pX, pY))
                drawCircle(color = SuccessGreen, radius = 6.dp.toPx(), center = Offset(pX, pY))

                // Dropoff Pin
                drawCircle(color = ErrorRed.copy(alpha = 0.25f), radius = 14.dp.toPx(), center = Offset(dX, dY))
                drawCircle(color = ErrorRed, radius = 6.dp.toPx(), center = Offset(dX, dY))

                // Polyline Route
                drawLine(
                    color = BrandBluePrimary,
                    start = Offset(pX, pY),
                    end = Offset(dX, dY),
                    strokeWidth = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(14f, 14f), 0f)
                )
            }

            // 9. DRAW REAL-TIME NEARBY AVAILABLE DRIVER MARKERS
            availableDrivers.forEach { driver ->
                val dX = getX(driver.currentLng)
                val dY = getY(driver.currentLat)

                if (dX in -50f..(width + 50f) && dY in -50f..(height + 50f)) {
                    // Record position for touch detection
                    driverPositions.add(Pair(driver, Offset(dX, dY)))

                    val isSelected = selectedDriverOnMap?.id == driver.id
                    val baseColor = if (driver.vehicleType == "CAR") BrandBluePrimary else AccentAmber

                    // Live Pulsing Radar Ring around available driver
                    val pulseRadius = (12.dp.toPx() * pulseScale) * (if (isSelected) 1.4f else 1.0f)
                    drawCircle(
                        color = baseColor.copy(alpha = pulseAlpha * (if (isSelected) 0.9f else 0.5f)),
                        radius = pulseRadius,
                        center = Offset(dX, dY)
                    )

                    if (isSelected) {
                        // Outer selection halo
                        drawCircle(
                            color = Color(0xFFFFD54F),
                            radius = 16.dp.toPx(),
                            center = Offset(dX, dY)
                        )
                    }

                    // Google Maps Pin Marker Base
                    drawCircle(
                        color = Color.White,
                        radius = 10.dp.toPx(),
                        center = Offset(dX, dY)
                    )
                    drawCircle(
                        color = baseColor,
                        radius = 8.dp.toPx(),
                        center = Offset(dX, dY)
                    )

                    // Calculate distance & ETA to passenger for overhead pill
                    val distKm = calculateDistance(passengerLat, passengerLng, driver.currentLat, driver.currentLng)
                    val etaMin = (distKm * 3.5 + 2).toInt().coerceAtLeast(1)
                    val driverFirstName = driver.name.split(" ")[0]
                    val labelText = "$driverFirstName • ${etaMin}m"

                    // Overhead Driver ETA Pill on Canvas
                    val labelStyle = TextStyle(
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                    val measured = textMeasurer.measure(labelText, labelStyle)
                    val pillWidth = measured.size.width + 16.dp.toPx()
                    val pillHeight = measured.size.height + 8.dp.toPx()
                    val pillLeft = dX - pillWidth / 2f
                    val pillTop = dY - 22.dp.toPx() - pillHeight

                    drawRoundRect(
                        color = Color.White.copy(alpha = 0.95f),
                        topLeft = Offset(pillLeft, pillTop),
                        size = androidx.compose.ui.geometry.Size(pillWidth, pillHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                    )
                    drawRoundRect(
                        color = baseColor.copy(alpha = 0.6f),
                        topLeft = Offset(pillLeft, pillTop),
                        size = androidx.compose.ui.geometry.Size(pillWidth, pillHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                        style = Stroke(width = 2f)
                    )
                    drawText(
                        textLayoutResult = measured,
                        topLeft = Offset(pillLeft + 8.dp.toPx(), pillTop + 4.dp.toPx())
                    )
                }
            }

            // 10. Draw Passenger Location (Blue Dot with Pulsing Halo)
            if (!isActiveRide) {
                val pX = getX(passengerLng)
                val pY = getY(passengerLat)

                drawCircle(
                    color = Color(0xFF4285F4).copy(alpha = 0.25f),
                    radius = 18.dp.toPx(),
                    center = Offset(pX, pY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(pX, pY)
                )
                drawCircle(
                    color = Color(0xFF4285F4),
                    radius = 6.dp.toPx(),
                    center = Offset(pX, pY)
                )
            }
        }

        // =========================================================
        // GOOGLE MAPS CONTROLS & OVERLAYS
        // =========================================================

        // Top Header Bar Overlay
        Column(
            modifier = Modifier
                .zIndex(10f)
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.White.copy(alpha = 0.95f),
                shape = RoundedCornerShape(12.dp),
                shadowElevation = 3.dp,
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Google Maps Branding Badge
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = Color(0xFF4285F4),
                            modifier = Modifier.size(22.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("G", color = Color.White, fontWeight = FontWeight.Black, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Column {
                            Text("Google Maps", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BrandBlueDark)
                            Text("${availableDrivers.size} drivers nearby", fontSize = 9.5.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Map Layer & Traffic Switches
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Traffic Layer Toggle Button
                        FilterChip(
                            selected = showTraffic,
                            onClick = { showTraffic = !showTraffic },
                            label = { Text("Traffic", fontSize = 9.5.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Traffic,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = if (showTraffic) Color.White else NeutralGray
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFFF44336),
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.height(26.dp)
                        )

                        // Map Type Dropdown Button
                        Box {
                            IconButton(
                                onClick = { showMapTypeMenu = !showMapTypeMenu },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(Icons.Default.Layers, contentDescription = "Map Style", tint = BrandBluePrimary, modifier = Modifier.size(18.dp))
                            }

                            DropdownMenu(
                                expanded = showMapTypeMenu,
                                onDismissRequest = { showMapTypeMenu = false }
                            ) {
                                DropdownMenuItem(
                                    text = { Text("Default Roadmap") },
                                    leadingIcon = { Icon(Icons.Default.Map, contentDescription = null) },
                                    onClick = { mapType = "ROADMAP"; showMapTypeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Satellite View") },
                                    leadingIcon = { Icon(Icons.Default.Satellite, contentDescription = null) },
                                    onClick = { mapType = "SATELLITE"; showMapTypeMenu = false }
                                )
                                DropdownMenuItem(
                                    text = { Text("Terrain View") },
                                    leadingIcon = { Icon(Icons.Default.Terrain, contentDescription = null) },
                                    onClick = { mapType = "TERRAIN"; showMapTypeMenu = false }
                                )
                            }
                        }
                    }
                }
            }

            // Vehicle Category Filter Chips Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    onClick = { vehicleFilter = "ALL" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (vehicleFilter == "ALL") BrandBluePrimary else Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.3f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(26.dp)
                ) {
                    Box(modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "All (${availableDrivers.size})",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vehicleFilter == "ALL") Color.White else BrandBlueDark
                        )
                    }
                }

                Surface(
                    onClick = { vehicleFilter = "CAR" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (vehicleFilter == "CAR") BrandBluePrimary else Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.3f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.DirectionsCar,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (vehicleFilter == "CAR") Color.White else BrandBluePrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Cars ($carCount)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vehicleFilter == "CAR") Color.White else BrandBlueDark
                        )
                    }
                }

                Surface(
                    onClick = { vehicleFilter = "TRICYCLE" },
                    shape = RoundedCornerShape(20.dp),
                    color = if (vehicleFilter == "TRICYCLE") AccentAmber else Color.White.copy(alpha = 0.9f),
                    border = BorderStroke(1.dp, AccentAmber.copy(alpha = 0.3f)),
                    shadowElevation = 2.dp,
                    modifier = Modifier.height(26.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.TwoWheeler,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (vehicleFilter == "TRICYCLE") Color.White else AccentAmber
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Motorbikes ($bikeCount)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (vehicleFilter == "TRICYCLE") Color.White else BrandBlueDark
                        )
                    }
                }
            }
        }

        // Floating Map Controls (Zoom & Recenter) on the Right Side
        Column(
            modifier = Modifier
                .zIndex(10f)
                .align(Alignment.CenterEnd)
                .padding(end = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Surface(
                onClick = { zoomLevel = (zoomLevel + 0.3f).coerceAtMost(2.5f) },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Add, contentDescription = "Zoom In", tint = BrandBlueDark, modifier = Modifier.size(16.dp))
                }
            }

            Surface(
                onClick = { zoomLevel = (zoomLevel - 0.3f).coerceAtLeast(0.8f) },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Remove, contentDescription = "Zoom Out", tint = BrandBlueDark, modifier = Modifier.size(16.dp))
                }
            }

            Surface(
                onClick = { zoomLevel = 1.0f },
                shape = CircleShape,
                color = Color.White,
                shadowElevation = 4.dp,
                modifier = Modifier.size(30.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MyLocation, contentDescription = "Recenter", tint = BrandBluePrimary, modifier = Modifier.size(16.dp))
                }
            }
        }

        // Google Maps Watermark Footer (Bottom Left)
        Row(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 12.dp, bottom = if (selectedDriverOnMap != null) 140.dp else 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Google Maps",
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = if (mapType == "SATELLITE") Color.White.copy(alpha = 0.8f) else Color.DarkGray.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "• Map data ©2026 Google",
                fontSize = 8.5.sp,
                color = if (mapType == "SATELLITE") Color.LightGray.copy(alpha = 0.7f) else NeutralGray
            )
        }

        // =========================================================
        // SELECTED DRIVER QUICK CARD SHEET OVERLAY
        // =========================================================
        AnimatedVisibility(
            visible = selectedDriverOnMap != null,
            enter = fadeIn() + androidx.compose.animation.slideInVertically { it },
            exit = fadeOut() + androidx.compose.animation.slideOutVertically { it },
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            selectedDriverOnMap?.let { selectedDriver ->
                val distKm = calculateDistance(passengerLat, passengerLng, selectedDriver.currentLat, selectedDriver.currentLng)
                val etaMin = (distKm * 3.5 + 2).toInt().coerceAtLeast(1)

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                        .testTag("driver_map_card_${selectedDriver.id}"),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f))
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .background(BrandBluePrimary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (selectedDriver.vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                                        contentDescription = null,
                                        tint = BrandBluePrimary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(selectedDriver.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandBlueDark)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Icon(Icons.Default.Verified, contentDescription = "Verified", tint = SuccessGreen, modifier = Modifier.size(14.dp))
                                    }
                                    Text(
                                        text = "${if (selectedDriver.vehicleType == "CAR") "Yellow Taxi Sedan" else "Tricycle / Moto"} • ${selectedDriver.vehiclePlate}",
                                        fontSize = 11.sp,
                                        color = NeutralGray
                                    )
                                }
                            }

                            IconButton(
                                onClick = { selectedDriverOnMap = null },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = NeutralGray, modifier = Modifier.size(16.dp))
                            }
                        }

                        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Star, contentDescription = null, tint = AccentAmber, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${selectedDriver.rating} ★ (120+ trips)", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = BrandBlueDark)
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = BrandBluePrimary.copy(alpha = 0.08f),
                                border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.2f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = BrandBluePrimary, modifier = Modifier.size(12.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${String.format("%.1f", distKm)} km • ~$etaMin min away",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandBluePrimary
                                    )
                                }
                            }
                        }

                        if (onSelectDriver != null) {
                            Button(
                                onClick = {
                                    onSelectDriver.invoke(selectedDriver)
                                    selectedDriverOnMap = null
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(36.dp)
                                    .testTag("request_ride_with_driver_btn"),
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Request Ride with ${selectedDriver.name.split(" ")[0]}", fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple tuple helper
private data class Tuple6<A, B, C, D, E, F>(
    val a: A, val b: B, val c: C, val d: D, val e: E, val f: F
)
