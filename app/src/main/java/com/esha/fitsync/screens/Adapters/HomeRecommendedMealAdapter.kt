package com.esha.fitsync.screens.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.esha.fitsync.R
import com.esha.fitsync.models.HomeRecommendedMeal
import com.google.android.material.button.MaterialButton

class HomeRecommendedMealAdapter(
    private var items: List<HomeRecommendedMeal>,
    private val onAddMeal: (HomeRecommendedMeal) -> Unit
) : RecyclerView.Adapter<HomeRecommendedMealAdapter.ViewHolder>() {

    fun submitList(newItems: List<HomeRecommendedMeal>) {
        items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val meal = items[position]

        holder.tvRecipeName.text = meal.name.ifBlank { "Nutritious Meal" }
        holder.tvRecipeBadge.text = meal.mealType.uppercase().ifBlank { "RECOMMENDED" }
        holder.tvRecipeKcalBadge.text = "${meal.calories} KCAL"
        holder.tvRecipeProtBadge.text = "${meal.protein.toInt()}g PROT"
        holder.tvRecipeSubtitle.text = "P: ${meal.protein.toInt()}g • C: ${meal.carbs.toInt()}g • F: ${meal.fat.toInt()}g"

        if (!meal.imageUrl.isNullOrBlank()) {
            Glide.with(holder.itemView.context)
                .load(meal.imageUrl)
                .placeholder(android.R.drawable.ic_menu_gallery)
                .error(android.R.drawable.ic_menu_gallery)
                .centerCrop()
                .into(holder.ivRecipeImage)
        } else {
            holder.ivRecipeImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnViewRecipeItem.visibility = View.GONE
        holder.btnAddRecipeItem.setOnClickListener {
            onAddMeal(meal)
        }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivRecipeImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        val tvRecipeBadge: TextView = itemView.findViewById(R.id.tvRecipeBadge)
        val tvRecipeKcalBadge: TextView = itemView.findViewById(R.id.tvRecipeKcalBadge)
        val tvRecipeProtBadge: TextView = itemView.findViewById(R.id.tvRecipeProtBadge)
        val tvRecipeName: TextView = itemView.findViewById(R.id.tvRecipeName)
        val tvRecipeSubtitle: TextView = itemView.findViewById(R.id.tvRecipeSubtitle)
        val btnViewRecipeItem: MaterialButton = itemView.findViewById(R.id.btnViewRecipeItem)
        val btnAddRecipeItem: MaterialButton = itemView.findViewById(R.id.btnAddRecipeItem)
    }
}
