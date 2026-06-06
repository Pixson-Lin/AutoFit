package com.pixson.autofit.data.health

import androidx.health.connect.client.records.Record

interface HealthConnectGateway {
    fun getSdkStatus(): Int
    suspend fun getGrantedPermissions(): Set<String>
    suspend fun insertRecords(records: List<Record>)
}
