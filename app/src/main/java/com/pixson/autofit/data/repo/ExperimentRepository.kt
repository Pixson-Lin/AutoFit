package com.pixson.autofit.data.repo

import com.pixson.autofit.data.local.dao.EnvironmentDao
import com.pixson.autofit.data.local.dao.ExperimentDao
import com.pixson.autofit.data.local.dao.HealthWriteEventDao
import com.pixson.autofit.data.local.dao.HeartbeatDao
import com.pixson.autofit.data.local.dao.ResultDao
import com.pixson.autofit.data.local.entity.EnvironmentSnapshotEntity
import com.pixson.autofit.data.local.entity.ExperimentEntity
import com.pixson.autofit.data.local.entity.ExperimentResultEntity
import com.pixson.autofit.data.local.entity.HealthWriteEventEntity
import com.pixson.autofit.data.local.entity.HeartbeatEntity
import com.pixson.autofit.domain.model.ExperimentStatus
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class ExperimentRepository(
    private val experimentDao: ExperimentDao,
    private val heartbeatDao: HeartbeatDao,
    private val healthWriteEventDao: HealthWriteEventDao,
    private val resultDao: ResultDao,
    private val environmentDao: EnvironmentDao,
) {

    suspend fun insertExperiment(experiment: ExperimentEntity) {
        experimentDao.insert(experiment)
    }

    suspend fun updateExperiment(experiment: ExperimentEntity) {
        experimentDao.update(experiment)
    }

    suspend fun getExperiment(id: UUID): ExperimentEntity? = experimentDao.getById(id)

    fun observeExperiment(id: UUID): Flow<ExperimentEntity?> = experimentDao.observeById(id)

    fun observeAllExperiments(): Flow<List<ExperimentEntity>> = experimentDao.observeAll()

    suspend fun getRunningExperiment(): ExperimentEntity? =
        experimentDao.getByStatus(ExperimentStatus.RUNNING)

    suspend fun updateExperimentStatus(id: UUID, status: ExperimentStatus) {
        experimentDao.updateStatus(id, status)
    }

    suspend fun insertHeartbeat(heartbeat: HeartbeatEntity) {
        heartbeatDao.insert(heartbeat)
    }

    suspend fun insertHeartbeats(heartbeats: List<HeartbeatEntity>) {
        heartbeatDao.insertAll(heartbeats)
    }

    suspend fun getHeartbeats(experimentId: UUID): List<HeartbeatEntity> =
        heartbeatDao.getByExperimentId(experimentId)

    fun observeHeartbeats(experimentId: UUID): Flow<List<HeartbeatEntity>> =
        heartbeatDao.observeByExperimentId(experimentId)

    suspend fun insertHealthWriteEvent(event: HealthWriteEventEntity) {
        healthWriteEventDao.insert(event)
    }

    suspend fun insertHealthWriteEvents(events: List<HealthWriteEventEntity>) {
        healthWriteEventDao.insertAll(events)
    }

    suspend fun getHealthWriteEvents(experimentId: UUID): List<HealthWriteEventEntity> =
        healthWriteEventDao.getByExperimentId(experimentId)

    fun observeHealthWriteEvents(experimentId: UUID): Flow<List<HealthWriteEventEntity>> =
        healthWriteEventDao.observeByExperimentId(experimentId)

    suspend fun upsertResult(result: ExperimentResultEntity) {
        resultDao.upsert(result)
    }

    suspend fun getResult(experimentId: UUID): ExperimentResultEntity? =
        resultDao.getByExperimentId(experimentId)

    fun observeResult(experimentId: UUID): Flow<ExperimentResultEntity?> =
        resultDao.observeByExperimentId(experimentId)

    fun observeAllResults(): Flow<List<ExperimentResultEntity>> = resultDao.observeAll()

    suspend fun insertEnvironmentSnapshot(snapshot: EnvironmentSnapshotEntity) {
        environmentDao.insert(snapshot)
    }

    suspend fun getEnvironmentSnapshot(experimentId: UUID): EnvironmentSnapshotEntity? =
        environmentDao.getByExperimentId(experimentId)

    fun observeEnvironmentSnapshot(experimentId: UUID): Flow<EnvironmentSnapshotEntity?> =
        environmentDao.observeByExperimentId(experimentId)
}
