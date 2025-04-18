package com.example.barbershopbookingapp

import android.content.Intent
import android.graphics.Color
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
class ShopDetailsActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var database: DatabaseReference
    private lateinit var slotList: ArrayList<String>
    private lateinit var slotAdapter: SlotAdapter
    private lateinit var recyclerView: RecyclerView
    private lateinit var txtShopName: TextView
    private lateinit var txtStatus: TextView
    private lateinit var txtRouteDetails: TextView
    private lateinit var googleMap: GoogleMap
    private var shopLocation: LatLng? = null
    private var userLocation: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shop_details)
        setStatusBarColorToWhite()

        txtShopName = findViewById(R.id.txtShopName)
        txtStatus = findViewById(R.id.txtStatus)
        val btnMessageShopOwner = findViewById<Button>(R.id.btnMessageShopOwner)

        recyclerView = findViewById(R.id.recyclerViewSlots)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        slotList = ArrayList()
        val shopUID = intent.getStringExtra("shopUID") ?: return

        slotAdapter = SlotAdapter(this, dateList = ArrayList(), shopUID) { selectedSlot ->
            openCustomerSlotBooking(shopUID, selectedSlot)
        }
        recyclerView.adapter = slotAdapter

        txtRouteDetails = findViewById(R.id.txtRouteDetails)
        txtShopName.text = intent.getStringExtra("shopName") ?: "Shop"

        database = FirebaseDatabase.getInstance().getReference("shops").child(shopUID)
        loadAvailableSlots(shopUID)

        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)

        btnMessageShopOwner.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            intent.putExtra("receiverUID", shopUID) // Send shop owner UID for chat
            startActivity(intent)
        }
    }
    //status bar
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

    private fun openCustomerSlotBooking(shopUID: String, selectedDate: String) {
        val intent = Intent(this, CustomerSlotBooking::class.java)
        intent.putExtra("shopUID", shopUID)
        intent.putExtra("selected_date", selectedDate)
        startActivity(intent)
    }

    private fun loadAvailableSlots(shopUID: String) {
        val slotDatabase = FirebaseDatabase.getInstance().getReference("slotsCreatedFromOwner").child(shopUID)

        val slotList = ArrayList<Pair<String, Int>>() // Store date + available slots count

        slotDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                slotList.clear()
                for (dateSnapshot in snapshot.children) {
                    val date = dateSnapshot.key ?: continue
                    var availableSlots = 0

                    // Count available slots
                    for (slotSnapshot in dateSnapshot.children) {
                        val status = slotSnapshot.child("status").getValue(String::class.java) ?: "unknown"
                        if (status == "available") {
                            availableSlots++
                        }
                    }

                    slotList.add(Pair(date, availableSlots)) // Store (date, available slots)
                }
                txtStatus.visibility = if (slotList.isEmpty()) View.VISIBLE else View.GONE
                txtStatus.text = if (slotList.isEmpty()) "Shop Closed" else ""
                recyclerView.visibility = if (slotList.isEmpty()) View.GONE else View.VISIBLE

                slotAdapter = SlotAdapter(this@ShopDetailsActivity, slotList, shopUID) { selectedDate ->
                    openCustomerSlotBooking(shopUID, selectedDate)
                }
                recyclerView.adapter = slotAdapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopDetailsActivity, "Failed to load slots", Toast.LENGTH_SHORT).show()
            }
        })
    }


    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val shopUID = intent.getStringExtra("shopUID") ?: return
        val shopLocationRef = FirebaseDatabase.getInstance().getReference("shopOwner").child(shopUID)
        shopLocationRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val lat = snapshot.child("latitude").getValue(Double::class.java)
                val lng = snapshot.child("longitude").getValue(Double::class.java)


                if (lat != null && lng != null) {
                    shopLocation = LatLng(lat, lng)
                    googleMap.addMarker(MarkerOptions().position(shopLocation!!).title(snapshot.child("shopName").getValue(String::class.java)))
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(shopLocation!!, 15f))
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopDetailsActivity, "Failed to load shop location", Toast.LENGTH_SHORT).show()
            }
        })
        googleMap.setOnMapClickListener { selectedLocation ->
            googleMap.clear()
            shopLocation?.let {
                googleMap.addMarker(MarkerOptions().position(it).title("Shop Location"))
            }
            googleMap.addMarker(MarkerOptions().position(selectedLocation).title("Your Location"))
            userLocation = selectedLocation
            shopLocation?.let { shopLatLng ->
                val polylineOptions = PolylineOptions()
                    .add(shopLatLng)
                    .add(selectedLocation)
                    .width(5f)
                    .color(android.graphics.Color.RED)
                googleMap.addPolyline(polylineOptions)
                val distance = calculateDistance(shopLatLng, selectedLocation)
                txtRouteDetails.text = "Distance: ${"%.2f".format(distance)} km"
                loadNearbyShops(selectedLocation)
            }
        }
    }

    private fun calculateDistance(start: LatLng, end: LatLng): Float {
        val result = FloatArray(1)
        Location.distanceBetween(
            start.latitude, start.longitude,
            end.latitude, end.longitude,
            result
        )
        return result[0] / 1000
    }

    private fun loadNearbyShops(userLocation: LatLng) {
        val shopDatabase = FirebaseDatabase.getInstance().getReference("shopOwner")
        shopDatabase.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var nearbyShops = ""
                for (shopSnapshot in snapshot.children) {
                    val lat = shopSnapshot.child("latitude").getValue(Double::class.java)
                    val lng = shopSnapshot.child("longitude").getValue(Double::class.java)
                    val shopName = shopSnapshot.child("shopName").getValue(String::class.java)
                    if (lat != null && lng != null && shopName != null) {
                        val shopLatLng = LatLng(lat, lng)
                        val distance = calculateDistance(userLocation, shopLatLng)
                        if (distance < 5) {
                            googleMap.addMarker(MarkerOptions().position(shopLatLng).title(shopName))
                            nearbyShops += "$shopName (${String.format("%.2f", distance)} km)\n"
                        }
                    }
                }
                txtRouteDetails.text = "Nearby Shops:\n$nearbyShops"
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ShopDetailsActivity, "Failed to load nearby shops", Toast.LENGTH_SHORT).show()
            }
        })
    }
}
