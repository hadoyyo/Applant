//Plant.kt
package pl.example.applant

import java.util.*

import java.util.UUID

data class Plant(
    val id: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String?,
    var wateringIntervalDays: Int,
    var room: Room? = null,
    var imageUri: String? = null,
    var wateringDates: MutableList<String> = mutableListOf(),
    var fertilizingDates: MutableList<String> = mutableListOf(),
    var repottingDates: MutableList<String> = mutableListOf(),
    var defaultImageResId: Int? = null,
    var isDefaultImage: Boolean = false,
    var note: String? = null // Nowe pole na notatkę
)


