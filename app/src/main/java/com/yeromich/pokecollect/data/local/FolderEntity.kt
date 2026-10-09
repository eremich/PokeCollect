package com.yeromich.pokecollect.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A user-made collection ("EX & V", "For trade") that groups cards the user owns. */
@Entity(tableName = "folders")
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long
)

/**
 * One owned card variant filed in one folder. Deleting a folder drops its rows; removing a card
 * from the collection is cleaned up by [CollectedCardDao] since the card table has a composite key.
 */
@Entity(
    tableName = "folder_cards",
    primaryKeys = ["folderId", "cardId", "variant"],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("cardId", "variant")]
)
data class FolderCardEntity(
    val folderId: Long,
    val cardId: String,
    val variant: String
)

/** A folder with its size and worth at last known prices. */
data class FolderSummary(
    val id: Long,
    val name: String,
    val entries: Int,
    val totalEur: Double
)

/** A card image used to draw a folder's little stack preview. */
data class FolderPreview(
    val folderId: Long,
    val imageSmall: String
)
