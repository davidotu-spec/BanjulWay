package com.example.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.PinDrop
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AccentAmber
import com.example.ui.theme.BrandBlueDark
import com.example.ui.theme.BrandBlueLight
import com.example.ui.theme.BrandBluePrimary
import com.example.ui.theme.NeutralGray
import com.example.ui.theme.PureWhite
import com.example.ui.theme.SuccessGreen

data class StatusStepInfo(
    val stepIndex: Int,
    val label: String,
    val icon: ImageVector,
    val testTag: String,
    val stepColor: Color
)

@Composable
fun RideStatusStepIndicator(
    currentStatus: String,
    modifier: Modifier = Modifier
) {
    val stepIndex = when (currentStatus) {
        "ARRIVED" -> 2
        "EN_ROUTE" -> 3
        "COMPLETED" -> 4
        else -> 1 // "REQUESTED" or "ACCEPTED"
    }

    val targetProgress = when (stepIndex) {
        1 -> 0.12f
        2 -> 0.42f
        3 -> 0.72f
        4 -> 1.00f
        else -> 0.12f
    }

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress,
        animationSpec = tween(durationMillis = 600),
        label = "step_progress_animation"
    )

    val steps = listOf(
        StatusStepInfo(1, "Driver Assigned", Icons.Default.Navigation, "status_step_assigned", BrandBluePrimary),
        StatusStepInfo(2, "Driver Arrived", Icons.Default.PinDrop, "status_step_arrived", AccentAmber),
        StatusStepInfo(3, "Trip In Progress", Icons.Default.DirectionsCar, "status_step_in_progress", SuccessGreen),
        StatusStepInfo(4, "Trip Completed", Icons.Default.VerifiedUser, "status_step_completed", SuccessGreen)
    )

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("ride_status_step_indicator"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header title & active badge
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Ride Progress",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlueDark
                )

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when (stepIndex) {
                        1 -> BrandBluePrimary.copy(alpha = 0.12f)
                        2 -> AccentAmber.copy(alpha = 0.15f)
                        3 -> SuccessGreen.copy(alpha = 0.15f)
                        else -> SuccessGreen.copy(alpha = 0.18f)
                    }
                ) {
                    Text(
                        text = "Step $stepIndex of 4",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = when (stepIndex) {
                            1 -> BrandBluePrimary
                            2 -> AccentAmber
                            else -> SuccessGreen
                        }
                    )
                }
            }

            // Visual Progress Bar Line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(CircleShape)
                        .align(Alignment.Center),
                    color = when (stepIndex) {
                        1 -> BrandBluePrimary
                        2 -> AccentAmber
                        else -> SuccessGreen
                    },
                    trackColor = BrandBlueLight
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Step Nodes & Labels
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                steps.forEach { step ->
                    val isPassed = stepIndex > step.stepIndex
                    val isCurrent = stepIndex == step.stepIndex

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .testTag(step.testTag),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Node Icon Circle
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isCurrent -> step.stepColor
                                        isPassed -> step.stepColor.copy(alpha = 0.85f)
                                        else -> NeutralGray.copy(alpha = 0.2f)
                                    }
                                )
                                .then(
                                    if (isCurrent) {
                                        Modifier.border(2.dp, PureWhite, CircleShape)
                                    } else Modifier
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isPassed) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = PureWhite,
                                    modifier = Modifier.size(15.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = step.icon,
                                    contentDescription = null,
                                    tint = if (isCurrent) PureWhite else NeutralGray,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = step.label,
                            fontSize = 9.5.sp,
                            fontWeight = if (isCurrent) FontWeight.ExtraBold else FontWeight.Medium,
                            color = if (isCurrent || isPassed) BrandBlueDark else NeutralGray,
                            textAlign = TextAlign.Center,
                            lineHeight = 11.sp
                        )
                    }
                }
            }
        }
    }
}
