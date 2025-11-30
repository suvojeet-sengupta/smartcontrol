package com.suvojeet.smartcontrol.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.suvojeet.smartcontrol.DiscoveryState
import com.suvojeet.smartcontrol.network.DiscoveredBulb

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    onNavigateBack: () -> Unit,
    onAddBulb: (String, String) -> Unit,
    discoveryState: DiscoveryState,
    discoveredBulbs: List<DiscoveredBulb>,
    onStartDiscovery: () -> Unit,
    onAddDiscoveredBulb: (DiscoveredBulb) -> Unit,
    onAddAllDiscovered: () -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var bulbName by remember { mutableStateOf("") }
    var bulbIp by remember { mutableStateOf("") }
    
    // Permission handling
    val context = LocalContext.current
    val permissionsToRequest = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
        arrayOf(
            android.Manifest.permission.BLUETOOTH_SCAN,
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.BLUETOOTH_ADMIN
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            onStartDiscovery()
        }
    }

    Scaffold(
        containerColor = Color.Black,
        topBar = {
            TopAppBar(
                title = { Text("Add Device", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    slideInHorizontally { width -> width } + fadeIn() with
                            slideOutHorizontally { width -> -width } + fadeOut()
                }
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(onNext = { currentStep = 1 })
                    1 -> DiscoveryStep(
                        discoveryState = discoveryState,
                        discoveredBulbs = discoveredBulbs,
                        onStartDiscovery = { permissionLauncher.launch(permissionsToRequest) },
                        onAddBulb = onAddDiscoveredBulb,
                        onAddAll = {
                            onAddAllDiscovered()
                            currentStep = 3
                        },
                        onSkip = { currentStep = 2 }
                    )
                    2 -> ConnectStep(
                        name = bulbName,
                        ip = bulbIp,
                        onNameChange = { bulbName = it },
                        onIpChange = { bulbIp = it },
                        onConnect = {
                            if (bulbName.isNotEmpty() && bulbIp.isNotEmpty()) {
                                onAddBulb(bulbName, bulbIp)
                                currentStep = 3
                            }
                        }
                    )
                    3 -> SuccessStep(onFinish = onNavigateBack)
                }
            }
        }
    }
}

@Composable
fun WelcomeStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.Lightbulb,
            contentDescription = null,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Add New Device",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Let's get your new smart bulb set up and ready to use.",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Start Setup", fontSize = 18.sp)
        }
    }
}

@Composable
fun DiscoveryStep(
    discoveryState: DiscoveryState,
    discoveredBulbs: List<DiscoveredBulb>,
    onStartDiscovery: () -> Unit,
    onAddBulb: (DiscoveredBulb) -> Unit,
    onAddAll: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
    ) {
        // Header
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xFF00BCD4),
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            "Find Devices",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Scanning via Wi-Fi and Bluetooth...",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Main content based on state
        when (discoveryState) {
            is DiscoveryState.Idle -> {
                Button(
                    onClick = onStartDiscovery,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Network", fontSize = 18.sp)
                }
            }
            
            is DiscoveryState.Scanning -> {
                CircularProgressIndicator(
                    color = Color(0xFF00BCD4),
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Scanning...",
                    color = Color.White,
                    fontSize = 16.sp
                )
            }
            
            is DiscoveryState.Success -> {
                // Show discovered bulbs
                LazyColumn(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(discoveredBulbs) { bulb ->
                        DiscoveredBulbCard(
                            bulb = bulb,
                            onAdd = { onAddBulb(bulb) }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                if (discoveredBulbs.isNotEmpty()) {
                    Button(
                        onClick = onAddAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        Text("Add All (${discoveredBulbs.size})", fontSize = 18.sp)
                    }
                } else {
                    Text(
                        "All discovered devices have been added!",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
            
            is DiscoveryState.NoDevicesFound -> {
                Icon(
                    imageVector = Icons.Default.Lightbulb,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "No devices found",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Make sure your bulbs are powered on.",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onStartDiscovery,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Try Again", fontSize = 18.sp)
                }
            }
            
            is DiscoveryState.Error -> {
                Text(
                    "Error: ${discoveryState.message}",
                    color = Color.Red,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onStartDiscovery,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Try Again", fontSize = 18.sp)
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Skip button
        TextButton(onClick = onSkip) {
            Text("Enter Manually", color = Color.Gray)
        }
    }
}

@Composable
fun DiscoveredBulbCard(
    bulb: DiscoveredBulb,
    onAdd: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00BCD4).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (bulb.isBle) Icons.Default.Bluetooth else Icons.Default.Wifi,
                        contentDescription = null,
                        tint = Color(0xFF00BCD4),
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column {
                    Text(
                        bulb.name,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        if (bulb.isBle) "Bluetooth" else bulb.ipAddress,
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
            
            IconButton(
                onClick = onAdd,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color(0xFF4CAF50)
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add",
                    tint = Color.White
                )
            }
        }
    }
}

@Composable
fun InstallStep(onNext: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .clip(CircleShape)
                .background(Color(0xFF1A1A1A)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = null,
                tint = Color.Yellow,
                modifier = Modifier.size(80.dp)
            )
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Install Device",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Screw in your bulb and turn on the power switch.",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Next", fontSize = 18.sp)
        }
    }
}

@Composable
fun ConnectStep(
    name: String,
    ip: String,
    onNameChange: (String) -> Unit,
    onIpChange: (String) -> Unit,
    onConnect: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            "Connect",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(32.dp))
        
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Device Name") },
            placeholder = { Text("e.g. Living Room") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00BCD4),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00BCD4),
                unfocusedLabelColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedTextField(
            value = ip,
            onValueChange = onIpChange,
            label = { Text("IP Address") },
            placeholder = { Text("e.g. 192.168.1.10") },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color(0xFF00BCD4),
                unfocusedBorderColor = Color.Gray,
                focusedLabelColor = Color(0xFF00BCD4),
                unfocusedLabelColor = Color.Gray
            ),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        Button(
            onClick = onConnect,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4)),
            enabled = name.isNotEmpty() && ip.isNotEmpty(),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Connect", fontSize = 18.sp)
        }
    }
}

@Composable
fun SuccessStep(onFinish: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            tint = Color(0xFF4CAF50),
            modifier = Modifier.size(120.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Setup Complete!",
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your device has been added successfully.",
            color = Color.Gray,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onFinish,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Finish", fontSize = 18.sp)
        }
    }
}
