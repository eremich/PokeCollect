package com.yeromich.pokecollect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedCardDao {

    @Query("SELECT * FROM collected_cards ORDER BY addedAt DESC")
    fun getAll(): Flow<List<CollectedCardEntity>>

    @Query("SELECT * FROM collected_cards WHERE id = :id LIMIT 1")
    fun getById(id: String): Flow<CollectedCardEntity?>

    @Query("SELECT * FROM collected_cards WHERE id = :cardId AND variant = :variant LIMIT 1")
    fun getByIdAndVariant(cardId: String, variant: String): Flow<CollectedCardEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CollectedCardEntity)

    @Query("UPDATE collected_cards SET quantity = quantity + 1 WHERE id = :id")
    suspend fun incrementQuantity(id: String)

    @Query("UPDATE collected_cards SET quantity = quantity + 1 WHERE id = :cardId AND variant = :variant")
    suspend fun incrementByVariant(cardId: String, variant: String)

    @Query("UPDATE collected_cards SET quantity = quantity - 1 WHERE id = :cardId AND variant = :variant AND quantity > 1")
    suspend fun decrementByVariant(cardId: String, variant: String)

    @Query("DELETE FROM collected_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM collected_cards WHERE id = :cardId AND variant = :variant")
    suspend fun deleteByIdAndVariant(cardId: String, variant: String)

    @Query("SELECT * FROM collected_cards WHERE id = :cardId AND variant = :variant LIMIT 1")
    suspend fun findByIdAndVariant(cardId: String, variant: String): CollectedCardEntity?

    /** Re-files copies under another variant, merging with any copies already stored there. */
    @Transaction
    suspend fun moveVariant(cardId: String, from: String, to: String) {
        val source = findByIdAndVariant(cardId, from) ?: return
        val target = findByIdAndVariant(cardId, to)
        deleteByIdAndVariant(cardId, from)
        upsert(target?.copy(quantity = target.quantity + source.quantity) ?: source.copy(variant = to))
    }

    @Query("SELECT COUNT(*) FROM collected_cards")
    fun getCount(): Flow<Int>
}
