package com.yeromich.pokecollect

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.yeromich.pokecollect.ui.PokeCollectApp
import com.yeromich.pokecollect.ui.theme.PokeCollectTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PokeCollectTheme {
                PokeCollectApp()
            }
        }
    }
}
