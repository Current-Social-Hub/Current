package com.current.hub

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Person
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.InCallService
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.*

class CallService : InCallService() {

    private val serviceScope = CoroutineScope(Dispatchers.Main + Job())
    private var timerJob: Job? = null

    private val callCallback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            CallManager.callState = state
            updateMetadata(call)
            updateNotification(call)

            if (state == Call.STATE_ACTIVE) {
                startTimer()
            } else if (state == Call.STATE_DISCONNECTED) {
                stopTimer()
                // Clear the call from CallManager immediately so UI screens dismiss automatically
                CallManager.updateCall(null)
                stopForeground(STOP_FOREGROUND_REMOVE)
            }
        }

        override fun onDetailsChanged(call: Call, details: Call.Details) {
            updateMetadata(call)
            updateNotification(call)
        }
    }

    private fun startTimer() {
        if (timerJob != null) return
        timerJob = serviceScope.launch {
            while (isActive) {
                delay(1000)
                CallManager.durationSeconds++
            }
        }
    }

    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    private fun updateMetadata(call: Call) {
        val number = call.details.handle?.schemeSpecificPart ?: ""
        if (number.isNotBlank()) {
            CallManager.lastNumber = number
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        Log.d("CallService", "onCallAdded: $call")
        call.registerCallback(callCallback)
        updateMetadata(call)
        CallManager.updateCall(call)
        
        updateNotification(call)
        
        val powerManager = getSystemService(android.content.Context.POWER_SERVICE) as android.os.PowerManager
        if (!powerManager.isInteractive) {
            val intent = Intent(this, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
            }
            startActivity(intent)
        }
    }

    override fun onCallRemoved(call: Call) {
        super.onCallRemoved(call)
        Log.d("CallService", "onCallRemoved: $call")
        call.unregisterCallback(callCallback)
        if (CallManager.currentCall == call) {
            CallManager.updateCall(null)
            stopTimer()
            stopForeground(STOP_FOREGROUND_REMOVE)
        }
    }

    private fun updateNotification(call: Call) {
        val channelId = "active_calls"
        val notificationManager = getSystemService(NotificationManager::class.java)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Active Calls",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                // We keep sound null as Telecom handles ringing, but allow vibration
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val number = call.details.handle?.schemeSpecificPart ?: ""
        val name = CallManager.lastName.ifBlank { number }
        val photoUri = CallManager.lastPhotoUri
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)

        val hangupIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = "HANGUP"
        }
        val hangupPendingIntent = PendingIntent.getBroadcast(this, 1, hangupIntent, PendingIntent.FLAG_IMMUTABLE)

        val answerIntent = Intent(this, CallActionReceiver::class.java).apply {
            action = "ANSWER"
        }
        val answerPendingIntent = PendingIntent.getBroadcast(this, 2, answerIntent, PendingIntent.FLAG_IMMUTABLE)

        val personBuilder = Person.Builder()
            .setName(name)
            .setImportant(true)
            
        if (photoUri != null) {
            try {
                personBuilder.setIcon(Icon.createWithContentUri(photoUri))
            } catch (e: Exception) {
                Log.e("CallService", "Failed to set person icon", e)
            }
        }
        
        val person = personBuilder.build()

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val style = when (call.state) {
                Call.STATE_RINGING -> Notification.CallStyle.forIncomingCall(person, hangupPendingIntent, answerPendingIntent)
                else -> Notification.CallStyle.forOngoingCall(person, hangupPendingIntent)
            }
            Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setStyle(style)
                .setFullScreenIntent(pendingIntent, true)
        } else {
            Notification.Builder(this, channelId)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(name)
                .setContentText(if (call.state == Call.STATE_RINGING) "Incoming call" else "Ongoing call")
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
        }

        builder.setCategory(Notification.CATEGORY_CALL)
            .setOngoing(true)
            .setAutoCancel(false)

        startForeground(100, builder.build())
    }

    @Deprecated("Deprecated in Java")
    override fun onCallAudioStateChanged(audioState: CallAudioState?) {
        super.onCallAudioStateChanged(audioState)
        audioState?.let {
            CallManager.isMuted = it.isMuted
            CallManager.audioRoute = it.route
            CallManager.supportedAudioRoutes = it.supportedRouteMask
        }
    }
    
    companion object {
        private var instance: CallService? = null
        
        fun endCall() {
            CallManager.currentCall?.disconnect()
        }

        fun acceptCall() {
            CallManager.currentCall?.answer(android.telecom.VideoProfile.STATE_AUDIO_ONLY)
        }

        fun rejectCall() {
            CallManager.currentCall?.disconnect()
        }
        
        fun setMuted(muted: Boolean) {
            instance?.setMuted(muted)
        }

        fun setAudioRoute(route: Int) {
            instance?.setAudioRoute(route)
        }
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        instance = null
    }
}

class CallActionReceiver : android.content.BroadcastReceiver() {
    override fun onReceive(context: android.content.Context?, intent: android.content.Intent?) {
        when (intent?.action) {
            "HANGUP" -> CallService.endCall()
            "ANSWER" -> CallService.acceptCall()
        }
    }
}

object CallManager {
    const val STATE_IDLE = -1
    var currentCall by mutableStateOf<Call?>(null)
    var isMuted by mutableStateOf(false)
    var callState by mutableIntStateOf(STATE_IDLE)
    
    var audioRoute by mutableIntStateOf(CallAudioState.ROUTE_EARPIECE)
    var supportedAudioRoutes by mutableIntStateOf(0)

    var lastNumber by mutableStateOf("")
    var lastName by mutableStateOf("")
    var lastPhotoUri by mutableStateOf<String?>(null)
    var durationSeconds by mutableIntStateOf(0)

    fun updateCall(call: Call?) {
        currentCall = call
        if (call == null) {
            callState = STATE_IDLE
            durationSeconds = 0
        } else {
            callState = call.state
            val number = call.details.handle?.schemeSpecificPart ?: ""
            if (number.isNotBlank()) {
                lastNumber = number
            }
        }
    }

    fun clear() {
        currentCall = null
        callState = STATE_IDLE
        lastNumber = ""
        lastName = ""
        lastPhotoUri = null
        isMuted = false
        audioRoute = CallAudioState.ROUTE_EARPIECE
        durationSeconds = 0
    }
}
