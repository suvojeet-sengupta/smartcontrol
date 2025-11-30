package com.suvojeet.smartcontrol.domain.usecase

import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.data.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetBulbsUseCase @Inject constructor(
    private val repository: DeviceRepository
) {
    operator fun invoke(): Flow<List<WizBulb>> {
        return repository.getBulbs()
    }
}
