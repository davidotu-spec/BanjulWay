package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
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
import com.example.ui.theme.*
import kotlin.math.sqrt

// Geographical bounds matching the actual Map boundaries for WayGo
private const val MIN_LAT = 13.4300
private const val MAX_LAT = 13.4800
private const val MIN_LNG = -16.7200
private const val MAX_LNG = -16.5600

data class Hotspot(
    val id: String,
    val name: String,
    val region: String, // "Banjul" or "Kanifing"
    val lat: Double,
    val lng: Double,
    val demandLevel: String, // "CRITICAL", "HIGH", "MODERATE"
    val bookingsPerHour: Int,
    val surgeMultiplier: Double,
    val estimatedWaitMinutes: Int,
    val activeTricycles: Int
)

val GAMBIAN_HOTSPOTS = listOf(
    Hotspot("albert_market", "Albert Market", "Banjul", 13.4533, -16.5746, "CRITICAL", 94, 1.45, 2, 6),
    Hotspot("arch_22", "Arch 22 Landmark", "Banjul", 13.4580, -16.5820, "HIGH", 54, 1.25, 4, 10),
    Hotspot("banjul_port", "Banjul Ferry Port", "Banjul", 13.4510, -16.5680, "CRITICAL", 88, 1.40, 3, 4),
    
    Hotspot("serekunda_market", "Serekunda Market", "Kanifing", 13.4382, -16.6780, "CRITICAL", 115, 1.55, 1, 14),
    Hotspot("senegambia", "Senegambia Strip", "Kanifing", 13.4420, -16.7110, "HIGH", 82, 1.30, 3, 9),
    Hotspot("kairaba_ave", "Kairaba Avenue Hub", "Kanifing", 13.4471, -16.6791, "HIGH", 68, 1.20, 4, 12),
    Hotspot("stadium", "Independence Stadium", "Kanifing", 13.4722, -16.6690, "MODERATE", 35, 1.05, 6, 8)
)

