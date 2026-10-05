package com.nunna.fitnessgym.data

import com.nunna.fitnessgym.anim.Exercise
import com.nunna.fitnessgym.core.Change
import com.nunna.fitnessgym.core.Classify
import com.nunna.fitnessgym.core.Gamification
import com.nunna.fitnessgym.core.Generator
import com.nunna.fitnessgym.core.Ladder
import com.nunna.fitnessgym.core.Performance
import com.nunna.fitnessgym.core.PlanSlot
import com.nunna.fitnessgym.core.Progression
import com.nunna.fitnessgym.core.Records
import com.nunna.fitnessgym.core.Role
import com.nunna.fitnessgym.core.SetPerf
import com.nunna.fitnessgym.core.Suggestion
import com.nunna.fitnessgym.core.Tracking
import com.nunna.fitnessgym.core.Units
import com.nunna.fitnessgym.core.UserSettings
import com.nunna.fitnessgym.data.db.GymDao
import com.nunna.fitnessgym.data.db.HistoryRow
import com.nunna.fitnessgym.data.db.PlanDayEntity
import com.nunna.fitnessgym.data.db.PlanSlotEntity
import com.nunna.fitnessgym.data.db.ProfileEntity
import com.nunna.fitnessgym.data.db.SessionEntity
import com.nunna.fitnessgym.data.db.SessionExerciseEntity
import com.nunna.fitnessgym.data.db.SessionFull
import com.nunna.fitnessgym.data.db.SetLogEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Wall clock, swappable in tests. */
object AppClock {
    var now: () -> Long = { System.currentTimeMillis() }
    var zone: ZoneId = ZoneId.systemDefault()
    fun today(): LocalDate = Instant.ofEpochMilli(now()).atZone(zone).toLocalDate()
    fun dateOf(ms: Long): LocalDate = Instant.ofEpochMilli(ms).atZone(zone).toLocalDate()
}

/** Result of ticking a set: records it beat and how long to rest. */
data class SetResult(val ok: Boolean, val error: String? = null, val records: List<String> = emptyList(), val restSec: Int = 0, val nextLabel: String? = null)

data class Summary(
    val sessionId: Long, val title: String, val minutes: Int, val sets: Int, val volume: Double, val unit: Units,
    val records: List<Pair<String, String>>, val xp: Int, val levelBefore: Int, val levelAfter: Int,
)

/** Input limits: anything outside these is a typo, not a set. */
object Limits {
    const val MAX_WEIGHT = 1000.0
    const val MAX_REPS = 200
    const val MAX_SECONDS = 6 * 3600
}

/**
 * Every workout operation. All writes go to the database immediately ("write-ahead"), so a crash or a
 * killed app never loses a logged set: the active workout is simply resumed.
 */
class GymRepo(private val dao: GymDao, private val content: ContentRepo) {
    val exercises: List<Exercise> get() = content.exercises
    fun exercise(id: String) = content.exercise(id)

    val profileFlow: Flow<ProfileEntity?> = dao.profileFlow()
    val settingsFlow: Flow<UserSettings?> = dao.profileFlow().map { p -> p?.let { UserSettings.fromJson(it.settingsJson) } }
    val planFlow = dao.planFlow()
    val activeFlow = dao.activeFlow()
    val doneFlow = dao.doneFlow()
    fun sessionFlow(id: Long) = dao.sessionFlow(id)
    fun historyFlow(exerciseId: String) = dao.historyFlow(exerciseId)

    suspend fun settings(): UserSettings? = dao.profile()?.let { UserSettings.fromJson(it.settingsJson) }

    // ---------------- onboarding & plan ----------------

    suspend fun completeOnboarding(s: UserSettings) {
        val v = s.validated()
        val seed = AppClock.now()
        val old = dao.profile()
        dao.saveProfile(ProfileEntity(1, v.toJson(), true, seed, 0, old?.xp ?: 0, old?.createdAt ?: AppClock.now()))
        storePlan(v, seed)
    }

