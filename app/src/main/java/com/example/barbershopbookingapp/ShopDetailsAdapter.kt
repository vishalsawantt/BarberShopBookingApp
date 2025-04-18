package com.example.barbershopbookingapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class ShopDetailsAdapter(
    private val context: Context,
    private val slotList: ArrayList<Triple<String, String, String>>, // Triple<Time, Price, Status>
    private val onSlotClick: (String, String) -> Unit
) : RecyclerView.Adapter<ShopDetailsAdapter.SlotViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SlotViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_slot_details, parent, false)
        return SlotViewHolder(view)
    }

    override fun onBindViewHolder(holder: SlotViewHolder, position: Int) {
        val (slotTime, price, status) = slotList[position]
        holder.txtSlotTime.text = slotTime
        holder.txtSlotPrice.text = "₹$price"

        // Set background color
        if (status == "booked") {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.faint_red))
            holder.itemView.alpha = 0.6f
        } else {
            holder.itemView.setBackgroundColor(ContextCompat.getColor(context, R.color.faint_green))
            holder.itemView.alpha = 1.0f
        }

        // Click listener
        holder.itemView.setOnClickListener {
            if (status == "booked") {
                Toast.makeText(context, "This slot is already booked", Toast.LENGTH_SHORT).show()
            } else {
                onSlotClick(slotTime, price)
            }
        }
    }

    override fun getItemCount(): Int = slotList.size

    class SlotViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtSlotTime: TextView = itemView.findViewById(R.id.txtSlotTime)
        val txtSlotPrice: TextView = itemView.findViewById(R.id.txtSlotPrice)
    }
}
