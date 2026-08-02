package com.example.universalcopy

import android.accessibilityservice.AccessibilityService
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Toast
import androidx.core.app.NotificationCompat

class CopyAccessibilityService : AccessibilityService() {

    private val ACTION_TRIGGER_COPY = "com.example.universalcopy.TRIGGER_COPY"

    private val copyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == ACTION_TRIGGER_COPY) {
                extractAndCopyText()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        val filter = IntentFilter(ACTION_TRIGGER_COPY)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(copyReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(copyReceiver, filter)
        }

        showPersistentNotification()
    }

    private fun showPersistentNotification() {
        val channelId = "universal_copy_service_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Universal Copy Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(ACTION_TRIGGER_COPY)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Universal Copy Siap")
            .setContentText("Ketuk untuk mengekstrak teks dari layar")
            .setSmallIcon(android.R.drawable.ic_menu_edit)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(1, notification)
    }

    private fun extractAndCopyText() {
        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Toast.makeText(this, "Tidak dapat membaca layar saat ini", Toast.LENGTH_SHORT).show()
            return
        }

        val textList = mutableListOf<String>()
        traverseNode(rootNode, textList)

        if (textList.isNotEmpty()) {
            val fullText = textList.joinToString("\n")
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Copied Text", fullText)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Teks disalin ke Clipboard", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Tidak ada teks yang ditemukan", Toast.LENGTH_SHORT).show()
        }
    }

    private fun traverseNode(node: AccessibilityNodeInfo?, textList: MutableList<String>) {
        if (node == null) return

        val text = node.text?.toString()
        val desc = node.contentDescription?.toString()

        if (!text.isNullOrBlank()) {
            textList.add(text)
        } else if (!desc.isNullOrBlank()) {
            textList.add(desc)
        }

        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), textList)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(copyReceiver)
    }
}
