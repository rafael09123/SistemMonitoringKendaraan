package com.project.qrcode.utils

import java.text.SimpleDateFormat
import java.util.*

fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
    val date = Date(timestamp)
    return sdf.format(date) // Format Date ke string
}
