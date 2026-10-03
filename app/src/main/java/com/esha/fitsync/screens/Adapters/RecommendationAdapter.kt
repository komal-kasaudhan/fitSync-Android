package com.esha.fitsync.screens.Adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.esha.fitsync.R
import com.esha.fitsync.models.RecipeRecommendation
import com.google.android.material.button.MaterialButton

class RecommendationAdapter(
    private var items: List<RecipeRecommendation>,
    private val onViewRecipe: (RecipeRecommendation) -> Unit,
    private val onAddMeal: (RecipeRecommendation) -> Unit
) : RecyclerView.Adapter<RecommendationAdapter.ViewHolder>() {

    fun submitList(newItems: List<RecipeRecommendation>) {
        this.items = newItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_recipe_recommendation, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]

        holder.tvName.text = item.name
        holder.tvKcal.text = "${item.calories} KCAL"
        holder.tvProt.text = "${item.protein}g PROT"

        val prep = if (item.prepTimeMin > 0) "${item.prepTimeMin}m" else "15m"
        val serving = item.servingSize ?: "1 serving"
        holder.tvSubtitle.text = "⏱️ $prep • $serving"

        // Badge
        holder.tvBadge.text = when {
            item.isVegan == true -> "VEGAN"
            item.isVegetarian == true -> "VEGETARIAN"
            item.isEggetarian == true -> "EGGETARIAN"
            item.protein >= 35 -> "HIGH PROTEIN"
            else -> "SEASONAL"
        }

        // Image loading via Coil
        if (!item.imageUrl.isNullOrEmpty()) {
            holder.ivImage.load(item.imageUrl) {
                crossfade(true)
                placeholder(android.R.drawable.ic_menu_gallery)
                error(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.ivImage.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        holder.btnView.setOnClickListener { onViewRecipe(item) }
        holder.btnAdd.setOnClickListener { onAddMeal(item) }
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivRecipeImage)
        val tvBadge: TextView = itemView.findViewById(R.id.tvRecipeBadge)
        val tvKcal: TextView = itemView.findViewById(R.id.tvRecipeKcalBadge)
        val tvProt: TextView = itemView.findViewById(R.id.tvRecipeProtBadge)
        val tvName: TextView = itemView.findViewById(R.id.tvRecipeName)
        val tvSubtitle: TextView = itemView.findViewById(R.id.tvRecipeSubtitle)
        val btnView: MaterialButton = itemView.findViewById(R.id.btnViewRecipeItem)
        val btnAdd: MaterialButton = itemView.findViewById(R.id.btnAddRecipeItem)
    }
}
