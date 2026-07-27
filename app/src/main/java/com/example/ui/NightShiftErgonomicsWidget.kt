package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness4
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Visibility
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

@Composable
fun NightShiftErgonomicsWidget(
    themeMode: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = themeMode == ThemeMode.DARK

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("night_shift_ergonomics_card"),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else PureWhite
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isDark) Color(0xFF334155) else Color.LightGray.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
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
                        imageVector = Icons.Default.NightsStay,
                        contentDescription = "Night Shift Settings",
                        tint = if (isDark) AccentAmber else BrandBluePrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "Night Shift Eye Ergonomics",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = if (isDark) PureWhite else BrandBlueDark
                    )
                }

                // Eye protection status pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (isDark) SuccessGreen.copy(alpha = 0.15f) else NeutralGray.copy(alpha = 0.1f)
                        )
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = null,
                            tint = if (isDark) SuccessGreen else NeutralGray,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (isDark) "PROTECTION: ACTIVE" else "LIGHT MODE",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SuccessGreen else NeutralGray
                        )
                    }
                }
            }

            Text(
                text = "Dimming screen blue emission and switching to a high-contrast dark theme dramatically reduces pupillary strain during night rides along unlit urban areas.",
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF94A3B8) else NeutralGray,
                lineHeight = 14.sp
            )

            // Segmented Theme Selector Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isDark) Color(0xFF0F172A) else BrandBlueLight)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // LIGHT BUTTON
                val lightActive = themeMode == ThemeMode.LIGHT
                val lightBg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (lightActive) (if (isDark) Color(0xFF334155) else PureWhite) else Color.Transparent,
                    label = "lightThemeBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(lightBg)
                        .border(
                            width = if (lightActive) 1.dp else 0.dp,
                            color = if (lightActive) (if (isDark) Color(0xFF475569) else Color.LightGray.copy(alpha = 0.5f)) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onThemeChange(ThemeMode.LIGHT) }
                        .padding(vertical = 6.dp)
                        .testTag("theme_btn_light"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LightMode,
                            contentDescription = "Light Mode",
                            tint = if (lightActive) BrandBluePrimary else (if (isDark) Color(0xFF64748B) else NeutralGray),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Light",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (lightActive) (if (isDark) PureWhite else BrandBlueDark) else (if (isDark) Color(0xFF64748B) else NeutralGray)
                        )
                    }
                }

                // DARK BUTTON
                val darkActive = themeMode == ThemeMode.DARK
                val darkBg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (darkActive) (if (isDark) Color(0xFF334155) else PureWhite) else Color.Transparent,
                    label = "darkThemeBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1.2f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(darkBg)
                        .border(
                            width = if (darkActive) 1.dp else 0.dp,
                            color = if (darkActive) (if (isDark) Color(0xFF475569) else Color.LightGray.copy(alpha = 0.5f)) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onThemeChange(ThemeMode.DARK) }
                        .padding(vertical = 6.dp)
                        .testTag("theme_btn_dark"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DarkMode,
                            contentDescription = "Dark Mode",
                            tint = if (darkActive) AccentAmber else (if (isDark) Color(0xFF64748B) else NeutralGray),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Dark (Night)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (darkActive) (if (isDark) PureWhite else BrandBlueDark) else (if (isDark) Color(0xFF64748B) else NeutralGray)
                        )
                    }
                }

                // SYSTEM BUTTON
                val systemActive = themeMode == ThemeMode.SYSTEM
                val systemBg by androidx.compose.animation.animateColorAsState(
                    targetValue = if (systemActive) (if (isDark) Color(0xFF334155) else PureWhite) else Color.Transparent,
                    label = "systemThemeBg"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(systemBg)
                        .border(
                            width = if (systemActive) 1.dp else 0.dp,
                            color = if (systemActive) (if (isDark) Color(0xFF475569) else Color.LightGray.copy(alpha = 0.5f)) else Color.Transparent,
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onThemeChange(ThemeMode.SYSTEM) }
                        .padding(vertical = 6.dp)
                        .testTag("theme_btn_system"),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Brightness4,
                            contentDescription = "System Default",
                            tint = if (systemActive) BrandBluePrimary else (if (isDark) Color(0xFF64748B) else NeutralGray),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "System",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (systemActive) (if (isDark) PureWhite else BrandBlueDark) else (if (isDark) Color(0xFF64748B) else NeutralGray)
                        )
                    }
                }
            }

            // High contrast / low vision advice
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isDark) Color(0xFF0F172A).copy(alpha = 0.4f) else BrandBlueLight.copy(alpha = 0.5f)
                    )
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = if (isDark) AccentAmber else BrandBlueSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Late-night tip: Keeping screen illumination below 20% in dark areas maintains your night-vision adaptation while looking for house signs or unlit road obstacles.",
                    fontSize = 8.5.sp,
                    color = if (isDark) Color(0xFF94A3B8) else NeutralGray,
                    lineHeight = 12.sp
                )
            }
        }
    }
}
