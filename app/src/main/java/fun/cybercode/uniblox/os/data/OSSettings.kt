package `fun`.cybercode.uniblox.os.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "os_settings")
data class OSSettings(
    @PrimaryKey val id: Int = 0,
    val isSetupComplete: Boolean = false,
    val userName: String = "",
    val userCountry: String = ""
)