    /** Save edited settings. The plan is rebuilt; finished workouts are never touched. */
    suspend fun updateSettings(s: UserSettings, newSeed: Boolean = false) {
        val p = dao.profile() ?: return completeOnboarding(s)
        val v = s.validated()
        val seed = if (newSeed) p.planSeed + 7919 else p.planSeed
        dao.saveProfile(p.copy(settingsJson = v.toJson(), planSeed = seed, nextDay = 0))
        storePlan(v, seed)
    }

    suspend fun regeneratePlan() {
        val s = settings() ?: return
        updateSettings(s, newSeed = true)
    }

    suspend fun exclude(exerciseId: String, excluded: Boolean) {
        val s = settings() ?: return
        val next = if (excluded) s.excluded + exerciseId else s.excluded - exerciseId
        updateSettings(s.copy(excluded = next))
    }

    private suspend fun storePlan(s: UserSettings, seed: Long) {
        val plan = Generator.generate(s, exercises, seed)
        dao.clearPlan()
        for ((i, d) in plan.days.withIndex()) {
            val id = dao.insertDay(PlanDayEntity(dayIndex = i, name = d.name, focus = d.focus))
            dao.insertSlots(d.slots.mapIndexed { pos, sl -> slotEntity(id, pos, sl) })
        }
    }

    private fun slotEntity(dayId: Long, pos: Int, sl: PlanSlot) = PlanSlotEntity(
        dayId = dayId, position = pos, exerciseId = sl.exerciseId, sets = sl.sets, repMin = sl.repMin, repMax = sl.repMax,
        rir = sl.rir, restSec = sl.restSec, seconds = sl.seconds, role = sl.role.name, tracking = sl.tracking.name,
    )

    // ---------------- workouts ----------------

    /** Starts the plan day (or resumes the workout already in progress). Returns the session id. */
    suspend fun startPlanDay(dayIndex: Int): Long {
        dao.active()?.let { return it.session.id }
        val day = dao.plan().firstOrNull { it.day.dayIndex == dayIndex } ?: return startEmpty()
        val sid = dao.insertSession(SessionEntity(planDayIndex = dayIndex, title = day.day.name, startedAt = AppClock.now()))
        val s = settings() ?: UserSettings()
        for ((pos, slot) in day.ordered.withIndex()) {
            val ex = exercise(slot.exerciseId) ?: continue
            addExerciseRow(sid, pos, ex, slot.sets, slot.repMin, slot.repMax, slot.rir, slot.restSec, slot.seconds, Tracking.valueOf(slot.tracking), s)
        }
        return sid
    }

    suspend fun startEmpty(title: String = "Quick workout"): Long {
        dao.active()?.let { return it.session.id }
        return dao.insertSession(SessionEntity(planDayIndex = null, title = title, startedAt = AppClock.now()))
    }

    suspend fun addExercise(sessionId: Long, exerciseId: String): Long? {
        val ex = exercise(exerciseId) ?: return null
        val s = settings() ?: UserSettings()
        val full = dao.session(sessionId) ?: return null
        val p = Generator.prescribe(ex, if (Classify.tracking(ex) == Tracking.CARDIO) Role.FINISHER else Role.ACCESSORY, s)
        val pos = (full.exercises.maxOfOrNull { it.ex.position } ?: -1) + 1
        return addExerciseRow(sessionId, pos, ex, p.sets, p.repMin, p.repMax, p.rir, p.restSec, p.seconds, p.tracking, s)
    }

    private suspend fun addExerciseRow(
        sid: Long, pos: Int, ex: Exercise, sets: Int, repMin: Int, repMax: Int, rir: Int, rest: Int, seconds: Int, tracking: Tracking, s: UserSettings,
    ): Long {
        val sug = suggestion(ex, tracking, sets, repMin, repMax, rir, seconds, s)
        val seId = dao.insertExercise(
            SessionExerciseEntity(
                sessionId = sid, position = pos, exerciseId = ex.id, targetSets = sets, repMin = repMin, repMax = repMax,
                rir = rir, restSec = rest, seconds = seconds, tracking = tracking.name, note = sug.note,
            ),
        )
        dao.insertSets((0 until sets.coerceAtLeast(1)).map { i -> prefill(seId, i, sug, tracking, repMin, repMax, seconds, s.units) })
        return seId
    }

