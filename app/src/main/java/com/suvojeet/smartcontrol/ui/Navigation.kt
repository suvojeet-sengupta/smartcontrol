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
    object Setup : Screen("setup")
    object BulbDetail : Screen("bulb_detail/{bulbId}") {
        fun createRoute(bulbId: String) = "bulb_detail/$bulbId"
    }
    object CreateGroup : Screen("create_group")
    object GroupDetail : Screen("group_detail/{groupId}") {
        fun createRoute(groupId: String) = "group_detail/$groupId"
    }
    object EnergyDashboard : Screen("energy_dashboard")
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
                groups = viewModel.groups,
                onNavigateToSetup = { navController.navigate(Screen.Setup.route) },
                onNavigateToCreateGroup = { navController.navigate(Screen.CreateGroup.route) },
                onDeleteBulbs = viewModel::deleteBulbs,
                onToggleBulb = viewModel::toggleBulb,
                onBrightnessChange = viewModel::updateBrightness,
                onNavigateToDetail = { bulbId ->
                    navController.navigate(Screen.BulbDetail.createRoute(bulbId))
                },
                onNavigateToGroupDetail = { groupId ->
                    navController.navigate(Screen.GroupDetail.createRoute(groupId))
                },
                onToggleGroup = viewModel::toggleGroup
            )
        }

        composable(Screen.Setup.route) {
            SetupScreen(
                onNavigateBack = { 
                    viewModel.resetDiscovery()
                    navController.popBackStack() 
                },
                onAddBulb = viewModel::addBulb,
                discoveryState = viewModel.discoveryState,
                discoveredBulbs = viewModel.discoveredBulbs,
                onStartDiscovery = viewModel::startDiscovery,
                onAddDiscoveredBulb = viewModel::addDiscoveredBulb,
                onAddAllDiscovered = viewModel::addAllDiscoveredBulbs
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

        composable(Screen.CreateGroup.route) {
            CreateGroupScreen(
                bulbs = viewModel.bulbs,
                onNavigateBack = { navController.popBackStack() },
                onCreateGroup = viewModel::createGroup
            )
        }

        composable(
            route = Screen.GroupDetail.route,
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
            val group = viewModel.groups.find { it.id == groupId } ?: return@composable

            GroupDetailScreen(
                group = group,
                onNavigateBack = { navController.popBackStack() },
                onToggleGroup = { viewModel.toggleGroup(groupId) },
                onBrightnessChange = { value -> viewModel.updateGroupBrightness(groupId, value) },
                onColorChange = { color -> viewModel.updateGroupColor(groupId, color) },
                onTemperatureChange = { temp -> viewModel.updateGroupTemperature(groupId, temp) },
                onSceneSelect = { scene -> viewModel.updateGroupScene(groupId, scene) }
            )
        }

        composable(Screen.EnergyDashboard.route) {
            EnergyDashboardScreen(
                navController = navController,
                viewModel = viewModel
            )
        }
    }
}
