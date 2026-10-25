package ch.gryphus.creditdebittracker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import ch.gryphus.creditdebittracker.ui.HomeScreen
import ch.gryphus.creditdebittracker.ui.PersonScreen
import ch.gryphus.creditdebittracker.ui.theme.CreditDebitTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            CreditDebitTheme {
                AppNav()
            }
        }
    }
}

@Composable
private fun AppNav() {
    val nav = rememberNavController()
    val vm: LedgerViewModel = viewModel()

    NavHost(navController = nav, startDestination = "home") {
        composable("home") {
            HomeScreen(
                vm = vm,
                onOpenPerson = { id -> nav.navigate("person/$id") },
            )
        }
        composable(
            route = "person/{personId}",
            arguments = listOf(navArgument("personId") { type = NavType.LongType }),
        ) { backStack ->
            val personId = backStack.arguments?.getLong("personId") ?: return@composable
            PersonScreen(
                vm = vm,
                personId = personId,
                onBack = { nav.popBackStack() },
            )
        }
    }
}
