package com.yeromich.pokecollect.data.repository

import com.yeromich.pokecollect.data.local.CollectionValue
import com.yeromich.pokecollect.data.local.FolderCardEntity
import com.yeromich.pokecollect.data.local.FolderDao
import com.yeromich.pokecollect.data.local.FolderEntity
import com.yeromich.pokecollect.data.local.toDomain
import com.yeromich.pokecollect.domain.model.Card
import com.yeromich.pokecollect.domain.model.Folder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject

private const val PREVIEW_CARDS = 3

class FolderRepositoryImpl @Inject constructor(
    private val dao: FolderDao
) : FolderRepository {

    override fun getFolders(): Flow<List<Folder>> =
        combine(dao.getSummaries(), dao.getPreviews()) { summaries, previews ->
            val imagesByFolder = previews.groupBy({ it.folderId }, { it.imageSmall })
            summaries.map { summary ->
                Folder(
                    id = summary.id,
                    name = summary.name,
                    entries = summary.entries,
                    totalEur = summary.totalEur,
                    previewImages = imagesByFolder[summary.id].orEmpty().take(PREVIEW_CARDS)
                )
            }
        }

    override fun getCards(folderId: Long): Flow<List<Card>> =
        dao.getCards(folderId).map { entities -> entities.map { it.toDomain() } }

    override fun getValue(folderId: Long): Flow<CollectionValue> = dao.getValue(folderId)

    override fun getFolderIds(cardId: String, variant: String): Flow<Set<Long>> =
        dao.getFolderIds(cardId, variant).map { it.toSet() }

    override suspend fun create(name: String): Long =
        dao.insert(FolderEntity(name = name.trim(), createdAt = System.currentTimeMillis()))

    override suspend fun rename(folderId: Long, name: String) = dao.rename(folderId, name.trim())

    override suspend fun delete(folderId: Long) = dao.delete(folderId)

    override suspend fun removeFromFolder(folderId: Long, cards: List<Card>) =
        cards.forEach { dao.removeMembership(folderId, it.id, it.variant) }

    override suspend fun addToFolder(folderId: Long, cards: List<Card>) =
        dao.insertMemberships(cards.map { FolderCardEntity(folderId, it.id, it.variant) })

    override suspend fun setMemberships(cardId: String, variant: String, folderIds: Set<Long>) =
        dao.setMemberships(cardId, variant, folderIds)
}
