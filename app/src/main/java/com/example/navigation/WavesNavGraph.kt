package com.example.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.components.StateScreen
import com.example.components.StateType
import com.example.components.WavesNavTab
import com.example.data.FirebaseAuthRepository
import com.example.screens.AddEditClientScreen
import com.example.screens.AddEditProductScreen
import com.example.screens.BankAccountScreen
import com.example.screens.BusinessProfileScreen
import com.example.screens.ClientDetailScreen
import com.example.screens.ClientListScreen
import com.example.screens.CreateInvoiceScreen
import com.example.screens.DashboardScreen
import com.example.screens.DeleteAccountOtpScreen
import com.example.screens.DeleteAccountReasonScreen
import com.example.screens.EmailVerificationScreen
import com.example.screens.ForgotPasswordScreen
import com.example.screens.InvoiceDetailScreen
import com.example.screens.InvoiceDefaultSetting
import com.example.screens.InvoiceDefaultSettingsScreen
import com.example.screens.InvoiceListScreen
import com.example.screens.InvoicePreviewScreen
import com.example.screens.LandingScreen
import com.example.screens.LogInScreen
import com.example.screens.ProductListScreen
import com.example.screens.ProfileScreen
import com.example.screens.ReportsScreen
import com.example.screens.SettingsHomeScreen
import com.example.screens.SignUpScreen
import com.example.screens.SplashScreen
import com.example.screens.TaxSettingsScreen

object WavesDestinations {
    const val SPLASH = "splash"
    const val LANDING = "landing"
    const val SIGN_UP = "signup"
    const val LOG_IN = "login"
    const val FORGOT_PASSWORD = "forgot_password"
    const val EMAIL_VERIFICATION = "verify_email/{email}/{emailSent}"
    const val STATE_SCREEN = "state_screen/{type}"
    const val DASHBOARD = "dashboard"
    const val INVOICES = "invoices"
    const val CLIENTS = "clients"
    const val PROFILE = "profile"
    const val SETTINGS = "settings"
    const val BUSINESS_PROFILE = "business_profile"
    const val CLIENT_DETAIL = "client_detail/{clientId}"
    const val ADD_EDIT_CLIENT = "add_edit_client/{clientId}"
    const val PRODUCTS = "products"
    const val ADD_EDIT_PRODUCT = "add_edit_product/{productId}"
    const val CREATE_INVOICE = "create_invoice"
    const val INVOICE_PREVIEW = "invoice_preview/{invoiceId}"
    const val INVOICE_DETAIL = "invoice_detail/{invoiceId}"
    const val REPORTS = "reports"
    const val TAX_SETTINGS = "tax_settings"
    const val BANK_SETTINGS = "bank_settings"
    const val INVOICE_PREFIX_SETTINGS = "invoice_prefix_settings"
    const val INVOICE_CURRENCY_SETTINGS = "invoice_currency_settings"
    const val INVOICE_PAYMENT_TERMS_SETTINGS = "invoice_payment_terms_settings"
    const val DELETE_ACCOUNT_REASON = "delete_account_reason"
    const val DELETE_ACCOUNT_OTP = "delete_account_otp"
}

