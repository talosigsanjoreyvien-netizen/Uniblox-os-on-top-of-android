package `fun`.cybercode.uniblox.os.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OSDao {
    @Query("SELECT * FROM os_settings WHERE id = 0")
    fun getSettings(): Flow<OSSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: OSSettings)
}
