package `fun`.cybercode.uniblox.os.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [OSSettings::class, DesktopConfig::class, WebApp::class, WidgetConfig::class], version = 3, exportSchema = false)
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
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
