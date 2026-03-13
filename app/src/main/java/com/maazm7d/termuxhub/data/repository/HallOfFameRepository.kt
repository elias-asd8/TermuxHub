package com.maazm7d.termuxhub.data.repository

import com.maazm7d.termuxhub.data.local.HallOfFameDao
import com.maazm7d.termuxhub.data.local.entities.HallOfFameEntity
import com.maazm7d.termuxhub.data.remote.MetadataClient
import com.maazm7d.termuxhub.data.remote.dto.HallOfFameMemberDto
import com.maazm7d.termuxhub.domain.model.HallOfFameMember
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class HallOfFameRepository @Inject constructor(
    private val metadataClient: MetadataClient,
    private val dao: HallOfFameDao
) {

    // Flow of cached members from database
    fun observeMembers(): Flow<List<HallOfFameMember>> =
        dao.getAll().map { entities ->
            entities.map { it.toDomain() }
        }

    // Refresh data from network and update database
    suspend fun refresh(): Result<Unit> = runCatching {
        val indexResponse = metadataClient.fetchHallOfFameIndex()
        if (!indexResponse.isSuccessful) {
            throw Exception("Failed to fetch index: ${indexResponse.code()}")
        }
        val members = indexResponse.body()?.members ?: throw Exception("Empty index")

        // Fetch markdown for each member sequentially (could be optimized with parallel requests)
        val resolvedMembers = members.map { dto ->
            val markdown = fetchMarkdownSafely(dto.id)
            dto.toDomain(markdown)
        }

        // Replace database content
        dao.clear()
        dao.insertAll(resolvedMembers.map { it.toEntity() })

        Timber.i("Hall of Fame refreshed successfully")
    }.onFailure { error ->
        Timber.e(error, "Hall of Fame refresh failed")
    }

    private suspend fun fetchMarkdownSafely(id: String): String {
        return try {
            metadataClient.fetchHallOfFameMarkdown(id).body() ?: ""
        } catch (e: Exception) {
            Timber.w(e, "Failed to fetch markdown for $id")
            ""
        }
    }

    // Extension functions to convert between DTO/Entity/Domain
    private fun HallOfFameMemberDto.toDomain(contribution: String) = HallOfFameMember(
        id = id,
        github = github,
        speciality = speciality,
        profileUrl = profile,
        contribution = contribution
    )

    private fun HallOfFameMember.toEntity() = HallOfFameEntity(
        id = id,
        github = github,
        speciality = speciality,
        profileUrl = profileUrl,
        contribution = contribution
    )

    private fun HallOfFameEntity.toDomain() = HallOfFameMember(
        id = id,
        github = github,
        speciality = speciality,
        profileUrl = profileUrl,
        contribution = contribution
    )
}
