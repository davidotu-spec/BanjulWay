package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverOnboardingForm(
    viewModel: BanjulWayViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var name by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("+220 ") }
    var vehicleType by remember { mutableStateOf("CAR") } // "CAR" or "TRICYCLE"
    var vehiclePlate by remember { mutableStateOf("") }
    var driverLicense by remember { mutableStateOf("") }
    var verificationInfo by remember { mutableStateOf("") }

    // Validation & Error state
    var nameError by remember { mutableStateOf<String?>(null) }
    var phoneError by remember { mutableStateOf<String?>(null) }
    var plateError by remember { mutableStateOf<String?>(null) }
    var licenseError by remember { mutableStateOf<String?>(null) }

    // Screen UI states
    var isSubmitting by remember { mutableStateOf(false) }
    var submitSuccess by remember { mutableStateOf(false) }
    var firestoreStatusMsg by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Card(
        modifier = modifier
            .fillMaxSize()
            .testTag("driver_onboarding_form_card"),
        colors = CardDefaults.cardColors(containerColor = BrandBlueLight),
        shape = RoundedCornerShape(0.dp) // Cover full screen
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp)
        ) {
            // Header Area
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .testTag("onboarding_back_btn")
                            .size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandBlueDark
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Driver Onboarding",
                        fontWeight = FontWeight.Black,
                        fontSize = 20.sp,
                        color = BrandBlueDark
                    )
                }

                Badge(
                    containerColor = BrandBluePrimary.copy(alpha = 0.15f),
                    contentColor = BrandBluePrimary,
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    Text(
                        "Gambia Partner Portal",
                        modifier = Modifier.padding(6.dp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp
                    )
                }
            }

            AnimatedVisibility(
                visible = submitSuccess,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp)
                        .testTag("onboarding_success_banner"),
                    colors = CardDefaults.cardColors(containerColor = SuccessGreen.copy(alpha = 0.12f)),
                    border = BorderStroke(1.5.dp, SuccessGreen)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(54.dp)
                        )
                        Text(
                            "Application Registered!",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = SuccessGreen
                        )
                        Text(
                            "Awesome job, $name! Your details were published straight to the cloud Firestore database collection ('driver_onboardings').",
                            fontSize = 13.sp,
                            color = BrandBlueDark,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Text(
                            "Status is currently PENDING within Room storage. You can instantly switch over to the 'Admin Panel' -> 'Verification' tab to approve your new registration!",
                            fontSize = 12.sp,
                            color = NeutralGray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) {
                            Text("Go back to Driver Hub", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!submitSuccess) {
                // Info Subheader
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Info",
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                "Submit to Firestore",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandBlueDark
                            )
                            Text(
                                "Enter standard details to launch your partner registration. Submissions sync directly with Firestore databases and fallback seamlessly when offline.",
                                fontSize = 11.sp,
                                color = NeutralGray
                            )
                        }
                    }
                }

                // FORM SECTION
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = PureWhite),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        // FULL NAME
                        Column {
                            Text(
                                "Driver Full Name *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandBlueDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = name,
                                onValueChange = {
                                    name = it
                                    nameError = null
                                },
                                placeholder = { Text("e.g. Modou Barrow") },
                                isError = nameError != null,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboard_input_name"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = GrayBorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Person, contentDescription = "Name", tint = BrandBlueSecondary)
                                }
                            )
                            nameError?.let {
                                Text(it, color = ErrorRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        // PHONE NUMBER
                        Column {
                            Text(
                                "Gambia Phone Number *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandBlueDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = phone,
                                onValueChange = {
                                    phone = it
                                    phoneError = null
                                },
                                placeholder = { Text("e.g. +220 9987654") },
                                isError = phoneError != null,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboard_input_phone"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = GrayBorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Phone, contentDescription = "Phone", tint = BrandBlueSecondary)
                                }
                            )
                            phoneError?.let {
                                Text(it, color = ErrorRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        // VEHICLE CLASS / TYPE
                        Column {
                            Text(
                                "Vehicle Class / Class type *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandBlueDark
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { vehicleType = "CAR" }
                                        .testTag("onboard_select_car"),
                                    border = BorderStroke(
                                        width = if (vehicleType == "CAR") 2.dp else 1.dp,
                                        color = if (vehicleType == "CAR") BrandBluePrimary else Color.LightGray
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (vehicleType == "CAR") BrandBluePrimary.copy(alpha = 0.08f) else PureWhite
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DirectionsCar,
                                            contentDescription = "Car",
                                            tint = if (vehicleType == "CAR") BrandBluePrimary else NeutralGray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Yellow Cab",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (vehicleType == "CAR") BrandBluePrimary else BrandBlueDark
                                        )
                                        Text("Standard saloon car", fontSize = 9.sp, color = NeutralGray)
                                    }
                                }

                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clickable { vehicleType = "TRICYCLE" }
                                        .testTag("onboard_select_tricycle"),
                                    border = BorderStroke(
                                        width = if (vehicleType == "TRICYCLE") 2.dp else 1.dp,
                                        color = if (vehicleType == "TRICYCLE") BrandBluePrimary else Color.LightGray
                                    ),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (vehicleType == "TRICYCLE") BrandBluePrimary.copy(alpha = 0.08f) else PureWhite
                                    )
                                ) {
                                    Column(
                                        modifier = Modifier.padding(12.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TwoWheeler,
                                            contentDescription = "Tuk",
                                            tint = if (vehicleType == "TRICYCLE") BrandBluePrimary else NeutralGray,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            "Tricycle Tuk",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = if (vehicleType == "TRICYCLE") BrandBluePrimary else BrandBlueDark
                                        )
                                        Text("Flexible auto caravan", fontSize = 9.sp, color = NeutralGray)
                                    }
                                }
                            }
                        }

                        // VEHICLE PLATE
                        Column {
                            Text(
                                "Vehicle License Plate *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandBlueDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = vehiclePlate,
                                onValueChange = {
                                    vehiclePlate = it
                                    plateError = null
                                },
                                placeholder = { Text("e.g. BJL 1948 B") },
                                isError = plateError != null,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboard_input_plate"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = GrayBorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.DirectionsCar, contentDescription = "Plate", tint = BrandBlueSecondary)
                                }
                            )
                            plateError?.let {
                                Text(it, color = ErrorRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        // DRIVER LICENSE
                        Column {
                            Text(
                                "Gambia Police License Code *",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandBlueDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = driverLicense,
                                onValueChange = {
                                    driverLicense = it
                                    licenseError = null
                                },
                                placeholder = { Text("e.g. DL-2026-9481") },
                                isError = licenseError != null,
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboard_input_license"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = GrayBorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.ContactMail, contentDescription = "License", tint = BrandBlueSecondary)
                                }
                            )
                            licenseError?.let {
                                Text(it, color = ErrorRed, fontSize = 10.sp, modifier = Modifier.padding(top = 2.dp))
                            }
                        }

                        // VERIFICATION INFORMATION (Background certified / details)
                        Column {
                            Text(
                                "Background Verification Credentials / Experience Details",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = BrandBlueDark
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = verificationInfo,
                                onValueChange = { verificationInfo = it },
                                placeholder = { Text("e.g. 5 yrs driving experience. Registered with municipal tuk Tuk union Serrekunda. No traffic violations.") },
                                minLines = 3,
                                maxLines = 5,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("onboard_input_verification"),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = BrandBluePrimary,
                                    unfocusedBorderColor = GrayBorderColor
                                ),
                                shape = RoundedCornerShape(10.dp),
                                leadingIcon = {
                                    Icon(Icons.Default.Security, contentDescription = "Security", tint = BrandBlueSecondary)
                                }
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // SUBMIT ACTION BUTTON
                        Button(
                            onClick = {
                                // Clear error flags
                                nameError = null
                                phoneError = null
                                plateError = null
                                licenseError = null

                                var hasError = false
                                if (name.isBlank()) {
                                    nameError = "Full Name is required."
                                    hasError = true
                                }
                                if (phone.length < 5) {
                                    phoneError = "Gambia phone code is too short."
                                    hasError = true
                                }
                                if (vehiclePlate.isBlank()) {
                                    plateError = "Vehicle Plate ID is required."
                                    hasError = true
                                }
                                if (driverLicense.length < 4) {
                                    licenseError = "Invalid standard driving license code."
                                    hasError = true
                                }

                                if (!hasError) {
                                    isSubmitting = true
                                    viewModel.onboardDriver(
                                        name = name,
                                        phone = phone,
                                        vehicleType = vehicleType,
                                        vehiclePlate = vehiclePlate,
                                        driverLicense = driverLicense,
                                        verificationInfo = verificationInfo,
                                        onComplete = { success ->
                                            isSubmitting = false
                                            submitSuccess = success
                                        }
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("onboard_submit_btn"),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary),
                            shape = RoundedCornerShape(12.dp),
                            enabled = !isSubmitting
                        ) {
                            if (isSubmitting) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Publishing to Firestore...")
                            } else {
                                Icon(Icons.Default.CloudUpload, contentDescription = "Upload", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Complete Cloud Onboarding", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

// Simple color helper
private val GrayBorderColor = Color(0xFFCBD5E1) // Slate-300