@Composable
fun WavesNavGraph(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = WavesDestinations.SPLASH
) {
    fun navigateToTab(tab: WavesNavTab) {
        navController.navigate(tab.route) {
            popUpTo(WavesDestinations.DASHBOARD) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        composable(WavesDestinations.SPLASH) {
            SplashScreen(
                onNavigateToLanding = {
                    val user = FirebaseAuthRepository.currentUser
                    val destination = when {
                        user == null -> WavesDestinations.LANDING
                        user.isEmailVerified -> WavesDestinations.DASHBOARD
                        else -> "verify_email/${Uri.encode(user.email.orEmpty())}/false"
                    }
                    navController.navigate(destination) {
                        popUpTo(WavesDestinations.SPLASH) { inclusive = true }
                    }
                }
            )
        }

        composable(WavesDestinations.LANDING) {
            LandingScreen(
                onNavigateToSignUp = { navController.navigate(WavesDestinations.SIGN_UP) },
                onNavigateToLogIn = { navController.navigate(WavesDestinations.LOG_IN) },
                onNavigateToDashboard = {
                    navController.navigate(WavesDestinations.DASHBOARD) {
                        popUpTo(WavesDestinations.LANDING) { inclusive = true }
                    }
                }
            )
        }

        composable(WavesDestinations.SIGN_UP) {
            SignUpScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogIn = {
                    navController.navigate(WavesDestinations.LOG_IN) {
                        popUpTo(WavesDestinations.SIGN_UP) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = { email, emailSent ->
                    navController.navigate("verify_email/${Uri.encode(email)}/$emailSent")
                }
            )
        }

        composable(WavesDestinations.LOG_IN) {
            LogInScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSignUp = {
                    navController.navigate(WavesDestinations.SIGN_UP) {
                        popUpTo(WavesDestinations.LOG_IN) { inclusive = true }
                    }
                },
                onNavigateToDashboard = {
                    navController.navigate(WavesDestinations.DASHBOARD) {
                        popUpTo(WavesDestinations.LANDING) { inclusive = true }
                    }
                },
                onNavigateToVerifyEmail = { email ->
                    navController.navigate("verify_email/${Uri.encode(email)}/false")
                },
                onNavigateToForgotPassword = {
                    navController.navigate(WavesDestinations.FORGOT_PASSWORD)
                }
            )
        }

        composable(WavesDestinations.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToLogIn = {
                    navController.navigate(WavesDestinations.LOG_IN) {
                        popUpTo(WavesDestinations.FORGOT_PASSWORD) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = WavesDestinations.EMAIL_VERIFICATION,
            arguments = listOf(
                navArgument("email") { type = NavType.StringType },
                navArgument("emailSent") { type = NavType.BoolType }
            )
        ) { backStackEntry ->
            val emailArg = Uri.decode(backStackEntry.arguments?.getString("email").orEmpty())
            val emailSent = backStackEntry.arguments?.getBoolean("emailSent") ?: false
            EmailVerificationScreen(
                email = emailArg,
                initialEmailSent = emailSent,
                onNavigateBack = {
                    if (!navController.popBackStack()) {
                        FirebaseAuthRepository.signOut()
                        navController.navigate(WavesDestinations.LOG_IN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onVerificationSuccess = {
                    navController.navigate(WavesDestinations.DASHBOARD) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = WavesDestinations.STATE_SCREEN,
            arguments = listOf(navArgument("type") { type = NavType.StringType })
        ) { backStackEntry ->
            val typeArg = backStackEntry.arguments?.getString("type") ?: "success"
            val stateType = when (typeArg.lowercase()) {
                "loading" -> StateType.LOADING
                "error" -> StateType.ERROR
                else -> StateType.SUCCESS
            }
            StateScreen(
                type = stateType,
                onPrimaryClick = { navController.popBackStack() },
                onSecondaryClick = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.DASHBOARD) {
            DashboardScreen(
                onNavigateToCreateInvoice = { navController.navigate(WavesDestinations.CREATE_INVOICE) },
                onNavigateToInvoiceList = { navController.navigate(WavesDestinations.INVOICES) },
                onNavigateToInvoiceDetail = { id -> navController.navigate("invoice_detail/$id") },
                onNavigateToBusinessProfile = { navController.navigate(WavesDestinations.BUSINESS_PROFILE) },
                onNavigateToReports = { navController.navigate(WavesDestinations.REPORTS) },
                onNavigateToProducts = { navController.navigate(WavesDestinations.PRODUCTS) },
                onNavigateToTab = ::navigateToTab
            )
        }

        composable(WavesDestinations.INVOICES) {
            InvoiceListScreen(
                onNavigateToCreateInvoice = { navController.navigate(WavesDestinations.CREATE_INVOICE) },
                onNavigateToInvoiceDetail = { id -> navController.navigate("invoice_detail/$id") },
                onNavigateToSettings = { navController.navigate(WavesDestinations.SETTINGS) },
                onNavigateToTab = ::navigateToTab
            )
        }

        composable(WavesDestinations.CLIENTS) {
            ClientListScreen(
                onNavigateToClientDetail = { id -> navController.navigate("client_detail/$id") },
                onNavigateToAddClient = { navController.navigate("add_edit_client/new") },
                onNavigateToTab = ::navigateToTab
            )
        }

        composable(WavesDestinations.PROFILE) {
            ProfileScreen(
                onNavigateToBusinessProfile = { navController.navigate(WavesDestinations.BUSINESS_PROFILE) },
                onNavigateToTaxSettings = { navController.navigate(WavesDestinations.TAX_SETTINGS) },
                onNavigateToBankSettings = { navController.navigate(WavesDestinations.BANK_SETTINGS) },
                onNavigateToProducts = { navController.navigate(WavesDestinations.PRODUCTS) },
                onNavigateToReports = { navController.navigate(WavesDestinations.REPORTS) },
                onNavigateToDeleteAccount = { navController.navigate(WavesDestinations.DELETE_ACCOUNT_REASON) },
                onNavigateToLogIn = {
                    FirebaseAuthRepository.signOut()
                    navController.navigate(WavesDestinations.LOG_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToTab = ::navigateToTab
            )
        }

        composable(WavesDestinations.SETTINGS) {
            SettingsHomeScreen(
                onNavigateToPrefixNumbering = { navController.navigate(WavesDestinations.INVOICE_PREFIX_SETTINGS) },
                onNavigateToCurrency = { navController.navigate(WavesDestinations.INVOICE_CURRENCY_SETTINGS) },
                onNavigateToPaymentTerms = { navController.navigate(WavesDestinations.INVOICE_PAYMENT_TERMS_SETTINGS) },
                onNavigateToReports = { navController.navigate(WavesDestinations.REPORTS) },
                onNavigateToDeleteAccount = { navController.navigate(WavesDestinations.DELETE_ACCOUNT_REASON) },
                onNavigateToLogIn = {
                    FirebaseAuthRepository.signOut()
                    navController.navigate(WavesDestinations.LOG_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onNavigateToTab = ::navigateToTab
            )
        }

        composable(WavesDestinations.BUSINESS_PROFILE) {
            BusinessProfileScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.INVOICE_PREFIX_SETTINGS) {
            InvoiceDefaultSettingsScreen(
                setting = InvoiceDefaultSetting.PREFIX_NUMBERING,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.INVOICE_CURRENCY_SETTINGS) {
            InvoiceDefaultSettingsScreen(
                setting = InvoiceDefaultSetting.CURRENCY,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.INVOICE_PAYMENT_TERMS_SETTINGS) {
            InvoiceDefaultSettingsScreen(
                setting = InvoiceDefaultSetting.PAYMENT_TERMS,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = WavesDestinations.CLIENT_DETAIL,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId") ?: "c1"
            ClientDetailScreen(
                clientId = clientId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToEditClient = { id -> navController.navigate("add_edit_client/$id") },
                onNavigateToCreateInvoice = { navController.navigate(WavesDestinations.CREATE_INVOICE) },
                onNavigateToInvoiceDetail = { id -> navController.navigate("invoice_detail/$id") }
            )
        }

        composable(
            route = WavesDestinations.ADD_EDIT_CLIENT,
            arguments = listOf(navArgument("clientId") { type = NavType.StringType })
        ) { backStackEntry ->
            val clientId = backStackEntry.arguments?.getString("clientId")
            AddEditClientScreen(
                clientId = clientId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.PRODUCTS) {
            ProductListScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToAddProduct = { navController.navigate("add_edit_product/new") },
                onNavigateToEditProduct = { id -> navController.navigate("add_edit_product/$id") }
            )
        }

        composable(
            route = WavesDestinations.ADD_EDIT_PRODUCT,
            arguments = listOf(navArgument("productId") { type = NavType.StringType })
        ) { backStackEntry ->
            val productId = backStackEntry.arguments?.getString("productId")
            AddEditProductScreen(
                productId = productId,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.CREATE_INVOICE) {
            CreateInvoiceScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { id -> navController.navigate("invoice_preview/$id") },
                onNavigateToAddClient = { navController.navigate("add_edit_client/new") }
            )
        }

        composable(
            route = WavesDestinations.INVOICE_PREVIEW,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: "INV-004"
            InvoicePreviewScreen(
                invoiceId = invoiceId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToInvoiceDetail = { id ->
                    navController.navigate("invoice_detail/$id") {
                        popUpTo(WavesDestinations.CREATE_INVOICE) { inclusive = true }
                    }
                }
            )
        }

        composable(
            route = WavesDestinations.INVOICE_DETAIL,
            arguments = listOf(navArgument("invoiceId") { type = NavType.StringType })
        ) { backStackEntry ->
            val invoiceId = backStackEntry.arguments?.getString("invoiceId") ?: "INV-004"
            InvoiceDetailScreen(
                invoiceId = invoiceId,
                onNavigateBack = { navController.popBackStack() },
                onNavigateToPreview = { id -> navController.navigate("invoice_preview/$id") }
            )
        }

        composable(WavesDestinations.REPORTS) {
            ReportsScreen(
                onNavigateBack = { navController.popBackStack() },
                onNavigateToSettings = { navController.navigate(WavesDestinations.SETTINGS) },
                onNavigateToTab = ::navigateToTab
            )
        }

        composable(WavesDestinations.TAX_SETTINGS) {
            TaxSettingsScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.BANK_SETTINGS) {
            BankAccountScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(WavesDestinations.DELETE_ACCOUNT_REASON) {
            DeleteAccountReasonScreen(
                onNavigateBack = { navController.popBackStack() },
                onOtpSent = { reason ->
                    navController.currentBackStackEntry?.savedStateHandle?.set("deletion_reason", reason)
                    navController.navigate(WavesDestinations.DELETE_ACCOUNT_OTP)
                }
            )
        }

        composable(WavesDestinations.DELETE_ACCOUNT_OTP) {
            val reason = navController.previousBackStackEntry
                ?.savedStateHandle
                ?.get<String>("deletion_reason")
                .orEmpty()
            DeleteAccountOtpScreen(
                reason = reason,
                onNavigateBack = { navController.popBackStack() },
                onAccountDeleted = {
                    FirebaseAuthRepository.signOut()
                    navController.navigate(WavesDestinations.LANDING) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
