package com.souspantry.app.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
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
import com.souspantry.app.ui.onboarding.AppGate
import com.souspantry.app.ui.onboarding.OnboardingViewModel
import com.souspantry.app.ui.pantry.BarcodeScanScreen
import com.souspantry.app.ui.pantry.PantryScreen
import com.souspantry.app.ui.ereceipt.EReceiptSyncScreen
import com.souspantry.app.ui.pantry.ReceiptScanScreen
import com.souspantry.app.ui.plancook.PlanCookScreen
import com.souspantry.app.ui.account.AccountScreen
import com.souspantry.app.ui.auth.AuthScreen
import com.souspantry.app.ui.founder.FounderNoteScreen
import com.souspantry.app.ui.paywall.PaywallScreen
import com.souspantry.app.ui.setup.SetupWizardScreen
import com.souspantry.app.ui.shopping.ShoppingScreen
import com.souspantry.app.ui.theme.Green
import com.souspantry.app.ui.theme.Navy
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
    data object SetupWizard: Screen("setupwizard","Setup")
    data object FounderNote: Screen("foundernote","Note")
    data object Auth      : Screen("auth",      "Sign In")
    data object Paywall   : Screen("paywall",   "Premium")
}

private val bottomNavItems = listOf(Screen.Home, Screen.Pantry, Screen.PlanCook, Screen.Shopping, Screen.Account)

@Composable
fun SousPantryNavHost() {
    val onboardingVm: OnboardingViewModel = hiltViewModel()
    val gate by onboardingVm.gate.collectAsState()

    // Wait until the persisted flags are actually known before composing the
    // NavHost — its startDestination is captured once, so gating on still-loading
    // defaults would make the wizard show/skip unreliably. Returning users who
    // finished setup have onboardingDone=true persisted, so they go straight to
    // Home and never see the wizard again.
    when (val g = gate) {
        AppGate.Loading   -> SplashScreen()
        is AppGate.Ready  -> SousPantryAppScaffold(
            startDest      = when {
                !g.signedIn        -> Screen.Auth.route
                !g.onboardingDone  -> Screen.SetupWizard.route
                !g.founderNoteSeen -> Screen.FounderNote.route
                else               -> Screen.Home.route
            },
            onboardingDone  = g.onboardingDone,
            founderNoteSeen = g.founderNoteSeen,
        )
    }
}

@Composable
private fun SplashScreen() {
    Box(modifier = Modifier.fillMaxSize().background(Navy), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = White)
    }
}

@Composable
private fun SousPantryAppScaffold(startDest: String, onboardingDone: Boolean, founderNoteSeen: Boolean) {
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
            composable(Screen.Account.route)  {
                AccountScreen(
                    onLoggedOut      = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }   // clear the whole back stack
                        }
                    },
                    onAccountDeleted = {
                        navController.navigate(Screen.Auth.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onUpgrade        = { navController.navigate(Screen.Paywall.route) },
                )
            }
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
            composable(Screen.Auth.route) {
                AuthScreen(onSignedIn = {
                    // After sign-in, run the setup wizard (first run) or go straight Home.
                    val dest = if (onboardingDone) Screen.Home.route else Screen.SetupWizard.route
                    navController.navigate(dest) {
                        popUpTo(Screen.Auth.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.SetupWizard.route) {
                SetupWizardScreen(onComplete = {
                    // Wizard done → founder note (first run) or straight Home.
                    val dest = if (founderNoteSeen) Screen.Home.route else Screen.FounderNote.route
                    navController.navigate(dest) {
                        popUpTo(Screen.SetupWizard.route) { inclusive = true }
                    }
                })
            }
            composable(Screen.FounderNote.route) {
                FounderNoteScreen(
                    onProceed = {
                        // "Let's get started" → paywall, then Home.
                        navController.navigate(Screen.Paywall.route)
                    },
                    onSkip = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(Screen.FounderNote.route) { inclusive = true }
                        }
                    },
                )
            }
            composable(Screen.Paywall.route) {
                PaywallScreen(
                    onPurchased = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }   // clear setup flow from the back stack
                        }
                    },
                    onDismiss = {
                        navController.navigate(Screen.Home.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                )
            }
        }
    }
}
