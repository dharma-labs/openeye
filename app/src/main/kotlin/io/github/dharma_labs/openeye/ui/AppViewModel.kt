package io.github.dharma_labs.openeye.ui

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.Application
import android.content.Context
import android.content.Intent
import android.provider.Settings as AndroidSettings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.dharma_labs.openeye.data.AppRepository
import io.github.dharma_labs.openeye.data.BlockEvent
import io.github.dharma_labs.openeye.data.BlockMode
import io.github.dharma_labs.openeye.data.Settings
import io.github.dharma_labs.openeye.data.ScheduleConfig
import io.github.dharma_labs.openeye.data.UsageSnapshot
import io.github.dharma_labs.openeye.data.UsageStatsRepository
import io.github.dharma_labs.openeye.data.WatchRecord
import io.github.dharma_labs.openeye.service.FocusTimerService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PermissionState(
    val accessibility: Boolean = false,
    val usageAccess: Boolean = false,
    val notifications: Boolean = false,
)

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = AppRepository.get(application)
    private val usageRepo = UsageStatsRepository(application)

    val settings: StateFlow<Settings> = repo.settings
    val events: StateFlow<List<BlockEvent>> = repo.events
    val watch: StateFlow<List<WatchRecord>> = repo.watch
    val ready: StateFlow<Boolean> = repo.ready

    private val _usage = MutableStateFlow(UsageSnapshot(granted = false))
    val usage: StateFlow<UsageSnapshot> = _usage.asStateFlow()

    private val _permissions = MutableStateFlow(readPermissions())
    val permissions: StateFlow<PermissionState> = _permissions.asStateFlow()

    fun refreshPermissions() {
        _permissions.value = readPermissions()
    }

    fun refreshUsage() {
        viewModelScope.launch { _usage.value = usageRepo.load() }
    }

    private fun readPermissions(): PermissionState {
        val app = getApplication<Application>()
        val manager = app.getSystemService(AccessibilityManager::class.java)
        val accessibility = manager
            ?.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            ?.any { it.resolveInfo?.serviceInfo?.packageName == app.packageName } == true
        return PermissionState(
            accessibility = accessibility,
            usageAccess = usageRepo.hasPermission(),
            notifications = NotificationManagerCompat.from(app).areNotificationsEnabled(),
        )
    }

    fun completeOnboarding() = repo.completeOnboarding()
    fun setMaster(enabled: Boolean) = repo.setMaster(enabled)
    fun setStrict(enabled: Boolean) = repo.setStrict(enabled)
    fun setThemeMode(mode: String) = repo.setThemeMode(mode)
    fun setTrackingEnabled(enabled: Boolean) = repo.setTrackingEnabled(enabled)
    fun setNudgeMinutes(minutes: Int) = repo.setNudgeMinutes(minutes)
    fun setCountLongVideos(enabled: Boolean) = repo.setCountLongVideos(enabled)
    fun setAmbientTimer(enabled: Boolean) {
        repo.setAmbientTimer(enabled)
        io.github.dharma_labs.openeye.service.GuardAccessibilityService.instance?.ambientSettingChanged()
    }
    fun setDailyLimit(platformId: String, minutes: Int) = repo.setDailyLimit(platformId, minutes)
    fun setPlatformEnabled(platformId: String, enabled: Boolean) = repo.setPlatformEnabled(platformId, enabled)
    fun setSurfaceMode(platformId: String, surfaceId: String, mode: BlockMode) =
        repo.setSurfaceMode(platformId, surfaceId, mode)

    fun updateSchedule(transform: (ScheduleConfig) -> ScheduleConfig) = repo.updateSchedule(transform)

    fun signContract(minutes: Int, why: String = "") = repo.signContract(minutes, why)
    fun setUsualFeeling(feeling: String) = repo.setUsualFeeling(feeling)
    fun addPlan(trigger: String, action: String) = repo.addPlan(trigger, action)
    fun removePlan(id: String) = repo.removePlan(id)
    fun setPlanEnabled(id: String, enabled: Boolean) = repo.setPlanEnabled(id, enabled)
    fun logFeeling(feeling: String, minutes: Long, source: String) = repo.logFeeling(feeling, minutes, source)
    fun advancePromptCursor() = repo.advancePromptCursor()
    fun markLapseHandled(day: Long) = repo.markLapseHandled(day)
    fun markPlanAsked() = repo.markPlanAsked()

    fun startFocus(minutes: Int) {
        repo.startFocus(minutes)
        FocusTimerService.start(getApplication())
    }

    fun stopFocus() {
        repo.stopFocus()
        FocusTimerService.stop(getApplication())
    }

    fun clearAll() = repo.clearAll()

    fun exportText(): String = repo.exportSummary()

    companion object {
        fun openAccessibilitySettings(context: Context) {
            runCatching {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }

        fun openUsageSettings(context: Context) {
            runCatching {
                context.startActivity(
                    Intent(AndroidSettings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
        }

        fun openNotificationSettings(context: Context) {
            runCatching {
                val intent = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, context.packageName)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }
}