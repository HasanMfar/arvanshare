package com.arvanshare.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arvanshare.app.ui.ComposePostScreen
import com.arvanshare.app.ui.FeedScreen
import com.arvanshare.app.ui.PostDetailScreen
import com.arvanshare.app.ui.SetupScreen
import com.arvanshare.app.ui.theme.ArvanShareTheme
import com.arvanshare.app.viewmodel.AppViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ArvanShareTheme {
                AppRoot()
            }
        }
    }
}

@Composable
fun AppRoot(vm: AppViewModel = viewModel()) {
    val settings by vm.settings.collectAsStateWithLifecycle()

    if (!settings.setupComplete) {
        SetupScreen(vm)
        return
    }

    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = "feed") {
        composable("feed") {
            FeedScreen(
                vm = vm,
                onCompose = { nav.navigate("compose") },
                onOpenPost = { post ->
                    vm.openDetail(post)
                    nav.navigate("detail/${post.postId}")
                },
            )
        }
        composable("compose") {
            ComposePostScreen(vm = vm, onDone = { nav.popBackStack() })
        }
        composable("detail/{postId}") {
            PostDetailScreen(vm = vm, onBack = { nav.popBackStack() })
        }
    }
}
