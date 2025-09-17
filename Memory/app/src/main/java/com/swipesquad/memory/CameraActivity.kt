package com.swipesquad.memory

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent

class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            QRCameraView(onQRCodeResult = { uri, value ->
                setResult(RESULT_OK, Intent().apply {
                    putExtra("image_uri", uri)
                    putExtra("qr_code_value", value)
                })
                finish()
            })
        }
    }
}