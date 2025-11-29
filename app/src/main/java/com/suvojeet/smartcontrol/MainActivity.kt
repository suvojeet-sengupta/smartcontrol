package com.suvojeet.smartcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.smartcontrol.ui.WizHomeScreen
import com.suvojeet.smartcontrol.ui.theme.SmartControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SmartControlTheme {
                // ViewModel yahan initialize hoga
                val viewModel: HomeViewModel = viewModel()
                
                // Screen ko data aur actions pass kar rahe hain
                WizHomeScreen(
                    bulbs = viewModel.bulbs,
                    onAddBulb = { name, ip -> viewModel.addBulb(name, ip) },
                    onDeleteBulb = { id -> viewModel.deleteBulb(id) },
                    onToggle = { id -> viewModel.toggleBulb(id) },
                    onBrightness = { id, value -> viewModel.updateBrightness(id, value) },
                    onColor = { id, color -> viewModel.updateColor(id, color) }
                )
            }
        }
    }
}