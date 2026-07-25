package com.example.ui

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.TrendingUp
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
import com.example.ui.theme.*
import kotlinx.coroutines.delay

data class DemandSurgeEvent(
    val id: String,
    val zoneName: String,
    val region: String,
    val surgeMultiplier: Double,
    val description: String,
    val hotspotName: String,
    val lat: Double,
    val lng: Double,
    val potentialEarningsGmd: Int,
    val totalDurationSeconds: Int = 30
)

@Composable
fun DemandSurgeNotificationCard(
    event: DemandSurgeEvent,
    isDark: Boolean,
    onNavigate: (DemandSurgeEvent) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var timeLeft by remember(event.id) { mutableStateOf(event.totalDurationSeconds) }

    LaunchedEffect(event.id) {
        timeLeft = event.totalDurationSeconds
        while (timeLeft > 0) {
            delay(1000L)
            timeLeft--
        }
        onDismiss()
    }

    // Pulsing alpha transition for the visual alert tag
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_notif")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    val cardBg = if (isDark) Color(0xFF1E1B4B) else Color(0xFFEEF2FF) // Deep indigo vs light indigo
    val borderCol = if (isDark) Color(0xFF4338CA) else Color(0xFFC7D2FE)
    val textPrimary = if (isDark) PureWhite else Color(0xFF1E1B4B)
    val textSecondary = if (isDark) Color(0xFFC7D2FE) else Color(0xFF4F46E5)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("demand_surge_notification_card_${event.id}"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, borderCol)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Surge Status & Close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Pulsing alert dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(ErrorRed.copy(alpha = pulseAlpha))
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(ErrorRed)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "DEMAND SURGE",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 9.sp,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Text(
                        text = "High Fare Potential",
                        color = if (isDark) AccentAmber else SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Dismiss Icon
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("dismiss_surge_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Dismiss Alert",
                        tint = textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            // Main Info Content
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${event.zoneName} Spike!",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp,
                        color = textPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Surge active at: ${event.hotspotName}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = textSecondary
                    )
                }

                // Surge Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(AccentAmber)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.ElectricBolt,
                                contentDescription = null,
                                tint = Color(0xFF78350F),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "${event.surgeMultiplier}x",
                                fontWeight = FontWeight.Black,
                                fontSize = 15.sp,
                                color = Color(0xFF78350F)
                            )
                        }
                        Text(
                            text = "MULTIPLICATIVE",
                            fontSize = 7.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78350F).copy(alpha = 0.8f)
                        )
                    }
                }
            }

            Text(
                text = "${event.description} Earnings projection: +${event.potentialEarningsGmd} GMD per ride in this zone.",
                fontSize = 10.5.sp,
                color = if (isDark) Color(0xFF94A3B8) else NeutralGray,
                lineHeight = 15.sp
            )

            // Dynamic Progress Countdown bar
            val progress = timeLeft.toFloat() / event.totalDurationSeconds.toFloat()
            val barColor = when {
                timeLeft > 15 -> SuccessGreen
                timeLeft > 7 -> AccentAmber
                else -> ErrorRed
            }

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Surge fading in ${timeLeft}s",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = barColor
                    )
                    Text(
                        text = "LIVE UPDATE",
                        fontSize = 8.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textSecondary.copy(alpha = 0.7f)
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp))
                        .background(if (isDark) Color(0xFF312E81) else Color(0xFFE0E7FF))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(2.5.dp))
                            .background(barColor)
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))

            // Action Button
            Button(
                onClick = { onNavigate(event) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDark) Color(0xFF4F46E5) else BrandBluePrimary,
                    contentColor = PureWhite
                ),
                contentPadding = PaddingValues(vertical = 10.dp),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("navigate_surge_btn")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Navigation,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp)
                    )
                    Text(
                        text = "Navigate & Route to ${event.zoneName}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
