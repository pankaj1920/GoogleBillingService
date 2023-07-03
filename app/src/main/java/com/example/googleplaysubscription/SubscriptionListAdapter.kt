package com.example.googleplaysubscription

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubscriptionListAdapter(private val subList: ArrayList<SubItemModel>) :
    RecyclerView.Adapter<SubscriptionListAdapter.Subscription_VH>() {

    private lateinit var mListener: OnItemClickListener

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Subscription_VH {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_subscription_list, parent, false)
        return Subscription_VH(view, mListener)
    }

    override fun getItemCount(): Int = subList.size

    override fun onBindViewHolder(holder: Subscription_VH, position: Int) {
        val currentItem = subList[position]
        holder.tvSubPlan.text = currentItem.subscriptionName
        holder.tvSubPrice.text = currentItem.formattedPrice
    }

    class Subscription_VH(itemView: View, listener: OnItemClickListener) :
        RecyclerView.ViewHolder(itemView) {

        val tvSubPlan: TextView = itemView.findViewById(R.id.tvSubPlan)
        val tvSubPrice: TextView = itemView.findViewById(R.id.tvSubPrice)

        init {
            itemView.setOnClickListener {
                listener.onItemClick(position = adapterPosition)
            }
        }
    }

    fun setOnItemClickListener(listener: OnItemClickListener) {
        mListener = listener
    }

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }
}