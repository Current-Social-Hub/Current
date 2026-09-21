package com.current.hub

import android.os.Bundle
import androidx.activity.ComponentActivity

class ComposeSmsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // This activity handles ACTION_SENDTO for SMS
        finish() 
    }
}
