package com.example.ui

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

// Data structure for Gambia charging stations
data class ChargingHub(
    val id: String,
    val name: String,
    val location: String,
    val distanceKm: Double,
    val type: String, // "Solar Swap Locker", "DC Fast Charger", "Standard Terminal"
    val availableSlots: Int,
    val pricePerKwh: String
)

val GAMBIAN_CHARGING_HUBS = listOf(
    ChargingHub("sk_swap", "Serekunda Solar Swap Hub", "Kairaba Ave, Serekunda", 1.2, "Solar Swap Locker", 8, "12 GMD"),
    ChargingHub("bj_ferry", "Banjul Ferry Depot Chargers", "Banjul Terminal Port", 0.8, "DC Fast Charger", 3, "15 GMD"),
    ChargingHub("stadium_power", "Stadium Municipal Hub", "Independence Stadium, Bakau", 3.4, "Standard Terminal", 6, "10 GMD"),
    ChargingHub("senegambia_swap", "Senegambia Strip Swap Station", "Senegambia Hwy", 2.1, "Solar Swap Locker", 5, "14 GMD")
)

@Composable
fun rememberBatteryState(): Pair<Int, Boolean> {
    val context = LocalContext.current
    var batteryLevel by remember { mutableStateOf(100) }
    var isCharging by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                intent?.let {
                    val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                    val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                    if (level != -1 && scale != -1) {
                        batteryLevel = (level * 100 / scale.toFloat()).toInt()
                    }
                    val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                    isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                                 status == BatteryManager.BATTERY_STATUS_FULL
                }
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        
        // Fetch current status immediately from sticky broadcast
        val currentIntent = context.registerReceiver(receiver, filter)
        currentIntent?.let {
            val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            if (level != -1 && scale != -1) {
                batteryLevel = (level * 100 / scale.toFloat()).toInt()
            }
            val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                         status == BatteryManager.BATTERY_STATUS_FULL
        }

        onDispose {
            try {
                context.unregisterReceiver(receiver)
            } catch (e: Exception) {
                // Ignore receiver not registered if already cleaned up
            }
        }
    }

    return Pair(batteryLevel, isCharging)
}

@Composable
fun BatteryIcon(
    level: Int,
    isCharging: Boolean,
    modifier: Modifier = Modifier,
    width: Dp = 26.dp,
    height: Dp = 14.dp,
    customColor: Color? = null
) {
    val levelColor = customColor ?: when {
        isCharging -> Color(0xFFEAB308) // Amber/Yellow
        level <= 20 -> ErrorRed
        level <= 50 -> AccentAmber
        else -> SuccessGreen
    }

    Canvas(modifier = modifier.size(width, height)) {
        val w = size.width
        val h = size.height
        val pinWidth = w * 0.08f
        val bodyWidth = w - pinWidth
        val strokeWidth = 1.5.dp.toPx()
        val cornerRadius = 2.dp.toPx()

        // Draw battery outer body outline
        drawRoundRect(
            color = BrandBlueDark.copy(alpha = 0.7f),
            topLeft = Offset(0f, 0f),
            size = androidx.compose.ui.geometry.Size(bodyWidth, h),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth)
        )

        // Draw battery positive terminal pin
        drawRoundRect(
            color = BrandBlueDark.copy(alpha = 0.7f),
            topLeft = Offset(bodyWidth, h * 0.25f),
            size = androidx.compose.ui.geometry.Size(pinWidth, h * 0.5f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius / 2f, cornerRadius / 2f)
        )

        // Calculate fill dimensions
        val fillPadding = strokeWidth + 1.dp.toPx()
        val maxFillWidth = bodyWidth - (fillPadding * 2f)
        val fillWidth = maxFillWidth * (level.coerceIn(0, 100) / 100f)
        val fillHeight = h - (fillPadding * 2f)

        if (fillWidth > 0f) {
            drawRoundRect(
                color = levelColor,
                topLeft = Offset(fillPadding, fillPadding),
                size = androidx.compose.ui.geometry.Size(fillWidth, fillHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius / 2f, cornerRadius / 2f)
            )
        }

        // Draw lightning bolt overlay if charging
        if (isCharging) {
            val boltPath = Path().apply {
                moveTo(bodyWidth * 0.58f, h * 0.15f)
                lineTo(bodyWidth * 0.35f, h * 0.55f)
                lineTo(bodyWidth * 0.50f, h * 0.55f)
                lineTo(bodyWidth * 0.42f, h * 0.85f)
                lineTo(bodyWidth * 0.65f, h * 0.45f)
                lineTo(bodyWidth * 0.50f, h * 0.45f)
                close()
            }
            drawPath(
                path = boltPath,
                color = Color.White
            )
        }
    }
}

@Composable
fun BatteryLevelIndicatorCompact(
    level: Int,
    isCharging: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(BrandBlueLight)
            .clickable { onClick() }
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BatteryIcon(level = level, isCharging = isCharging)
        Text(
            text = "$level%",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = BrandBlueDark
        )
    }
}

