package com.nunna.fitnessgym.data.db

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

/** Single row (id = 1): onboarding answers (JSON), plan seed, rotation pointer, XP. */
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val settingsJson: String,
    val onboarded: Boolean,
    val planSeed: Long,
    val nextDay: Int = 0,
    val xp: Int = 0,
    val createdAt: Long,
)

@Entity(tableName = "plan_day")
data class PlanDayEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayIndex: Int,
    val name: String,
    val focus: String,
)

@Entity(
    tableName = "plan_slot",
    foreignKeys = [ForeignKey(entity = PlanDayEntity::class, parentColumns = ["id"], childColumns = ["dayId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("dayId")],
)
data class PlanSlotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dayId: Long,
    val position: Int,
    val exerciseId: String,
    val sets: Int,
    val repMin: Int,
    val repMax: Int,
    val rir: Int,
    val restSec: Int,
    val seconds: Int,
    val role: String,
    val tracking: String,
)

data class PlanDayWithSlots(
    @Embedded val day: PlanDayEntity,
    @Relation(parentColumn = "id", entityColumn = "dayId") val slots: List<PlanSlotEntity>,
) {
    val ordered get() = slots.sortedBy { it.position }
}

/** status: "active" while training, "done" when finished. Discarded workouts are deleted. */
@Entity(tableName = "session", indices = [Index("status"), Index("startedAt")])
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val planDayIndex: Int?,
    val title: String,
    val startedAt: Long,
    val endedAt: Long? = null,
    val status: String = "active",
    val xpEarned: Int = 0,
)

@Entity(
    tableName = "session_exercise",
    foreignKeys = [ForeignKey(entity = SessionEntity::class, parentColumns = ["id"], childColumns = ["sessionId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionId"), Index("exerciseId")],
)
data class SessionExerciseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val position: Int,
    val exerciseId: String,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val rir: Int,
    val restSec: Int,
    val seconds: Int,
    val tracking: String,
    val note: String = "",
    val skipped: Boolean = false,
)

/** One set. Rows exist for every target set from the start (prefilled); [done] marks it logged. */
@Entity(
    tableName = "set_log",
    foreignKeys = [ForeignKey(entity = SessionExerciseEntity::class, parentColumns = ["id"], childColumns = ["sessionExerciseId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("sessionExerciseId")],
)
data class SetLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionExerciseId: Long,
    val setIndex: Int,
    val weight: Double? = null,
    val unit: String = "KG",
    val reps: Int? = null,
    val seconds: Int? = null,
    val rir: Int? = null,
    val done: Boolean = false,
    val completedAt: Long? = null,
    val pr: String? = null,
)

data class SessionExerciseWithSets(
    @Embedded val ex: SessionExerciseEntity,
    @Relation(parentColumn = "id", entityColumn = "sessionExerciseId") val sets: List<SetLogEntity>,
) {
    val orderedSets get() = sets.sortedBy { it.setIndex }
}

data class SessionFull(
    @Embedded val session: SessionEntity,
    @Relation(entity = SessionExerciseEntity::class, parentColumn = "id", entityColumn = "sessionId")
    val exercises: List<SessionExerciseWithSets>,
) {
    val ordered get() = exercises.sortedBy { it.ex.position }
    val doneSets get() = exercises.sumOf { e -> e.sets.count { it.done } }
    val totalSets get() = exercises.filter { !it.ex.skipped }.sumOf { it.sets.size }
}

/** A logged set with its session/target context, for progression and records. */
data class HistoryRow(
    val sessionId: Long,
    val startedAt: Long,
    val targetSets: Int,
    val repMin: Int,
    val repMax: Int,
    val targetRir: Int,
    val targetSeconds: Int,
    val weight: Double?,
    val unit: String,
    val reps: Int?,
    val seconds: Int?,
    val rir: Int?,
    val setId: Long,
)
