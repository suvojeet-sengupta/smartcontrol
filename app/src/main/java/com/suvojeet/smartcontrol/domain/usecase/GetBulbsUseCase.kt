package com.suvojeet.smartcontrol.domain.usecase

import com.suvojeet.smartcontrol.WizBulb
import com.suvojeet.smartcontrol.data.DeviceRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

import com.suvojeet.smartcontrol.data.Resource

class GetBulbsUseCase @Inject constructor(
    private val repository: DeviceRepository
) {
    operator fun invoke(): Flow<Resource<List<WizBulb>>> {
        return repository.getBulbs()
    }
}
