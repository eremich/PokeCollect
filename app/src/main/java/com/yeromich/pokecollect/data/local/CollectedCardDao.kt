package com.yeromich.pokecollect.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectedCardDao {

    @Query("SELECT * FROM collected_cards ORDER BY addedAt DESC")
    fun getAll(): Flow<List<CollectedCardEntity>>

    @Query("SELECT * FROM collected_cards WHERE id = :id")
    fun getById(id: String): Flow<CollectedCardEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: CollectedCardEntity)

    @Query("UPDATE collected_cards SET quantity = quantity + 1 WHERE id = :id")
    suspend fun incrementQuantity(id: String)

    @Query("DELETE FROM collected_cards WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("SELECT COUNT(*) FROM collected_cards")
    fun getCount(): Flow<Int>
}
