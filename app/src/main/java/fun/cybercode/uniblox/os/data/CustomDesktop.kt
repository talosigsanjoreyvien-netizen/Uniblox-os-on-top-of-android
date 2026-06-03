package `fun`.cybercode.uniblox.os.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "desktops")
data class DesktopConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val hasStartMenu: Boolean = true,
    val hasAppDrawer: Boolean = false,
    val isSelected: Boolean = false
)

@Entity(tableName = "widgets")
data class WidgetConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val desktopId: Long,
    val type: String, // "wifi", "slider", "app"
    val metadata: String? = null, // packageName for type "app"
    val x: Float,
    val y: Float
)

@Entity(tableName = "web_apps")
data class WebApp(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val url: String,
    val iconUrl: String? = null
)
