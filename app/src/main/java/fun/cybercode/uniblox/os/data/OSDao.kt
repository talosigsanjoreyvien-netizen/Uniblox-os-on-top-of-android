package `fun`.cybercode.uniblox.os.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface OSDao {
    @Query("SELECT * FROM os_settings WHERE id = 0")
    fun getSettings(): Flow<OSSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: OSSettings)

    @Query("SELECT * FROM desktops")
    fun getAllDesktops(): Flow<List<DesktopConfig>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveDesktop(desktop: DesktopConfig): Long

    @Update
    suspend fun updateDesktop(desktop: DesktopConfig)

    @Query("UPDATE desktops SET isSelected = 0")
    suspend fun deselectAllDesktops()

    @Query("UPDATE desktops SET isSelected = 1 WHERE id = :id")
    suspend fun selectDesktopById(id: Long)

    @Query("SELECT * FROM widgets WHERE desktopId = :desktopId")
    fun getWidgetsForDesktop(desktopId: Long): kotlinx.coroutines.flow.Flow<List<`fun`.cybercode.uniblox.os.data.WidgetConfig>>

    @Insert(onConflict = androidx.room.OnConflictStrategy.REPLACE)
    suspend fun saveWidget(widget: `fun`.cybercode.uniblox.os.data.WidgetConfig)

    @Delete
    suspend fun deleteWidget(widget: `fun`.cybercode.uniblox.os.data.WidgetConfig)

    @Query("UPDATE widgets SET x = :x, y = :y WHERE id = :id")
    suspend fun updateWidgetPosition(id: Long, x: Float, y: Float)

    @Query("SELECT * FROM web_apps")
    fun getAllWebApps(): Flow<List<WebApp>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveWebApp(webApp: WebApp)
}
