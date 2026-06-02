package app.lamla.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * Room migrations.
 *
 * The app ships to real devices, so schema changes MUST preserve user data — we
 * add explicit migrations rather than relying on destructive fallback (which would
 * wipe everyone's timetable, deadlines, and captures on update).
 *
 * v1 → v2: grade tracking. Adds `scoreObtained` (nullable; null = not yet graded)
 * and `scoreMax` (defaults to 100) to `deadlines`, so a graded assessment can feed
 * the CWA projection.
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE deadlines ADD COLUMN scoreObtained REAL")
        db.execSQL("ALTER TABLE deadlines ADD COLUMN scoreMax REAL NOT NULL DEFAULT 100")
    }
}

/** All migrations, in order, for the database builder. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
