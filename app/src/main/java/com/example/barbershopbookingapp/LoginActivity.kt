package com.example.barbershopbookingapp

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPref: android.content.SharedPreferences
    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        databaseRef = FirebaseDatabase.getInstance().getReference("shopOwner")
        sharedPref = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        // Check if the user is already logged in
        val isLoggedIn = sharedPref.getBoolean("isLoggedIn", false)
        val currentUser = auth.currentUser

        if (isLoggedIn && currentUser != null) {
            // Redirect directly to ShopkeeperDashboardActivity
            startActivity(Intent(this, ShopkeeperDashboardActivity::class.java))
            finish()
        }

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val btnSignup = findViewById<TextView>(R.id.btnSignup)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = auth.currentUser
                        user?.uid?.let { uid ->
                            checkIfUserIsShopOwner(uid, email, password)
                        }
                    } else {
                        Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        btnSignup.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }

        //circular back button
        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        btnBack.setOnClickListener {
            val intent = Intent(this, RoleSelectionActivity::class.java)
            startActivity(intent)
            finish()
        }

        setStatusBarColorToWhite()
        storeLoginStatus()
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

    private fun checkIfUserIsShopOwner(uid: String, email: String, password: String) {
        databaseRef.child(uid).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                // Save credentials to SharedPreferences
                val editor = sharedPref.edit()
                editor.putString("email", email)
                editor.putString("password", password)
                editor.apply()

                Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, ShopkeeperDashboardActivity::class.java))
                finish()
            } else {
                // User is not a shop owner
                auth.signOut()
                Toast.makeText(this, "Access Denied: You are not a shop owner please first register", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Error checking user role", Toast.LENGTH_SHORT).show()
        }
    }

    private fun storeLoginStatus() {
        val sharedPreferences = getSharedPreferences("UserPref", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true)
        editor.apply()
    }
}
