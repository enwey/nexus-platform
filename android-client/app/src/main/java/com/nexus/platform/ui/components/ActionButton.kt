package com.nexus.platform.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.nexus.platform.ui.theme.BackgroundSurfaceElevated
import com.nexus.platform.ui.theme.BorderLight
import com.nexus.platform.ui.theme.PrimaryEnd
import com.nexus.platform.ui.theme.PrimaryStart
import com.nexus.platform.ui.theme.White

@Composable
fun ActionButton(
    text: String,
    onClick: () -> Unit,
    primary: Boolean = true,
    modifier: Modifier = Modifier
) {
    val gradient = Brush.linearGradient(listOf(PrimaryStart, PrimaryEnd))
    val buttonShape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
    
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = White
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
        shape = buttonShape,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    if (primary) gradient else Brush.linearGradient(
                        listOf(BackgroundSurfaceElevated, BackgroundSurfaceElevated)
                    ),
                    shape = buttonShape
                )
                .then(
                    if (primary) Modifier else Modifier.border(
                        width = 1.dp,
                        color = BorderLight,
                        shape = buttonShape
                    )
                ),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
        }
    }
}
