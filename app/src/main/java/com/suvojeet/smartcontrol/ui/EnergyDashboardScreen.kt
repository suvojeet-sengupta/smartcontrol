package com.suvojeet.smartcontrol.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suvojeet.smartcontrol.HomeViewModel
import com.suvojeet.smartcontrol.data.DailyEnergyUsage
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnergyDashboardScreen(
    navController: NavController,
    viewModel: HomeViewModel
) {
    val usageHistory = viewModel.energyUsageHistory
    val todayUsageWh = viewModel.todayUsage
    
    // Cost estimation: avg ₹8 per kWh (approx $0.10)
    val costPerKwh = 8.0
    val todayCost = (todayUsageWh / 1000f) * costPerKwh

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Energy Dashboard") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Today's Usage",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Text(
                        text = String.format("%.2f Wh", todayUsageWh),
                        style = MaterialTheme.typography.displayMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("Est. Cost: ₹%.2f", todayCost),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            // Chart Section
            Text(
                text = "Last 7 Days",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )

            if (usageHistory.isNotEmpty()) {
                EnergyBarChart(
                    data = usageHistory,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No historical data yet")
                }
            }
        }
    }
}

@Composable
fun EnergyBarChart(
    data: List<DailyEnergyUsage>,
    modifier: Modifier = Modifier
) {
    val maxUsage = data.maxOfOrNull { it.energyWh } ?: 1f
    val primaryColor = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface

    Canvas(modifier = modifier) {
        val barWidth = size.width / (data.size * 2f)
        val spacing = size.width / (data.size * 2f)
        val availableHeight = size.height - 40.dp.toPx() // Leave room for labels

        data.forEachIndexed { index, item ->
            val barHeight = (item.energyWh / maxUsage) * availableHeight
            val x = (index * (barWidth + spacing)) + spacing / 2

            // Draw Bar
            drawRect(
                color = primaryColor,
                topLeft = Offset(x, availableHeight - barHeight),
                size = Size(barWidth, barHeight)
            )

            // Draw Label (Day)
            val dayLabel = try {
                val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(item.date)
                SimpleDateFormat("EEE", Locale.getDefault()).format(date!!)
            } catch (e: Exception) {
                "?"
            }

            drawContext.canvas.nativeCanvas.apply {
                drawText(
                    dayLabel,
                    x + barWidth / 2,
                    size.height,
                    android.graphics.Paint().apply {
                        color = onSurface.toArgb()
                        textSize = 32f
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}
