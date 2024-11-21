package dev.phma.notificationmonitor

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.util.Log

data class NotificationBody(val title: String?, val text: String?)

object HandleNotification  {

    private var appContext: Context? = null

    fun initialize(appContext: Context) {
        HandleNotification.appContext = appContext
    }

    private const val PREFS_NAME = "HandledNotifications"
    private const val KEY_PROCESSED_NOTIFICATIONS = "processed_notifications"
    private const val TAG = "HandleNotification"

    val functionMap: Map<String, (NotificationBody) -> Unit> = mapOf(
        "com.imaginecurve.curve.prd" to ::handleCurve,
        "io.heckel.ntfy" to ::handleCurve
    )

    private fun handleCurve(body: NotificationBody) {
        if (body.title == null || body.text == null) return;
        val regex = Regex("(.+) SEK - (.+)")

        val results = regex.find(body.text) ?: return
        val sum = results.groups.get(1)?.value
        val store = results.groups.get(2)?.value

        Log.d(TAG, "123bought at ${store} for ${sum}")
    }


    private fun _handle(packageName: String, notification: Notification) {
        // Check if notification is already processed, check body and get regex for type
        val body = getNotificationBody(notification)
        if (body.title == null || body.text == null) return; // This seams to happen when notifications are grouped
        if (isNotificationProcessed(packageName, notification.`when`)) return;
        val function = functionMap[packageName] ?: run {
            throw Error("Function for application not found second time")
        }

        function.invoke(body)

        saveProcessedNotification(packageName, notification.`when`)
    }


    private fun saveProcessedNotification(packageName: String, time: Long) {
        appContext?.let {  context ->
            val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            val editor = sharedPreferences.edit()

            // Fetch existing processed notifications
            val processedNotifications = sharedPreferences.getStringSet(KEY_PROCESSED_NOTIFICATIONS, mutableSetOf()) ?: mutableSetOf()

            // Create a new mutable set to avoid modifying the original set
            val newProcessedNotifications = processedNotifications.toMutableSet()

            // Add new notification identifier (package name + time)
            newProcessedNotifications.add("$packageName-$time")

            // Save back the updated set
            editor.putStringSet(KEY_PROCESSED_NOTIFICATIONS, newProcessedNotifications)
            editor.apply()  // Commit changes
        } ?: {
            Log.e(TAG, "Could not save notification data because context is null")
        }
    }

    private fun isNotificationProcessed(packageName: String, time: Long): Boolean {
        val context = appContext ?: run {
            Log.e("HandleNotification", "Context is null, cannot get processed notification.")
            throw Error("Context is null, cannot get processed notification.")
        }

        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val processedNotifications = sharedPreferences.getStringSet(KEY_PROCESSED_NOTIFICATIONS, mutableSetOf())

        // Return true if notification identifier exists in processed notifications
        return processedNotifications?.contains("$packageName-$time") ?: false
    }

    fun getNotificationBody(notification: Notification): NotificationBody {
        val extras: Bundle = notification.extras

        val title = extras.getString(Notification.EXTRA_TITLE)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()

        return NotificationBody(title, text)
    }

    fun handle(packageName: String, notification: Notification) {
        if (functionMap[packageName] != null) _handle(packageName, notification)
    }

}