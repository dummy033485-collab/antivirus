package com.safeshield.app.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.safeshield.app.data.repository.SignatureRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/** Refreshes the offline hash database. Never required for scanning to work. */
@HiltWorker
class SignatureUpdateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val signatures: SignatureRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        signatures.seedIfEmpty()
        return if (signatures.updateFromRemote() != null) Result.success() else Result.retry()
    }
}
