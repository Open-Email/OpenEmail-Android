@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)

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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mercata.pingworks.composing_screen.ComposingScreen
import com.mercata.pingworks.contact_details.ContactDetailsScreen
import com.mercata.pingworks.home_screen.HomeScreen
import com.mercata.pingworks.message_details.MessageDetailsScreen
import com.mercata.pingworks.profile_screen.ProfileScreen
import com.mercata.pingworks.registration.RegistrationScreen
import com.mercata.pingworks.save_keys_suggestion.SaveKeysSuggestionScreen
import com.mercata.pingworks.settings_screen.SettingsScreen
import com.mercata.pingworks.sign_in.SignInScreen
import com.mercata.pingworks.sign_in.enter_keys_screen.EnterKeysScreen
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
                        composable(
                            route = "ContactDetailsScreen/{address}/{type}",
                            arguments = listOf(
                                navArgument("address") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                                navArgument("type") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                            ),
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                            popEnterTransition = { fadeIn() },
                            popExitTransition = { fadeOut() },
                        ) {
                            ContactDetailsScreen(
                                navController,
                                this
                            )
                        }
                        composable(route = "SettingsScreen") {
                            SettingsScreen(navController = navController)
                        }
                        composable(route = "SignInScreen") {
                            SignInScreen(navController = navController)
                        }
                        composable(
                            route = "EnterKeysScreen/{address}",
                            arguments = listOf(
                                navArgument("address") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                            )
                        ) {
                            EnterKeysScreen(navController = navController)
                        }
                        composable(route = "RegistrationScreen") {
                            RegistrationScreen(navController = navController)
                        }
                        composable(route = "SaveKeysSuggestionScreen") {
                            SaveKeysSuggestionScreen(navController = navController)
                        }
                        composable(route = "ProfileScreen") {
                            ProfileScreen(
                                navController = navController,
                                animatedVisibilityScope = this
                            )
                        }
                        composable(
                            route = "HomeScreen",
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                            popEnterTransition = { fadeIn() },
                            popExitTransition = { fadeOut() },
                        ) {

                            HomeScreen(
                                navController,
                                this,
                            )
                        }
                        composable(
                            route = "MessageDetailsScreen/{messageId}/{outbox}/{deletable}",
                            arguments = listOf(
                                navArgument("messageId") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                                navArgument("outbox") {
                                    type = NavType.BoolType
                                    nullable = false
                                },
                                navArgument("deletable") {
                                    type = NavType.BoolType
                                    nullable = false
                                }
                            ),
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                            popEnterTransition = { fadeIn() },
                            popExitTransition = { fadeOut() },
                        ) {
                            MessageDetailsScreen(
                                navController,
                                this
                            )
                        }
                        composable(
                            route = "ComposingScreen/{contactAddress}/{replyMessageId}/{draftId}",
                            arguments = listOf(
                                navArgument("contactAddress") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("replyMessageId") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                                navArgument("draftId") {
                                    type = NavType.StringType
                                    nullable = true
                                },
                            ),
                            enterTransition = { fadeIn() },
                            exitTransition = { fadeOut() },
                            popEnterTransition = { fadeIn() },
                            popExitTransition = { fadeOut() },
                        ) {
                            ComposingScreen(
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