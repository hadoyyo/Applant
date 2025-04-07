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

class Plants : Fragment() {

    private lateinit var fab: FloatingActionButton
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyTextView: TextView
    private lateinit var plantAdapter: PlantAdapter
    private lateinit var plantStorage: PlantStorage
    private lateinit var roomStorage: RoomStorage

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_plants, container, false)
        fab = view.findViewById(R.id.fab_add_plant)
        recyclerView = view.findViewById(R.id.recycler_view)
        emptyTextView = view.findViewById(R.id.text_empty)

        // Konfiguracja RecyclerView
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        plantStorage = PlantStorage(requireContext())
        roomStorage = RoomStorage(requireContext())
        plantAdapter = PlantAdapter(plantStorage.getPlants())
        recyclerView.adapter = plantAdapter

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
                val intent = Intent(requireContext(), AddPlantActivity::class.java)
                startActivityForResult(intent, REQUEST_ADD_PLANT)
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (isAdded) {
            when {
                requestCode == REQUEST_ADD_PLANT && resultCode == Activity.RESULT_OK -> updateUI()
                requestCode == REQUEST_EDIT_PLANT && resultCode == Activity.RESULT_OK -> updateUI()
                requestCode == REQUEST_EDIT_ROOM && resultCode == Activity.RESULT_OK -> updateUI()
            }
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

        val plants = plantStorage.getPlants()

        if (plants.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyTextView.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyTextView.visibility = View.GONE
            plantAdapter.updatePlants(plants)
        }
    }

    companion object {
        private const val REQUEST_ADD_PLANT = 2
        private const val REQUEST_EDIT_PLANT = 3
        private const val REQUEST_EDIT_ROOM = 4
    }
}