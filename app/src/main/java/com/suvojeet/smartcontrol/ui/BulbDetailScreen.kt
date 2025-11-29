package com.suvojeet.smartcontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.ui.components.ColorPicker
import com.suvojeet.smartcontrol.ui.components.TemperatureSlider

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BulbDetailScreen(
    bulb: WizBulb,
    onNavigateBack: () -> Unit,
    onToggleBulb: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onSceneSelect: (String?) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Color", "Dynamic", "White")

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        bulb.name,
                        fontWeight = FontWeight.Bold
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (bulb.isOn) Color.White else Color(0xFF2A2A2A))
                            .clickable(onClick = onToggleBulb),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (bulb.isOn) Color.Black else Color.Gray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Row
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Black,
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF00BCD4)
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            // Tab Content
            when (selectedTab) {
                0 -> ColorTab(bulb, onColorChange, onBrightnessChange, onSceneSelect)
                1 -> DynamicTab(onSceneSelect)
                2 -> WhiteTab(bulb, onTemperatureChange, onBrightnessChange, onSceneSelect)
            }
        }
    }
}

@Composable
fun ColorTab(
    bulb: WizBulb,
    onColorChange: (Color) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onSceneSelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp)
    ) {
        // Color Picker
        ColorPicker(
            selectedColor = bulb.getComposeColor(),
            onColorSelected = { color ->
                onColorChange(color)
                onSceneSelect(null) // Clear scene when manual color selected
            }
        )

        // Quick Color Presets
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val presetColors = listOf(
                Color(0xFF1E88E5) to "Blue",
                Color(0xFF43A047) to "Green",
                Color(0xFFE53935) to "Red",
                Color(0xFFFB8C00) to "Orange",
                Color(0xFF8E24AA) to "Purple"
            )
            
            presetColors.forEach { (color, name) ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onColorChange(color) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        name,
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                }
            }
        }

        // Brightness Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Brightness",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${bulb.brightness.toInt()}%",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = bulb.brightness,
                onValueChange = onBrightnessChange,
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = bulb.getComposeColor(),
                    inactiveTrackColor = Color(0xFF2A2A2A)
                )
            )
        }
    }
}

@Composable
fun DynamicTab(onSceneSelect: (String) -> Unit) {
    val dynamicScenes = listOf(
        "fireplace" to "🔥 Fireplace",
        "fall" to "🍂 Fall",
        "club" to "🎵 Club",
        "sunset" to "🌅 Sunset",
        "romance" to "💕 Romance",
        "party" to "🎉 Party",
        "pastel" to "🌈 Pastel",
        "spring" to "🌸 Spring",
        "summer" to "☀️ Summer",
        "forest" to "🌲 Forest",
        "jungle" to "🌴 Jungle",
        "ocean" to "🌊 Ocean",
        "christmas" to "🎄 Christmas",
        "diwali" to "🪔 Diwali"
    )

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(dynamicScenes) { (sceneId, sceneName) ->
            SceneCard(
                name = sceneName,
                onClick = { onSceneSelect(sceneId) }
            )
        }
    }
}

@Composable
fun WhiteTab(
    bulb: WizBulb,
    onTemperatureChange: (Int) -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onSceneSelect: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // Temperature Presets
        Text(
            "White",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            val tempPresets = listOf(
                2700 to "Warmest",
                3200 to "Warm",
                4500 to "Daylight",
                6500 to "Cool"
            )
            
            tempPresets.forEach { (temp, name) ->
                TemperaturePresetButton(
                    name = name,
                    temperature = temp,
                    onClick = { 
                        onTemperatureChange(temp)
                        onSceneSelect(null)
                    }
                )
            }
        }

        // Temperature Slider
        TemperatureSlider(
            temperature = bulb.temperature,
            onTemperatureChange = { 
                onTemperatureChange(it)
                onSceneSelect(null)
            }
        )

        Divider(color = Color(0xFF2A2A2A))

        // Functional Modes
        Text(
            "Functional",
            color = Color.White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        val functionalScenes = listOf(
            "nightlight" to "🌙 Night Light",
            "cozy" to "☕ Cozy",
            "focus" to "🎯 Focus",
            "relax" to "🧘 Relax"
        )

        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(functionalScenes) { (sceneId, sceneName) ->
                SceneCard(
                    name = sceneName,
                    onClick = { onSceneSelect(sceneId) }
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Brightness Slider
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Brightness",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "${bulb.brightness.toInt()}%",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Slider(
                value = bulb.brightness,
                onValueChange = onBrightnessChange,
                valueRange = 10f..100f,
                colors = SliderDefaults.colors(
                    thumbColor = Color.White,
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0xFF2A2A2A)
                )
            )
        }
    }
}

@Composable
fun SceneCard(
    name: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                name,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(8.dp)
            )
        }
    }
}

@Composable
fun TemperaturePresetButton(
    name: String,
    temperature: Int,
    onClick: () -> Unit
) {
    val color = when {
        temperature < 3000 -> Color(0xFFFF9800)
        temperature < 4500 -> Color(0xFFFFE082)
        temperature < 6000 -> Color(0xFFEEEEEE)
        else -> Color(0xFFBBDEFB)
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.3f))
                .padding(4.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            name,
            fontSize = 11.sp,
            color = Color.Gray,
            fontWeight = FontWeight.Medium
        )
    }
}