    private fun prefill(seId: Long, i: Int, sug: Suggestion, t: Tracking, repMin: Int, repMax: Int, seconds: Int, u: Units) = SetLogEntity(
        sessionExerciseId = seId, setIndex = i, unit = u.name,
        weight = if (t == Tracking.WEIGHT_REPS || t == Tracking.WEIGHT_TIME) sug.weight else null,
        reps = when (t) { Tracking.WEIGHT_REPS, Tracking.REPS -> sug.reps ?: (if (sug.change == Change.FIRST) repMax else repMin); else -> null },
        seconds = when (t) { Tracking.TIME, Tracking.CARDIO, Tracking.WEIGHT_TIME -> sug.seconds ?: seconds; else -> null },
    )

    /** Next-time suggestion for an exercise from its finished history (double progression). */
    suspend fun suggestion(ex: Exercise, tracking: Tracking, sets: Int, repMin: Int, repMax: Int, rir: Int, seconds: Int, s: UserSettings): Suggestion {
        val perf = performances(dao.history(ex.id))
        val ladder = Ladder.forExercise(ex.equipment, s.profile, s.units)
        return Progression.suggest(tracking, perf, Performance(emptyList(), sets, repMin, repMax, seconds, rir, s.units), ladder, s.units)
    }

    fun performances(rows: List<HistoryRow>): List<Performance> =
        rows.groupBy { it.sessionId }.values.sortedByDescending { it.first().startedAt }.map { g ->
            val f = g.first()
            Performance(
                g.map { SetPerf(it.weight, it.reps, it.seconds, it.rir) }, f.targetSets, f.repMin, f.repMax, f.targetSeconds, f.targetRir,
                runCatching { Units.valueOf(f.unit) }.getOrDefault(Units.KG),
            )
        }

    suspend fun updateSet(setId: Long, weight: Double?, reps: Int?, seconds: Int?, rir: Int?) {
        val cur = dao.set(setId) ?: return
        dao.updateSet(cur.copy(weight = weight, reps = reps, seconds = seconds, rir = rir))
    }

    fun validate(t: Tracking, weight: Double?, reps: Int?, seconds: Int?): String? {
        if (weight != null && (weight < 0 || weight > Limits.MAX_WEIGHT)) return "Weight must be between 0 and ${Limits.MAX_WEIGHT.toInt()}"
        return when (t) {
            Tracking.WEIGHT_REPS -> when {
                weight == null -> "Enter the weight"
                reps == null || reps !in 1..Limits.MAX_REPS -> "Enter reps (1–${Limits.MAX_REPS})"
                else -> null
            }
            Tracking.REPS -> if (reps == null || reps !in 1..Limits.MAX_REPS) "Enter reps (1–${Limits.MAX_REPS})" else null
            Tracking.WEIGHT_TIME -> if (weight == null) "Enter the weight" else if (seconds == null || seconds !in 1..Limits.MAX_SECONDS) "Enter the time" else null
            Tracking.TIME, Tracking.CARDIO -> if (seconds == null || seconds !in 1..Limits.MAX_SECONDS) "Enter the time" else null
        }
    }

