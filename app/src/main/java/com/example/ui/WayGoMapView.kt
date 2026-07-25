package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.DriverEntity
import com.example.data.TripEntity
import com.example.ui.theme.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.semantics.semantics

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
    progress: Float = 0f
) {
    val textMeasurer = rememberTextMeasurer()
    val oceanColor = Color(0xFFD3E2EE)
    val landColor = Color(0xFFE8EDF2)    // Clean Minimalism slate-grey map backdrop
    val roadColor = Color(0xFFCBD5E1)    // Slate-300 clean road borders
    val roadFillColor = Color(0xFFFFFFFF)

    val isActiveRide = activeTrip != null && activeTrip.status in listOf("ACCEPTED", "ARRIVED", "EN_ROUTE")
    val boxHeight = if (isActiveRide) 320.dp else 260.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(boxHeight)
            .clip(RoundedCornerShape(24.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(24.dp))
            .background(landColor)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height

            // Coordinate mapping functions
            fun getX(lng: Double): Float {
                val ratio = (lng - MIN_LNG) / (MAX_LNG - MIN_LNG)
                return (ratio * width).toFloat()
            }

            fun getY(lat: Double): Float {
                // Inverted because Y goes down
                val ratio = (MAX_LAT - lat) / (MAX_LAT - MIN_LAT)
                return (ratio * height).toFloat()
            }

            // 1. Draw land mass backplane
            drawRect(color = landColor)

            // Draw Clean Minimalism grid overlay to replicate the tailwind style exactly
            val gridSize = 40.dp.toPx()
            var curX = 0f
            while (curX < width) {
                drawLine(
                    color = Color(0xFF94A3B8).copy(alpha = 0.15f),
                    start = Offset(curX, 0f),
                    end = Offset(curX, height),
                    strokeWidth = 1f
                )
                curX += gridSize
            }
            var curY = 0f
            while (curY < height) {
                drawLine(
                    color = Color(0xFF94A3B8).copy(alpha = 0.15f),
                    start = Offset(0f, curY),
                    end = Offset(width, curY),
                    strokeWidth = 1f
                )
                curY += gridSize
            }

            // Draw Atlantic Ocean along the very top and left
            val waterPath = Path().apply {
                moveTo(0f, 0f)
                lineTo(width, 0f)
                quadraticTo(width * 0.7f, height * 0.3f, width * 0.5f, height * 0.15f)
                quadraticTo(width * 0.2f, height * 0.4f, 0f, height * 0.25f)
                close()
            }
            drawPath(waterPath, color = oceanColor)

            // Draw Banjul Peninsular River delta on the right
            val riverPath = Path().apply {
                moveTo(width, height * 0.3f)
                quadraticTo(width * 0.85f, height * 0.45f, width * 0.9f, height * 0.7f)
                quadraticTo(width * 0.8f, height * 0.85f, width * 0.85f, height)
                lineTo(width, height)
                close()
            }
            drawPath(riverPath, color = oceanColor)

            // 2. Draw Main Gambian Highways/Roads
            // Banjul-Serekunda Highway
            val highwayStart = Offset(getX(-16.7110), getY(13.4380))
            val highwayMid = Offset(getX(-16.6500), getY(13.4490))
            val highwayBanjul = Offset(getX(-16.5820), getY(13.4580))
            
            // Draw road outline
            drawLine(roadColor, highwayStart, highwayMid, strokeWidth = 8f)
            drawLine(roadColor, highwayMid, highwayBanjul, strokeWidth = 8f)
            // Draw road inner fill
            drawLine(roadFillColor, highwayStart, highwayMid, strokeWidth = 4f)
            drawLine(roadFillColor, highwayMid, highwayBanjul, strokeWidth = 4f)

            // Kairaba Avenue (Serrekunda)
            val kairabaStart = Offset(getX(-16.6820), getY(13.4310))
            val kairabaEnd = Offset(getX(-16.6710), getY(13.4750))
            drawLine(roadColor, kairabaStart, kairabaEnd, strokeWidth = 7f)
            drawLine(roadFillColor, kairabaStart, kairabaEnd, strokeWidth = 3f.coerceAtLeast(1f))

            // Atlantic Boulevard (beach road)
            val beachRoadStart = Offset(getX(-16.7110), getY(13.4420))
            val beachRoadEnd = Offset(getX(-16.6800), getY(13.4750))
            drawLine(roadColor, beachRoadStart, beachRoadEnd, strokeWidth = 6f)
            drawLine(Color.White, beachRoadStart, beachRoadEnd, strokeWidth = 2.5f)

            // 3. Draw Landmarks
            GAMBIAN_LANDMARKS.forEach { landmark ->
                val x = getX(landmark.lng)
                val y = getY(landmark.lat)

                // Landmark Dot
                drawCircle(
                    color = BrandBlueDark.copy(alpha = 0.5f),
                    radius = 4.5f.dp.toPx(),
                    center = Offset(x, y)
                )
                drawCircle(
                    color = BrandBlueSecondary,
                    radius = 2.5f.dp.toPx(),
                    center = Offset(x, y)
                )

                // Text labels
                val textStyle = TextStyle(
                    fontSize = 8.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlueDark.copy(alpha = 0.8f)
                )
                val textLayoutResult = textMeasurer.measure(
                    text = landmark.name,
                    style = textStyle
                )
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(x - textLayoutResult.size.width / 2f, y + 4.dp.toPx())
                )
            }

            // 4. Draw Active Trip Path (if booking exists)
            activeTrip?.let { trip ->
                val pX = getX(trip.pickupLng)
                val pY = getY(trip.pickupLat)
                val dX = getX(trip.dropoffLng)
                val dY = getY(trip.dropoffLat)

                // Draw pickup location marker
                drawCircle(
                    color = SuccessGreen.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = Offset(pX, pY)
                )
                drawCircle(
                    color = SuccessGreen,
                    radius = 5.dp.toPx(),
                    center = Offset(pX, pY)
                )

                // Draw dropoff/destination marker
                drawCircle(
                    color = ErrorRed.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = Offset(dX, dY)
                )
                drawCircle(
                    color = ErrorRed,
                    radius = 5.dp.toPx(),
                    center = Offset(dX, dY)
                )

                // Draw route line (dashed style)
                drawLine(
                    color = BrandBluePrimary,
                    start = Offset(pX, pY),
                    end = Offset(dX, dY),
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(12f, 12f), 0f
                    )
                )
            }

            // Draw Driver Approaching Pickup Path (ACCEPTED state visual routing line)
            if (activeTrip != null && activeTrip.status == "ACCEPTED") {
                val dLat = simulatedDriverLat ?: (activeTrip.pickupLat + 0.012)
                val dLng = simulatedDriverLng ?: (activeTrip.pickupLng - 0.009)
                val dX = getX(dLng)
                val dY = getY(dLat)
                val pX = getX(activeTrip.pickupLng)
                val pY = getY(activeTrip.pickupLat)

                drawLine(
                    color = AccentAmber,
                    start = Offset(dX, dY),
                    end = Offset(pX, pY),
                    strokeWidth = 3.dp.toPx(),
                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                        floatArrayOf(12f, 12f), 0f
                    )
                )
            }

            // 5. Draw Idle Online Drivers
            drivers.forEach { driver ->
                if (driver.isOnline && driver.approvalStatus == "APPROVED" && (activeTrip == null || activeTrip.driverId != driver.id)) {
                    val dX = getX(driver.currentLng)
                    val dY = getY(driver.currentLat)
                    val color = if (driver.vehicleType == "CAR") BrandBluePrimary else AccentAmber

                    // Glowing ring
                    drawCircle(
                        color = color.copy(alpha = 0.25f),
                        radius = 8.dp.toPx(),
                        center = Offset(dX, dY)
                    )
                    // Inner core
                    drawCircle(
                        color = color,
                        radius = 4.dp.toPx(),
                        center = Offset(dX, dY)
                    )
                }
            }

            // 6. Draw Active Driving Vehicle (or passenger location)
            if (activeTrip != null && activeTrip.status in listOf("ACCEPTED", "ARRIVED", "EN_ROUTE")) {
                val driverLat = simulatedDriverLat ?: activeTrip.pickupLat
                val driverLng = simulatedDriverLng ?: activeTrip.pickupLng
                val dX = getX(driverLng)
                val dY = getY(driverLat)
                val color = if (activeTrip.vehicleType == "CAR") BrandBluePrimary else AccentAmber

                // Animate bigger glowing circle
                drawCircle(
                    color = color.copy(alpha = 0.35f),
                    radius = 14.dp.toPx(),
                    center = Offset(dX, dY)
                )
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(dX, dY)
                )
                drawCircle(
                    color = color,
                    radius = 6.dp.toPx(),
                    center = Offset(dX, dY)
                )
            } else {
                // Just draw current Passenger dot resting at home/default
                val pX = getX(passengerLng)
                val pY = getY(passengerLat)
                drawCircle(
                    color = BrandBlueSecondary.copy(alpha = 0.3f),
                    radius = 12.dp.toPx(),
                    center = Offset(pX, pY)
                )
                drawCircle(
                    color = BrandBlueSecondary,
                    radius = 5.dp.toPx(),
                    center = Offset(pX, pY)
                )
            }
        }

        // Top right indicator
        Row(
            modifier = Modifier
                .padding(8.dp)
                .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(8.dp))
                .border(0.5.dp, BrandBluePrimary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "info",
                tint = BrandBlueDark,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "Live WayGo Map",
                color = BrandBlueDark,
                fontWeight = FontWeight.Bold,
                fontSize = 9.sp
            )
        }

        // Real-time progress visualizer HUD Overlay
        if (isActiveRide) {
            val trip = activeTrip!!
            val driverLat = simulatedDriverLat ?: trip.pickupLat
            val driverLng = simulatedDriverLng ?: trip.pickupLng

            val distanceToPickup = calculateDistance(driverLat, driverLng, trip.pickupLat, trip.pickupLng)
            val distanceToDropoff = calculateDistance(driverLat, driverLng, trip.dropoffLat, trip.dropoffLng)

            val (displayDist, statusLabel) = if (trip.status == "ACCEPTED") {
                Pair(distanceToPickup, "Driver approaching pickup")
            } else if (trip.status == "ARRIVED") {
                Pair(0.0, "Driver at your pickup location")
            } else {
                Pair(distanceToDropoff, "In transit to destination")
            }

            val trackProgress = when (trip.status) {
                "ACCEPTED" -> (progress / 0.35f).coerceIn(0f, 1f)
                "ARRIVED" -> 1.0f
                "EN_ROUTE" -> ((progress - 0.38f) / 0.62f).coerceIn(0f, 1f)
                else -> 1.0f
            }

            Card(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(10.dp)
                    .fillMaxWidth()
                    .semantics { testTag = "realtime_progress_hud" },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.95f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.15f))
            ) {
                Column(
                    modifier = Modifier
                        .padding(12.dp)
                        .fillMaxWidth()
                ) {
                    // HUD Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val infiniteTransition = rememberInfiniteTransition(label = "pulse")
                            val pulseAlpha by infiniteTransition.animateFloat(
                                initialValue = 0.4f,
                                targetValue = 1.0f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(800, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "pulse_alpha"
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(SuccessGreen.copy(alpha = pulseAlpha))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = statusLabel,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                color = BrandBlueDark
                            )
                        }

                        Text(
                            text = if (trip.status == "ARRIVED") "Ready" else String.format("%.2f km left", displayDist),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandBluePrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Progress Track Bar
                    BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val maxWidthPx = constraints.maxWidth
                        val density = LocalDensity.current
                        val maxWidthDp = with(density) { maxWidthPx.toDp() }

                        // Background line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(BrandBlueLight)
                        )

                        // Filled active progress line
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(trackProgress)
                                .height(4.dp)
                                .background(BrandBluePrimary)
                                .align(Alignment.CenterStart)
                        )

                        // End node circles
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .align(Alignment.Center)
                        ) {
                            // Start Node Icon
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, BrandBlueSecondary, CircleShape)
                                    .align(Alignment.CenterStart),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (trip.status == "ACCEPTED") Icons.Default.MyLocation else Icons.Default.Place,
                                    contentDescription = "start_node",
                                    tint = BrandBlueSecondary,
                                    modifier = Modifier.size(10.dp)
                                )
                            }

                            // End Node Icon
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color.White)
                                    .border(1.5.dp, if (trip.status == "ACCEPTED") SuccessGreen else ErrorRed, CircleShape)
                                    .align(Alignment.CenterEnd),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (trip.status == "ACCEPTED") Icons.Default.CheckCircle else Icons.Default.Place,
                                    contentDescription = "end_node",
                                    tint = if (trip.status == "ACCEPTED") SuccessGreen else ErrorRed,
                                    modifier = Modifier.size(10.dp)
                                )
                            }
                        }

                        // Sliding Active Driver Vehicle Icon
                        val iconSize = 22.dp
                        val maxOffset = maxWidthDp - iconSize
                        val currentOffset = maxOffset * trackProgress

                        Box(
                            modifier = Modifier
                                .offset(x = currentOffset)
                                .size(iconSize)
                                .clip(CircleShape)
                                .background(BrandBluePrimary)
                                .border(1.dp, Color.White, CircleShape)
                                .align(Alignment.CenterStart),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.DirectionsCar,
                                contentDescription = "Driver Sliding Indicator",
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    // Labels below progress track
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (trip.status == "ACCEPTED") "Driver Dispatch" else "Pickup Location",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeutralGray
                        )
                        Text(
                            text = if (trip.status == "ACCEPTED") "Your Location" else "Destination",
                            fontSize = 8.5.sp,
                            fontWeight = FontWeight.Medium,
                            color = NeutralGray
                        )
                    }
                }
            }
        }
    }
}
