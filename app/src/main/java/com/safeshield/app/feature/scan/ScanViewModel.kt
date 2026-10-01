package com.safeshield.app.feature.scan

import androidx.lifecycle.ViewModel
import com.safeshield.app.domain.model.ScanProgress
import com.safeshield.app.service.ScanController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class ScanViewModel @Inject constructor(
    private val controller: ScanController,
) : ViewModel() {

    val progress: StateFlow<ScanProgress> = controller.progress
    val finishedScanId: StateFlow<Long?> = controller.lastSummaryId

    fun pause() = controller.pause()
    fun resume() = controller.resume()
    fun cancel() = controller.cancel()
    fun acknowledge() = controller.acknowledgeResult()
}
