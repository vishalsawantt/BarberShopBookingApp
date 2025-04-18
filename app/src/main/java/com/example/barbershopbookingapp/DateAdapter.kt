package com.example.barbershopbookingapp

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.database.FirebaseDatabase

class DateAdapter(
    private val context: Context,
    private var dateList: MutableList<String>,
    private val shopUID: String
) : RecyclerView.Adapter<DateAdapter.DateViewHolder>() {

    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DateViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_date, parent, false)
        return DateViewHolder(view)
    }

    override fun onBindViewHolder(holder: DateViewHolder, position: Int) {
        val date = dateList[position]
        holder.dateTextView.text = date

        // Highlight selected date
        holder.itemView.setBackgroundResource(
            if (position == selectedPosition) R.drawable.selected_date_background
            else R.drawable.default_date_background
        )

        // Open DatewiseSlots activity on click
        holder.itemView.setOnClickListener {
            val adapterPosition = holder.adapterPosition
            if (adapterPosition != RecyclerView.NO_POSITION) {
                selectedPosition = adapterPosition
                notifyDataSetChanged()

                val intent = Intent(context, DatewiseSlots::class.java)
                intent.putExtra("selectedDate", dateList[adapterPosition])
                intent.putExtra("shopUID", shopUID)
                context.startActivity(intent)
            }
        }

        // Delete button functionality
        holder.deleteButton.setOnClickListener {
            confirmDeleteDate(date, position)
        }
    }

    override fun getItemCount(): Int = dateList.size

    class DateViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val dateTextView: TextView = itemView.findViewById(R.id.tvDate)
        val deleteButton: ImageButton = itemView.findViewById(R.id.btnDeleteDate)
    }

    private fun confirmDeleteDate(date: String, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Delete Date?")
            .setMessage("Are you sure you want to delete this date? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteDateFromFirebase(date, position)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteDateFromFirebase(date: String, position: Int) {
        val databaseRef = FirebaseDatabase.getInstance()
            .getReference("slotsCreatedFromOwner")
            .child(shopUID)
            .child(date)

        // First, get the data before deleting
        databaseRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val deletedData = snapshot.value

                // Move deleted data to OwnerDeletedData
                val deletedDataRef = FirebaseDatabase.getInstance()
                    .getReference("OwnerDeletedData")
                    .child(shopUID)
                    .child(date)

                deletedDataRef.setValue(deletedData).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Now delete the original data
                        databaseRef.removeValue().addOnCompleteListener { deleteTask ->
                            if (deleteTask.isSuccessful) {
                                Toast.makeText(context, "Data deleted", Toast.LENGTH_SHORT).show()
                                dateList.removeAt(position)
                                notifyItemRemoved(position)
                            } else {
                                Toast.makeText(context, "Failed to delete date", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Failed to store deleted data", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(context, "Date not found", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener {
            Toast.makeText(context, "Error fetching data", Toast.LENGTH_SHORT).show()
        }
    }

}