@OptIn(ExperimentalTextApi::class)
@Composable
fun HotspotMapPreview(
    modifier: Modifier = Modifier,
    onNavigateToHotspot: (Hotspot) -> Unit = {}
) {
    var selectedRegion by remember { mutableStateOf("ALL") } // "ALL", "Banjul", "Kanifing"
    val filteredHotspots = remember(selectedRegion) {
        if (selectedRegion == "ALL") GAMBIAN_HOTSPOTS
        else GAMBIAN_HOTSPOTS.filter { it.region.equals(selectedRegion, ignoreCase = true) }
    }
    
    var selectedHotspot by remember { mutableStateOf<Hotspot?>(GAMBIAN_HOTSPOTS.firstOrNull { it.id == "serekunda_market" }) }
    
    // Animate the pulse rate for hotspots
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_transition")
    val pulseRatio by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_ratio"
    )
    
    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    
    // Color schemes for visualization
    val oceanColor = Color(0xFFD3E2EE)
    val landColor = Color(0xFFE8EDF2)
    val roadColor = Color(0xFFCBD5E1)
    val roadFillColor = Color(0xFFFFFFFF)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(PureWhite, RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Whatshot,
                        contentDescription = "Hotspot",
                        tint = ErrorRed,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Demand Hotspot Map",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandBlueDark
                    )
                }
                Text(
                    text = "High booking velocity & surge tracking",
                    fontSize = 10.sp,
                    color = NeutralGray
                )
            }
            
            // Live Status indicator
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(SuccessGreen.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(SuccessGreen)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("LIVE", color = SuccessGreen, fontSize = 8.sp, fontWeight = FontWeight.Bold)
            }
        }

        // Region Filter Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("ALL", "Banjul", "Kanifing").forEach { region ->
                val isSelected = selectedRegion == region
                val filterBg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isSelected) BrandBluePrimary else BrandBlueLight,
                    label = "filterBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(filterBg)
                        .clickable { selectedRegion = region }
                        .padding(vertical = 6.dp)
                        .testTag("filter_btn_$region"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (region == "ALL") "All Regions" else region,
                        color = if (isSelected) Color.White else NeutralGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }
        }

        // Map Canvas Box with Coordinate Tap Detection
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(12.dp))
                .background(landColor)
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            // Coordinate mapping helper inside this layout scope
            fun getX(lng: Double): Float {
                val ratio = (lng - MIN_LNG) / (MAX_LNG - MIN_LNG)
                return (ratio * widthPx).toFloat()
            }

            fun getY(lat: Double): Float {
                val ratio = (MAX_LAT - lat) / (MAX_LAT - MIN_LAT)
                return (ratio * heightPx).toFloat()
            }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("hotspot_map_canvas")
                    .pointerInput(filteredHotspots) {
                        detectTapGestures { offset ->
                            // Find the closest hotspot to click coordinate within 30dp radius
                            var closest: Hotspot? = null
                            var minDist = Float.MAX_VALUE
                            val clickRadius = 30.dp.toPx()

                            filteredHotspots.forEach { hotspot ->
                                val hX = getX(hotspot.lng)
                                val hY = getY(hotspot.lat)
                                val dist = sqrt((offset.x - hX) * (offset.x - hX) + (offset.y - hY) * (offset.y - hY))
                                if (dist < clickRadius && dist < minDist) {
                                    minDist = dist
                                    closest = hotspot
                                }
                            }

                            if (closest != null) {
                                selectedHotspot = closest
                            }
                        }
                    }
            ) {
                // 1. Draw Map Backplane & Grid
                drawRect(color = landColor)

                val gridSize = 32.dp.toPx()
                var curX = 0f
                while (curX < widthPx) {
                    drawLine(
                        color = Color(0xFF94A3B8).copy(alpha = 0.1f),
                        start = Offset(curX, 0f),
                        end = Offset(curX, heightPx),
                        strokeWidth = 1f
                    )
                    curX += gridSize
                }
                var curY = 0f
                while (curY < heightPx) {
                    drawLine(
                        color = Color(0xFF94A3B8).copy(alpha = 0.1f),
                        start = Offset(0f, curY),
                        end = Offset(widthPx, curY),
                        strokeWidth = 1f
                    )
                    curY += gridSize
                }

                // 2. Draw Atlantic Ocean (top/left)
                val waterPath = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(widthPx, 0f)
                    quadraticTo(widthPx * 0.7f, heightPx * 0.3f, widthPx * 0.5f, heightPx * 0.15f)
                    quadraticTo(widthPx * 0.2f, heightPx * 0.4f, 0f, heightPx * 0.25f)
                    close()
                }
                drawPath(waterPath, color = oceanColor)

                // 3. Draw River Delta (right side Banjul peninsula)
                val riverPath = Path().apply {
                    moveTo(widthPx, heightPx * 0.3f)
                    quadraticTo(widthPx * 0.85f, heightPx * 0.45f, widthPx * 0.9f, heightPx * 0.7f)
                    quadraticTo(widthPx * 0.8f, heightPx * 0.85f, widthPx * 0.85f, heightPx)
                    lineTo(widthPx, heightPx)
                    close()
                }
                drawPath(riverPath, color = oceanColor)

                // 4. Draw Primary Highway (Banjul-Serekunda Highway)
                val startH = Offset(getX(-16.7110), getY(13.4380))
                val midH = Offset(getX(-16.6500), getY(13.4490))
                val endH = Offset(getX(-16.5820), getY(13.4580))
                drawLine(roadColor, startH, midH, strokeWidth = 6f)
                drawLine(roadColor, midH, endH, strokeWidth = 6f)
                drawLine(roadFillColor, startH, midH, strokeWidth = 3f)
                drawLine(roadFillColor, midH, endH, strokeWidth = 3f)

                // Kairaba Avenue
                val startK = Offset(getX(-16.6820), getY(13.4310))
                val endK = Offset(getX(-16.6710), getY(13.4750))
                drawLine(roadColor, startK, endK, strokeWidth = 5f)
                drawLine(roadFillColor, startK, endK, strokeWidth = 2f)

                // 5. Draw Hotspot Glowing Heat Rings & Central Dots
                filteredHotspots.forEach { hotspot ->
                    val hX = getX(hotspot.lng)
                    val hY = getY(hotspot.lat)
                    val isCurrent = selectedHotspot?.id == hotspot.id

                    val baseColor = when (hotspot.demandLevel) {
                        "CRITICAL" -> ErrorRed
                        "HIGH" -> AccentAmber
                        else -> SuccessGreen
                    }

                    // Heat Ring Pulses
                    val ringRadiusMultiplier = if (isCurrent) 1.5f else 1.0f
                    val baseRadius = when (hotspot.demandLevel) {
                        "CRITICAL" -> 22.dp.toPx()
                        "HIGH" -> 16.dp.toPx()
                        else -> 12.dp.toPx()
                    }

                    // Draw animated heat-map halo
                    drawCircle(
                        color = baseColor.copy(alpha = 0.15f / (if (isCurrent) 1f else 1.5f)),
                        radius = baseRadius * pulseRatio * ringRadiusMultiplier,
                        center = Offset(hX, hY)
                    )

                    // Secondary ring for critical hotspots
                    if (hotspot.demandLevel == "CRITICAL") {
                        drawCircle(
                            color = baseColor.copy(alpha = 0.08f),
                            radius = baseRadius * (pulseRatio + 0.3f) * ringRadiusMultiplier,
                            center = Offset(hX, hY)
                        )
                    }

                    // Inner indicator circle
                    drawCircle(
                        color = if (isCurrent) PureWhite else baseColor.copy(alpha = 0.4f),
                        radius = if (isCurrent) 9.dp.toPx() else 6.dp.toPx(),
                        center = Offset(hX, hY)
                    )
                    
                    drawCircle(
                        color = baseColor,
                        radius = if (isCurrent) 5.dp.toPx() else 4.dp.toPx(),
                        center = Offset(hX, hY)
                    )

                    // Draw a mini surge indicator badge like "1.5x" directly on the canvas above selected hotspot
                    if (isCurrent && hotspot.surgeMultiplier > 1.0) {
                        val textStyle = TextStyle(
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        val badgeText = "${hotspot.surgeMultiplier}x"
                        val textLayout = textMeasurer.measure(badgeText, style = textStyle)
                        val textWidth = textLayout.size.width.toFloat()
                        val textHeight = textLayout.size.height.toFloat()
                        
                        // Draw badge backing box
                        val rectWidth = textWidth + 12f
                        val rectHeight = textHeight + 6f
                        val rx = hX - rectWidth / 2f
                        val ry = hY - 24.dp.toPx() - rectHeight / 2f
                        
                        drawRoundRect(
                            color = baseColor,
                            topLeft = Offset(rx, ry),
                            size = androidx.compose.ui.geometry.Size(rectWidth, rectHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(4.dp.toPx())
                        )
                        
                        drawText(
                            textLayoutResult = textLayout,
                            topLeft = Offset(rx + 6f, ry + 3f)
                        )
                    }
                }
            }

            // Floating Helper tooltip info
            Text(
                text = "Tap any glowing region to view surge & wait time",
                fontSize = 8.sp,
                fontWeight = FontWeight.Medium,
                color = BrandBlueDark.copy(alpha = 0.7f),
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
                    .background(PureWhite.copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .border(0.5.dp, Color.LightGray.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 4.dp, vertical = 2.dp)
            )
        }

        // Horizontal Quick-Click Hotspot List
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(filteredHotspots) { hotspot ->
                val isSelected = selectedHotspot?.id == hotspot.id
                val indicatorColor = when (hotspot.demandLevel) {
                    "CRITICAL" -> ErrorRed
                    "HIGH" -> AccentAmber
                    else -> SuccessGreen
                }
                
                Card(
                    onClick = { selectedHotspot = hotspot },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) BrandBlueLight else PureWhite
                    ),
                    modifier = Modifier.testTag("hotspot_item_${hotspot.id}"),
                    border = androidx.compose.foundation.BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) BrandBluePrimary else Color.LightGray.copy(alpha = 0.3f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(indicatorColor)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = hotspot.name.replace(" Hub", "").replace(" Landmark", ""),
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) BrandBluePrimary else BrandBlueDark
                        )
                    }
                }
            }
        }

        // Interactive Selected Hotspot Analytics & Action Card
        selectedHotspot?.let { hotspot ->
            Card(
                modifier = Modifier.fillMaxWidth().testTag("hotspot_detail_card"),
                colors = CardDefaults.cardColors(containerColor = BrandBlueLight.copy(alpha = 0.4f)),
                border = androidx.compose.foundation.BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.1f))
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Hotspot details row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = hotspot.name,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp,
                                color = BrandBlueDark
                            )
                            Text(
                                text = "${hotspot.region} Area • ${hotspot.activeTricycles} active tricycles nearby",
                                fontSize = 9.sp,
                                color = NeutralGray
                            )
                        }
                        
                        // Surge multiplier pill
                        if (hotspot.surgeMultiplier > 1.0) {
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ErrorRed.copy(alpha = 0.12f))
                                    .padding(horizontal = 6.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = "Surge",
                                    tint = ErrorRed,
                                    modifier = Modifier.size(10.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${hotspot.surgeMultiplier}x Surge",
                                    color = ErrorRed,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Black
                                )
                            }
                        } else {
                            Text(
                                text = "Standard Rates",
                                color = SuccessGreen,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))

                    // Hotspot stats grid
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Stat 1: Demand level
                        Column {
                            Text("Demand Velocity", fontSize = 8.sp, color = NeutralGray)
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Whatshot,
                                    contentDescription = null,
                                    tint = when (hotspot.demandLevel) {
                                        "CRITICAL" -> ErrorRed
                                        "HIGH" -> AccentAmber
                                        else -> SuccessGreen
                                    },
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(2.dp))
                                Text(
                                    text = hotspot.demandLevel,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    color = when (hotspot.demandLevel) {
                                        "CRITICAL" -> ErrorRed
                                        "HIGH" -> AccentAmber
                                        else -> SuccessGreen
                                    }
                                )
                            }
                        }

                        // Stat 2: Bookings per hour
                        Column {
                            Text("Est. Requests", fontSize = 8.sp, color = NeutralGray)
                            Text(
                                text = "${hotspot.bookingsPerHour} / hr",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = BrandBlueDark
                            )
                        }

                        // Stat 3: Est wait time
                        Column {
                            Text("Avg. Wait Time", fontSize = 8.sp, color = NeutralGray)
                            Text(
                                text = "${hotspot.estimatedWaitMinutes} min",
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = BrandBlueDark
                            )
                        }
                    }

                    // Navigation Action button
                    Button(
                        onClick = { onNavigateToHotspot(hotspot) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(32.dp)
                            .testTag("hotspot_nav_btn"),
                        contentPadding = PaddingValues(vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Navigation,
                            contentDescription = "Navigate",
                            tint = Color.White,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Simulate GPS Navigation Here", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
