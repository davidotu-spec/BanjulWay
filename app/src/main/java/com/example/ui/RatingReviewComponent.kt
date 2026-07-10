package com.example.ui

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun RatingReviewComponent(
    driverName: String,
    vehicleType: String,
    vehiclePlate: String,
    driverRating: Float,
    tripId: String,
    onSubmitRating: (Int, String, List<String>, Int) -> Unit, // stars, comment, tags, tipGmd
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var ratingStars by remember { mutableIntStateOf(5) }
    var reviewComment by remember { mutableStateOf("") }
    val selectedTags = remember { mutableStateListOf<String>() }
    var selectedTipAmount by remember { mutableIntStateOf(0) } // 0, 25, 50, 100, or custom
    var showCustomTipField by remember { mutableStateOf(false) }
    var customTipInput by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    // Context-dependent tags
    val ratingDescription = when (ratingStars) {
        1 -> "Terrible"
        2 -> "Disappointing"
        3 -> "Fair / Average"
        4 -> "Very Good"
        5 -> "Exceptional Journey!"
        else -> "Excellent"
    }

    val driverProfileTagline = when (ratingStars) {
        in 1..2 -> "What went wrong? Tell us how we can improve."
        3 -> "Any feedback on how to make it better?"
        else -> "Help reward ${driverName} with your appreciation!"
    }

    // Dynamic tags
    val availableTags = remember(ratingStars) {
        if (ratingStars >= 4) {
            listOf("Safe Driving", "Polite & Respectful", "Excellent Music", "Clean Vehicle", "Fast Pickup", "Great Route Guidance")
        } else {
            listOf("Rough Driving", "Late Arrival", "Unclean Vehicle", "Polite but Slow", "Navigation Issue", "Unprofessional")
        }
    }

    // Clear tag choices if they are no longer in the dynamic list
    LaunchedEffect(ratingStars) {
        selectedTags.clear()
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("premium_rating_review_card"),
        colors = CardDefaults.cardColors(containerColor = PureWhite),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Drag handle / Notch indicator
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(5.dp)
                    .clip(CircleShape)
                    .background(Color.LightGray.copy(alpha = 0.6f))
                    .align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subheader
            Text(
                text = "Thank you for riding with WayGo!",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                color = BrandBlueDark,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Your honest review supports our Gambian transport safety checks.",
                fontSize = 12.sp,
                color = NeutralGray,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp, bottom = 12.dp)
            )

            Divider(color = BrandBluePrimary.copy(alpha = 0.08f))

            Spacer(modifier = Modifier.height(14.dp))

            // Driver Profile Header Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandBlueLight),
                border = BorderStroke(1.dp, BrandBluePrimary.copy(alpha = 0.1f))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Vehicle Type Avatar Circle
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(BrandBluePrimary.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (vehicleType == "CAR") Icons.Default.DirectionsCar else Icons.Default.TwoWheeler,
                            contentDescription = vehicleType,
                            tint = BrandBluePrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = driverName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = BrandBlueDark
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "RatingStar",
                                tint = AccentAmber,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = String.format("%.1f", driverRating),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandBlueDark
                            )
                            Text(
                                text = "• Licensed Driver • $vehiclePlate",
                                fontSize = 11.sp,
                                color = NeutralGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Dynamic Instruction Guideline
            Text(
                text = driverProfileTagline,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = BrandBlueDark,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Interactive Stars Area
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                (1..5).forEach { star ->
                    IconButton(
                        onClick = { ratingStars = star },
                        modifier = Modifier
                            .size(46.dp)
                            .testTag("star_select_$star")
                    ) {
                        Icon(
                            imageVector = if (star <= ratingStars) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = "Rate $star Stars",
                            tint = if (star <= ratingStars) AccentAmber else Color.LightGray,
                            modifier = Modifier.size(42.dp)
                        )
                    }
                }
            }

            // Star Rating Label Description
            Text(
                text = ratingDescription,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (ratingStars >= 4) SuccessGreen else if (ratingStars == 3) AccentAmber else ErrorRed,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Feedback Tag Chips
            Text(
                text = "Select matching tags:",
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = BrandBlueDark,
                modifier = Modifier.align(Alignment.Start).padding(bottom = 8.dp)
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                availableTags.forEach { tag ->
                    val isTagSelected = selectedTags.contains(tag)
                    FilterChip(
                        selected = isTagSelected,
                        onClick = {
                            if (isTagSelected) {
                                selectedTags.remove(tag)
                            } else {
                                selectedTags.add(tag)
                            }
                        },
                        label = {
                            Text(tag, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        },
                        leadingIcon = if (isTagSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        } else null,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = BrandBluePrimary.copy(alpha = 0.12f),
                            selectedLabelColor = BrandBluePrimary,
                            selectedLeadingIconColor = BrandBluePrimary
                        ),
                        modifier = Modifier.testTag("chip_tag_$tag")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Written Commentary Area
            OutlinedTextField(
                value = reviewComment,
                onValueChange = { if (it.length <= 250) reviewComment = it },
                label = { Text("Describe your experience (optional)") },
                placeholder = { Text("e.g. He is very polite and arrived on time at the market corner.") },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("input_review_comment"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandBluePrimary,
                    unfocusedBorderColor = Color.LightGray
                ),
                maxLines = 3,
                minLines = 2,
                supportingText = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text("${reviewComment.length}/250", fontSize = 10.sp, color = NeutralGray)
                    }
                }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // driver tip setup
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandBlueLight.copy(alpha = 0.8f)),
                border = BorderStroke(1.dp, BrandBlueSecondary.copy(alpha = 0.15f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.MonetizationOn,
                                contentDescription = "Tipping",
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Add a Tip for $driverName",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = BrandBlueDark
                            )
                        }
                        
                        Text(
                            "100% goes to driver!",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = SuccessGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val tipValues = listOf(0, 25, 50, 100)
                        tipValues.forEach { amount ->
                            val isSelected = selectedTipAmount == amount && !showCustomTipField
                            OutlinedButton(
                                onClick = {
                                    selectedTipAmount = amount
                                    showCustomTipField = false
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(38.dp)
                                    .testTag("tip_option_$amount"),
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    containerColor = if (isSelected) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent,
                                    contentColor = if (isSelected) SuccessGreen else BrandBlueDark
                                ),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SuccessGreen else Color.LightGray
                                ),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text(
                                    text = if (amount == 0) "No Tip" else "${amount} GMD",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Custom tip button choice
                        OutlinedButton(
                            onClick = { showCustomTipField = true },
                            modifier = Modifier
                                .weight(1.1f)
                                .height(38.dp)
                                .testTag("tip_option_custom"),
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (showCustomTipField) SuccessGreen.copy(alpha = 0.12f) else Color.Transparent,
                                contentColor = if (showCustomTipField) SuccessGreen else BrandBlueDark
                            ),
                            border = BorderStroke(
                                width = if (showCustomTipField) 1.5.dp else 1.dp,
                                color = if (showCustomTipField) SuccessGreen else Color.LightGray
                            ),
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Text("Custom", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    AnimatedVisibility(
                        visible = showCustomTipField,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Column(modifier = Modifier.padding(top = 10.dp)) {
                            OutlinedTextField(
                                value = customTipInput,
                                onValueChange = {
                                    if (it.all { char -> char.isDigit() }) {
                                        customTipInput = it
                                        selectedTipAmount = it.toIntOrNull() ?: 0
                                    }
                                },
                                placeholder = { Text("Enter Tip in Gambian Dalasis (GMD)") },
                                leadingIcon = { Text("GMD", fontWeight = FontWeight.Black, fontSize = 12.sp, color = SuccessGreen, modifier = Modifier.padding(start = 10.dp)) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp)
                                    .testTag("input_custom_tip"),
                                singleLine = true,
                                shape = RoundedCornerShape(8.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = SuccessGreen,
                                    unfocusedBorderColor = Color.LightGray
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Buttons Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("btn_rating_dismiss"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = NeutralGray)
                ) {
                    Text("Skip", fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        val tip = if (showCustomTipField) customTipInput.toIntOrNull() ?: 0 else selectedTipAmount
                        onSubmitRating(ratingStars, reviewComment, selectedTags.toList(), tip)
                    },
                    modifier = Modifier
                        .weight(2f)
                        .height(48.dp)
                        .testTag("btn_rating_submit"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandBluePrimary)
                ) {
                    Icon(Icons.Default.CloudDone, contentDescription = "Submit", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Submit & Finish Journey", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
