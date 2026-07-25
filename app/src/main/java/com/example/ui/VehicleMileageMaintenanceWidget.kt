package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SettingsBackupRestore
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VehicleMileageEntity
import com.example.ui.theme.*

@Composable
fun VehicleMileageMaintenanceWidget(
    mileage: VehicleMileageEntity?,
    driverId: String,
    onUpdateMileage: (String, Double) -> Unit,
    onResetOil: (String) -> Unit,
    onResetTire: (String) -> Unit,
    onToggleSimulating: (String, Boolean) -> Unit,
    isDark: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Local input state for custom mileage editing
    var isEditingMileage by remember { mutableStateOf(false) }
    var mileageInputText by remember { mutableStateOf("") }

    val cardBg = if (isDark) Color(0xFF1E293B) else PureWhite
    val borderCol = if (isDark) Color.White.copy(alpha = 0.12f) else Color.LightGray.copy(alpha = 0.5f)
    val textColor = if (isDark) PureWhite else BrandBlueDark
    val subtitleColor = if (isDark) Color.LightGray else NeutralGray

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderCol, RoundedCornerShape(20.dp))
            .testTag("vehicle_mileage_maintenance_card"),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DirectionsCar,
                        contentDescription = "Vehicle Odometer Icon",
                        tint = BrandBlueSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "Vehicle & Odometer Status",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = textColor
                    )
                }

                // Simulation active badge
                if (mileage?.isSimulatingMileage == true) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(SuccessGreen.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Active Simulation indicator",
                                tint = SuccessGreen,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = "SIM ACTIVE",
                                fontWeight = FontWeight.ExtraBold,
                                color = SuccessGreen,
                                fontSize = 8.5.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (mileage == null) {
                // If mileage data is empty, offer initialization
                Button(
                    onClick = { onUpdateMileage(driverId, 12450.0) },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                    modifier = Modifier.fillMaxWidth().testTag("init_odometer_btn")
                ) {
                    Text("Initialize Vehicle Odometer Logs", fontWeight = FontWeight.Bold)
                }
            } else {
                // Main Stats Content
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "TOTAL MILEAGE (ODOMETER)",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = subtitleColor,
                            letterSpacing = 0.5.sp
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        if (isEditingMileage) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                OutlinedTextField(
                                    value = mileageInputText,
                                    onValueChange = { mileageInputText = it },
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Done
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onDone = {
                                            val inputVal = mileageInputText.toDoubleOrNull()
                                            if (inputVal != null && inputVal >= 0) {
                                                onUpdateMileage(driverId, inputVal)
                                                isEditingMileage = false
                                            } else {
                                                android.widget.Toast.makeText(context, "Enter a valid odometer number", android.widget.Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    ),
                                    placeholder = { Text("Odometer", fontSize = 12.sp) },
                                    singleLine = true,
                                    modifier = Modifier
                                        .width(130.dp)
                                        .height(48.dp)
                                        .testTag("odometer_input_field"),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandBlueSecondary,
                                        focusedLabelColor = BrandBlueSecondary,
                                        unfocusedBorderColor = borderCol
                                    )
                                )

                                Button(
                                    onClick = {
                                        val inputVal = mileageInputText.toDoubleOrNull()
                                        if (inputVal != null && inputVal >= 0) {
                                            onUpdateMileage(driverId, inputVal)
                                            isEditingMileage = false
                                        } else {
                                            android.widget.Toast.makeText(context, "Enter valid number", android.widget.Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 10.dp),
                                    modifier = Modifier
                                        .height(36.dp)
                                        .testTag("save_odometer_btn"),
                                    colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen)
                                ) {
                                    Text("Set", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = String.format("%,.1f km", mileage.currentMileage),
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textColor,
                                    modifier = Modifier.testTag("odometer_readout_text")
                                )

                                IconButton(
                                    onClick = {
                                        mileageInputText = mileage.currentMileage.toInt().toString()
                                        isEditingMileage = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_odometer_icon_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Edit mileage manually",
                                        tint = BrandBlueSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Test Action Buttons
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(
                            onClick = {
                                onUpdateMileage(driverId, mileage.currentMileage + 500.0)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                contentColor = textColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("quick_add_500km_btn")
                        ) {
                            Text("+500 km", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                onUpdateMileage(driverId, mileage.currentMileage + 4500.0)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9),
                                contentColor = textColor
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("quick_add_4500km_btn")
                        ) {
                            Text("+4.5k km", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 14.dp),
                    thickness = 1.dp,
                    color = borderCol
                )

                // ROUTINE MAINTENANCE SECTION
                Text(
                    text = "ROUTINE SERVICE REMINDERS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = subtitleColor,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Engine Oil Change progress & alert
                val oilDriven = mileage.currentMileage - mileage.lastOilChangeMileage
                val oilProgress = (oilDriven / mileage.oilChangeInterval).coerceIn(0.0, 1.0).toFloat()
                val isOilCritical = oilDriven >= mileage.oilChangeInterval

                MaintenanceProgressBar(
                    title = "Engine Oil Service",
                    subtitle = if (isOilCritical) "CRITICAL: Oil Change Overdue!" else "Oil healthy. Next in ${(mileage.oilChangeInterval - oilDriven).toInt()} km",
                    progress = oilProgress,
                    isCritical = isOilCritical,
                    icon = Icons.Default.LocalGasStation,
                    isDark = isDark,
                    onReset = {
                        onResetOil(driverId)
                        android.widget.Toast.makeText(context, "Engine Oil Change Service Logged!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    testTagSuffix = "oil"
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Tire tread & rotation check
                val tireDriven = mileage.currentMileage - mileage.lastTireCheckMileage
                val tireProgress = (tireDriven / mileage.tireCheckInterval).coerceIn(0.0, 1.0).toFloat()
                val isTireCritical = tireDriven >= mileage.tireCheckInterval

                MaintenanceProgressBar(
                    title = "Tire Inspection & Rotation",
                    subtitle = if (isTireCritical) "CRITICAL: Tire Check Required!" else "Tires healthy. Next in ${(mileage.tireCheckInterval - tireDriven).toInt()} km",
                    progress = tireProgress,
                    isCritical = isTireCritical,
                    icon = Icons.Default.Build,
                    isDark = isDark,
                    onReset = {
                        onResetTire(driverId)
                        android.widget.Toast.makeText(context, "Tire Rotation & Check Service Logged!", android.widget.Toast.LENGTH_SHORT).show()
                    },
                    testTagSuffix = "tire"
                )

                Spacer(modifier = Modifier.height(18.dp))

                // 3. Automated simulation wiggler switch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Speed,
                                contentDescription = "Speed Dial Simulation Icon",
                                tint = if (mileage.isSimulatingMileage) SuccessGreen else subtitleColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Column {
                                Text(
                                    text = "Simulate Trip Driving",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                                Text(
                                    text = if (mileage.isSimulatingMileage) "Accumulating +30 km every 10s" else "Drive to accumulate mileage",
                                    fontSize = 9.5.sp,
                                    color = subtitleColor
                                )
                            }
                        }

                        Switch(
                            checked = mileage.isSimulatingMileage,
                            onCheckedChange = { onToggleSimulating(driverId, it) },
                            modifier = Modifier.testTag("mileage_sim_switch"),
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = PureWhite,
                                checkedTrackColor = SuccessGreen
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun MaintenanceProgressBar(
    title: String,
    subtitle: String,
    progress: Float,
    isCritical: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isDark: Boolean,
    onReset: () -> Unit,
    testTagSuffix: String
) {
    val barColor = if (isCritical) ErrorRed else BrandBlueSecondary
    val textColor = if (isDark) PureWhite else BrandBlueDark
    val subtitleColor = if (isCritical) ErrorRed else (if (isDark) Color.LightGray else NeutralGray)

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = barColor,
                    modifier = Modifier.size(16.dp)
                )
                Column {
                    Text(
                        text = title,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = textColor
                    )
                    Text(
                        text = subtitle,
                        fontSize = 10.5.sp,
                        color = subtitleColor,
                        fontWeight = if (isCritical) FontWeight.ExtraBold else FontWeight.Medium
                    )
                }
            }

            if (isCritical) {
                Button(
                    onClick = onReset,
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed),
                    shape = RoundedCornerShape(6.dp),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    modifier = Modifier
                        .height(28.dp)
                        .testTag("reset_service_btn_$testTagSuffix")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SettingsBackupRestore,
                            contentDescription = "Reset Maintenance Service Log",
                            tint = Color.White,
                            modifier = Modifier.size(11.dp)
                        )
                        Text("Reset", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
        }
    }
}
