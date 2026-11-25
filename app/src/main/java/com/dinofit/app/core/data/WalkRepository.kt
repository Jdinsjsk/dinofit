package com.dinofit.app.core.data

import com.dinofit.app.core.common.FitnessMath
import com.dinofit.app.core.database.WalkDao
import com.dinofit.app.core.database.WalkEntity
import com.dinofit.app.core.database.WalkPointEntity
import com.dinofit.app.core.model.LatLng
import com.dinofit.app.core.model.Walk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

interface WalkRepository {
    val walks: Flow<List<Walk>>
    val activeWalk: Flow<Walk?>
    fun points(walkId: Long): Flow<List<LatLng>>
    /** Démarre une marche, ou renvoie celle déjà en cours. */
    suspend fun startOrResume(): Long
    /** Ajoute un point GPS s'il passe le filtre ; renvoie true s'il a été gardé. */
    suspend fun addPoint(walkId: Long, point: LatLng, timeMs: Long, accuracyM: Float): Boolean
    suspend fun stepsOf(walkId: Long): Int
    suspend fun updateSteps(walkId: Long, steps: Int)
    suspend fun current(walkId: Long): Walk?
    /** Termine la marche en cours. Une marche sans aucun point ni pas est supprimée. */
    suspend fun finishActive()
    suspend fun delete(walkId: Long)
}

@Singleton
internal class OfflineFirstWalkRepository @Inject constructor(
    private val dao: WalkDao,
) : WalkRepository {
    private val mutex = Mutex()

    override val walks: Flow<List<Walk>> = dao.finished().map { rows -> rows.map { it.toModel() } }

    override val activeWalk: Flow<Walk?> = dao.active().map { it?.toModel() }

    override fun points(walkId: Long): Flow<List<LatLng>> =
        dao.points(walkId).map { rows -> rows.map { LatLng(it.lat, it.lon) } }

    override suspend fun startOrResume(): Long = mutex.withLock {
        dao.activeNow()?.id ?: dao.insertWalk(WalkEntity(startedAt = System.currentTimeMillis()))
    }

    override suspend fun addPoint(walkId: Long, point: LatLng, timeMs: Long, accuracyM: Float): Boolean = mutex.withLock {
        val walk = dao.walk(walkId) ?: return@withLock false
        val last = dao.lastPoint(walkId)
        val prev = last?.let { LatLng(it.lat, it.lon) }
        if (!FitnessMath.acceptPoint(prev, last?.time, point, timeMs, accuracyM)) return@withLock false
        dao.insertPoint(WalkPointEntity(walkId = walkId, time = timeMs, lat = point.lat, lon = point.lon))
        if (prev != null) dao.updateDistance(walkId, walk.distanceM + FitnessMath.haversineM(prev, point))
        true
    }

    override suspend fun stepsOf(walkId: Long): Int = dao.walk(walkId)?.steps ?: 0

    override suspend fun updateSteps(walkId: Long, steps: Int) = dao.updateSteps(walkId, steps)

    override suspend fun current(walkId: Long): Walk? = dao.walk(walkId)?.toModel()

    override suspend fun finishActive() {
        mutex.withLock {
            val walk = dao.activeNow() ?: return@withLock
            if (dao.countPoints(walk.id) == 0 && walk.steps == 0) dao.delete(walk.id)
            else dao.finish(walk.id, System.currentTimeMillis())
        }
    }

    override suspend fun delete(walkId: Long) = dao.delete(walkId)
}

private fun WalkEntity.toModel() = Walk(id, startedAt, endedAt, distanceM, steps)
