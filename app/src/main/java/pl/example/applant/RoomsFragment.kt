//RoomsFragment.kt
package pl.example.applant

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton

class RoomsFragment : Fragment() {

    private lateinit var fab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var roomAdapter: RoomAdapter
    private lateinit var roomStorage: RoomStorage

    private val handler = Handler(Looper.getMainLooper())
    private val updateRunnable = object : Runnable {
        override fun run() {
            updateUI()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_rooms, container, false)
        fab = view.findViewById(R.id.fab_add_room)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyTextView = view.findViewById(R.id.text_empty)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        roomStorage = RoomStorage(requireContext())
        roomAdapter = RoomAdapter(roomStorage.getRooms())
        recyclerView.adapter = roomAdapter

        handler.post(updateRunnable)

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fab.setOnClickListener {
            val intent = Intent(requireContext(), AddRoomActivity::class.java)
            startActivityForResult(intent, REQUEST_ADD_ROOM)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_ADD_ROOM && resultCode == Activity.RESULT_OK) {
            updateUI()
        }
    }

    private fun updateUI() {
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

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacks(updateRunnable)
    }

    companion object {
        private const val REQUEST_ADD_ROOM = 2
    }
}