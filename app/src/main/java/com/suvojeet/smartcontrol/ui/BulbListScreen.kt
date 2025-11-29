package com.suvojeet.smartcontrol.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.suvojeet.smartcontrol.WizBulb

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BulbListScreen(
    bulbs: List<WizBulb>,
    onAddBulb: (String, String) -> Unit,
    onDeleteBulbs: (List<String>) -> Unit,
    onToggleBulb: (String) -> Unit,
    onBrightnessChange: (String, Float) -> Unit,
    onNavigateToDetail: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
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
                navigationIcon = {
                    if (isSelectionMode) {
                        IconButton(onClick = { selectedBulbIds = emptySet() }) {
                            Icon(Icons.Default.Close, contentDescription = "Close Selection", tint = Color.White)
                        }
                    }
                },
                actions = {
                    if (isSelectionMode) {
                        IconButton(onClick = { showDeleteConfirmation = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Selected", tint = Color.White)
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
                    onClick = { showAddDialog = true },
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Bulb")
                }
            }
        }
    ) { padding ->
        
        if (bulbs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
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
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
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

        if (showAddDialog) {
            AddBulbDialog(
                onDismiss = { showAddDialog = false },
                onConfirm = { name, ip ->
                    onAddBulb(name, ip)
                    showAddDialog = false
                }
            )
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
                
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Brightness",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "${displayBrightness.toInt()}%",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                // Idle State UI
                
                // Bottom Progress Bar
                if (isAvailable && bulb.isOn) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .fillMaxWidth(fraction = displayBrightness / 100f)
                            .height(6.dp)
                            .background(cyanColor)
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Icon
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                         if (isSelectionMode) {
                            Icon(
                                imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                                contentDescription = null,
                                tint = if (isSelected) Color(0xFF4CAF50) else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                             Icon(
                                imageVector = Icons.Default.Lightbulb,
                                contentDescription = null,
                                tint = if (isAvailable && bulb.isOn) Color.White else Color.Gray,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Name
                    Text(
                        text = bulb.name,
                        color = if (isAvailable) Color.White else Color.Gray,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )

                    // Switch
                    if (!isSelectionMode) {
                        Switch(
                            checked = isAvailable && bulb.isOn,
                            onCheckedChange = { onToggle() },
                            enabled = isAvailable,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = cyanColor,
                                checkedTrackColor = cyanColor.copy(alpha = 0.3f),
                                uncheckedThumbColor = Color.Gray,
                                uncheckedTrackColor = Color.DarkGray,
                                uncheckedBorderColor = Color.Gray
                            )
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddBulbDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1A1A),
        title = { 
            Text(
                "Add New Bulb",
                color = Color.White,
                fontWeight = FontWeight.Bold
            ) 
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Bulb Name") },
                    placeholder = { Text("Living Room") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF4CAF50),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    placeholder = { Text("192.168.1.5") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF4CAF50),
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color(0xFF4CAF50),
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { 
                    if (name.isNotEmpty() && ip.isNotEmpty()) {
                        onConfirm(name, ip)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF4CAF50),
                    contentColor = Color.White
                ),
                enabled = name.isNotEmpty() && ip.isNotEmpty()
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color.Gray
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
