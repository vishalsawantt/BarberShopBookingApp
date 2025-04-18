package com.example.barbershopbookingapp

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class ShopkeeperNotificationFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookingAdapter
    private val bookingList = mutableListOf<Booking>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_shopkeeper_notification, container, false)

        recyclerView = view.findViewById(R.id.recyclerSKViewBookings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        adapter = BookingAdapter(bookingList, isCustomer = false)
        recyclerView.adapter = adapter

        fetchShopkeeperBookings()
        setStatusBarColorToWhite()
        return view
    }

    private fun setStatusBarColorToWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.WHITE

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
    }

    private fun fetchShopkeeperBookings() {
        val shopOwnerUID = auth.currentUser?.uid ?: return

        database.child("slotsCreatedFromOwner").child(shopOwnerUID)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    bookingList.clear()

                    for (dateSnapshot in snapshot.children) {
                        val date = dateSnapshot.key ?: continue

                        for (timeSnapshot in dateSnapshot.children) {
                            val time = timeSnapshot.key ?: continue
                            val slotData = timeSnapshot.getValue(Booking::class.java)

                            if (slotData?.status == "booked") {
                                bookingList.add(
                                    Booking(
                                        date = date,
                                        time = time,
                                        amountPaid = slotData.amountPaid,
                                        status = slotData.status,
                                        userUID = slotData.userUID,
                                        customerName = "Fetching..."
                                    )
                                )
                            }
                        }
                    }
                    adapter.notifyDataSetChanged()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Failed to load bookings.", Toast.LENGTH_SHORT).show()
                }
            })
    }
}
