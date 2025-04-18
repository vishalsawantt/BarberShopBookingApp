package com.example.barbershopbookingapp

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth

class RoleSelectionActivity : AppCompatActivity() {
    private lateinit var btnShopkeeper: Button
    private lateinit var btnCustomer: Button
    private lateinit var sharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_role_selection)

        sharedPreferences = getSharedPreferences("UserPref", MODE_PRIVATE)

        // Check if user is already logged in
        checkIfUserIsLoggedIn()

        btnShopkeeper = findViewById(R.id.btnShopkeeper)
        btnCustomer = findViewById(R.id.btnCustomer)

        btnShopkeeper.setOnClickListener {
            saveUserRole("Shopkeeper")
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }

        btnCustomer.setOnClickListener {
            saveUserRole("Customer")
            startActivity(Intent(this, CustomerLoginActivity::class.java))
            finish()
        }

        setStatusBarColorToWhite()
    }

    private fun saveUserRole(role: String) {
        val editor = sharedPreferences.edit()
        editor.putString("userRole", role)
        editor.putBoolean("isLoggedIn", false)
        editor.apply()
    }

    private fun checkIfUserIsLoggedIn() {
        val userRole = sharedPreferences.getString("userRole", null)
        val isLoggedIn = sharedPreferences.getBoolean("isLoggedIn", false)

        val currentUser = FirebaseAuth.getInstance().currentUser

        if (userRole != null && isLoggedIn && currentUser != null) {
            // User is logged in, send them to the correct dashboard
            when (userRole) {
                "Shopkeeper" -> startActivity(Intent(this, ShopkeeperDashboardActivity::class.java))
                "Customer" -> startActivity(Intent(this, CustomerDashboardActivity::class.java))
            }
            finish()
        }
    }

    private fun setStatusBarColorToWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val window = window
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.WHITE

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            }
        }
    }
}
