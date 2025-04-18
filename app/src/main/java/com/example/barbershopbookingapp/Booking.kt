package com.example.barbershopbookingapp

data class Booking(
    var shopName: String = "",
    val date: String = "",
    val time: String = "",
    val amountPaid: String = "",
    val status: String = "",
    val userUID: String = "",
    var customerName: String = ""
)
