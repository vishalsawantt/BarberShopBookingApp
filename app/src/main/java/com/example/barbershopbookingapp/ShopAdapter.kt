package com.example.barbershopbookingapp

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ShopAdapter(private val context: Context, private var shopList: ArrayList<Shopkeeper>) :
    RecyclerView.Adapter<ShopAdapter.ShopViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShopViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_shop, parent, false)
        return ShopViewHolder(view)
    }

    override fun onBindViewHolder(holder: ShopViewHolder, position: Int) {
        val shop = shopList[position]
        holder.shopName.text = shop.shopName
        holder.ownerName.text = "Owner: ${shop.ownerName}"

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, ShopDetailsActivity::class.java)
            intent.putExtra("shopUID", shop.uid)
            intent.putExtra("shopName", shop.shopName)
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount(): Int {
        return shopList.size
    }

    class ShopViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val shopName: TextView = itemView.findViewById(R.id.txtShopName)
        val ownerName: TextView = itemView.findViewById(R.id.txtOwnerName)
    }

    // Update the list dynamically for search results
    fun updateList(newList: ArrayList<Shopkeeper>) {
        shopList = newList
        notifyDataSetChanged()
    }
}
