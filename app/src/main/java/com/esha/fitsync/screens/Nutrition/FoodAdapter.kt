package com.esha.fitsync.screens.Nutrition

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.esha.fitsync.databinding.ItemFoodSearchResultBinding

// 🥣 Data Model for Food Items
data class FoodItem(
    val id: String,
    val name: String,
    val category: String,
    val servingType: String,
    val servingWeight: Double,
    val calories: Int,
    val protein: Double,
    val carbs: Double,
    val fat: Double
)

class FoodAdapter(
    private val onItemClick: (FoodItem) -> Unit
) : ListAdapter<FoodItem, FoodAdapter.FoodViewHolder>(FoodDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FoodViewHolder {
        val binding = ItemFoodSearchResultBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return FoodViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FoodViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class FoodViewHolder(private val binding: ItemFoodSearchResultBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: FoodItem) {
            binding.tvFoodName.text = item.name
            binding.tvFoodServingInfo.text = "${item.servingType} (${item.servingWeight.toInt()}g) • ${item.category}"
            binding.tvItemCalories.text = "${item.calories} kcal"
            binding.tvItemProtein.text = "${item.protein}g Prot"

            binding.cardFoodItem.setOnClickListener { onItemClick(item) }
            binding.btnAddFoodItem.setOnClickListener { onItemClick(item) }
        }
    }

    class FoodDiffCallback : DiffUtil.ItemCallback<FoodItem>() {
        override fun areItemsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: FoodItem, newItem: FoodItem): Boolean = oldItem == newItem
    }
}