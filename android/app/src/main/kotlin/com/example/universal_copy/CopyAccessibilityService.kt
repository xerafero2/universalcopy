package com.example.universal_copy // Sesuaikan package name Anda!

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

    private val TRIGGER_ACTION = "com.example.universal_copy.TRIGGER_COPY"

    // Menangkap klik dari notifikasi
    private val copyTriggerReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == TRIGGER_ACTION) {
                extractAndCopyText()
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        
        // Daftarkan Receiver (untuk Android 13/API 33 ke atas gunakan RECEIVER_EXPORTED)
        val filter = IntentFilter(TRIGGER_ACTION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(copyTriggerReceiver, filter, RECEIVER_EXPORTED)
        } else {
            registerReceiver(copyTriggerReceiver, filter)
        }

        showPersistentNotification()
    }

    private fun showPersistentNotification() {
        val channelId = "universal_copy_channel"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, 
                "Universal Copy Trigger", 
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(TRIGGER_ACTION)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Universal Copy Aktif")
            .setContentText("Ketuk untuk menyalin teks di layar ini")
            .setSmallIcon(android.R.drawable.ic_menu_copy) 
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()

        notificationManager.notify(1, notification)
    }

    // Fungsi untuk memindai layar dan menyalin teks
    private fun extractAndCopyText() {
        val rootNode = rootInActiveWindow ?: run {
            Toast.makeText(this, "Gagal membaca layar", Toast.LENGTH_SHORT).show()
            return
        }

        val textList = mutableListOf<String>()
        traverseNode(rootNode, textList)

        if (textList.isNotEmpty()) {
            // Gabungkan semua teks dengan baris baru
            val fullText = textList.joinToString("\n")
            
            // Salin ke Clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Universal Copied Text", fullText)
            clipboard.setPrimaryClip(clip)

            Toast.makeText(this, "Teks berhasil disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "Tidak ada teks yang terdeteksi", Toast.LENGTH_SHORT).show()
        }
    }

    // Fungsi rekursif untuk membaca seluruh UI Android (View Tree)
    private fun traverseNode(node: AccessibilityNodeInfo?, textList: MutableList<String>) {
        if (node == null) return
        
        val nodeText = node.text?.toString()
        val nodeContentDesc = node.contentDescription?.toString()

        // Ambil teks dari elemen atau content description-nya
        if (!nodeText.isNullOrBlank()) {
            textList.add(nodeText)
        } else if (!nodeContentDesc.isNullOrBlank()) {
            textList.add(nodeContentDesc)
        }

        // Looping ke semua anak elemen (child views)
        for (i in 0 until node.childCount) {
            traverseNode(node.getChild(i), textList)
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Biarkan kosong, kita memicunya secara manual via notifikasi, bukan otomatis merespon event
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(copyTriggerReceiver)
    }
}