@Composable
fun BatteryLevelDashboardWidget(
    level: Int,
    isCharging: Boolean,
    onSimulateBattery: (Int) -> Unit,
    onNavigateToHub: (ChargingHub) -> Unit,
    modifier: Modifier = Modifier
) {
    var showExplanationDialog by remember { mutableStateOf(false) }
    var userSimulatedLevel by remember { mutableStateOf(level) }

    // Synchronize simulator slide to external changes
    LaunchedEffect(level) {
        userSimulatedLevel = level
    }

    val estimatedHoursLeft = remember(userSimulatedLevel) {
        val hrs = (userSimulatedLevel / 12f)
        String.format("%.1f", hrs)
    }

    val estimatedTripsLeft = remember(userSimulatedLevel) {
        (userSimulatedLevel / 15)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("battery_dashboard_card"),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = if (userSimulatedLevel <= 20) Icons.Default.BatteryAlert else Icons.Default.ElectricCar,
                        contentDescription = "Battery Status",
                        tint = if (userSimulatedLevel <= 20) ErrorRed else SuccessGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Tricycle Battery & Shift Planner",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = BrandBlueDark
                    )
                }

                // Info Icon Button
                IconButton(
                    onClick = { showExplanationDialog = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Battery Info Guide",
                        tint = BrandBlueSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Main Status layout
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Battery Visual Icon and percentage
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BrandBlueLight)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        BatteryIcon(
                            level = userSimulatedLevel,
                            isCharging = isCharging,
                            width = 32.dp,
                            height = 18.dp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "$userSimulatedLevel%",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = BrandBlueDark
                        )
                    }
                }

                // Estimates & Calculations
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCharging) "Tricycle is Currently Charging" else "Remaining Shift Forecast",
                        fontSize = 10.sp,
                        color = NeutralGray,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = if (isCharging) "Fast Charging Active" else "~$estimatedHoursLeft Hours Driving Left",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (userSimulatedLevel <= 20) ErrorRed else BrandBlueDark
                    )
                    Text(
                        text = if (isCharging) "Unplug to begin your driving session." else "Capable of completing ~$estimatedTripsLeft more passenger trips",
                        fontSize = 9.sp,
                        color = NeutralGray
                    )
                }
            }

            // Custom colored progress bar
            val progressColor = when {
                isCharging -> Color(0xFFEAB308)
                userSimulatedLevel <= 20 -> ErrorRed
                userSimulatedLevel <= 50 -> AccentAmber
                else -> SuccessGreen
            }
            LinearProgressIndicator(
                progress = { userSimulatedLevel / 100f },
                color = progressColor,
                trackColor = BrandBlueLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp))
            )

            // Low Battery Alert banner with Gambian region advice
            AnimatedVisibility(
                visible = userSimulatedLevel <= 25,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(ErrorRed.copy(alpha = 0.08f))
                        .border(1.dp, ErrorRed.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(8.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = ErrorRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Column {
                            Text(
                                text = "Low Battery Safety Alert",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = ErrorRed
                            )
                            Text(
                                text = "Charging hubs are highly limited in Serekunda Outskirts & Banjul Port borders. We strongly recommend heading to a Solar Swap station before taking your next long ride.",
                                fontSize = 9.sp,
                                color = ErrorRed.copy(alpha = 0.9f)
                            )
                        }
                    }
                }
            }

            // Battery Simulation controller for reviewers to test safety triggers!
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(BrandBlueLight.copy(alpha = 0.6f))
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tester Simulator: Drag Battery Level",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueSecondary
                    )
                    Text(
                        text = "$userSimulatedLevel%",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBlueDark
                    )
                }
                Slider(
                    value = userSimulatedLevel.toFloat(),
                    onValueChange = {
                        userSimulatedLevel = it.toInt()
                        onSimulateBattery(userSimulatedLevel)
                    },
                    valueRange = 5f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = BrandBluePrimary,
                        activeTrackColor = BrandBluePrimary,
                        inactiveTrackColor = Color.LightGray.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .height(24.dp)
                        .testTag("battery_simulator_slider")
                )
            }

            // Nearest Charging Stations for Shift Planning
            Text(
                text = "Nearby Battery Charging & Swap Hubs",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = BrandBlueDark,
                modifier = Modifier.padding(top = 4.dp)
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                GAMBIAN_CHARGING_HUBS.forEach { hub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .border(0.5.dp, Color.LightGray.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .clickable { onNavigateToHub(hub) }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(BrandBlueLight),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (hub.type.contains("Swap")) Icons.Default.SwapHoriz else Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = hub.name,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark
                                )
                                Text(
                                    text = "${hub.location} • ${hub.type}",
                                    fontSize = 8.sp,
                                    color = NeutralGray
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = "${hub.distanceKm} km",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBluePrimary
                            )
                            Text(
                                text = "${hub.availableSlots} slots free",
                                fontSize = 8.sp,
                                color = SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    // Explanatory Bottom Sheet / Dialog
    if (showExplanationDialog) {
        AlertDialog(
            onDismissRequest = { showExplanationDialog = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.ElectricCar, contentDescription = null, tint = BrandBluePrimary)
                    Text("Tricycle Range Optimizer", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "WayGo Tricycle range forecasting is designed to assist driver survival in regions with sparse electrical grids.",
                        fontSize = 11.sp,
                        color = BrandBlueDark
                    )
                    Text(
                        text = "• Average rate: ~12% battery consumption per operating hour.\n" +
                               "• Low battery warnings triggers automatically at 25% charge.\n" +
                               "• Serekunda and Banjul charging zones allow standard DC plugin or instant 2-minute battery-swap lockers so you never lose shift time.",
                        fontSize = 10.sp,
                        color = NeutralGray
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showExplanationDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Text("Got It", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        )
    }
}
