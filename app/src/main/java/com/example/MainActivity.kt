package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AdminScreen
import com.example.ui.BanjulWayViewModel
import com.example.ui.BanjulWayViewModelFactory
import com.example.ui.DriverScreen
import com.example.ui.PassengerScreen
import com.example.ui.theme.*

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Fetch lazy initialized repo from application class
                val app = application as BanjulWayApplication
                val factory = BanjulWayViewModelFactory(app.repository)
                val viewModel: BanjulWayViewModel = viewModel(factory = factory)

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = BrandBlueLight
                ) {
                    BanjulWayMasterApp(viewModel)
                }
            }
        }
    }
}

@Composable
fun BanjulWayMasterApp(viewModel: BanjulWayViewModel) {
    val activeRole by viewModel.currentRole.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val activeNotif by viewModel.activePushNotification.collectAsState()

    // Status bar push notification triggers
    LaunchedEffect(activeNotif) {
        activeNotif?.let { notif ->
            showAndroidSystemNotification(
                context = context,
                title = notif.title,
                message = notif.message
            )
            // Auto-dismiss HUD overlay after 5 seconds
            kotlinx.coroutines.delay(5000)
            if (viewModel.activePushNotification.value?.id == notif.id) {
                viewModel.dismissActivePushNotification()
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding() // avoid camera notch collision nicely
        ) {
            // Clean Minimalism Segment Switcher
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("role_switcher_card"),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFE2E8F0)), // Slate-200 background
                elevation = CardDefaults.cardElevation(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Passenger segment
                    RoleSegmentButton(
                        label = "Passenger",
                        icon = Icons.Default.DirectionsCar,
                        isActive = activeRole == "PASSENGER",
                        onClick = { viewModel.setRole("PASSENGER") },
                        modifier = Modifier.weight(1f).testTag("segment_passenger")
                    )

                    // Driver segment
                    RoleSegmentButton(
                        label = "Driver Hub",
                        icon = Icons.Default.TwoWheeler,
                        isActive = activeRole == "DRIVER",
                        onClick = { viewModel.setRole("DRIVER") },
                        modifier = Modifier.weight(1f).testTag("segment_driver")
                    )

                    // Admin segment
                    RoleSegmentButton(
                        label = "Admin Panel",
                        icon = Icons.Default.AdminPanelSettings,
                        isActive = activeRole == "ADMIN",
                        onClick = { viewModel.setRole("ADMIN") },
                        modifier = Modifier.weight(1f).testTag("segment_admin")
                    )
                }
            }

            // Active View Swap
            Box(modifier = Modifier.weight(1f)) {
                when (activeRole) {
                    "PASSENGER" -> PassengerScreen(viewModel = viewModel)
                    "DRIVER" -> DriverScreen(viewModel = viewModel)
                    "ADMIN" -> AdminScreen(viewModel = viewModel)
                }
            }
        }

        // Heads-up Push Notification HUD sliding down elegantly!
        androidx.compose.animation.AnimatedVisibility(
            visible = activeNotif != null,
            enter = androidx.compose.animation.slideInVertically(initialOffsetY = { -it }) + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.slideOutVertically(targetOffsetY = { -it }) + androidx.compose.animation.fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(16.dp)
                .statusBarsPadding()
        ) {
            activeNotif?.let { notif ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("heads_up_notification_hud")
                        .clickable {
                            // If they click on it, take them directly to the Driver segment to manage!
                            viewModel.setRole("DRIVER")
                            viewModel.setActiveDriver(notif.driverId)
                            viewModel.dismissActivePushNotification()
                        },
                    colors = CardDefaults.cardColors(containerColor = BrandBlueDark),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.5.dp, AccentAmber.copy(alpha = 0.5f))
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
                                // Animated glowing notification bell
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = "Notification Bell",
                                    tint = AccentAmber,
                                    modifier = Modifier.size(22.dp)
                                )
                                Column {
                                    Text(
                                        text = "BanjulWay • Driver Push Alert",
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = notif.title,
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }

                            // Dismiss Icon Button
                            IconButton(
                                onClick = { viewModel.dismissActivePushNotification() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss Alert",
                                    tint = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Target driver info
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier
                                .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Driver",
                                tint = AccentAmber,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "Dispatched to: ${notif.driverName}",
                                color = AccentAmber,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = notif.message,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // Actions on HUD
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = {
                                    // Accept booking directly via push notification!
                                    notif.payload?.let { trip ->
                                        viewModel.setRole("DRIVER")
                                        viewModel.setActiveDriver(notif.driverId)
                                        viewModel.acceptBooking(trip.id, notif.driverId)
                                    }
                                    viewModel.dismissActivePushNotification()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .weight(1.3f)
                                    .height(36.dp)
                                    .testTag("hud_accept_btn"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Accept Now", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.dismissActivePushNotification()
                                },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(36.dp),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("Ignore", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

fun showAndroidSystemNotification(context: Context, title: String, message: String) {
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        0,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
    )

    val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        Notification.Builder(context, "banjulway_driver_alerts")
    } else {
        @Suppress("DEPRECATION")
        Notification.Builder(context)
    }

    val notification = builder
        .setContentTitle(title)
        .setContentText(message)
        .setSmallIcon(android.R.drawable.stat_notify_chat) // standard built-in icon
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    notificationManager.notify(System.currentTimeMillis().toInt(), notification)
}

@Composable
fun RoleSegmentButton(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backColor = if (isActive) BrandBluePrimary else Color.Transparent
    val contentColor = if (isActive) Color.White else BrandBlueDark.copy(alpha = 0.55f)

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backColor)
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                color = contentColor,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1
            )
        }
    }
}
