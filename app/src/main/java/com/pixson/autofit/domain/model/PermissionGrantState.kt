package com.pixson.autofit.domain.model

/**
 * Snapshot-friendly permission state stored in [EnvironmentSnapshotEntity].
 * Uses distinct values from [android.content.pm.PackageManager] where applicable.
 */
object PermissionGrantState {
    const val GRANTED = 0
    const val DENIED = 1
    const val NOT_REQUIRED = 2
    const val NOT_APPLICABLE = 3
}
