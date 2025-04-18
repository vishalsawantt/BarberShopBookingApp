package com.example.barbershopbookingapp

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: DatabaseReference
    private lateinit var googleMap: GoogleMap
    private var selectedLatLng: LatLng? = null
    private var marker: Marker? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance().getReference("shopOwner")
        // Initialize Google Map
        val mapFragment = supportFragmentManager.findFragmentById(R.id.mapFragment) as SupportMapFragment
        mapFragment.getMapAsync(this)
        val txtSignin = findViewById<TextView>(R.id.txtSignin)

        findViewById<android.widget.Button>(R.id.btnRegister).setOnClickListener {
            registerShopkeeper()
        }

        //already signup so open login page
        txtSignin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
        setStatusBarColorToWhite()
    }

    //my method for white bar
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        val defaultLocation = LatLng(19.0760, 72.8777) // Example: Mumbai
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        // Add marker on tap
        googleMap.setOnMapClickListener { latLng ->
            marker?.remove() // Remove existing marker
            marker = googleMap.addMarker(MarkerOptions().position(latLng).title("Shop Location"))
            selectedLatLng = latLng
        }
    }

    private fun registerShopkeeper() {
        //Declaration and Initialization of the View
        val ownerName = findViewById<EditText>(R.id.etOwnerName).text.toString()
        val shopName = findViewById<EditText>(R.id.etShopName).text.toString()
        val address = findViewById<EditText>(R.id.etAddress).text.toString()
        val email = findViewById<EditText>(R.id.etEmail).text.toString()
        val password = findViewById<EditText>(R.id.etPassword).text.toString()
        val mobileNo = "+91" + findViewById<EditText>(R.id.etMobileNo).text.toString().trim()

        if (ownerName.isEmpty() || shopName.isEmpty() || address.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (selectedLatLng == null) {
            Toast.makeText(this, "Please select shop location on the map", Toast.LENGTH_SHORT).show()
            return
        }

        // Register user with Firebase Authentication
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uid = auth.currentUser?.uid!!
                    val shopkeeper = Shopkeeper(
                        uid, ownerName, shopName, address, email, mobileNo,
                        selectedLatLng!!.latitude, selectedLatLng!!.longitude
                    )
                    // Store in Firebase Realtime Database
                    database.child(uid).setValue(shopkeeper)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Shopkeeper registered successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, ShopkeeperSlotFragment::class.java))
                            finish()
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Registration Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}