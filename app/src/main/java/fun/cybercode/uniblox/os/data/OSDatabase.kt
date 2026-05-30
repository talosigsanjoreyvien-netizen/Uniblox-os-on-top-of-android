package `fun`.cybercode.uniblox.os.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OSSettings::class], version = 1, exportSchema = false)
abstract class OSDatabase : RoomDatabase() {
    abstract fun osDao(): OSDao

    companion object {
        @Volatile
        private var INSTANCE: OSDatabase? = null

        fun getDatabase(context: Context): OSDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    OSDatabase::class.java,
                    "os_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
