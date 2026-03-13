package com.maazm7d.termuxhub.domain.usecase

import com.maazm7d.termuxhub.data.repository.HallOfFameRepository
import javax.inject.Inject

class RefreshHallOfFameUseCase @Inject constructor(
    private val repository: HallOfFameRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.refresh()
    }
}
