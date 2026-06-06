package com.pixson.autofit.data.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.Record

class HealthConnectGatewayImpl(
    private val context: Context,
) : HealthConnectGateway {

    private val client: HealthConnectClient? = runCatching {
        HealthConnectClient.getOrCreate(context)
    }.getOrNull()

    override fun getSdkStatus(): Int =
        HealthConnectClient.getSdkStatus(context)

    override suspend fun getGrantedPermissions(): Set<String> {
        val activeClient = client ?: return emptySet()
        return activeClient.permissionController.getGrantedPermissions()
    }

    override suspend fun insertRecords(records: List<Record>) {
        val activeClient = client
            ?: throw IllegalStateException("Health Connect client is not available")
        activeClient.insertRecords(records)
    }
}
