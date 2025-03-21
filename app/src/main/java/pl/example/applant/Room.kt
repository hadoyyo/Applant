// Room.kt
package pl.example.applant

import java.util.UUID

data class Room(
    var id: String = UUID.randomUUID().toString(),
    var name: String,
    var description: String,
    var imageUri: String? = null,
    var defaultImageResId: Int? = null, // ID zasobu domyślnego zdjęcia
    var isDefaultImage: Boolean = false // Czy zdjęcie jest domyślne
)