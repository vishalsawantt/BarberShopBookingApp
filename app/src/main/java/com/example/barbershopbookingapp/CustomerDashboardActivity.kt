package com.example.barbershopbookingapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class CustomerDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customer_dashbord)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_menu)

        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(CustomerMainFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.slotfragment -> {
                    replaceFragment(CustomerMainFragment())
                    true
                }
                R.id.chatfragment -> {
                    replaceFragment(CustomerChatFragment())
                    true
                }
                R.id.bookstatus -> {
                    replaceFragment(CustomerNotificationFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}
