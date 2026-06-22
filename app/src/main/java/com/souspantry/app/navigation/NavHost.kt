package com.souspantry.app.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Kitchen
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.souspantry.app.ui.home.HomeScreen
import com.souspantry.app.ui.onboarding.OnboardingScreen
import com.souspantry.app.ui.onboarding.OnboardingViewModel
import com.souspantry.app.ui.pantry.BarcodeScanScreen
import com.souspantry.app.ui.pantry.PantryScreen
import com.souspantry.app.ui.ereceipt.EReceiptSyncScreen
import com.souspantry.app.ui.pantry.ReceiptScanScreen
import com.souspantry.app.ui.plancook.PlanCookScreen
import com.souspantry.app.ui.account.AccountScreen
import com.souspantry.app.ui.shopping.ShoppingScreen
import com.souspantry.app.ui.theme.Green
import com.souspantry.app.ui.theme.White

sealed class Screen(val route: String, val label: String, val icon: ImageVector? = null) {
    data object Home      : Screen("home",      "Home",        Icons.Filled.Home)
    data object Pantry    : Screen("pantry",    "Pantry",      Icons.Filled.Kitchen)
    data object PlanCook  : Screen("plancook",  "Plan & Cook", Icons.Filled.MenuBook)
    data object Shopping  : Screen("shopping",  "Shopping",    Icons.Filled.ShoppingCart)
    data object Account   : Screen("account",   "Account",     Icons.Filled.AccountCircle)
    data object Barcode   : Screen("barcode",   "Scan Barcode")
    data object Receipt   : Screen("receipt",   "Scan Receipt")
    data object EReceipt  : Screen("ereceipt",  "eReceipt Sync")
    data object Onboarding: Screen("onboarding","Onboarding")
    data object Auth      : Screen("auth",      "Sign In")
    data object Paywall   : Screen("paywall",   "Premium")
}

private val bottomNavItems = listOf(Screen.Home, Screen.Pantry, Screen.PlanCook, Screen.Shopping, Screen.Account)

@Composable
fun SousPantryNavHost() {
    val onboardingVm: OnboardingViewModel = hiltViewModel()
    val onboardingDone by onboardingVm.onboardingDone.collectAsState()
    val startDest = if (onboardingDone) Screen.Home.route else Screen.Onboarding.route
    val navController = rememberNavController()
    val navBackStack  by navController.currentBackStackEntryAsState()
    val currentDest   = navBackStack?.destination
    val showBottomBar = currentDest?.route in bottomNavItems.map { it.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = White) {
                    bottomNavItems.forEach { screen ->
                        val selected = currentDest?.hierarchy?.any { it.route == screen.route } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon   = { Icon(screen.icon!!, screen.label) },
                            label  = { Text(screen.label) },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor   = Green, selectedTextColor   = Green,
                                unselectedIconColor = Green.copy(alpha = 0.45f),
                                unselectedTextColor = Green.copy(alpha = 0.45f),
                                indicatorColor      = White,
                            )
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = startDest,
            modifier         = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Home.route)     {
                HomeScreen(
                    onNavigateToTab = { route ->
                        navController.navigate(route) {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                            restoreState    = true
                        }
                    },
                )
            }
            composable(Screen.Pantry.route)   { PantryScreen(
                onBarcodeScan  = { navController.navigate(Screen.Barcode.route) },
                onReceiptScan  = { navController.navigate(Screen.Receipt.route) },
                onSyncEreceipt = { navController.navigate(Screen.EReceipt.route) },
            ) }
            composable(Screen.PlanCook.route) { PlanCookScreen() }
            composable(Screen.Shopping.route) { ShoppingScreen() }
            composable(Screen.Account.route)  { AccountScreen() }
            composable(Screen.Barcode.route) {
                BarcodeScanScreen(
                    onDismiss = { navController.popBackStack() },
                    onSaved   = { navController.popBackStack() },
                )
            }
            composable(Screen.Receipt.route) {
                ReceiptScanScreen(
                    onDismiss = { navController.popBackStack() },
                    onSaved   = { navController.popBackStack() },
                )
            }
            composable(Screen.EReceipt.route) {
                EReceiptSyncScreen(onBack = { navController.popBackStack() })
            }
            composable(Screen.Onboarding.route) {
                OnboardingScreen(onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.Auth.route)     { /* Task 7 — AuthScreen */ }
            composable(Screen.Paywall.route)  { /* Task 8 — PaywallScreen */ }
        }
    }
}
