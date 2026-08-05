package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.*

@Composable
fun AccountVerificationDialog(
    userEmail: String,
    userRole: String,
    generatedCode: String,
    verificationMessage: String,
    isDark: Boolean = false,
    onVerifyCode: (String) -> Boolean,
    onResendEmail: () -> Unit,
    onDismiss: () -> Unit
) {
    var inputCode by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    var isVerifying by remember { mutableStateOf(false) }

    val dialogBg = if (isDark) Color(0xFF1E293B) else PureWhite
    val textPrimary = if (isDark) PureWhite else BrandBlueDark
    val textSecondary = if (isDark) Color(0xFF94A3B8) else NeutralGray
    val containerBg = if (isDark) Color(0xFF0F172A) else BrandBlueLight

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
                .padding(16.dp)
                .testTag("account_verification_dialog"),
            shape = RoundedCornerShape(24.dp),
            color = dialogBg,
            tonalElevation = 6.dp,
            shadowElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Email Badge Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(BrandBluePrimary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MarkEmailRead,
                        contentDescription = "Email Verification Icon",
                        tint = BrandBluePrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Verify Your $userRole Account",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = textPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "A confirmation email with a security verification code was dispatched to:",
                    fontSize = 12.5.sp,
                    color = textSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    color = containerBg,
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Text(
                        text = userEmail.ifBlank { "your email" },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandBluePrimary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Simulated Inbox Email Notification Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, BrandBluePrimary.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        .clickable {
                            inputCode = generatedCode
                            errorMessage = ""
                            successMessage = "Code $generatedCode auto-filled from simulated email!"
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = BrandBlueLight.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Email,
                            contentDescription = "Incoming Email",
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📩 Simulated Email Inbox",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandBlueDark
                                )
                                Text(
                                    text = "Tap to autofill",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandBluePrimary
                                )
                            }
                            Text(
                                text = "Subject: WayGo Account Verification Code",
                                fontSize = 11.sp,
                                color = textSecondary
                            )
                            Text(
                                text = "Your Code: $generatedCode",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandBlueDark
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OTP Verification Code Input
                OutlinedTextField(
                    value = inputCode,
                    onValueChange = {
                        if (it.length <= 6) {
                            inputCode = it.filter { char -> char.isDigit() }
                            errorMessage = ""
                        }
                    },
                    label = { Text("Enter 6-Digit Verification Code") },
                    placeholder = { Text("e.g. 849201") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verification_code_input"),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = BrandBluePrimary,
                        unfocusedBorderColor = NeutralGray
                    )
                )

                AnimatedVisibility(visible = errorMessage.isNotBlank()) {
                    Text(
                        text = errorMessage,
                        color = ErrorRed,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                AnimatedVisibility(visible = successMessage.isNotBlank()) {
                    Row(
                        modifier = Modifier.padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = successMessage,
                            color = SuccessGreen,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Verify Button
                Button(
                    onClick = {
                        if (inputCode.length < 6 && inputCode != generatedCode) {
                            errorMessage = "Please enter the 6-digit verification code."
                            return@Button
                        }
                        isVerifying = true
                        val success = onVerifyCode(inputCode)
                        if (!success) {
                            errorMessage = "Invalid code. Please check your email or tap autofill."
                            isVerifying = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("submit_verification_btn"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandBluePrimary,
                        contentColor = PureWhite
                    ),
                    enabled = !isVerifying
                ) {
                    if (isVerifying) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = PureWhite,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Verify & Activate Account", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            onResendEmail()
                            inputCode = ""
                            errorMessage = ""
                            successMessage = "A new confirmation email has been dispatched!"
                        },
                        modifier = Modifier.testTag("resend_verification_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Resend",
                            modifier = Modifier.size(16.dp),
                            tint = BrandBluePrimary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Resend Email", fontSize = 12.sp, color = BrandBluePrimary, fontWeight = FontWeight.SemiBold)
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("cancel_verification_btn")
                    ) {
                        Text("Cancel", fontSize = 12.sp, color = textSecondary)
                    }
                }
            }
        }
    }
}
