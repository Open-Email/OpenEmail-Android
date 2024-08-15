package com.mercata.pingworks

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mercata.pingworks.broadcast_list.BroadcastListScreen
import com.mercata.pingworks.message_details.MessageDetailsScreen

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navController = rememberNavController()
            MaterialTheme() {

                NavHost(navController = navController, startDestination = "BroadcastListState") {
                    composable(route = "BroadcastListState") { BroadcastListScreen(navController = navController) }
                    composable(route = "MessageDetailsScreen") { MessageDetailsScreen(navController = navController) }
                }
            }
        }
    }
}