    /** Ticks a set: validates, saves, checks personal records, and returns the rest to take. */
    suspend fun completeSet(setId: Long, weight: Double?, reps: Int?, seconds: Int?, rir: Int?): SetResult {
        val cur = dao.set(setId) ?: return SetResult(false, "Set not found")
        val se = dao.exercise(cur.sessionExerciseId) ?: return SetResult(false, "Exercise not found")
        val t = Tracking.valueOf(se.tracking)
        validate(t, weight, reps, seconds)?.let { return SetResult(false, it) }
        val earlier = dao.doneSetsOf(se.exerciseId, setId).map { SetPerf(it.weight, it.reps, it.seconds, it.rir) }
        val recs = Records.newRecords(SetPerf(weight, reps, seconds, rir), earlier, t)
        dao.updateSet(cur.copy(weight = weight, reps = reps, seconds = seconds, rir = rir, done = true, completedAt = AppClock.now(), pr = recs.firstOrNull()))
        // Carry the values forward into the remaining empty sets, so the next set is one tap.
        for (later in dao.setsOf(se.id).filter { !it.done && it.setIndex > cur.setIndex }) {
            val upd = later.copy(
                weight = later.weight ?: weight, reps = later.reps ?: reps, seconds = later.seconds ?: seconds,
            )
            if (upd != later) dao.updateSet(upd)
        }
        val full = dao.session(se.sessionId)
        val next = full?.ordered?.filter { !it.ex.skipped }?.firstNotNullOfOrNull { e -> e.orderedSets.firstOrNull { !it.done }?.let { e } }
        val nextName = next?.let { exercise(it.ex.exerciseId)?.name }
        val rest = if (next == null) 0 else se.restSec
        return SetResult(true, null, recs, rest, nextName)
    }

    suspend fun uncompleteSet(setId: Long) {
        val cur = dao.set(setId) ?: return
        dao.updateSet(cur.copy(done = false, completedAt = null, pr = null))
    }

    suspend fun addSet(seId: Long) {
        val sets = dao.setsOf(seId)
        val last = sets.lastOrNull()
        val se = dao.exercise(seId) ?: return
        dao.insertSet(
            SetLogEntity(
                sessionExerciseId = seId, setIndex = (last?.setIndex ?: -1) + 1, weight = last?.weight, unit = last?.unit ?: "KG",
                reps = last?.reps ?: se.repMin.takeIf { it > 0 }, seconds = last?.seconds ?: se.seconds.takeIf { it > 0 },
            ),
        )
    }

    /** Removes a set. The last remaining set of an exercise can't be removed (remove the exercise instead). */
    suspend fun removeSet(setId: Long): Boolean {
        val cur = dao.set(setId) ?: return false
        val sets = dao.setsOf(cur.sessionExerciseId)
        if (sets.size <= 1) return false
        dao.deleteSet(cur)
        sets.filter { it.id != setId }.forEachIndexed { i, s -> if (s.setIndex != i) dao.updateSet(s.copy(setIndex = i)) }
        return true
    }

    suspend fun setSkipped(seId: Long, skipped: Boolean) {
        val se = dao.exercise(seId) ?: return
        dao.updateExercise(se.copy(skipped = skipped))
    }

    suspend fun removeExercise(seId: Long) = dao.deleteExercise(seId)

    suspend fun moveExercise(seId: Long, delta: Int) {
        val se = dao.exercise(seId) ?: return
        val all = dao.session(se.sessionId)?.ordered?.map { it.ex }?.toMutableList() ?: return
        val i = all.indexOfFirst { it.id == seId }
        val j = (i + delta).coerceIn(0, all.size - 1)
        if (i == j) return
        val moved = all.removeAt(i); all.add(j, moved)
        all.forEachIndexed { pos, e -> if (e.position != pos) dao.updateExercise(e.copy(position = pos)) }
    }

    /** Swaps an exercise that has no logged sets yet; logged work is never rewritten. */
    suspend fun swapExercise(seId: Long, newExerciseId: String): Boolean {
        val se = dao.exercise(seId) ?: return false
        val sets = dao.setsOf(seId)
        if (sets.any { it.done }) return false
        val ex = exercise(newExerciseId) ?: return false
        val s = settings() ?: UserSettings()
        val oldT = Tracking.valueOf(se.tracking)
        val t = Classify.tracking(ex)
        val p = if (t != oldT) Generator.prescribe(ex, Role.ACCESSORY, s) else null
        val sug = suggestion(ex, t, p?.sets ?: se.targetSets, p?.repMin ?: se.repMin, p?.repMax ?: se.repMax, se.rir, p?.seconds ?: se.seconds, s)
        val updated = se.copy(
            exerciseId = ex.id, tracking = t.name, note = sug.note,
            targetSets = p?.sets ?: se.targetSets, repMin = p?.repMin ?: se.repMin, repMax = p?.repMax ?: se.repMax,
            seconds = p?.seconds ?: se.seconds, restSec = p?.restSec ?: se.restSec,
        )
        dao.updateExercise(updated)
        for (old in sets) dao.deleteSet(old)
        dao.insertSets((0 until updated.targetSets.coerceAtLeast(1)).map { prefill(seId, it, sug, t, updated.repMin, updated.repMax, updated.seconds, s.units) })
        return true
    }

