package `fun`.cybercode.uniblox.os.data

import kotlinx.coroutines.flow.Flow

class OSRepository(private val osDao: OSDao) {
    val settings: Flow<OSSettings?> = osDao.getSettings()

    suspend fun saveSettings(settings: OSSettings) {
        osDao.saveSettings(settings)
    }
}
