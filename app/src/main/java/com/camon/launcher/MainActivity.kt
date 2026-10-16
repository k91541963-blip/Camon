package com.camon.launcher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.camon.launcher.ui.CamonRoot
import com.camon.launcher.ui.LauncherViewModel
import com.camon.launcher.ui.theme.CamonTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val vm = LauncherViewModel(application as CamonApp)
        setContent {
            CamonTheme(darkTheme = vm.settings.value.darkTheme) {
                CamonRoot(vm)
            }
        }
    }
}
