@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalLayoutApi::class)

package com.mercata.pingworks

import android.content.Intent
import android.net.Uri
import android.os.Build
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
import com.mercata.pingworks.composing_screen.ComposingScreen
import com.mercata.pingworks.contact_details.ContactDetailsScreen
import com.mercata.pingworks.contacts_screen.ContactsScreen
import com.mercata.pingworks.home_screen.HomeScreen
import com.mercata.pingworks.message_details.MessageDetailsScreen
import com.mercata.pingworks.registration.RegistrationScreen
import com.mercata.pingworks.save_keys_suggestion.SaveKeysSuggestionScreen
import com.mercata.pingworks.settings_screen.SettingsScreen
import com.mercata.pingworks.settings_screen.personal.PersonalSettingsScreen
import com.mercata.pingworks.sign_in.SignInScreen
import com.mercata.pingworks.theme.AppTheme
import com.mercata.pingworks.utils.getMimeType
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

class MainActivity : AppCompatActivity() {

    private lateinit var navController: NavHostController

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
                            route = "ContactDetailsScreen/{address}",
                            arguments = listOf(
                                navArgument("address") {
                                    type = NavType.StringType
                                    nullable = false
                                }
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
                        composable(route = "ContactsScreen") {
                            ContactsScreen(navController = navController, this)
                        }
                        composable(route = "SettingsScreen") {
                            SettingsScreen(navController = navController)
                        }
                        composable(route = "PersonalSettingsScreen") {
                            PersonalSettingsScreen(navController = navController)
                        }
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
                            route = "MessageDetailsScreen/{messageId}/{outbox}",
                            arguments = listOf(
                                navArgument("messageId") {
                                    type = NavType.StringType
                                    nullable = false
                                },
                                navArgument("outbox") {
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
            processNewIntent()
        }
    }

    private fun processNewIntent() {
        val uris: ArrayList<Uri> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent?.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent?.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)
        } ?: arrayListOf()

        if (uris.isEmpty()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                intent?.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)?.let {
                    uris.add(it)
                }
            } else {
                intent?.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let {
                    uris.add(it)
                }
            }
        }

        val fileUris =
            uris.filter { it.getMimeType(this) != null }.takeIf { it.isNotEmpty() } ?: return
        navController.navigate(
            "ComposingScreen/null/null/null/${
                fileUris.joinToString(",") {
                    URLEncoder.encode(
                        it.toString(),
                        StandardCharsets.UTF_8.toString()
                    )
                }
            }"
        )
    }
}