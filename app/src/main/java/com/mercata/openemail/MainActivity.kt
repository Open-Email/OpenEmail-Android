@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)

package com.mercata.openemail

import android.content.Intent
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
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.mercata.openemail.composing_screen.ComposingScreen
import com.mercata.openemail.contact_details.ContactDetailsScreen
import com.mercata.openemail.home_screen.HomeScreen
import com.mercata.openemail.message_details.MessageDetailsScreen
import com.mercata.openemail.profile_screen.ProfileScreen
import com.mercata.openemail.registration.RegistrationScreen
import com.mercata.openemail.repository.ProcessIncomingIntentsRepository
import com.mercata.openemail.save_keys_suggestion.SaveKeysSuggestionScreen
import com.mercata.openemail.settings_screen.SettingsScreen
import com.mercata.openemail.sign_in.SignInScreen
import com.mercata.openemail.sign_in.enter_keys_screen.EnterKeysScreen
import com.mercata.openemail.sign_in.qr_code_scanner_screen.QRCodeScannerScreen
import com.mercata.openemail.theme.AppTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class MainActivity : AppCompatActivity(), KoinComponent {

    private lateinit var navController: NavHostController

    private val newIntentRepository: ProcessIncomingIntentsRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppTheme {
                SharedTransitionLayout {
                    navController = rememberNavController()
                    NavHost(
                        enterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                tween(animationDuration)
                            )
                        },
                        exitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.Start,
                                tween(animationDuration)
                            )
                        },
                        popEnterTransition = {
                            slideIntoContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                tween(animationDuration)
                            )
                        },
                        popExitTransition = {
                            slideOutOfContainer(
                                AnimatedContentTransitionScope.SlideDirection.End,
                                tween(animationDuration)
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
                        composable(route = "QRCodeScannerScreen") {
                            QRCodeScannerScreen(navController = navController)
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
                            route = "MessageDetailsScreen/{messageId}/{scope}",
                            arguments = listOf(
                                navArgument("messageId") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                                navArgument("scope") {
                                    type = NavType.StringType
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
                            route = "ComposingScreen/{contactAddress}/{replyMessageId}/{draftId}/{attachmentUri}",
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
                                navArgument("attachmentUri") {
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

            intent?.let {
                newIntentRepository.processNewIntent(it)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        newIntentRepository.processNewIntent(intent)
    }
}