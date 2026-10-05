package com.nunna.fitnessgym

import android.app.Application
import android.content.Context
import com.nunna.fitnessgym.data.ContentRepo
import com.nunna.fitnessgym.data.GymRepo
import com.nunna.fitnessgym.data.db.GymDatabase

/** Tiny service locator. Tests call [useTestDatabase] before launching the activity. */
object AppGraph {
    @Volatile private var dbRef: GymDatabase? = null
    @Volatile private var repoRef: GymRepo? = null

    /** False in tests: stops endless animation / ticker loops so the UI can go idle. */
    @Volatile var animations: Boolean = true

    fun repo(ctx: Context): GymRepo = repoRef ?: synchronized(this) {
        repoRef ?: run {
            val db = dbRef ?: GymDatabase.open(ctx.applicationContext).also { dbRef = it }
            GymRepo(db.dao(), ContentRepo.get(ctx)).also { repoRef = it }
        }
    }

    fun useTestDatabase(ctx: Context): GymDatabase {
        dbRef?.close()
        val db = GymDatabase.inMemory(ctx.applicationContext)
        dbRef = db
        repoRef = GymRepo(db.dao(), ContentRepo.get(ctx))
        animations = false
        return db
    }
}

class FitnessGymApp : Application()
