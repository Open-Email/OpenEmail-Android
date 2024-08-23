@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.mercata.pingworks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mercata.pingworks.broadcast_list.BroadcastListScreen
import com.mercata.pingworks.message_details.MessageDetailsScreen
import com.mercata.pingworks.registration.RegistrationScreen
import com.mercata.pingworks.save_keys_suggestion.SaveKeysSuggestionScreen
import com.mercata.pingworks.sign_in.SignInScreen
import com.mercata.pingworks.theme.AppTheme

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                SharedTransitionLayout {
                    val navController = rememberNavController()
                    NavHost(
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                tween(300)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                tween(300)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                tween(300)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                tween(300)
                            )
                        },
                        navController = navController,
                        startDestination = "SignInScreen"
                    ) {
                        composable(route = "SignInScreen") {
                            SignInScreen(navController = navController)
                        }
                        composable(route = "RegistrationScreen") {
                            RegistrationScreen(navController = navController)
                        }
                        composable(route = "SaveKeysSuggestionScreen") {
                            SaveKeysSuggestionScreen(navController = navController)
                        }
                        composable(
                            route = "BroadcastListScreen",
                            enterTransition = {
                                fadeIn(animationSpec = tween(300))
                            },
                            exitTransition = {
                                fadeOut(animationSpec = tween(300))
                            },
                            popEnterTransition = {
                                fadeIn(animationSpec = tween(300))
                            },
                            popExitTransition = {
                                fadeOut(animationSpec = tween(300))
                            },
                        ) {

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
                            ),
                            enterTransition = {
                                fadeIn()
                            },
                            exitTransition = {
                                fadeOut()
                            },
                            popEnterTransition = {
                                fadeIn()
                            },
                            popExitTransition = {
                                fadeOut()
                            },
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