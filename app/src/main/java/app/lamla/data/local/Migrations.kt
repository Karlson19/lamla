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
 *
 * v2 → v3: smart attendance. Adds `attendance_records` (one verdict per class meeting)
 * and `venue_locations` (pinned GPS per venue, for geofence auto-marking).
 */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE deadlines ADD COLUMN scoreObtained REAL")
        db.execSQL("ALTER TABLE deadlines ADD COLUMN scoreMax REAL NOT NULL DEFAULT 100")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS attendance_records (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                classSessionId INTEGER NOT NULL,
                courseId INTEGER NOT NULL,
                dateEpochDay INTEGER NOT NULL,
                status TEXT NOT NULL,
                markedAtEpochMs INTEGER NOT NULL,
                auto INTEGER NOT NULL DEFAULT 0,
                FOREIGN KEY(courseId) REFERENCES courses(id) ON DELETE CASCADE,
                FOREIGN KEY(classSessionId) REFERENCES class_sessions(id) ON DELETE CASCADE
            )
            """.trimIndent()
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS index_attendance_records_courseId ON attendance_records(courseId)")
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_attendance_records_classSessionId_dateEpochDay ON attendance_records(classSessionId, dateEpochDay)")

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS venue_locations (
                id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
                venueKey TEXT NOT NULL,
                displayName TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                radiusMeters REAL NOT NULL
            )
            """.trimIndent()
        )
        db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_venue_locations_venueKey ON venue_locations(venueKey)")
    }
}

/** All migrations, in order, for the database builder. */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2, MIGRATION_2_3)
