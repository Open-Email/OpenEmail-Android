@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.material3.MaterialTheme
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mercata.pingworks.broadcast_list.BroadcastListScreen
import com.mercata.pingworks.message_details.MessageDetailsScreen

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme() {
                SharedTransitionLayout {
                    val navController = rememberNavController()
                    NavHost(
                        navController = navController,
                        startDestination = "BroadcastListState"
                    ) {
                        composable(route = "BroadcastListState") {
                            BroadcastListScreen(
                                navController,
                                this,
                            )
                        }
                        composable(
                            route = "MessageDetailsScreen/{messageId}",
                            arguments = listOf(
                                navArgument("messageId") {
                                    type = NavType.StringType
                                    nullable = false
                                }
                            )
                        ) {
                            MessageDetailsScreen(
                                navController,
                                this
                            )
                        }
                    }
                }
            }
        }
    }
}