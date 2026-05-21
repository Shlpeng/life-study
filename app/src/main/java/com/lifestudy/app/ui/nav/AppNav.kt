package com.lifestudy.app.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.lifestudy.app.ui.screens.CategoryScreen
import com.lifestudy.app.ui.screens.DetailScreen
import com.lifestudy.app.ui.screens.HomeScreen

object Routes {
    const val HOME = "home"
    const val CATEGORY = "category/{categoryId}"
    const val DETAIL = "detail/{categoryId}/{itemId}"

    fun category(id: String) = "category/$id"
    fun detail(categoryId: String, itemId: String) = "detail/$categoryId/$itemId"
}

@Composable
fun AppNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onCategoryClick = { nav.navigate(Routes.category(it)) })
        }
        composable(
            Routes.CATEGORY,
            arguments = listOf(navArgument("categoryId") { type = NavType.StringType }),
        ) { entry ->
            val id = entry.arguments?.getString("categoryId").orEmpty()
            CategoryScreen(
                categoryId = id,
                onBack = { nav.popBackStack() },
                onItemClick = { itemId -> nav.navigate(Routes.detail(id, itemId)) },
            )
        }
        composable(
            Routes.DETAIL,
            arguments = listOf(
                navArgument("categoryId") { type = NavType.StringType },
                navArgument("itemId") { type = NavType.StringType },
            ),
        ) { entry ->
            DetailScreen(
                categoryId = entry.arguments?.getString("categoryId").orEmpty(),
                itemId = entry.arguments?.getString("itemId").orEmpty(),
                onBack = { nav.popBackStack() },
            )
        }
    }
}
