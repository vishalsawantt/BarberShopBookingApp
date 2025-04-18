package com.example.barbershopbookingapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CustomerNotificationFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var auth: FirebaseAuth
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: BookingAdapter
    private val bookingList = mutableListOf<Booking>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.activity_customer_notification, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewBookings)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().reference

        adapter = BookingAdapter(bookingList, isCustomer = true)
        recyclerView.adapter = adapter

        fetchUserBookings()

        return view
    }

    private fun fetchUserBookings() {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val databaseRef = FirebaseDatabase.getInstance().reference.child("slotsCreatedFromOwner")

        databaseRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val userBookings = mutableListOf<Booking>()
                val shopUIDsToFetch = mutableSetOf<String>()

                for (shopSnapshot in snapshot.children) { // shopUID level
                    val shopUID = shopSnapshot.key ?: continue

                    for (dateSnapshot in shopSnapshot.children) { // date level
                        val date = dateSnapshot.key ?: continue

                        for (timeSnapshot in dateSnapshot.children) { // time level
                            val time = timeSnapshot.key ?: continue
                            val slotData = timeSnapshot.getValue(Booking::class.java)

                            if (slotData?.userUID == userUID) { // Only get bookings for this user
                                userBookings.add(
                                    Booking(
                                        shopName = shopUID,
                                        date = date,
                                        time = time,
                                        amountPaid = slotData.amountPaid,
                                        status = slotData.status,
                                        userUID = userUID
                                    )
                                )
                                shopUIDsToFetch.add(shopUID)
                            }
                        }
                    }
                }

                // Fetch Shop Names from shopOwner/shopUID/shopName
                fetchShopNames(shopUIDsToFetch) { shopNames ->
                    userBookings.forEach { booking ->
                        booking.shopName = shopNames[booking.shopName] ?: "Unknown Shop"
                    }
                    adapter = BookingAdapter(userBookings, isCustomer = true)
                    recyclerView.adapter = adapter
                }
            }

            private fun fetchShopNames(shopUIDs: Set<String>, callback: (Map<String, String>) -> Unit) {
                val databaseRef = FirebaseDatabase.getInstance().reference.child("shopOwner")
                val shopNamesMap = mutableMapOf<String, String>()

                val tasks = shopUIDs.map { shopUID ->
                    databaseRef.child(shopUID).child("shopName").get().continueWith { task ->
                        if (task.isSuccessful) {
                            shopNamesMap[shopUID] = task.result?.value as? String ?: "Unknown Shop"
                        }
                    }
                }

                Tasks.whenAllComplete(tasks).addOnCompleteListener {
                    callback(shopNamesMap)
                }
            }


            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchShopName(shopUID: String, callback: (String) -> Unit) {
        database.child("shopOwner").child(shopUID).child("shopName")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val shopName = snapshot.value as? String ?: "Unknown Shop"
                    callback(shopName)
                }
                override fun onCancelled(error: DatabaseError) {
                    callback("Unknown Shop")
                }
            })
    }
}
