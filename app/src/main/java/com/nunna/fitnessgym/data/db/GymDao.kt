package com.nunna.fitnessgym.data.db

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.Executor

@Dao
interface GymDao {
    // ---- profile ----
    @Query("SELECT * FROM profile WHERE id = 1")
    fun profileFlow(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun profile(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(p: ProfileEntity)

    // ---- plan ----
    @Transaction
    @Query("SELECT * FROM plan_day ORDER BY dayIndex")
    fun planFlow(): Flow<List<PlanDayWithSlots>>

    @Transaction
    @Query("SELECT * FROM plan_day ORDER BY dayIndex")
    suspend fun plan(): List<PlanDayWithSlots>

    @Query("DELETE FROM plan_day")
    suspend fun clearPlan()

    @Insert
    suspend fun insertDay(d: PlanDayEntity): Long

    @Insert
    suspend fun insertSlots(s: List<PlanSlotEntity>)

    // ---- sessions ----
    @Insert
    suspend fun insertSession(s: SessionEntity): Long

    @Update
    suspend fun updateSession(s: SessionEntity)

    @Query("DELETE FROM session WHERE id = :id")
    suspend fun deleteSession(id: Long)

    @Transaction
    @Query("SELECT * FROM session WHERE status = 'active' ORDER BY startedAt DESC LIMIT 1")
    fun activeFlow(): Flow<SessionFull?>

    @Transaction
    @Query("SELECT * FROM session WHERE status = 'active' ORDER BY startedAt DESC LIMIT 1")
    suspend fun active(): SessionFull?

    @Transaction
    @Query("SELECT * FROM session WHERE id = :id")
    fun sessionFlow(id: Long): Flow<SessionFull?>

    @Transaction
    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun session(id: Long): SessionFull?

    @Transaction
    @Query("SELECT * FROM session WHERE status = 'done' ORDER BY startedAt DESC")
    fun doneFlow(): Flow<List<SessionFull>>

    @Query("SELECT startedAt FROM session WHERE status = 'done'")
    suspend fun doneStarts(): List<Long>

    @Insert
    suspend fun insertExercise(e: SessionExerciseEntity): Long

    @Update
    suspend fun updateExercise(e: SessionExerciseEntity)

    @Query("SELECT * FROM session_exercise WHERE id = :id")
    suspend fun exercise(id: Long): SessionExerciseEntity?

    @Query("DELETE FROM session_exercise WHERE id = :id")
    suspend fun deleteExercise(id: Long)

    @Insert
    suspend fun insertSets(s: List<SetLogEntity>)

    @Insert
    suspend fun insertSet(s: SetLogEntity): Long

    @Update
    suspend fun updateSet(s: SetLogEntity)

    @Delete
    suspend fun deleteSet(s: SetLogEntity)

    @Query("SELECT * FROM set_log WHERE id = :id")
    suspend fun set(id: Long): SetLogEntity?

    @Query("SELECT * FROM set_log WHERE sessionExerciseId = :seId ORDER BY setIndex")
    suspend fun setsOf(seId: Long): List<SetLogEntity>

    /** Completed sets of an exercise in finished workouts, newest workout first. */
    @Query(
        """SELECT s.id AS sessionId, s.startedAt AS startedAt, e.targetSets AS targetSets, e.repMin AS repMin,
           e.repMax AS repMax, e.rir AS targetRir, e.seconds AS targetSeconds, l.weight AS weight, l.unit AS unit,
           l.reps AS reps, l.seconds AS seconds, l.rir AS rir, l.id AS setId
           FROM set_log l JOIN session_exercise e ON l.sessionExerciseId = e.id JOIN session s ON e.sessionId = s.id
           WHERE e.exerciseId = :exerciseId AND l.done = 1 AND s.status = 'done'
           ORDER BY s.startedAt DESC, l.setIndex""",
    )
    suspend fun history(exerciseId: String): List<HistoryRow>

    @Query(
        """SELECT s.id AS sessionId, s.startedAt AS startedAt, e.targetSets AS targetSets, e.repMin AS repMin,
           e.repMax AS repMax, e.rir AS targetRir, e.seconds AS targetSeconds, l.weight AS weight, l.unit AS unit,
           l.reps AS reps, l.seconds AS seconds, l.rir AS rir, l.id AS setId
           FROM set_log l JOIN session_exercise e ON l.sessionExerciseId = e.id JOIN session s ON e.sessionId = s.id
           WHERE e.exerciseId = :exerciseId AND l.done = 1 AND s.status = 'done'
           ORDER BY s.startedAt DESC, l.setIndex""",
    )
    fun historyFlow(exerciseId: String): Flow<List<HistoryRow>>

    /** Every completed set of an exercise (finished workouts and the current one), for PR checks. */
    @Query(
        """SELECT l.* FROM set_log l JOIN session_exercise e ON l.sessionExerciseId = e.id JOIN session s ON e.sessionId = s.id
           WHERE e.exerciseId = :exerciseId AND l.done = 1 AND l.id != :exceptSetId AND s.status IN ('done', 'active')""",
    )
    suspend fun doneSetsOf(exerciseId: String, exceptSetId: Long): List<SetLogEntity>

    @Query("DELETE FROM session")
    suspend fun clearSessions()
}

@Database(
    entities = [ProfileEntity::class, PlanDayEntity::class, PlanSlotEntity::class, SessionEntity::class, SessionExerciseEntity::class, SetLogEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class GymDatabase : RoomDatabase() {
    abstract fun dao(): GymDao

    companion object {
        fun open(ctx: Context): GymDatabase =
            Room.databaseBuilder(ctx, GymDatabase::class.java, "fitnessgym.db").build()

        /** In-memory database whose queries run inline, for deterministic tests. */
        fun inMemory(ctx: Context): GymDatabase {
            val direct = Executor { it.run() }
            return Room.inMemoryDatabaseBuilder(ctx, GymDatabase::class.java)
                .allowMainThreadQueries().setQueryExecutor(direct).setTransactionExecutor(direct).build()
        }
    }
}
