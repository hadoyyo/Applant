//PlantAdapter.kt
package pl.example.applant

import android.content.Intent
import android.graphics.Typeface
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson
import java.text.SimpleDateFormat
import java.util.*

class PlantAdapter(private var plants: List<Plant>) :
    RecyclerView.Adapter<PlantAdapter.PlantViewHolder>() {

    class PlantViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val descriptionTextView: TextView = view.findViewById(R.id.text_description)
        val imageView: ImageView = view.findViewById(R.id.image_plant)
        val roomTextView: TextView = view.findViewById(R.id.text_room)
        val wateringIntervalTextView: TextView = view.findViewById(R.id.text_watering_interval)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlantViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_plant, parent, false)
        return PlantViewHolder(view)
    }

    private fun sortDates(dates: List<String>): List<String> {
        return dates.sortedWith(compareBy {
            val parts = it.split("-")
            // Parsowanie daty w formacie dd-MM-yyyy
            Calendar.getInstance().apply {
                set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt()) // dd-MM-yyyy
            }.timeInMillis
        })
    }

    override fun onBindViewHolder(holder: PlantViewHolder, position: Int) {
        val plant = plants[position]
        val context = holder.itemView.context

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

        holder.nameTextView.text = plant.name
        holder.descriptionTextView.text = plant.description

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, PlantDetailActivity::class.java)
            intent.putExtra("plant", Gson().toJson(plant))
            holder.itemView.context.startActivity(intent)
        }

        val room = plant.room
        if (room != null) {
            holder.roomTextView.text = context.getString(R.string.room) + ": ${room.name}"
        } else {
            holder.roomTextView.text = context.getString(R.string.no_room_assigned)
        }

        holder.wateringIntervalTextView.setTextColor(ContextCompat.getColor(context, R.color.text))
        holder.wateringIntervalTextView.setTypeface(null, Typeface.NORMAL)
        holder.wateringIntervalTextView.visibility = View.VISIBLE

        if (plant.wateringIntervalDays > 0) {
            if (plant.wateringDates.isNotEmpty()) {
                val sortedDates = sortDates(plant.wateringDates)
                val lastWateringDate = sortedDates.last()
                val lastWateringCalendar = parseDate(lastWateringDate)

                val nextWateringCalendar = Calendar.getInstance().apply {
                    timeInMillis = lastWateringCalendar.timeInMillis
                    add(Calendar.DAY_OF_YEAR, plant.wateringIntervalDays)
                }

                val currentDate = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }

                nextWateringCalendar.set(Calendar.HOUR_OF_DAY, 0)
                nextWateringCalendar.set(Calendar.MINUTE, 0)
                nextWateringCalendar.set(Calendar.SECOND, 0)
                nextWateringCalendar.set(Calendar.MILLISECOND, 0)

                if (!currentDate.before(nextWateringCalendar)) {
                    holder.wateringIntervalTextView.text = context.getString(R.string.requires_watering)
                    holder.wateringIntervalTextView.setTextColor(ContextCompat.getColor(context, R.color.blue))
                    holder.wateringIntervalTextView.setTypeface(null, Typeface.BOLD)
                } else {
                    val daysRemaining = ((nextWateringCalendar.timeInMillis - currentDate.timeInMillis) / (1000 * 60 * 60 * 24)).toInt()
                    holder.wateringIntervalTextView.text = when (daysRemaining) {
                        1 -> context.getString(R.string.day_until_watering)
                        else -> context.getString(R.string.days_until_watering, daysRemaining)
                    }
                }
            } else {
                holder.wateringIntervalTextView.text = when (plant.wateringIntervalDays) {
                    1 -> context.getString(R.string.watering_every_day)
                    else -> context.getString(R.string.watering_every_x_days, plant.wateringIntervalDays)
                }
            }
        } else {
            holder.wateringIntervalTextView.visibility = View.GONE
        }

    }

    private fun parseDate(dateString: String): Calendar {
        val parts = dateString.split("-")
        return Calendar.getInstance().apply {
            set(parts[2].toInt(), parts[1].toInt() - 1, parts[0].toInt())
        }
    }

    override fun getItemCount() = plants.size

    fun updatePlants(newPlants: List<Plant>) {
        this.plants = newPlants
        notifyDataSetChanged()
    }
}