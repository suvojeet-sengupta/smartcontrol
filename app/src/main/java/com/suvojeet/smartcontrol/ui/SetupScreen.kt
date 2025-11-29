package com.suvojeet.smartcontrol.ui

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun SetupScreen(
    onNavigateBack: () -> Unit,
    onAddBulb: (String, String) -> Unit
) {
    var currentStep by remember { mutableStateOf(0) }
    var bulbName by remember { mutableStateOf("") }
    var bulbIp by remember { mutableStateOf("") }

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
                    1 -> InstallStep(onNext = { currentStep = 2 })
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
