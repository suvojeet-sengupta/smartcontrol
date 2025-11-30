package com.suvojeet.smartcontrol.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.suvojeet.smartcontrol.BulbGroup
import com.suvojeet.smartcontrol.WizBulb

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BulbListScreen(
    bulbs: List<WizBulb>,
    groups: List<BulbGroup> = emptyList(),
    onNavigateToSetup: () -> Unit,
    onNavigateToCreateGroup: () -> Unit,
    onNavigateToEnergyDashboard: () -> Unit,
    onDeleteBulbs: (List<String>) -> Unit,
    onToggleBulb: (String) -> Unit,
    onBrightnessChange: (String, Float) -> Unit,
    onNavigateToDetail: (String) -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    onToggleGroup: (String) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) } // 0 = Bulbs, 1 = Groups
    val tabs = listOf("Bulbs", "Groups")
    
    var selectedBulbIds by remember { mutableStateOf(setOf<String>()) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    val isSelectionMode = selectedBulbIds.isNotEmpty()

    Scaffold(
        containerColor = Color(0xFF0A0A0A),
        topBar = {
            TopAppBar(
                title = { 
                    if (isSelectionMode) {
                        Text(
                            "${selectedBulbIds.size} Selected",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )
                    } else {
                        Text(
                            "Smart Control",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedBulbIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel Selection")
                        }
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    } else {
                        IconButton(onClick = onNavigateToEnergyDashboard) {
                            Icon(Icons.Default.Bolt, contentDescription = "Energy Dashboard")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0A0A0A),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            if (!isSelectionMode) {
                FloatingActionButton(
                    onClick = { 
                        if (selectedTab == 0) onNavigateToSetup() else onNavigateToCreateGroup()
                    },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF0A0A0A),
                contentColor = Color.White,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = Color(0xFF4CAF50)
                        )
                    }
                },
                divider = {}
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { 
                            Text(
                                title,
                                fontSize = 16.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            ) 
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (selectedTab == 0) {
                // Bulbs List
                if (bulbs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = Color.Gray,
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No bulbs added yet",
                                color = Color.Gray,
                                fontSize = 18.sp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap + to add your first bulb",
                                color = Color.DarkGray,
                                fontSize = 14.sp
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(bulbs, key = { it.id }) { bulb ->
                            val isSelected = selectedBulbIds.contains(bulb.id)
                            BulbCard(
                                bulb = bulb,
                                isSelectionMode = isSelectionMode,
                                isSelected = isSelected,
                                onToggle = { onToggleBulb(bulb.id) },
                                onBrightnessChange = { newBrightness -> onBrightnessChange(bulb.id, newBrightness) },
                                onClick = {
                                    if (isSelectionMode) {
                                        if (isSelected) {
                                            selectedBulbIds = selectedBulbIds - bulb.id
                                        } else {
                                            selectedBulbIds = selectedBulbIds + bulb.id
                                        }
                                    } else {
                                        onNavigateToDetail(bulb.id)
                                    }
                                },
                                onLongClick = {
                                    if (!isSelectionMode) {
                                        selectedBulbIds = selectedBulbIds + bulb.id
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                // Groups List
                if (groups.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No groups created.\nTap + to create one.",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(groups) { group ->
                            GroupCard(
                                group = group,
                                onToggle = { onToggleGroup(group.id) },
                                onClick = { onNavigateToGroupDetail(group.id) }
                            )
                        }
                    }
                }
            }
        }

        if (showDeleteConfirmation) {
            AlertDialog(
                onDismissRequest = { showDeleteConfirmation = false },
                containerColor = Color(0xFF1A1A1A),
                title = { Text("Delete Devices?", color = Color.White, fontWeight = FontWeight.Bold) },
                text = { Text("Are you sure you want to delete ${selectedBulbIds.size} selected device(s)?", color = Color.Gray) },
                confirmButton = {
                    Button(
                        onClick = {
                            onDeleteBulbs(selectedBulbIds.toList())
                            selectedBulbIds = emptySet()
                            showDeleteConfirmation = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                    ) {
                        Text("Delete", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteConfirmation = false }) {
                        Text("Cancel", color = Color.Gray)
                    }
                }
            )
        }
    }
}

@Composable
fun GroupCard(
    group: BulbGroup,
    onToggle: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1A1A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    group.name,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${group.bulbIds.size} lights",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            IconButton(
                onClick = onToggle,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(if (group.isOn) Color.White else Color(0xFF2A2A2A))
            ) {
                Icon(
                    Icons.Default.PowerSettingsNew,
                    contentDescription = "Power",
                    tint = if (group.isOn) Color.Black else Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BulbCard(
    bulb: WizBulb,
    isSelectionMode: Boolean,
    isSelected: Boolean,
    onToggle: () -> Unit,
    onBrightnessChange: (Float) -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isAvailable = bulb.isAvailable
    
    // Haptic feedback for that physical dimmer feel! 📳
    val haptic = LocalHapticFeedback.current
    
    // Gesture handling for brightness
    var accumulatedDrag by remember { mutableStateOf(0f) }
    var isDragging by remember { mutableStateOf(false) }
    var dragBrightness by remember { mutableStateOf<Float?>(null) }
    
    val cyanColor = Color(0xFF03A9F4)
    val cardBackgroundColor = Color(0xFF111729) // Darker blue-grey background
    
    // Determine brightness to display: local drag value or actual bulb value
    val displayBrightness = dragBrightness ?: bulb.brightness
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .combinedClickable(
                enabled = isAvailable || isSelectionMode,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .pointerInput(Unit) {
                if (isAvailable && !isSelectionMode && bulb.isOn) {
                    detectHorizontalDragGestures(
                        onDragStart = { 
                            isDragging = true 
                            dragBrightness = bulb.brightness
                            // Haptic feedback on drag start 👌
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        },
                        onDragEnd = { 
                            isDragging = false 
                            accumulatedDrag = 0f
                            dragBrightness?.let { finalBrightness ->
                                onBrightnessChange(finalBrightness)
                            }
                            dragBrightness = null
                        },
                        onDragCancel = { 
                            isDragging = false 
                            accumulatedDrag = 0f 
                            dragBrightness = null
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        accumulatedDrag += dragAmount
                        
                        // Sensitivity: 2px drag = 1% brightness change
                        if (kotlin.math.abs(accumulatedDrag) > 2) {
                            val changeAmount = (accumulatedDrag / 2).toInt()
                            val currentBase = dragBrightness ?: bulb.brightness
                            val newBrightness = (currentBase + changeAmount).coerceIn(10f, 100f)
                            
                            if (newBrightness != currentBase) {
                                dragBrightness = newBrightness
                                accumulatedDrag = 0f
                                // Haptic feedback on brightness change ✨
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }
                    }
                }
            },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A2A2A) else cardBackgroundColor
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF4CAF50)) else null
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            
            if (isDragging && isAvailable && bulb.isOn) {
                // Drag State UI
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction = displayBrightness / 100f)
                        .background(cyanColor)
                )
                
                // Percentage Text
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${displayBrightness.toInt()}%",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Normal State UI
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            bulb.name,
                            color = if (isAvailable) Color.White else Color.Gray,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (isAvailable) bulb.ipAddress else "Offline",
                            color = if (isAvailable) Color.Gray else Color.Red,
                            fontSize = 14.sp
                        )
                    }

                    IconButton(
                        onClick = onToggle,
                        enabled = isAvailable,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (bulb.isOn) Color.White 
                                else if (isAvailable) Color(0xFF2A2A2A) 
                                else Color.Transparent
                            )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PowerSettingsNew,
                            contentDescription = "Power",
                            tint = if (bulb.isOn) Color.Black else Color.Gray
                        )
                    }
                }
            }
        }
    }
}
