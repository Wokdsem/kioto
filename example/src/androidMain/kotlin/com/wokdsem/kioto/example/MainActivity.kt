package com.wokdsem.kioto.example

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.wokdsem.kioto.NodeHost
import com.wokdsem.kioto.example.features.demo.Demo

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            NodeHost(navigation = nodeNav, onStackCleared = { Demo.Token })
        }
    }

}