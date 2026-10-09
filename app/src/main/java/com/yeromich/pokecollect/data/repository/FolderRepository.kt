package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.Folder
import kotlinx.coroutines.flow.Flow

interface FolderRepository {
    fun getFolders(): Flow<List<Folder>>
    fun getCards(folderId: Long): Flow<List<Card>>
    fun getValue(folderId: Long): Flow<CollectionValue>
    fun getFolderIds(cardId: String, variant: String): Flow<Set<Long>>
    suspend fun create(name: String): Long
    suspend fun rename(folderId: Long, name: String)
    suspend fun delete(folderId: Long)
    suspend fun setMemberships(cardId: String, variant: String, folderIds: Set<Long>)
}
