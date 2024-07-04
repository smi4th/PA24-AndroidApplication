package com.pariscaretaker.projet

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import android.util.Log

class HousingAdapter(
    private val housingList: List<Housing>,
    private val clickListener: (Housing) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_ITEM = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_ITEM
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_HEADER) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.empty_activity, parent, false)
            HeaderViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.housing_item_layout, parent, false)
            HousingViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is HousingViewHolder) {
            val adjustedPosition = position - 1
            if (adjustedPosition >= 0 && adjustedPosition < housingList.size) {
                val housing = housingList[adjustedPosition]
                holder.bind(housing, clickListener)
            } else {
                Log.e("HousingAdapter", "Invalid position: $position, adjustedPosition: $adjustedPosition for housingList size: ${housingList.size}")
            }
        }
    }

    override fun getItemCount(): Int {
        return housingList.size + 1
    }

    class HeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    }

    class HousingViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title: TextView = view.findViewById(R.id.housing_title)
        private val price: TextView = view.findViewById(R.id.housing_price)
        private val city: TextView = view.findViewById(R.id.housing_city)

        fun bind(housing: Housing, clickListener: (Housing) -> Unit) {
            title.text = housing.title
            price.text = housing.price + " €"
            city.text = housing.city
            itemView.setOnClickListener { clickListener(housing) }
        }
    }
}
