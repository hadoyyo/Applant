//RoomStorage.kt
package pl.example.applant

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class RoomStorage(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("rooms_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val plantStorage = PlantStorage(context) // Dodajemy PlantStorage

    fun saveRooms(rooms: List<Room>) {
        val json = gson.toJson(rooms)
        prefs.edit().putString("rooms_list", json).apply()
    }

    fun getRooms(): List<Room> {
        val json = prefs.getString("rooms_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Room>>() {}.type
        return gson.fromJson(json, type)
    }

    fun updateRoom(updatedRoom: Room) {
        val rooms = getRooms().toMutableList()
        val index = rooms.indexOfFirst { it.id == updatedRoom.id }

        if (index != -1) {
            rooms[index] = updatedRoom
            saveRooms(rooms)
        }
    }

    fun deleteRoom(roomId: String) {
        val rooms = getRooms().toMutableList()
        val roomToDelete = rooms.find { it.id == roomId }

        if (roomToDelete != null) {
            // Usuń pokój
            rooms.remove(roomToDelete)
            saveRooms(rooms)

            // Zaktualizuj rośliny, które były przypisane do tego pokoju
            updatePlantsAfterRoomDeletion(roomId)
        }
    }

    private fun updatePlantsAfterRoomDeletion(roomId: String) {
        val plants = plantStorage.getPlants().toMutableList()
        plants.forEach { plant ->
            if (plant.room?.id == roomId) {
                plant.room = null // Usuń przypisanie do pokoju
            }
        }
        plantStorage.savePlants(plants) // Zapisz zaktualizowane rośliny
    }

    fun getRoomByName(roomName: String): Room? {
        val rooms = getRooms()
        return rooms.find { it.name == roomName }
    }
}