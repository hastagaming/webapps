package com.web.apps.ui.statistics

import androidx.lifecycle.ViewModel
import com.web.apps.data.local.entity.ContainerEntity
import com.web.apps.data.repository.ContainerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    containerRepository: ContainerRepository
) : ViewModel() {

    val sortedContainers: Flow<List<ContainerEntity>> =
        containerRepository.observeAllContainers()
            .map { list -> list.sortedByDescending { it.totalUsageMillis } }
}