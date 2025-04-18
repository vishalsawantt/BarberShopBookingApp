package com.example.barbershopbookingapp

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.*

class BookingAdapter(
    private val bookings: List<Booking>,
    private val isCustomer: Boolean
) : RecyclerView.Adapter<BookingAdapter.BookingViewHolder>() {

    class BookingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvShopName: TextView = view.findViewById(R.id.tvShopName)
        val tvUserName: TextView = view.findViewById(R.id.tvUserName)
        val tvBookingDate: TextView = view.findViewById(R.id.tvBookingDate)
        val tvBookingTime: TextView = view.findViewById(R.id.tvBookingTime)
        val tvAmountPaid: TextView = view.findViewById(R.id.tvAmountPaid)
        val tvStatus: TextView = view.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookingViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_booking, parent, false)
        return BookingViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookingViewHolder, position: Int) {
        val booking = bookings[position]

        // Customer View: Show shop name, amount paid, and status
        if (isCustomer) {
            holder.tvShopName.visibility = View.VISIBLE
            holder.tvShopName.text = "🏬 ${booking.shopName}"
            holder.tvUserName.visibility = View.GONE
            holder.tvStatus.visibility = View.VISIBLE
            holder.tvStatus.text = "✅ Status: ${booking.status}"
        }
        // Shopkeeper View: Show customer name, amount paid
        else {
            holder.tvShopName.visibility = View.GONE
            holder.tvUserName.visibility = View.VISIBLE
            holder.tvUserName.text = "👤 Fetching..."
            fetchCustomerName(booking.userUID) { name ->
                holder.tvUserName.text = "👤 $name"
            }
            holder.tvStatus.visibility = View.GONE
        }

        holder.tvBookingDate.text = "📅 Date: ${booking.date}"
        holder.tvBookingTime.text = "⏰ Time: ${booking.time}"
        holder.tvAmountPaid.text = "💰 Amount Paid: ₹${booking.amountPaid}"
    }

    override fun getItemCount(): Int = bookings.size

    // Fetch customer name using userUID
    private fun fetchCustomerName(userUID: String, callback: (String) -> Unit) {
        val database = FirebaseDatabase.getInstance().reference
        database.child("customersDetails").child(userUID).child("name")

            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val customerName = snapshot.value as? String ?: "Unknown User"
                    callback(customerName)
                }

                override fun onCancelled(error: DatabaseError) {
                    callback("Unknown User")
                }
            })
    }
}
