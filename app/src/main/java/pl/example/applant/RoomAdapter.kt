package pl.example.applant

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.gson.Gson

class RoomAdapter(private var rooms: List<Room>) : RecyclerView.Adapter<RoomAdapter.RoomViewHolder>() {

    class RoomViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView = view.findViewById(R.id.text_name)
        val descriptionTextView: TextView = view.findViewById(R.id.text_description)
        val imageView: ImageView = view.findViewById(R.id.image_room)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_room, parent, false)
        return RoomViewHolder(view)
    }

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        val room = rooms[position]

        holder.nameTextView.text = room.name
        holder.descriptionTextView.text = room.description

        // Obsługa domyślnych zdjęć
        if (room.isDefaultImage) {
            room.defaultImageResId?.also { resId ->
                holder.imageView.setImageResource(resId)
            }
        } else if (!room.imageUri.isNullOrEmpty()) {
            Glide.with(holder.imageView.context)
                .load(room.imageUri)
                .into(holder.imageView)
        } else {
            holder.imageView.setImageResource(R.drawable.room_1)
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(holder.itemView.context, RoomDetailActivity::class.java)
            intent.putExtra("room", Gson().toJson(room))
            holder.itemView.context.startActivity(intent)
        }
    }

    override fun getItemCount() = rooms.size

    fun updateRooms(newRooms: List<Room>) {
        this.rooms = newRooms
        notifyDataSetChanged()
    }
}