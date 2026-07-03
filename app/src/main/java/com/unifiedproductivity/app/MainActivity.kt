package com.unifiedproductivity.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.unifiedproductivity.app.ui.AppRoot
import com.unifiedproductivity.app.ui.theme.UnifiedProductivityTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as UnifiedProductivityApp).container
        setContent {
            UnifiedProductivityTheme {
                AppRoot(container)
            }
        }
    }
}
