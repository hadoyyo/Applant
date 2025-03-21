//RoomPlantsAdapter.kt
package pl.example.applant

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class RoomPlantsAdapter(
    private var plants: List<Plant>,
    private val onRemoveAssignmentClicked: (Plant) -> Unit
) : RecyclerView.Adapter<RoomPlantsAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val descriptionTextView: TextView = view.findViewById(R.id.text_description)
        val imageView: ImageView = view.findViewById(R.id.image_plant)
        val removeButton: Button = view.findViewById(R.id.button_remove_assignment)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room_plant, parent, false)
        return PlantViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]

        holder.nameTextView.text = plant.name
        holder.descriptionTextView.text = plant.description

        if (plant.isDefaultImage) {
            plant.defaultImageResId?.also { resId ->
                holder.imageView.setImageResource(resId)
            }
        } else if (!plant.imageUri.isNullOrEmpty()) {
            Glide.with(holder.imageView.context)
                .load(plant.imageUri)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.plant_1)
        }

        holder.removeButton.setOnClickListener {
            onRemoveAssignmentClicked(plant)
        }
    }

    override fun getItemCount() = plants.size

    fun updatePlants(newPlants: List<Plant>) {
        this.plants = newPlants
        notifyDataSetChanged()
    }
}