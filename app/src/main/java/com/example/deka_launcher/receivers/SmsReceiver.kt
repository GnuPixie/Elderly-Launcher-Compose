package com.example.deka_launcher.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            messages?.forEach { smsMessage ->
                val sender = smsMessage.originatingAddress ?: "Unknown"
                val messageBody = smsMessage.messageBody
                
                // Store the message in your app's database or handle it as needed
                Log.d("SmsReceiver", "Received SMS from $sender: $messageBody")
                
                // You can also show a notification here
                // TODO: Implement notification system for new messages
            }
        }
    }
} 