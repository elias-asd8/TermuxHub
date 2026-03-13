package com.maazm7d.termuxhub.domain.usecase

import com.maazm7d.termuxhub.data.repository.HallOfFameRepository
import com.maazm7d.termuxhub.domain.model.HallOfFameMember
import javax.inject.Inject

class GetHallOfFameMembersUseCase @Inject constructor(
    private val repository: HallOfFameRepository
) {
    suspend operator fun invoke(): List<HallOfFameMember> =
        repository.loadMembers()
}
