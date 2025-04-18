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

class CustomerChatFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: ChatListAdapter
    private val shopList = mutableListOf<ChatUser>()


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?

    ): View {
        val view = inflater.inflate(R.layout.fragment_customer_chat, container, false)
        recyclerView = view.findViewById(R.id.recyclerViewCustomerChat)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        setStatusBarColorToWhite()


        // Set up Adapter and Click Listener
        adapter = ChatListAdapter(shopList) { shop ->
            val intent = Intent(requireContext(), ChatActivity::class.java)
            intent.putExtra("receiverUID", shop.uid)
            intent.putExtra("ownerName", shop.name)
            startActivity(intent)
        }

        recyclerView.adapter = adapter

        loadShopkeepers()

        return view
    }

    private fun setStatusBarColorToWhite() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            activity?.window?.apply {
                clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
                addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                statusBarColor = Color.WHITE

                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                }
            }
        }
    }


    private fun loadShopkeepers() {
        val userUID = FirebaseAuth.getInstance().currentUser?.uid ?: return
        val db = FirebaseFirestore.getInstance()
        val databaseRef = FirebaseDatabase.getInstance().getReference("shopOwner")

        db.collection("userChats").document(userUID).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    shopList.clear()
                    val shopIds = document.data?.keys ?: emptySet()

                    for (shopUID in shopIds) {
                        Log.d("FirestoreDebug", "Fetching shop details for UID: $shopUID")

                        // Fetch shop name from FD
                        databaseRef.child(shopUID).child("shopName").get()
                            .addOnSuccessListener { shopSnapshot ->
                                if (shopSnapshot.exists()) {
                                    val shopName = shopSnapshot.value.toString()
                                    Log.d("FirestoreDebug", "Fetched Shop Name: $shopName")
                                    shopList.add(ChatUser(shopUID, shopName))
                                } else {
                                    Log.e("FirestoreDebug", "Shop name not found for: $shopUID")
                                    shopList.add(ChatUser(shopUID, "Unknown Shop"))
                                }
                                adapter.notifyDataSetChanged()
                            }
                            .addOnFailureListener { e ->
                                Log.e("FirestoreDebug", "Error fetching shop name: ${e.message}")
                            }
                    }
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "Failed to load chat list", Toast.LENGTH_SHORT).show()
            }
    }

}
