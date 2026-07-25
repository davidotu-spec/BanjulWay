package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SosEmergencyButton(
    pLat: Double,
    pLng: Double,
    activeTripId: String?,
    modifier: Modifier = Modifier
) {
    var showSosDialog by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .testTag("sos_floating_trigger")
            .size(54.dp)
            .clip(CircleShape)
            .background(ErrorRed)
            .clickable { showSosDialog = true },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Security,
                contentDescription = "SOS",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "SOS",
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.Black
            )
        }
    }

    if (showSosDialog) {
        SosEmergencyDialog(
            pLat = pLat,
            pLng = pLng,
            activeTripId = activeTripId,
            onDismiss = { showSosDialog = false }
        )
    }
}

@Composable
fun SosEmergencyDialog(
    pLat: Double,
    pLng: Double,
    activeTripId: String?,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val clipboardManager = LocalClipboardManager.current

    // Contact registration state
    var editContactName by remember { mutableStateOf("Mary Jallow") }
    var editContactPhone by remember { mutableStateOf("+220 712 3456") }
    var isEditingContact by remember { mutableStateOf(false) }

    // Multi-tier emergency choice
    var targetAuthority by remember { mutableStateOf("POLICE") } // "POLICE", "FIRE_RESCUE", "RED_CROSS", "CONTACT"
    
    // Dispatch simulation state
    var countdownValue by remember { mutableStateOf(5) }
    var isCountdownActive by remember { mutableStateOf(false) }
    val alertLogs = remember { mutableStateListOf<String>() }
    var alertTransmitted by remember { mutableStateOf(false) }

    // Start direct countdown effect
    LaunchedEffect(isCountdownActive) {
        if (isCountdownActive && countdownValue > 0) {
            while (countdownValue > 0) {
                delay(1000)
                countdownValue--
            }
            if (countdownValue == 0) {
                isCountdownActive = false
                alertTransmitted = true
                val authorityName = when (targetAuthority) {
                    "POLICE" -> "Gambia Police Force (Command Unit)"
                    "FIRE_RESCUE" -> "West Coast Region Fire & Rescue Service"
                    "RED_CROSS" -> "Gambia Red Cross Ambulance Service"
                    else -> "$editContactName ($editContactPhone)"
                }
                alertLogs.add("🚨 SUCCESS: SOS Distress Signal broadcasted to $authorityName")
                alertLogs.add("📡 Precise Coords: ${String.format("%.5f", pLat)} N, ${String.format("%.5f", pLng)} W")
                alertLogs.add("🚔 Local Patrol is being routed to your active GPS location!")
                if (activeTripId != null) {
                    alertLogs.add("📱 Linked active WayGo Ride Ref: $activeTripId")
                }
            }
        }
    }

    AlertDialog(
        onDismissRequest = {
            if (!isCountdownActive) {
                onDismiss()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("sos_emergency_dialog"),
        shape = RoundedCornerShape(20.dp),
        containerColor = BrandBlueLight,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = "Alert",
                    tint = ErrorRed,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = "WayGo Emergency Guard",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlueDark
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "If you feel unsafe, in danger, or need immediate assistance, initiate a rescue broadcast below. Your details will be communicated with live telemetry markers.",
                    fontSize = 11.5.sp,
                    color = BrandBlueDark.copy(alpha = 0.8f)
                )

                // Current GPS Coordinate display card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color.LightGray.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Live Telemetry Coords (GPS)",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = NeutralGray
                            )
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.15f)),
                                shape = RoundedCornerShape(4.dp)
                            ) {
                                Text(
                                    text = "HIGH ACCURACY",
                                    color = SuccessGreen,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Black,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Lat: ${String.format("%.5f", pLat)} • Lng: ${String.format("%.5f", pLng)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlueDark
                            )

                            IconButton(
                                onClick = {
                                    val coordsStr = "My GPS Coords: Lat $pLat, Lng $pLng. WayGo distress guard alert."
                                    clipboardManager.setText(AnnotatedString(coordsStr))
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentCopy,
                                    contentDescription = "Copy text",
                                    tint = BrandBluePrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Choose Responder authorities
                Text(
                    text = "Designate dispatch agency:",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandBlueDark
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf(
                        "POLICE" to "Police Force",
                        "FIRE_RESCUE" to "Fire & Rescue",
                        "RED_CROSS" to "Red Cross",
                        "CONTACT" to "Primary Contact"
                    ).forEach { (authKey, label) ->
                        val isSelected = targetAuthority == authKey
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (!isCountdownActive && !alertTransmitted) {
                                        targetAuthority = authKey
                                    }
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) ErrorRed.copy(alpha = 0.12f) else Color.White
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) ErrorRed else Color.LightGray.copy(alpha = 0.4f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(6.dp).fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) ErrorRed else BrandBlueDark,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Show fields if Primary Contact is selected
                if (targetAuthority == "CONTACT") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.15f))
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "SOS Emergency Contact Registration",
                                    fontSize = 10.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark
                                )

                                Text(
                                    text = if (isEditingContact) "Done" else "Edit",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBluePrimary,
                                    modifier = Modifier.clickable { isEditingContact = !isEditingContact }
                                )
                            }

                            Spacer(modifier = Modifier.height(4.dp))

                            if (isEditingContact) {
                                OutlinedTextField(
                                    value = editContactName,
                                    onValueChange = { editContactName = it },
                                    label = { Text("Contact Name") },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = editContactPhone,
                                    onValueChange = { editContactPhone = it },
                                    label = { Text("Phone Number") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            } else {
                                Text(
                                    text = "Name: $editContactName",
                                    fontSize = 11.5.sp,
                                    color = BrandBlueDark
                                )
                                Text(
                                    text = "Phone: $editContactPhone",
                                    fontSize = 11.5.sp,
                                    color = NeutralGray
                                )
                            }
                        }
                    }
                }

                // Interactive Trigger & Visual Countdown Indicator
                if (isCountdownActive) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ErrorRed.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "TRANSMITTING DISTRESS SIGNAL IN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = ErrorRed
                        )

                        Text(
                            text = "$countdownValue",
                            fontSize = 38.sp,
                            fontWeight = FontWeight.Black,
                            color = ErrorRed
                        )

                        Text(
                            text = "Hold CANCEL immediately to abort request.",
                            fontSize = 10.sp,
                            color = NeutralGray
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = {
                                isCountdownActive = false
                                countdownValue = 5
                                alertLogs.add("⚠️ Dispatch canceled by user safely.")
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBlueDark),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text("ABORT SIGNAL", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else if (!alertTransmitted) {
                    Button(
                        onClick = {
                            isCountdownActive = true
                            alertLogs.clear()
                            alertLogs.add("⚠️ Initiating emergency broadcast channel...")
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .testTag("btn_trigger_emergency"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Security, contentDescription = "Distress")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("TRIGGER SOS SIGNAL NOW", fontSize = 13.sp, fontWeight = FontWeight.Black)
                    }
                }

                // Real-time Action Log
                if (alertLogs.isNotEmpty() || alertTransmitted) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color.DarkGray),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Emergency Response Logs:",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )

                            alertLogs.forEach { log ->
                                Row(
                                    verticalAlignment = Alignment.Top,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "•",
                                        color = SuccessGreen,
                                        fontSize = 11.sp,
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                    Text(
                                        text = log,
                                        color = if (log.startsWith("🚨") || log.startsWith("📡") || log.startsWith("🚔")) SuccessGreen else Color.LightGray,
                                        fontSize = 10.sp,
                                        lineHeight = 13.sp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (!isCountdownActive) {
                Button(
                    onClick = { onDismiss() },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Text("Close Console")
                }
            }
        }
    )
}
