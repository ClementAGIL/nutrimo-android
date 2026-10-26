// Single activity hosting the Compose navigation graph.
package com.clement.nutrimo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.clement.nutrimo.ui.NutrimoRoot
import com.clement.nutrimo.ui.theme.NutrimoTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { NutrimoTheme { NutrimoRoot() } }
    }
}
