package com.suvojeet.smartcontrol

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.viewmodel.compose.viewModel
import com.suvojeet.smartcontrol.ui.SmartControlNavigation
import com.suvojeet.smartcontrol.ui.theme.SmartControlTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            SmartControlTheme {
                // ViewModel yahan initialize hoga
                val viewModel: HomeViewModel = viewModel()
                
                // Screen ko data aur actions pass kar rahe hain
                SmartControlNavigation(viewModel = viewModel)
            }
        }
    }
}