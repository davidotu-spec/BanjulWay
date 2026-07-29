package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

data class HourlyEarningData(
    val hourLabel: String, // e.g. "08:00", "10:00", "12:00", "14:00", "16:00", "18:00"
    val earningsGmd: Int,
    val isTopHour: Boolean = false
)

data class DailyPerformanceSummary(
    val driverId: String,
    val driverName: String,
    val totalHoursDriven: Double,
    val totalTripsCompleted: Int,
    val totalEarningsGmd: Int,
    val topEarningHours: String,
    val totalTipsGmd: Int,
    val acceptanceRatePercent: Int,
    val averageRating: Float,
    val hourlyBreakdown: List<HourlyEarningData>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailyPerformanceSummaryModal(
    summary: DailyPerformanceSummary,
    onDismiss: () -> Unit,
    isDark: Boolean = false
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val cardBg = if (isDark) Color(0xFF1E293B) else PureWhite
    val textColor = if (isDark) PureWhite else BrandBlueDark
    val subtitleColor = if (isDark) Color.LightGray else NeutralGray
    val borderCol = if (isDark) Color.White.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.4f)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 600.dp)
                .padding(vertical = 16.dp)
                .clip(RoundedCornerShape(28.dp))
                .border(1.dp, borderCol, RoundedCornerShape(28.dp))
                .testTag("daily_performance_summary_modal"),
            color = cardBg,
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
            ) {
                // Top Hero Gradient Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BrandBluePrimary,
                                    BrandBlueDark
                                )
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Drag Indicator
                        Box(
                            modifier = Modifier
                                .width(40.dp)
                                .height(4.dp)
                                .clip(CircleShape)
                                .background(PureWhite.copy(alpha = 0.4f))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Trophy Icon Badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(AccentAmber.copy(alpha = 0.2f))
                                .border(2.dp, AccentAmber, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = "Trophy Badge",
                                tint = AccentAmber,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Text(
                            text = "Shift Completed!",
                            fontWeight = FontWeight.Black,
                            fontSize = 22.sp,
                            color = PureWhite
                        )

                        Text(
                            text = "Daily Performance Summary • ${summary.driverName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = PureWhite.copy(alpha = 0.85f),
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Status Pill Badge
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(SuccessGreen.copy(alpha = 0.25f))
                                .padding(horizontal = 12.dp, vertical = 4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Ready to Rest & Offline",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SuccessGreen
                                )
                            }
                        }
                    }
                }

                // Main Stats Content Container
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "CORE SHIFT METRICS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = subtitleColor,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // 2x2 Grid of Key Metrics
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat 1: Total Hours Driven
                        MetricTile(
                            title = "Hours Driven",
                            value = String.format("%.1f hrs", summary.totalHoursDriven),
                            subtext = "Active on shift",
                            icon = Icons.Default.AccessTime,
                            iconTint = BrandBlueSecondary,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_hours_driven")
                        )

                        // Stat 2: Trips Completed
                        MetricTile(
                            title = "Trips Completed",
                            value = "${summary.totalTripsCompleted}",
                            subtext = "Passsengers served",
                            icon = Icons.Default.DirectionsCar,
                            iconTint = SuccessGreen,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_trips_completed")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Stat 3: Total Earnings
                        MetricTile(
                            title = "Total Revenue",
                            value = "GMD ${summary.totalEarningsGmd}",
                            subtext = "Incl. GMD ${summary.totalTipsGmd} tips",
                            icon = Icons.Default.Payments,
                            iconTint = SuccessGreen,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_total_revenue")
                        )

                        // Stat 4: Top-Earning Hours
                        MetricTile(
                            title = "Top-Earning Hours",
                            value = summary.topEarningHours,
                            subtext = "Peak revenue window",
                            icon = Icons.Default.TrendingUp,
                            iconTint = AccentAmber,
                            isDark = isDark,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("stat_top_earning_hours")
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Top Earning Window Highlight Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("top_earning_window_banner"),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isDark) Color(0xFF1E293B) else BrandBlueLight
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandBlueSecondary.copy(alpha = 0.2f))
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(AccentAmber.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = AccentAmber,
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Peak Earning Window",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = subtitleColor
                                )
                                Text(
                                    text = summary.topEarningHours,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor
                                )
                                Text(
                                    text = "Highest passenger demand window recorded during your shift.",
                                    fontSize = 10.5.sp,
                                    color = subtitleColor,
                                    modifier = Modifier.padding(top = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Hourly Earning Bar Graph
                    Text(
                        text = "SHIFT EARNINGS DISTRIBUTION",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = subtitleColor,
                        letterSpacing = 0.8.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    HourlyEarningsBarChart(
                        hourlyData = summary.hourlyBreakdown,
                        isDark = isDark,
                        modifier = Modifier.fillMaxWidth().height(120.dp)
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    // Additional Accomplishments Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Rating Score
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = AccentAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = String.format("%.1f ★ Average", summary.averageRating),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "Shift rating",
                                    fontSize = 10.sp,
                                    color = subtitleColor
                                )
                            }
                        }

                        // Acceptance Rate
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "${summary.acceptanceRatePercent}% Acceptance",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = "Ride response score",
                                    fontSize = 10.sp,
                                    color = subtitleColor
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Action Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                android.widget.Toast.makeText(
                                    context,
                                    "Shift summary exported to driver records!",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("btn_export_summary"),
                            shape = RoundedCornerShape(14.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, borderCol)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = "Export",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Export", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onDismiss,
                            modifier = Modifier
                                .weight(1.5f)
                                .height(50.dp)
                                .testTag("btn_close_and_rest"),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                        ) {
                            Text("Close & Rest", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = PureWhite)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun MetricTile(
    title: String,
    value: String,
    subtext: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val tileBg = if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
    val textColor = if (isDark) PureWhite else BrandBlueDark
    val subtitleColor = if (isDark) Color.LightGray else NeutralGray

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = tileBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color.White.copy(alpha = 0.08f) else Color.LightGray.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = subtitleColor
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = textColor
            )

            Text(
                text = subtext,
                fontSize = 9.5.sp,
                color = subtitleColor,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@Composable
fun HourlyEarningsBarChart(
    hourlyData: List<HourlyEarningData>,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    if (hourlyData.isEmpty()) return

    val maxEarning = (hourlyData.maxOfOrNull { it.earningsGmd } ?: 1).coerceAtLeast(100)
    val gridColor = if (isDark) Color.White.copy(alpha = 0.1f) else Color.LightGray.copy(alpha = 0.3f)
    val textColor = if (isDark) Color.LightGray else NeutralGray

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            hourlyData.forEach { item ->
                val barFraction = (item.earningsGmd.toFloat() / maxEarning).coerceIn(0.1f, 1f)
                val barColor = if (item.isTopHour) AccentAmber else BrandBluePrimary

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Bottom,
                    modifier = Modifier.weight(1f)
                ) {
                    if (item.isTopHour) {
                        Text(
                            text = "Peak",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Black,
                            color = AccentAmber
                        )
                    }

                    Box(
                        modifier = Modifier
                            .width(22.dp)
                            .fillMaxHeight(barFraction)
                            .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        barColor,
                                        barColor.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Hours Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            hourlyData.forEach { item ->
                Text(
                    text = item.hourLabel,
                    fontSize = 9.sp,
                    fontWeight = if (item.isTopHour) FontWeight.Bold else FontWeight.Normal,
                    color = if (item.isTopHour) AccentAmber else textColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
