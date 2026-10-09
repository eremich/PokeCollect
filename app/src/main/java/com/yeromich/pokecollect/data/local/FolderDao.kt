package com.yeromich.pokecollect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {

    @Insert
    suspend fun insert(folder: FolderEntity): Long

    @Query("UPDATE folders SET name = :name WHERE id = :folderId")
    suspend fun rename(folderId: Long, name: String)

    @Query("DELETE FROM folders WHERE id = :folderId")
    suspend fun delete(folderId: Long)

    // Joins on collected_cards so a stale membership row can never be counted or shown.
    @Query(
        """
        SELECT f.id AS id, f.name AS name,
               COUNT(c.id) AS entries,
               COALESCE(SUM(c.priceEur * c.quantity), 0) AS totalEur
        FROM folders f
        LEFT JOIN folder_cards fc ON fc.folderId = f.id
        LEFT JOIN collected_cards c ON c.id = fc.cardId AND c.variant = fc.variant
        GROUP BY f.id
        ORDER BY f.createdAt
        """
    )
    fun getSummaries(): Flow<List<FolderSummary>>

    @Query(
        """
        SELECT fc.folderId AS folderId, c.imageSmall AS imageSmall
        FROM folder_cards fc
        JOIN collected_cards c ON c.id = fc.cardId AND c.variant = fc.variant
        ORDER BY c.addedAt DESC
        """
    )
    fun getPreviews(): Flow<List<FolderPreview>>

    @Query(
        """
        SELECT c.* FROM collected_cards c
        JOIN folder_cards fc ON c.id = fc.cardId AND c.variant = fc.variant
        WHERE fc.folderId = :folderId
        ORDER BY c.addedAt DESC
        """
    )
    fun getCards(folderId: Long): Flow<List<CollectedCardEntity>>

    @Query(
        """
        SELECT COALESCE(SUM(c.priceEur * c.quantity), 0) AS totalEur,
               COALESCE(SUM(CASE WHEN c.priceEur IS NOT NULL THEN 1 ELSE 0 END), 0) AS pricedEntries,
               COUNT(*) AS entries
        FROM collected_cards c
        JOIN folder_cards fc ON c.id = fc.cardId AND c.variant = fc.variant
        WHERE fc.folderId = :folderId
        """
    )
    fun getValue(folderId: Long): Flow<CollectionValue>

    @Query("SELECT folderId FROM folder_cards WHERE cardId = :cardId AND variant = :variant")
    fun getFolderIds(cardId: String, variant: String): Flow<List<Long>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertMemberships(rows: List<FolderCardEntity>)

    @Query("DELETE FROM folder_cards WHERE cardId = :cardId AND variant = :variant")
    suspend fun clearMemberships(cardId: String, variant: String)

    /** Files one card variant in exactly [folderIds], replacing whatever it was in before. */
    @Transaction
    suspend fun setMemberships(cardId: String, variant: String, folderIds: Set<Long>) {
        clearMemberships(cardId, variant)
        insertMemberships(folderIds.map { FolderCardEntity(it, cardId, variant) })
    }
}
