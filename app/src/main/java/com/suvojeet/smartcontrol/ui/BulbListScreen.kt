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
import com.suvojeet.smartcontrol.WizBulb

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BulbListScreen(
    bulbs: List<WizBulb>,
    onAddBulb: (String, String) -> Unit,
    onDeleteBulbs: (List<String>) -> Unit,
    onToggleBulb: (String) -> Unit,
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
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val isAvailable = bulb.isAvailable
    val cardAlpha = if (isAvailable) 1f else 0.6f

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                enabled = isAvailable || isSelectionMode,
                onClick = onClick,
                onLongClick = onLongClick
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFF2A2A2A) else Color(0xFF1A1A1A)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = if (isSelected) BorderStroke(2.dp, Color(0xFF4CAF50)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selection Indicator or Bulb Icon
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        if (isSelectionMode) {
                            if (isSelected) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFF2A2A2A)
                        } else {
                            if (isAvailable && bulb.isOn) bulb.getComposeColor().copy(alpha = 0.2f)
                            else Color(0xFF2A2A2A)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelectionMode) {
                    Icon(
                        imageVector = if (isSelected) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                        contentDescription = null,
                        tint = if (isSelected) Color(0xFF4CAF50) else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = if (isAvailable && bulb.isOn) bulb.getComposeColor() else Color.Gray,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Bulb info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = bulb.name,
                    color = if (isAvailable) Color.White else Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                
                if (isAvailable) {
                    Text(
                        text = bulb.ipAddress,
                        color = Color.Gray,
                        fontSize = 13.sp
                    )
                    if (bulb.isOn) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Brightness: ${bulb.brightness.toInt()}%",
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                } else {
                    Text(
                        text = "Device Unavailable",
                        color = Color.Red.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Power button (only show if not in selection mode)
            if (!isSelectionMode) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(
                            if (isAvailable && bulb.isOn) Color.White
                            else Color(0xFF2A2A2A)
                        )
                        .clickable(enabled = isAvailable, onClick = onToggle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PowerSettingsNew,
                        contentDescription = "Power",
                        tint = if (isAvailable && bulb.isOn) Color.Black else Color.Gray,
                        modifier = Modifier.size(28.dp)
                    )
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
