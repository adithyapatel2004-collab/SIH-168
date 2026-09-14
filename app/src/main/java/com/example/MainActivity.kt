package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.example.engine.DeadReckoningViewModel
import com.example.ui.NavigationScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.SpaceNavyDark

class MainActivity : ComponentActivity() {

  private val viewModel: DeadReckoningViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContent {
      MyApplicationTheme {
        val permissionLauncher = rememberLauncherForActivityResult(
          ActivityResultContracts.RequestMultiplePermissions()
        ) { /* Permissions result handled gracefully in SensorService */ }

        LaunchedEffect(Unit) {
          val hasFineLocation = ContextCompat.checkSelfPermission(
            this@MainActivity,
            Manifest.permission.ACCESS_FINE_LOCATION
          ) == PackageManager.PERMISSION_GRANTED

          if (!hasFineLocation) {
            permissionLauncher.launch(
              arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
              )
            )
          }
        }

        Surface(
          modifier = Modifier.fillMaxSize(),
          color = SpaceNavyDark
        ) {
          NavigationScreen(viewModel = viewModel)
        }
      }
    }
  }
}
