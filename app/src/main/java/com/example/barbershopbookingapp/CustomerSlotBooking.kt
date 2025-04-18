package com.example.barbershopbookingapp

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.razorpay.Checkout
import com.razorpay.PaymentResultListener
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CustomerSlotBooking : AppCompatActivity(), PaymentResultListener {

    private lateinit var recyclerView: RecyclerView

    private var shopUID: String? = null
    private var selectedDate: String? = null
    private var selectedSlotTime: String? = null
    private var selectedSlotPrice: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_slotbooking)

        recyclerView = findViewById(R.id.recyclerViewSlots)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        shopUID = intent.getStringExtra("shopUID")
        selectedDate = intent.getStringExtra("selected_date")

        if (shopUID == null || selectedDate == null) {
            Toast.makeText(this, "Error: Missing shop or date", Toast.LENGTH_LONG).show()
            finish()
            return
        }
        setStatusBarColorToWhite()
        val txtSelectedDate = findViewById<TextView>(R.id.txtSelectedDate)
        txtSelectedDate.text = "Selected Date: $selectedDate"
        loadAvailableSlots()
//        val crashButton = Button(this)
//        crashButton.text = "Test Crash"
//        crashButton.setOnClickListener {
//            throw RuntimeException("Test Crash") // Force a crash
//        }
    }

    //white status bar
    private fun setStatusBarColorToWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE

            // To make the status bar text dark (better visibility on a light background)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }

    private fun loadAvailableSlots() {
        val slotDatabase = FirebaseDatabase.getInstance().getReference("slotsCreatedFromOwner")
            .child(shopUID!!)
            .child(selectedDate!!)

        slotDatabase.get().addOnSuccessListener { snapshot ->
            val tempList = ArrayList<Triple<String, String, String>>()

            for (slotSnapshot in snapshot.children) {
                val slotTime = slotSnapshot.key ?: continue
                val price = slotSnapshot.child("price").getValue(String::class.java) ?: "N/A"
                val status = slotSnapshot.child("status").getValue(String::class.java) ?: "unknown"

                tempList.add(Triple(slotTime, price, status))
            }

            val shopDetailsAdapter = ShopDetailsAdapter(this, tempList) { slotTime, price ->
                selectedSlotTime = slotTime
                selectedSlotPrice = price
                startPayment(price)
            }
            recyclerView.adapter = shopDetailsAdapter
        }.addOnFailureListener {
            Toast.makeText(this, "Failed to load slots", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startPayment(price: String) {
        val checkout = Checkout()
        checkout.setKeyID("rzp_test_uedaSo3ugF8wtM")

        val priceInPaise = (price.toFloat() * 100).toInt()
        val user = FirebaseAuth.getInstance().currentUser
        val email = user?.email
        val phone = user?.phoneNumber
        val shopName = "Classic Men's Salon" //default set in razorpay ui

        try {
            val options = JSONObject()
            options.put("name", shopName)
            options.put("description", "Booking slot: $selectedSlotTime on $selectedDate")
            options.put("currency", "INR")
            options.put("amount", priceInPaise)
            options.put("prefill.email", email)   //default in razorpay ui
            options.put("prefill.contact", phone ?: "9999999999") //default in razorpay ui

            checkout.open(this, options)
        } catch (e: Exception) {
            Toast.makeText(this, "Payment error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onPaymentSuccess(razorpayPaymentId: String?) {
        if (razorpayPaymentId != null && selectedSlotTime != null && selectedSlotPrice != null) {

            val userUID = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val customerRef = FirebaseDatabase.getInstance().getReference("customersDetails").child(userUID)

            // First fetch user details from customerDetails
            customerRef.get().addOnSuccessListener { snapshot ->
                val userEmail = snapshot.child("email").getValue(String::class.java) ?: "N/A"
                val userPhone = snapshot.child("mobile").getValue(String::class.java) ?: "N/A"
                val userName = snapshot.child("name").getValue(String::class.java) ?: "N/A"

                val currentTimeMillis = System.currentTimeMillis()
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                val formattedTime = sdf.format(Date(currentTimeMillis))

                val bookingData = mapOf(
                    "status" to "booked",
                    "paymentId" to razorpayPaymentId,
                    "amountPaid" to selectedSlotPrice,
                    "price" to selectedSlotPrice,
                    "userUID" to userUID,
                    "email" to userEmail,
                    "phone" to userPhone,
                    "customerName" to userName,
                    "timeBooked" to formattedTime,
                    "timestamp" to currentTimeMillis,
                    "timestamp" to System.currentTimeMillis()
                ) as Map<String, Any>

                val slotRef = FirebaseDatabase.getInstance().getReference("slotsCreatedFromOwner")
                    .child(shopUID!!)
                    .child(selectedDate!!)
                    .child(selectedSlotTime!!)

                // Now store booking data in slot node
                slotRef.updateChildren(bookingData).addOnSuccessListener {
                    Toast.makeText(this, "Slot booked successfully!", Toast.LENGTH_SHORT).show()
                    loadAvailableSlots()
                    showConfirmationDialog()
                }.addOnFailureListener { exception ->
                    Log.e("BookingUpdateError", "Failed to update booking: ${exception.message}", exception)
                    Toast.makeText(this, "Failed to book. Try again.", Toast.LENGTH_LONG).show()
                }

            }.addOnFailureListener {
                Toast.makeText(this, "Failed to fetch user info", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Payment success but missing slot details", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showConfirmationDialog() {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("Booking Confirmed 🎉")
        builder.setMessage("Your slot at $selectedSlotTime on $selectedDate is successfully booked!")

        builder.setPositiveButton("OK") { dialog, _ ->
            dialog.dismiss()
        }

        val dialog = builder.create()
        dialog.setCancelable(false)
        dialog.show()
    }

    override fun onPaymentError(code: Int, response: String?) {
        // Show user-friendly message
        Toast.makeText(this, "Payment Failed: $response", Toast.LENGTH_SHORT).show()

        // Log detailed info to Crashlytics
        FirebaseCrashlytics.getInstance().log("Payment failed - Code: $code, Response: $response")
        FirebaseCrashlytics.getInstance().setCustomKey("PaymentErrorCode", code)
        FirebaseCrashlytics.getInstance().setCustomKey("PaymentErrorResponse", response ?: "null")

        FirebaseCrashlytics.getInstance().recordException(
            RuntimeException("Razorpay Payment Failed - Code: $code, Response: $response")
        )
    }
}
