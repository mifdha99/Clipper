package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.ClipperEditorScreen
import com.example.ui.screens.DownloadsScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LibraryScreen
import com.example.ui.theme.CrimsonRedPrimary
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.TubeClipTheme
import com.example.ui.viewmodel.ClipEditorViewModel
import com.example.ui.viewmodel.DownloadViewModel
import com.example.util.configureAppCheck

enum class AppScreen(val title: String) {
    EXPLORE("Jelajahi"),
    DOWNLOADS("Download"),
    EDITOR("Studio Klip"),
    LIBRARY("Koleksi")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Mandatory App Check configuration with launch intent before UI/Firebase
        configureAppCheck(this, intent)

        enableEdgeToEdge()

        setContent {
            TubeClipTheme {
                TubeClipApp()
            }
        }
    }
}

@Composable
fun TubeClipApp(
    viewModel: ClipEditorViewModel = viewModel(),
    downloadViewModel: DownloadViewModel = viewModel()
) {
    var currentScreen by remember { mutableStateOf(AppScreen.EXPLORE) }
    val context = LocalContext.current

    // Android back handler: if not in Explore, return to Explore
    BackHandler(enabled = currentScreen != AppScreen.EXPLORE) {
        currentScreen = AppScreen.EXPLORE
    }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFF10131B),
                contentColor = Color.White
            ) {
                NavigationBarItem(
                    selected = currentScreen == AppScreen.EXPLORE,
                    onClick = { currentScreen = AppScreen.EXPLORE },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.EXPLORE) Icons.Filled.Home else Icons.Outlined.Home,
                            contentDescription = "Jelajahi"
                        )
                    },
                    label = {
                        Text(
                            text = "Jelajahi",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == AppScreen.EXPLORE) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = CrimsonRedPrimary,
                        indicatorColor = CrimsonRedPrimary,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_explore")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.DOWNLOADS,
                    onClick = { currentScreen = AppScreen.DOWNLOADS },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.DOWNLOADS) Icons.Filled.FileDownload else Icons.Outlined.FileDownload,
                            contentDescription = "Download"
                        )
                    },
                    label = {
                        Text(
                            text = "Download",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == AppScreen.DOWNLOADS) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = CrimsonRedPrimary,
                        indicatorColor = CrimsonRedPrimary,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_downloads")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.EDITOR,
                    onClick = { currentScreen = AppScreen.EDITOR },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.EDITOR) Icons.Filled.ContentCut else Icons.Outlined.ContentCut,
                            contentDescription = "Studio Klip"
                        )
                    },
                    label = {
                        Text(
                            text = "Studio",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == AppScreen.EDITOR) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = CrimsonRedPrimary,
                        indicatorColor = CrimsonRedPrimary,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_studio")
                )

                NavigationBarItem(
                    selected = currentScreen == AppScreen.LIBRARY,
                    onClick = { currentScreen = AppScreen.LIBRARY },
                    icon = {
                        Icon(
                            imageVector = if (currentScreen == AppScreen.LIBRARY) Icons.Filled.VideoLibrary else Icons.Outlined.VideoLibrary,
                            contentDescription = "Koleksi"
                        )
                    },
                    label = {
                        Text(
                            text = "Koleksi",
                            fontSize = 11.sp,
                            fontWeight = if (currentScreen == AppScreen.LIBRARY) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.White,
                        selectedTextColor = CrimsonRedPrimary,
                        indicatorColor = CrimsonRedPrimary,
                        unselectedIconColor = Color(0xFF94A3B8),
                        unselectedTextColor = Color(0xFF94A3B8)
                    ),
                    modifier = Modifier.testTag("nav_library")
                )
            }
        },
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxSize()
    ) { innerPadding ->
        when (currentScreen) {
            AppScreen.EXPLORE -> {
                HomeScreen(
                    viewModel = viewModel,
                    onNavigateToEditor = { currentScreen = AppScreen.EDITOR },
                    onNavigateToDownloads = { currentScreen = AppScreen.DOWNLOADS },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.DOWNLOADS -> {
                DownloadsScreen(
                    viewModel = downloadViewModel,
                    onEditFile = { file ->
                        val uri = Uri.fromFile(file)
                        viewModel.importVideoFromUri(
                            context = context,
                            uri = uri,
                            onSuccess = { currentScreen = AppScreen.EDITOR },
                            onError = { /* ignored */ }
                        )
                    },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.EDITOR -> {
                ClipperEditorScreen(
                    viewModel = viewModel,
                    onNavigateBack = { currentScreen = AppScreen.EXPLORE },
                    modifier = Modifier.padding(innerPadding)
                )
            }
            AppScreen.LIBRARY -> {
                LibraryScreen(
                    viewModel = viewModel,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}
