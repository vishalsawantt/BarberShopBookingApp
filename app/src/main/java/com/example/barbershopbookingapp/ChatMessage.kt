package com.example.barbershopbookingapp


import java.text.SimpleDateFormat
import java.util.*

data class ChatMessage(
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",
    val timestamp: Long = 0
) {
    fun getFormattedTime(): String {
        val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(timestamp))
    }

    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) //
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(Date(timestamp))
    }
}
