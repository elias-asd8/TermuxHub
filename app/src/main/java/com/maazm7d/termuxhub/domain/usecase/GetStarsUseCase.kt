package com.maazm7d.termuxhub.domain.usecase

import com.maazm7d.termuxhub.data.repository.ToolRepository
import javax.inject.Inject

class GetStarsUseCase @Inject constructor(
    private val repository: ToolRepository
) {
    suspend operator fun invoke(): Map<String, Int> =
        repository.fetchStars()
}
