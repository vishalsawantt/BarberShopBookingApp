package com.example.barbershopbookingapp

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.text.TextUtils
import android.view.View
import android.view.WindowManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import java.util.*

class ChatActivity : AppCompatActivity() {

    private lateinit var txtChatOwnerName: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var edtMessage: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var adapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private val db = FirebaseFirestore.getInstance()
    private val currentUser = FirebaseAuth.getInstance().currentUser
    private var receiverUID: String? = null
    private var chatId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        recyclerView = findViewById(R.id.recyclerViewChat)
        edtMessage = findViewById(R.id.edtMessage)
        btnSend = findViewById(R.id.btnSend)
        txtChatOwnerName = findViewById(R.id.txtChatOwnerName)

        // Get the name of the chat partner (shopkeeper or customer)
        val receiverName = intent.getStringExtra("receiverName")
        val ownerName = intent.getStringExtra("ownerName")

        setStatusBarColorToWhite()


        // If receiverName is null, use ownerName; otherwise, use receiverName
        txtChatOwnerName.text = receiverName ?: ownerName ?: "Chat"


        receiverUID = intent.getStringExtra("receiverUID")

        if (currentUser == null || receiverUID == null) {
            Toast.makeText(this, "Error: Invalid chat", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        chatId = if (currentUser!!.uid < receiverUID!!) {
            "${currentUser!!.uid}_${receiverUID}"
        } else {
            "${receiverUID}_${currentUser!!.uid}"
        }

        recyclerView.layoutManager = LinearLayoutManager(this)
        adapter = ChatAdapter(messageList, currentUser!!.uid)
        recyclerView.adapter = adapter


        loadMessages()

        btnSend.setOnClickListener {
            sendMessage()
        }
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

    private fun loadMessages() {
        db.collection("chats").document(chatId!!).collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { value, error ->
                if (error != null) {
                    Toast.makeText(this, "Failed to load messages", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                val newMessages = mutableListOf<ChatMessage>()

                for (doc in value!!) {
                    val message = doc.toObject(ChatMessage::class.java)
                    newMessages.add(message)
                }

                // 🔹 Check if the user is already at the bottom before updating
                val shouldScroll = (recyclerView.adapter?.itemCount ?: 0) < newMessages.size

                messageList.clear()
                messageList.addAll(newMessages)
                adapter.notifyDataSetChanged()

                // 🔹 Scroll to bottom only if the user was already at the bottom
                if (shouldScroll) {
                    recyclerView.scrollToPosition(messageList.size - 1)
                }
            }
    }


    private fun sendMessage() {
        val messageText = edtMessage.text.toString().trim()
        if (TextUtils.isEmpty(messageText)) return

        val message = ChatMessage(
            senderId = currentUser!!.uid,
            receiverId = receiverUID!!,
            message = messageText,
            timestamp = System.currentTimeMillis()
        )

        db.collection("chats").document(chatId!!).collection("messages")
            .add(message)
            .addOnSuccessListener {
                edtMessage.setText("")
                addToChatList()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to send message", Toast.LENGTH_SHORT).show()
            }
    }

    private fun addToChatList() {
        val userChatRef = db.collection("userChats")

        userChatRef.document(currentUser!!.uid).update(receiverUID!!, true)
            .addOnFailureListener {
                userChatRef.document(currentUser!!.uid).set(mapOf(receiverUID!! to true))
            }

        userChatRef.document(receiverUID!!).update(currentUser!!.uid, true)
            .addOnFailureListener {
                userChatRef.document(receiverUID!!).set(mapOf(currentUser!!.uid to true))
            }
    }
}
