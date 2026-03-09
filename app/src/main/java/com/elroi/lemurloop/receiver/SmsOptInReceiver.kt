package com.elroi.lemurloop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import com.elroi.lemurloop.domain.manager.AccountabilityManager
import com.elroi.lemurloop.domain.manager.SettingsManager
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class SmsOptInReceiver : BroadcastReceiver() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface SmsOptInEntryPoint {
        fun settingsManager(): SettingsManager
        fun accountabilityManager(): AccountabilityManager
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

        // BUG-1 FIX: goAsync() must be called ONCE per onReceive(), not inside the loop.
        // A single PendingResult covers all messages in this broadcast.
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val entryPoint = EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    SmsOptInEntryPoint::class.java
                )
                val settings = entryPoint.settingsManager()

                for (message in messages) {
                    val body = message.displayMessageBody?.trim() ?: continue
                    val sender = message.displayOriginatingAddress ?: continue

                    // Look for a 4-digit code in the message
                    val codeRegex = Regex("\\b(\\d{4})\\b")
                    val match = codeRegex.find(body)
                    val code = match?.groupValues?.get(1) ?: continue

                    val pendingCodes = settings.pendingBuddyCodesFlow.first()

                    // Normalize phone numbers for comparison (remove spaces, dashes, etc.)
                    val normalizedSender = sender.replace(Regex("[^\\d+]"), "")

                    val matchingEntry = pendingCodes.find { entry ->
                        val parts = entry.split(":")
                        if (parts.size != 2) return@find false
                        val entryCode = parts[0]
                        val entryPhone = parts[1].replace(Regex("[^\\d+]"), "")

                        entryCode == code && (normalizedSender.endsWith(entryPhone) || entryPhone.endsWith(normalizedSender))
                    }

                    if (matchingEntry != null) {
                        Log.d("SmsOptInReceiver", "Match found for code $code from $sender. Confirming buddy.")
                        val phoneFromEntry = matchingEntry.split(":")[1]

                        settings.addConfirmedBuddyNumber(phoneFromEntry)
                        settings.removePendingBuddyCode(code, phoneFromEntry)

                        // Send confirmation "Thank you" SMS
                        entryPoint.accountabilityManager().sendBuddyConfirmationSuccess(phoneFromEntry)
                    }
                }
            } catch (e: Exception) {
                Log.e("SmsOptInReceiver", "Error processing SMS opt-in", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
