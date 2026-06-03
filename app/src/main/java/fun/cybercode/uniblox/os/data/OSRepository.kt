package `fun`.cybercode.uniblox.os.data

import kotlinx.coroutines.flow.Flow

class OSRepository(private val osDao: OSDao) {
    val settings: Flow<OSSettings?> = osDao.getSettings()
    val allDesktops: Flow<List<DesktopConfig>> = osDao.getAllDesktops()
    val allWebApps: Flow<List<WebApp>> = osDao.getAllWebApps()

    suspend fun saveSettings(settings: OSSettings) {
        osDao.saveSettings(settings)
    }

    suspend fun saveDesktop(desktop: DesktopConfig): Long = osDao.saveDesktop(desktop)

    suspend fun updateDesktop(desktop: DesktopConfig) = osDao.updateDesktop(desktop)

    suspend fun selectDesktop(desktopId: Long) {
        osDao.deselectAllDesktops()
        osDao.selectDesktopById(desktopId)
    }

    suspend fun deselectAllDesktops() = osDao.deselectAllDesktops()

    suspend fun saveWebApp(webApp: WebApp) = osDao.saveWebApp(webApp)
    
    fun getWidgetsForDesktop(desktopId: Long) = osDao.getWidgetsForDesktop(desktopId)

    suspend fun saveWidget(widget: `fun`.cybercode.uniblox.os.data.WidgetConfig) = osDao.saveWidget(widget)

    suspend fun deleteWidget(widget: `fun`.cybercode.uniblox.os.data.WidgetConfig) = osDao.deleteWidget(widget)

    suspend fun updateWidgetPosition(id: Long, x: Float, y: Float) = osDao.updateWidgetPosition(id, x, y)
}
