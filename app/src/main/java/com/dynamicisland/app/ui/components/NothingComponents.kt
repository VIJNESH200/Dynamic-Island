package com.dynamicisland.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dynamicisland.app.ui.theme.BatteryGreen
import com.dynamicisland.app.ui.theme.DarkBorder
import com.dynamicisland.app.ui.theme.DarkSurfaceCard
import com.dynamicisland.app.ui.theme.NothingGrayText
import com.dynamicisland.app.ui.theme.NothingRed
import com.dynamicisland.app.ui.theme.NothingWhite

/**
 * Clean Nothing OS styled card with subtle border and dark background.
 */
@Composable
fun NothingCard(
    modifier: Modifier = Modifier,
    border: BorderStroke? = BorderStroke(1.dp, DarkBorder),
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = DarkSurfaceCard
        ),
        border = border,
        content = content
    )
}

/**
 * Status indicator badge with dot and text.
 */
@Composable
fun StatusBadge(
    isActive: Boolean,
    activeText: String = "ACTIVE",
    inactiveText: String = "INACTIVE",
    modifier: Modifier = Modifier
) {
    val dotColor = if (isActive) BatteryGreen else NothingRed
    val badgeBg = if (isActive) BatteryGreen.copy(alpha = 0.12f) else NothingRed.copy(alpha = 0.12f)
    val textColor = if (isActive) BatteryGreen else NothingRed

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(badgeBg)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(dotColor)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = if (isActive) activeText else inactiveText,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

/**
 * Styled switch matching Nothing OS red and dark palette.
 */
@Composable
fun NothingSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        colors = SwitchDefaults.colors(
            checkedThumbColor = NothingWhite,
            checkedTrackColor = NothingRed,
            uncheckedThumbColor = NothingGrayText,
            uncheckedTrackColor = DarkBorder,
            uncheckedBorderColor = DarkBorder
        )
    )
}
