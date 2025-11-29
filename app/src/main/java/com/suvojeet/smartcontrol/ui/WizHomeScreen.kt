package com.suvojeet.smartcontrol.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.PowerSettingsNew
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WizHomeScreen(
    bulbs: List<WizBulb>,
    onAddBulb: (String, String) -> Unit,
    onDeleteBulb: (String) -> Unit,
    onToggle: (String) -> Unit,
    onBrightness: (String, Float) -> Unit,
    onColor: (String, Color) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            TopAppBar(
                title = { Text("Suvojeet's Control", color = Color.White) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1E1E1E))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = Color(0xFF64B5F6)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Bulb")
            }
        }
    ) { padding ->
        
        if (bulbs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No bulbs found. Add one! ➕", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(bulbs) { bulb ->
                    BulbControlCard(bulb, onToggle, onBrightness, onColor, onDeleteBulb)
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
    }
}

@Composable
fun AddBulbDialog(onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add New Device") },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Device Name") }
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address (e.g. 192.168.1.5)") }
                )
            }
        },
        confirmButton = {
            Button(onClick = { if (name.isNotEmpty() && ip.isNotEmpty()) onConfirm(name, ip) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun BulbControlCard(
    bulb: WizBulb,
    onToggle: (String) -> Unit,
    onBrightness: (String, Float) -> Unit,
    onColor: (String, Color) -> Unit,
    onDelete: (String) -> Unit
) {
    val bulbColor = bulb.getComposeColor()
    
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = if(bulb.isOn) bulbColor else Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(bulb.name, color = Color.White, fontWeight = FontWeight.Bold)
                    Text(bulb.ipAddress, color = Color.Gray, fontSize = 12.sp)
                }
                
                IconButton(onClick = { onDelete(bulb.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Gray)
                }

                IconButton(
                    onClick = { onToggle(bulb.id) },
                    modifier = Modifier.background(if(bulb.isOn) Color.White else Color.DarkGray, CircleShape)
                ) {
                    Icon(Icons.Default.PowerSettingsNew, contentDescription = null, tint = if(bulb.isOn) Color.Black else Color.White)
                }
            }

            if (bulb.isOn) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Brightness", color = Color.Gray, fontSize = 12.sp)
                Slider(
                    value = bulb.brightness,
                    onValueChange = { onBrightness(bulb.id, it) },
                    valueRange = 10f..100f,
                    colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = bulbColor)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    val colors = listOf(Color.White, Color.Red, Color.Green, Color.Blue, Color(0xFFFFD54F))
                    colors.forEach { c ->
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(c).clickable { onColor(bulb.id, c) }
                        )
                    }
                }
            }
        }
    }
}