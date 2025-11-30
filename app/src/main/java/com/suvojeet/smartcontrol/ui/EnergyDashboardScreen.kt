package com.suvojeet.smartcontrol.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.suvojeet.smartcontrol.HomeViewModel
import com.suvojeet.smartcontrol.WizBulb
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
    val bulbs = viewModel.bulbs
    
    // Cost estimation: avg ₹8 per kWh (approx $0.10)
    val costPerKwh = 8.0
    val todayCost = (todayUsageWh / 1000f) * costPerKwh
    
    var selectedBulbId by remember { mutableStateOf<String?>(null) } // null = All
    var showWattageDialog by remember { mutableStateOf<WizBulb?>(null) }

    if (showWattageDialog != null) {
        WattageSettingsDialog(
            bulb = showWattageDialog!!,
            onDismiss = { showWattageDialog = null },
            onConfirm = { wattage ->
                viewModel.updateBulbWattage(showWattageDialog!!.id, wattage)
                showWattageDialog = null
            }
        )
    }

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = { Text("Energy Dashboard", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Summary Cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    SummaryCard(
                        title = "Today's Usage",
                        value = String.format("%.1f Wh", todayUsageWh),
                        icon = Icons.Default.Bolt,
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFF6366F1), Color(0xFF8B5CF6))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                    SummaryCard(
                        title = "Est. Cost",
                        value = String.format("₹%.2f", todayCost),
                        icon = Icons.Default.Bolt, // Could use a currency icon if available
                        gradient = Brush.linearGradient(
                            colors = listOf(Color(0xFF10B981), Color(0xFF34D399))
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Filter Chips
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    item {
                        FilterChip(
                            selected = selectedBulbId == null,
                            onClick = { selectedBulbId = null },
                            label = { Text("All Bulbs") }
                        )
                    }
                    items(bulbs) { bulb ->
                        FilterChip(
                            selected = selectedBulbId == bulb.id,
                            onClick = { selectedBulbId = bulb.id },
                            label = { Text(bulb.name) },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Lightbulb,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )
                    }
                }
            }

            // Per-Bulb Usage List
            item {
                Text(
                    text = "Device Usage",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            val filteredBulbs = if (selectedBulbId == null) bulbs else bulbs.filter { it.id == selectedBulbId }
            
            items(filteredBulbs) { bulb ->
                val usage = viewModel.getBulbUsageToday(bulb.id)
                val percentage = if (todayUsageWh > 0) usage / todayUsageWh else 0f
                
                BulbUsageCard(
                    bulb = bulb,
                    usage = usage,
                    percentage = percentage,
                    onEditWattage = { showWattageDialog = bulb }
                )
            }

            // Chart Section
            item {
                Text(
                    text = "History (Last 7 Days)",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                if (usageHistory.isNotEmpty()) {
                    EnergyBarChart(
                        data = usageHistory,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(250.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .background(Color(0xFF1A1A1A), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No historical data yet", color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SummaryCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    gradient: Brush,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent)
    ) {
        Box(
            modifier = Modifier
                .background(gradient)
                .padding(20.dp)
        ) {
            Column {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BulbUsageCard(
    bulb: WizBulb,
    usage: Float,
    percentage: Float,
    onEditWattage: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (bulb.isOn) Color(0xFFFFD700) else Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = bulb.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Text(
                            text = "${bulb.wattage.toInt()}W Rated Power",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
                }
                
                IconButton(onClick = onEditWattage) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = String.format("%.1f Wh", usage),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = String.format("%.0f%%", percentage * 100),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Progress Bar
            val animatedProgress by animateFloatAsState(
                targetValue = percentage,
                label = "progress"
            )
            
            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = Color(0xFF6366F1),
                trackColor = Color(0xFF333333)
            )
        }
    }
}

@Composable
fun WattageSettingsDialog(
    bulb: WizBulb,
    onDismiss: () -> Unit,
    onConfirm: (Float) -> Unit
) {
    var wattageText by remember { mutableStateOf(bulb.wattage.toString()) }
    val commonWattages = listOf(5f, 7f, 9f, 12f, 15f)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = {
            Text("Set Bulb Wattage", color = Color.White)
        },
        text = {
            Column {
                Text(
                    "Select or enter the rated wattage for ${bulb.name}. This is used to calculate energy usage.",
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                // Common Wattage Chips
                Text("Common Values:", color = Color.White, style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    commonWattages.take(4).forEach { watts ->
                        SuggestionChip(
                            onClick = { wattageText = watts.toString() },
                            label = { Text("${watts.toInt()}W") },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = if (wattageText.toFloatOrNull() == watts) Color(0xFF6366F1) else Color.Transparent,
                                labelColor = if (wattageText.toFloatOrNull() == watts) Color.White else Color.Gray
                            )
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = wattageText,
                    onValueChange = { wattageText = it },
                    label = { Text("Wattage (W)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF6366F1),
                        unfocusedLabelColor = Color.Gray,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    wattageText.toFloatOrNull()?.let { onConfirm(it) }
                }
            ) {
                Text("Save", color = Color(0xFF6366F1))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        }
    )
}

@Composable
fun EnergyBarChart(
    data: List<DailyEnergyUsage>,
    modifier: Modifier = Modifier
) {
    val maxUsage = data.maxOfOrNull { it.energyWh } ?: 1f
    val primaryColor = Color(0xFF6366F1)
    val onSurface = Color.White

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
