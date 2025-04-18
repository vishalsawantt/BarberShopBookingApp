package com.example.barbershopbookingapp

import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FirebaseFirestore

class ShopkeeperChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private val customerList = mutableListOf<ChatUser>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_shopkeeper_chat, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewShopkeeperChat)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setStatusBarColorToWhite()

        adapter = ChatListAdapter(customerList) { customer ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("receiverUID", customer.uid)
            intent.putExtra("receiverName", customer.name)
            startActivity(intent)
        }

        recyclerView.adapter = adapter
        loadCustomers()
        return view
    }

    private fun setStatusBarColorToWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.WHITE

                // To make the status bar text dark (better visibility on a light background)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
    }

    private fun loadCustomers() {
        val shopUID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val databaseRef = FirebaseDatabase.getInstance().getReference("customersDetails")

        db.collection("userChats").document(shopUID).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    customerList.clear()
                    val customerIds = document.data?.keys ?: emptySet()

                    for (customerUID in customerIds) {
                        val customerRef = databaseRef.child(customerUID).child("name")
//                      Log.d("FirestoreDebug", "Fetching customer details from: ${customerRef.path}")
                        Log.d("FirestoreDebug", "Fetching customer details from: ${customerRef.toString()}")

                        customerRef.get()
                            .addOnSuccessListener { customerSnapshot ->
                                if (customerSnapshot.exists()) {
                                    val customerName = customerSnapshot.value.toString()
                                    Log.d("FirestoreDebug", "Fetched Customer Name: $customerName")
                                    customerList.add(ChatUser(customerUID, customerName))
                                } else {
                                    Log.e("FirestoreDebug", "Customer name not found for: $customerUID")
                                    customerList.add(ChatUser(customerUID, "Unknown Customer"))
                                }
                                adapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirestoreDebug", "Error fetching customer name: ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load chat list", Toast.LENGTH_SHORT).show()
            }
    }
}
