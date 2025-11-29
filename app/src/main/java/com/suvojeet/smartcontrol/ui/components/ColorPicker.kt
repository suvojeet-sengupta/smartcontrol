package com.suvojeet.smartcontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

@Composable
fun ColorPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "Select Color",
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        HorizontalGradientPicker(
            selectedColor = selectedColor,
            onColorSelected = onColorSelected
        )
    }
}

@Composable
fun HorizontalGradientPicker(
    selectedColor: Color,
    onColorSelected: (Color) -> Unit
) {
    var selectedPosition by remember { mutableStateOf(0.5f) }
    
    val rainbowColors = listOf(
        Color(0xFFFF0000), // Red
        Color(0xFFFF7F00), // Orange
        Color(0xFFFFFF00), // Yellow
        Color(0xFF00FF00), // Green
        Color(0xFF00FFFF), // Cyan
        Color(0xFF0000FF), // Blue
        Color(0xFF8B00FF), // Purple
        Color(0xFFFF00FF)  // Magenta
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clip(RoundedCornerShape(40.dp))
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures { offset ->
                        val position = (offset.x / size.width).coerceIn(0f, 1f)
                        selectedPosition = position
                        
                        // Map position to color
                        val colorIndex = (position * (rainbowColors.size - 1))
                        val lowerIndex = colorIndex.toInt().coerceIn(0, rainbowColors.size - 2)
                        val upperIndex = lowerIndex + 1
                        val fraction = colorIndex - lowerIndex
                        
                        val lowerColor = rainbowColors[lowerIndex]
                        val upperColor = rainbowColors[upperIndex]
                        
                        val r = lowerColor.red + (upperColor.red - lowerColor.red) * fraction
                        val g = lowerColor.green + (upperColor.green - lowerColor.green) * fraction
                        val b = lowerColor.blue + (upperColor.blue - lowerColor.blue) * fraction
                        
                        onColorSelected(Color(r, g, b))
                    }
                }
        ) {
            // Draw gradient background
            drawRect(
                brush = Brush.horizontalGradient(rainbowColors),
                size = size
            )
            
            // Draw selector circle
            val selectorX = selectedPosition * size.width
            val selectorY = size.height / 2
            
            // Outer white ring
            drawCircle(
                color = Color.White,
                radius = 20.dp.toPx(),
                center = Offset(selectorX, selectorY),
                style = Stroke(width = 3.dp.toPx())
            )
            
            // Inner circle showing selected color
            drawCircle(
                color = selectedColor,
                radius = 16.dp.toPx(),
                center = Offset(selectorX, selectorY)
            )
        }
    }
}
