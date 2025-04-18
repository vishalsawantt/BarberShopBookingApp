package com.example.barbershopbookingapp

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.Context
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class ShopkeeperSlotFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var edtTimeSlot: EditText
    private lateinit var edtSlotPrice: EditText
    private lateinit var btnAddSlot: Button
    private lateinit var btnSelectDate: Button
    private lateinit var tvSelectedDate: TextView
    private lateinit var recyclerViewDates: RecyclerView
    private lateinit var dateAdapter: DateAdapter
    private lateinit var tvWelcome: TextView

    private val availableDates = mutableListOf<String>() // List of available dates
    private val createdSlots = mutableListOf<String>() // Slots for selected date
    private var selectedDate: String = "" // Default selected date

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_shopkeeper_slots, container, false)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("slotsCreatedFromOwner")
        sharedPreferences = requireActivity().getSharedPreferences("ShopkeeperSlots", Context.MODE_PRIVATE)

        tvWelcome = view.findViewById(R.id.tvWelcome)
        edtTimeSlot = view.findViewById(R.id.edtTimeSlot)
        edtSlotPrice = view.findViewById(R.id.edtDefaultPrice)
        btnAddSlot = view.findViewById(R.id.btnAddSlot)
        btnSelectDate = view.findViewById(R.id.btnSelectDate)
        tvSelectedDate = view.findViewById(R.id.tvSelectedDate)
        recyclerViewDates = view.findViewById(R.id.recyclerViewDates)

        recyclerViewDates.layoutManager = LinearLayoutManager(context, RecyclerView.VERTICAL, false)
        dateAdapter = DateAdapter(requireContext(), availableDates, auth.currentUser?.uid ?: "")
        recyclerViewDates.adapter = dateAdapter

        btnSelectDate.setOnClickListener {
            showDatePickerDialog()
        }

        btnAddSlot.setOnClickListener {
            val newSlot = edtTimeSlot.text.toString().trim()
            val price = edtSlotPrice.text.toString().trim()
            val shopUID = auth.currentUser?.uid ?: return@setOnClickListener

            if (newSlot.isNotEmpty() && price.isNotEmpty() && selectedDate.isNotEmpty()) {
                createdSlots.add(newSlot)
                saveSlotToFirebase(shopUID, selectedDate, newSlot, price)
                edtTimeSlot.text.clear()
                edtSlotPrice.text.clear()
            } else {
                Toast.makeText(requireContext(), "Enter valid slot, price, and select date!", Toast.LENGTH_SHORT).show()
            }
        }

        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)
        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut() // Logout from Firebase

                    // Clear SharedPreferences
                    val sharedPreferences = requireActivity().getSharedPreferences("UserPref", Context.MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isLoggedIn", false)
                    editor.apply()

                    Toast.makeText(requireContext(), "Logged out!", Toast.LENGTH_SHORT).show()

                    // open Roleselectionactivity
                    val intent = Intent(requireActivity(), RoleSelectionActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }

        loadAvailableDates()
        auth = FirebaseAuth.getInstance()
        fetchOwnerName()
        setStatusBarColorToWhite()
        return view
    }

    private fun setStatusBarColorToWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.WHITE

                // To make the status bar text dark (better visibility on a light background)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
    }

    private fun fetchOwnerName() {
        val shopUID = auth.currentUser?.uid ?: return
        val shopOwnerRef = FirebaseDatabase.getInstance().getReference("shopOwner")

        shopOwnerRef.child(shopUID).child("ownerName").get()
            .addOnSuccessListener { snapshot ->
                tvWelcome.text = "Welcome, ${snapshot.getValue(String::class.java) ?: "Shopkeeper"}!"
            }
            .addOnFailureListener {
                tvWelcome.text = "Welcome, Shopkeeper!"
            }
    }

    private fun showDatePickerDialog() {
        val calendar = Calendar.getInstance()
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH)
        val day = calendar.get(Calendar.DAY_OF_MONTH)

        val datePickerDialog = DatePickerDialog(requireContext(), { _, selectedYear, selectedMonth, selectedDay ->
            val selectedCalendar = Calendar.getInstance()
            selectedCalendar.set(selectedYear, selectedMonth, selectedDay)
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            selectedDate = dateFormat.format(selectedCalendar.time)
            tvSelectedDate.text = "Selected Date: $selectedDate"
            loadSlotsForDate(auth.currentUser?.uid ?: "", selectedDate)
        }, year, month, day)

        datePickerDialog.show()
    }

    private fun loadAvailableDates() {
        val shopUID = auth.currentUser?.uid ?: return
        database.child(shopUID).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                availableDates.clear()
                for (dateSnapshot in snapshot.children) {
                    val date = dateSnapshot.key ?: continue
                    availableDates.add(date)
                }
                dateAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun loadSlotsForDate(shopUID: String, date: String) {
        database.child(shopUID).child(date).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                createdSlots.clear()
                for (slotSnapshot in snapshot.children) {
                    val slot = slotSnapshot.key ?: continue
                    val price = slotSnapshot.child("price").getValue(String::class.java) ?: "0"
                    createdSlots.add("$slot | ₹$price")
                }
                dateAdapter.notifyDataSetChanged()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }


    private fun saveSlotToFirebase(shopUID: String, date: String, slot: String, price: String) {
        val slotData = mapOf("status" to "available", "price" to price)
        database.child(shopUID).child(date).child(slot).setValue(slotData)
            .addOnSuccessListener {
                Toast.makeText(requireContext(), "Slot saved successfully!", Toast.LENGTH_SHORT).show()
                loadSlotsForDate(shopUID, date)
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Error saving slot", Toast.LENGTH_SHORT).show()
            }
    }
}
