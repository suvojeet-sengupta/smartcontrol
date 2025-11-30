package com.suvojeet.smartcontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.suvojeet.smartcontrol.BulbGroup
import com.suvojeet.smartcontrol.WizBulb

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    group: BulbGroup,
    onNavigateBack: () -> Unit,
    onToggleGroup: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onColorChange: (Color) -> Unit,
    onTemperatureChange: (Int) -> Unit,
    onSceneSelect: (String?) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Color", "Dynamic", "White")

    // Create a dummy bulb object to reuse existing Tab Composables
    // This is a bit of a hack to avoid duplicating all the UI logic
    // In a real app, we might refactor the tabs to take simple parameters instead of a WizBulb object
    val dummyBulb = WizBulb(
        id = group.id,
        name = group.name,
        ipAddress = "",
        isOn = group.isOn,
        brightness = group.brightness,
        // We don't track color/temp/scene on the group object itself perfectly yet, 
        // so we might default these or need to add them to BulbGroup if we want UI state persistence
        // For now, we'll just use defaults or what's passed in if we enhanced BulbGroup
    )

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        group.name,
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
                            .background(if (group.isOn) Color.White else Color(0xFF2A2A2A))
                            .clickable(onClick = onToggleGroup),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (group.isOn) Color.Black else Color.Gray
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
            // Reusing the tabs from BulbDetailScreen. 
            // Note: This requires BulbDetailScreen.kt to expose these composables (which it does).
            when (selectedTab) {
                0 -> ColorTab(dummyBulb, onColorChange, onBrightnessChange, onSceneSelect)
                1 -> DynamicTab(null, onSceneSelect) // Group doesn't track current scene yet
                2 -> WhiteTab(dummyBulb, onTemperatureChange, onBrightnessChange, onSceneSelect)
            }
        }
    }
}
