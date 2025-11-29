package com.suvojeet.smartcontrol.ui

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.suvojeet.smartcontrol.HomeViewModel

sealed class Screen(val route: String) {
    object BulbList : Screen("bulb_list")
    object BulbDetail : Screen("bulb_detail/{bulbId}") {
        fun createRoute(bulbId: String) = "bulb_detail/$bulbId"
    }
}

@Composable
fun SmartControlNavigation(viewModel: HomeViewModel = viewModel()) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.BulbList.route
    ) {
        composable(Screen.BulbList.route) {
            BulbListScreen(
                bulbs = viewModel.bulbs,
                onAddBulb = { name, ip -> viewModel.addBulb(name, ip) },
                onDeleteBulb = { id -> viewModel.deleteBulb(id) },
                onToggleBulb = { id -> viewModel.toggleBulb(id) },
                onNavigateToDetail = { bulbId ->
                    navController.navigate(Screen.BulbDetail.createRoute(bulbId))
                }
            )
        }

        composable(
            route = Screen.BulbDetail.route,
            arguments = listOf(navArgument("bulbId") { type = NavType.StringType })
        ) { backStackEntry ->
            val bulbId = backStackEntry.arguments?.getString("bulbId") ?: return@composable
            val bulb = viewModel.bulbs.find { it.id == bulbId } ?: return@composable

            BulbDetailScreen(
                bulb = bulb,
                onNavigateBack = { navController.popBackStack() },
                onToggleBulb = { viewModel.toggleBulb(bulbId) },
                onBrightnessChange = { value -> viewModel.updateBrightness(bulbId, value) },
                onColorChange = { color -> viewModel.updateColor(bulbId, color) },
                onTemperatureChange = { temp -> viewModel.updateTemperature(bulbId, temp) },
                onSceneSelect = { scene -> viewModel.updateScene(bulbId, scene) }
            )
        }
    }
}
