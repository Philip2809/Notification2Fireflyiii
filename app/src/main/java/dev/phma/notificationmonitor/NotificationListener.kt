package dev.phma.notificationmonitor

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class NotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        HandleNotification.initialize(applicationContext)

        var notifications = getActiveNotifications()
        for (sbn in notifications) {
            HandleNotification.handle(sbn.packageName, sbn.notification)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn != null) {
            HandleNotification.handle(sbn.packageName, sbn.notification)
        }
    }

}