package pl.example.applant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class Rooms : Fragment() {

    private lateinit var fab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var roomAdapter: RoomAdapter
    private lateinit var roomStorage: RoomStorage

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_rooms, container, false)
        fab = view.findViewById(R.id.fab_add_room)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyTextView = view.findViewById(R.id.text_empty)

        // Konfiguracja RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        roomStorage = RoomStorage(requireContext())
        roomAdapter = RoomAdapter(roomStorage.getRooms())
        recyclerView.adapter = roomAdapter

        // Opóźnione pokazanie FAB z animacją
        fab.postDelayed({
            if (isAdded) {
                fab.visibility = View.VISIBLE
                val anim = AnimationUtils.loadAnimation(requireContext(), R.anim.fade_in)
                fab.startAnimation(anim)
            }
        }, 400)

        // Inicjalna aktualizacja UI
        updateUI()

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab.setOnClickListener {
            if (isAdded) {
                val intent = Intent(requireContext(), AddRoomActivity::class.java)
                startActivityForResult(intent, REQUEST_ADD_ROOM)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isAdded && requestCode == REQUEST_ADD_ROOM && resultCode == Activity.RESULT_OK) {
            updateUI()
        }
    }

    override fun onResume() {
        super.onResume()
        if (isAdded) {
            updateUI()
        }
    }

    private fun updateUI() {
        if (!isAdded) return

        val rooms = roomStorage.getRooms()

        if (rooms.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
            roomAdapter.updateRooms(rooms)
        }
    }

    companion object {
        private const val REQUEST_ADD_ROOM = 2
    }
}