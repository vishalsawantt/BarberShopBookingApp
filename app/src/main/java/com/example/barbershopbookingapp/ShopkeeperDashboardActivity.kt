package com.example.barbershopbookingapp

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomnavigation.BottomNavigationView

class ShopkeeperDashboardActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shopkeeper_dashboard)

        val bottomNavigationView = findViewById<BottomNavigationView>(R.id.bottom_navigation_menu)

        // Set default fragment
        if (savedInstanceState == null) {
            replaceFragment(ShopkeeperSlotFragment())
        }

        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.slotfragment -> {
                    replaceFragment(ShopkeeperSlotFragment())
                    true
                }
                R.id.chatfragment -> {
                    replaceFragment(ShopkeeperChatFragment())
                    true
                }
                R.id.bookstatus -> {
                    replaceFragment(ShopkeeperNotificationFragment())
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
