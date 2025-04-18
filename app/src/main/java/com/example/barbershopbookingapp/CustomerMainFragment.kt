package com.example.barbershopbookingapp

import android.app.AlertDialog
import android.content.Context.MODE_PRIVATE
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*

class CustomerMainFragment : Fragment() {

    private lateinit var database: DatabaseReference
    private lateinit var shopList: ArrayList<Shopkeeper>
    private lateinit var filteredList: ArrayList<Shopkeeper>
    private lateinit var shopAdapter: ShopAdapter  //show the shop list with shopowner name and click item open ShopDetailsActivity
    private lateinit var tvCustomerName: TextView
    private lateinit var searchView: SearchView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.activity_customer_main, container, false)

        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewShops)
        searchView = view.findViewById(R.id.searchViewShops)
        tvCustomerName = view.findViewById(R.id.tvCustomerName)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        shopList = ArrayList()
        filteredList = ArrayList()
        shopAdapter = ShopAdapter(requireContext(), filteredList)
        recyclerView.adapter = shopAdapter

        database = FirebaseDatabase.getInstance().getReference("shopOwner")

        val btnLogout = view.findViewById<ImageButton>(R.id.btnLogout)

        btnLogout.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setTitle("Logout Confirmation")
                .setMessage("Are you sure you want to log out?")
                .setPositiveButton("Yes") { _, _ ->
                    FirebaseAuth.getInstance().signOut()
                    Toast.makeText(requireContext(), "Logged out!", Toast.LENGTH_SHORT).show()

                    // Clear login status from SharedPreferences
                    val sharedPreferences = requireActivity().getSharedPreferences("UserPref", MODE_PRIVATE)
                    val editor = sharedPreferences.edit()
                    editor.putBoolean("isLoggedIn", false)
                    editor.apply()

                    // Navigate to RoleSelectionActivity
                    val intent = Intent(requireActivity(), RoleSelectionActivity::class.java)
                    startActivity(intent)
                    requireActivity().finish()
                }
                .setNegativeButton("Cancel", null)
                .show()
        }


        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                shopList.clear()
                for (shopSnapshot in snapshot.children) {
                    val shop = shopSnapshot.getValue(Shopkeeper::class.java)
                    if (shop != null) {
                        shopList.add(shop)
                    }
                }
                filteredList.clear()
                filteredList.addAll(shopList)
                shopAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(requireContext(), "Failed to fetch data", Toast.LENGTH_SHORT).show()
                Log.e("FirebaseError", error.message)
            }
        })
        fetchCustomerName()
        setupSearchView()
        return view
    }

    private fun fetchCustomerName() {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        if (userId != null) {
            val customerRef = FirebaseDatabase.getInstance().getReference("customersDetails").child(userId)
            customerRef.addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val customerName = snapshot.child("name").getValue(String::class.java)
                    if (customerName != null) {
                        tvCustomerName.text = "Hello, $customerName!"
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    Log.e("FirebaseError", error.message)
                }
            })
        }
    }

    //status bar
    private fun setupSearchView() {
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterShops(query)
                return true
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                filterShops(newText)
                return true
            }
        })
    }

    private fun filterShops(query: String?) {
        val searchText = query?.lowercase() ?: ""
        val filteredResults = ArrayList<Shopkeeper>()
        if (searchText.isEmpty()) {
            filteredResults.addAll(shopList)
        } else {
            for (shop in shopList) {
                if (shop.shopName.lowercase().contains(searchText) ||
                    shop.ownerName.lowercase().contains(searchText) ||
                    shop.address.lowercase().contains(searchText)) {
                    filteredResults.add(shop)
                }
            }
        }
        shopAdapter.updateList(filteredResults)
    }
}
