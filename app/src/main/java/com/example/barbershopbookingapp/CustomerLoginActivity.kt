package com.example.barbershopbookingapp

import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

class CustomerLoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var googleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_login)

        auth = FirebaseAuth.getInstance()
        sharedPreferences = getSharedPreferences("CustomerPrefs", MODE_PRIVATE)

        val etEmail = findViewById<EditText>(R.id.etEmail)
        val etPassword = findViewById<EditText>(R.id.etPassword)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val tvSignup = findViewById<TextView>(R.id.tvSignup)
        val btnGoogleLogin = findViewById<LinearLayout>(R.id.btnGoogleLogin)

        // Configure Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        btnLogin.setOnClickListener {
            val email = etEmail.text.toString().trim()
            val password = etPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val databaseRef = FirebaseDatabase.getInstance().getReference()

            // Check if the email exists in shopOwnerDetails first
            databaseRef.child("shopOwnerDetails").get().addOnSuccessListener { shopSnapshot ->
                var isShopOwner = false

                for (shopData in shopSnapshot.children) {
                    val shopEmail = shopData.child("email").value.toString()
                    if (shopEmail == email) {
                        isShopOwner = true
                        break
                    }
                }

                if (isShopOwner) {
                    Toast.makeText(this, "You are not registered. Please log in from the shop owner page.", Toast.LENGTH_LONG).show()
                    return@addOnSuccessListener
                }

                // Check if the email exists in customersDetails
                databaseRef.child("customersDetails").get().addOnSuccessListener { customerSnapshot ->
                    var isCustomer = false

                    for (customerData in customerSnapshot.children) {
                        val customerEmail = customerData.child("email").value.toString()
                        if (customerEmail == email) {
                            isCustomer = true
                            break
                        }
                    }

                    if (isCustomer) {
                        // Proceed with Customer Login
                        auth.signInWithEmailAndPassword(email, password)
                            .addOnCompleteListener { task ->
                                if (task.isSuccessful) {
                                    // Save login details in SharedPreferences
                                    val editor = sharedPreferences.edit()
                                    editor.putString("email", email)
                                    editor.putString("password", password)
                                    editor.apply()

                                    Toast.makeText(this, "Login Successful", Toast.LENGTH_SHORT).show()
                                    startActivity(Intent(this, CustomerDashboardActivity::class.java))
                                    finish()
                                } else {
                                    Toast.makeText(this, "Login Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                    } else {
                        Toast.makeText(this, "No customer account found with this email. Please sign up.", Toast.LENGTH_LONG).show()
                    }
                }.addOnFailureListener {
                    Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(this, "Database Error: ${it.message}", Toast.LENGTH_SHORT).show()
            }
        }


        tvSignup.setOnClickListener {
            startActivity(Intent(this, CustomerSignupActivity::class.java))
        }

        // Handle Google Sign-In click
        btnGoogleLogin.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            googleLoginLauncher.launch(signInIntent)
        }

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

    // Google Sign-In result handler
    private val googleLoginLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        handleSignInResult(task)
    }

    private fun handleSignInResult(task: Task<GoogleSignInAccount>) {
        try {
            val account = task.getResult(Exception::class.java)
            firebaseAuthWithGoogle(account)
        } catch (e: Exception) {
            Toast.makeText(this, "Google Sign-In Failed: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun firebaseAuthWithGoogle(account: GoogleSignInAccount?) {
        if (account == null) return

        val credential = GoogleAuthProvider.getCredential(account.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    checkUserExists(user)
                } else {
                    Toast.makeText(this, "Google Sign-In Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun checkUserExists(user: FirebaseUser?) {
        if (user == null) return

        val database: DatabaseReference = FirebaseDatabase.getInstance().getReference("customersDetails")

        database.child(user.uid).get().addOnSuccessListener { dataSnapshot ->
            if (dataSnapshot.exists()) {
                // User already exists, navigate to dashboard
                Toast.makeText(this, "Login Successful!", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CustomerDashboardActivity::class.java))
                finish()
            } else {
                // New user, save details in Firebase
                val customerData = CustomerDetails(user.uid, user.displayName ?: "Unknown", user.email ?: "", "")
                database.child(user.uid).setValue(customerData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Account Created Successfully!", Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this, CustomerDashboardActivity::class.java))
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Database Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun storeLoginStatus() {
        val sharedPreferences = getSharedPreferences("UserPref", MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putBoolean("isLoggedIn", true) // Mark user as logged in
        editor.apply()
    }
}
