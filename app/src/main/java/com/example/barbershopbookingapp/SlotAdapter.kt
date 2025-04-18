package com.example.barbershopbookingapp

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SlotAdapter(
    private val context: Context,
    private val dateList: List<Pair<String, Int>>, // Pair<Date, Available Slot Count>
    private val shopUID: String,
    private val onDateClick: (String) -> Unit
) : RecyclerView.Adapter<SlotAdapter.DateViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_slot, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val (date, availableSlots) = dateList[position]

        holder.txtDate.text = "Date: $date"
        holder.txtAvailableSlots.text = "Available Slots: $availableSlots"

        holder.itemView.setOnClickListener {
            onDateClick(date)
        }
    }

    override fun getItemCount(): Int = dateList.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val txtDate: TextView = itemView.findViewById(R.id.txtDate)
        val txtAvailableSlots: TextView = itemView.findViewById(R.id.txtAvailableSlots)
    }
}
