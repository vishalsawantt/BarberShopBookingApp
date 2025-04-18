package com.example.barbershopbookingapp

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*

class DatewiseSlots : AppCompatActivity() {

    private lateinit var database: DatabaseReference
    private lateinit var slotContainer: LinearLayout
    private lateinit var txtSelectedDate: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_datewise_slots)

        txtSelectedDate = findViewById(R.id.txtSelectedDate)
        slotContainer = findViewById(R.id.slotContainer)

        val selectedDate = intent.getStringExtra("selectedDate")
        val shopUID = intent.getStringExtra("shopUID")

        if (selectedDate == null || shopUID == null) {
            Toast.makeText(this, "Invalid data", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        txtSelectedDate.text = "Slots for: $selectedDate"

        database = FirebaseDatabase.getInstance().getReference("slotsCreatedFromOwner")
            .child(shopUID).child(selectedDate)

        loadSlots()
        setStatusBarColorToWhite()
    }

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

    private fun loadSlots() {
        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                slotContainer.removeAllViews()

                if (!snapshot.exists()) {
                    val noSlotsText = TextView(this@DatewiseSlots).apply {
                        text = "No slots available for this date."
                        textSize = 18f
                        setTextColor(Color.RED)
                        gravity = Gravity.CENTER
                        setPadding(16, 16, 16, 16)
                    }
                    slotContainer.addView(noSlotsText)
                    return
                }

                for (slotSnapshot in snapshot.children) {
                    val slot = slotSnapshot.key ?: continue
                    val price = slotSnapshot.child("price").getValue(String::class.java) ?: "0"
                    val status = slotSnapshot.child("status").getValue(String::class.java) ?: "available"

                    // Create MaterialCardView for each slot
                    val cardView = MaterialCardView(this@DatewiseSlots).apply {
                        layoutParams = LinearLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.WRAP_CONTENT
                        ).apply {
                            setMargins(8, 8, 8, 8)
                        }
                        radius = 12f
                        elevation = 4f
                        strokeWidth = 2
                        strokeColor = if (status == "booked") Color.RED else Color.GREEN
                        setBackgroundColor(if (status == "booked") Color.parseColor("#FFCDD2") else Color.parseColor("#C8E6C9"))
                    }

                    val slotTextView = TextView(this@DatewiseSlots).apply {
                        text = "$slot - ₹$price"
                        textSize = 16f
                        setPadding(16, 16, 16, 16)
                        setTextColor(Color.BLACK)
                    }

                    cardView.addView(slotTextView)
                    slotContainer.addView(cardView)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@DatewiseSlots, "Failed to load slots", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
