package com.suvojeet.smartcontrol.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
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
import kotlin.math.roundToInt

@Composable
fun TemperatureSlider(
    temperature: Int,
    onTemperatureChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val minTemp = 2700
    val maxTemp = 6500
    
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Temperature",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                "${temperature}K",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .clip(RoundedCornerShape(30.dp))
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTapGestures { offset ->
                            val position = (offset.x / size.width).coerceIn(0f, 1f)
                            val newTemp = (minTemp + position * (maxTemp - minTemp)).roundToInt()
                            onTemperatureChange(newTemp)
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures { change, _ ->
                            change.consume()
                            val position = (change.position.x / size.width).coerceIn(0f, 1f)
                            val newTemp = (minTemp + position * (maxTemp - minTemp)).roundToInt()
                            onTemperatureChange(newTemp)
                        }
                    }
            ) {
                // Draw gradient background (warm to cool)
                val warmColor = Color(0xFFFF9800) // Orange
                val neutralColor = Color(0xFFFFE082) // Light yellow
                val coolColor = Color(0xFFBBDEFB) // Light blue
                
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(warmColor, neutralColor, coolColor)
                    ),
                    size = size
                )
                
                // Calculate slider position
                val position = ((temperature - minTemp).toFloat() / (maxTemp - minTemp))
                    .coerceIn(0f, 1f)
                val sliderX = position * size.width
                val sliderY = size.height / 2
                
                // Draw slider thumb
                // Outer white ring
                drawCircle(
                    color = Color.White,
                    radius = 18.dp.toPx(),
                    center = Offset(sliderX, sliderY),
                    style = Stroke(width = 3.dp.toPx())
                )
                
                // Inner circle with temperature color
                val thumbColor = when {
                    temperature < 3500 -> warmColor
                    temperature < 5000 -> neutralColor
                    else -> coolColor
                }
                
                drawCircle(
                    color = thumbColor,
                    radius = 14.dp.toPx(),
                    center = Offset(sliderX, sliderY)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Warm",
                fontSize = 11.sp,
                color = Color.Gray
            )
            Text(
                "Cool",
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}