    suspend fun discard(sessionId: Long) = dao.deleteSession(sessionId)

    /**
     * Finishes a workout: un-ticked sets are dropped (only real work is kept), XP is awarded, and the plan
     * rotation moves to the next day. A workout with nothing logged is discarded instead (returns null).
     */
    suspend fun finish(sessionId: Long): Summary? {
        val full = dao.session(sessionId) ?: return null
        if (full.doneSets == 0) { dao.deleteSession(sessionId); return null }
        val s = settings() ?: UserSettings()
        for (e in full.exercises) {
            for (st in e.sets) if (!st.done) dao.deleteSet(st)
            if (e.sets.none { it.done }) dao.deleteExercise(e.ex.id)
        }
        val records = full.ordered.flatMap { e -> e.orderedSets.filter { it.done && it.pr != null }.map { (exercise(e.ex.exerciseId)?.name ?: e.ex.exerciseId) to it.pr!! } }
        val xp = full.doneSets * Gamification.XP_SET + Gamification.XP_WORKOUT + records.size * Gamification.XP_PR
        val end = AppClock.now()
        dao.updateSession(full.session.copy(status = "done", endedAt = end, xpEarned = xp))
        val prof = dao.profile()
        val before = Gamification.level(prof?.xp ?: 0)
        if (prof != null) {
            val days = dao.plan().size.coerceAtLeast(1)
            val next = full.session.planDayIndex?.let { (it + 1) % days } ?: prof.nextDay
            dao.saveProfile(prof.copy(xp = prof.xp + xp, nextDay = next))
        }
        val volume = full.exercises.sumOf { e ->
            e.sets.filter { it.done && it.weight != null && it.reps != null }.sumOf { st ->
                val w = st.weight!!; val unit = runCatching { Units.valueOf(st.unit) }.getOrDefault(s.units)
                (if (unit == s.units) w else if (s.units == Units.KG) unit.toKg(w) else w * 2.20462) * st.reps!!
            }
        }
        return Summary(
            sessionId, full.session.title, ((end - full.session.startedAt) / 60000).toInt().coerceAtLeast(1), full.doneSets, volume, s.units,
            records, xp, before, Gamification.level((prof?.xp ?: 0) + xp),
        )
    }

    suspend fun deleteSession(id: Long) = dao.deleteSession(id)

    suspend fun workoutDays(): List<LocalDate> = dao.doneStarts().map { AppClock.dateOf(it) }

    suspend fun resetAll() {
        dao.clearSessions(); dao.clearPlan()
        dao.profile()?.let { dao.saveProfile(it.copy(onboarded = false, xp = 0, nextDay = 0)) }
    }

    /** CSV of every logged set: date, workout, exercise, set, weight, unit, reps, seconds, RIR. */
    fun csv(done: List<SessionFull>): String = buildString {
        appendLine("date,workout,exercise,set,weight,unit,reps,seconds,rir")
        for (s in done.sortedBy { it.session.startedAt }) {
            val d = AppClock.dateOf(s.session.startedAt)
            for (e in s.ordered) for (st in e.orderedSets.filter { it.done }) {
                val name = (exercise(e.ex.exerciseId)?.name ?: e.ex.exerciseId).replace("\"", "'")
                appendLine("$d,\"${s.session.title}\",\"$name\",${st.setIndex + 1},${st.weight ?: ""},${st.unit},${st.reps ?: ""},${st.seconds ?: ""},${st.rir ?: ""}")
            }
        }
    }
}
