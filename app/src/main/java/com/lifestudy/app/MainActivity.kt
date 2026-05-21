package com.lifestudy.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.lifestudy.app.data.AppRepository
import com.lifestudy.app.ui.nav.AppNav
import com.lifestudy.app.ui.theme.LifeStudyTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppRepository.init(applicationContext)
        enableEdgeToEdge()
        setContent { App() }
    }
}

@Composable
private fun App() {
    LifeStudyTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AppNav()
        }
    }
}
