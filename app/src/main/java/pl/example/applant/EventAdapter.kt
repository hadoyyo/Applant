package pl.example.applant

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class EventAdapter(
    private var data: Map<Int, Map<String, List<Int>>>,
    private val context: Context
) : RecyclerView.Adapter<EventAdapter.YearViewHolder>() {

    inner class YearViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textYear: TextView = itemView.findViewById(R.id.text_year)
        val recyclerMonths: RecyclerView = itemView.findViewById(R.id.recycler_months)
        val textEmptyMessage: TextView = itemView.findViewById(R.id.text_empty_message)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): YearViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_year, parent, false)
        return YearViewHolder(view)
    }

    override fun onBindViewHolder(holder: YearViewHolder, position: Int) {
        val year = data.keys.elementAt(position)
        val monthsMap = data[year] ?: emptyMap()

        holder.textYear.text = year.toString()

        if (monthsMap.isEmpty()) {
            holder.textEmptyMessage.visibility = View.VISIBLE
            holder.recyclerMonths.visibility = View.GONE
        } else {
            holder.textEmptyMessage.visibility = View.GONE
            holder.recyclerMonths.visibility = View.VISIBLE

            val monthAdapter = MonthAdapter(monthsMap, context)
            holder.recyclerMonths.layoutManager = LinearLayoutManager(holder.itemView.context)
            holder.recyclerMonths.adapter = monthAdapter
        }
    }

    override fun getItemCount(): Int {
        return data.size
    }

    fun updateData(newData: Map<Int, Map<String, List<Int>>>) {
        data = newData
        notifyDataSetChanged()
    }

    // Adapter dla miesięcy
    inner class MonthAdapter(
        private val monthsMap: Map<String, List<Int>>,
        private val context: Context
    ) : RecyclerView.Adapter<MonthAdapter.MonthViewHolder>() {

        private val seasonColors = mapOf(
            "Zima" to Triple(Color.parseColor("#B3E5FC"), "☃️", Color.parseColor("#03A9F4")),
            "Wiosna" to Triple(Color.parseColor("#C8E6C9"), "🌱", Color.parseColor("#4CAF50")),
            "Lato" to Triple(Color.parseColor("#FFF9C4"), "☀️", Color.parseColor("#FFEB3B")),
            "Jesień" to Triple(Color.parseColor("#FFCCBC"), "🍂", Color.parseColor("#FF5722"))
        )

        inner class MonthViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textMonth: TextView = itemView.findViewById(R.id.text_month)
            val recyclerDays: RecyclerView = itemView.findViewById(R.id.recycler_days)
            val textSeasonEmoji: TextView = itemView.findViewById(R.id.text_season_emoji)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MonthViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_month, parent, false)
            return MonthViewHolder(view)
        }

        override fun onBindViewHolder(holder: MonthViewHolder, position: Int) {
            val month = monthsMap.keys.elementAt(position)
            val days = monthsMap[month] ?: emptyList()

            holder.textMonth.text = month

            val season = when (month) {
                context.getString(R.string.december), context.getString(R.string.january), context.getString(R.string.february) -> "Zima"
                context.getString(R.string.march), context.getString(R.string.april), context.getString(R.string.may) -> "Wiosna"
                context.getString(R.string.june), context.getString(R.string.july), context.getString(R.string.august) -> "Lato"
                context.getString(R.string.september), context.getString(R.string.october), context.getString(R.string.november) -> "Jesień"
                else -> ""
            }

            val (color, emoji, circleColor) = seasonColors[season] ?: Triple(Color.WHITE, "", Color.GRAY)
            holder.itemView.setBackgroundColor(color)
            holder.textSeasonEmoji.text = emoji

            val dayAdapter = DayAdapter(days, circleColor)
            holder.recyclerDays.layoutManager = LinearLayoutManager(holder.itemView.context, LinearLayoutManager.HORIZONTAL, false)
            holder.recyclerDays.adapter = dayAdapter
        }

        override fun getItemCount(): Int {
            return monthsMap.size
        }

        inner class DayAdapter(private val days: List<Int>, private val circleColor: Int) :
            RecyclerView.Adapter<DayAdapter.DayViewHolder>() {

            inner class DayViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
                val textDay: TextView = itemView.findViewById(R.id.text_day)
            }

            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DayViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_day, parent, false)
                view.findViewById<View>(R.id.circle_background).setBackgroundColor(circleColor)
                return DayViewHolder(view)
            }

            override fun onBindViewHolder(holder: DayViewHolder, position: Int) {
                holder.textDay.text = days[position].toString()
            }

            override fun getItemCount(): Int {
                return days.size
            }
        }
    }
}
