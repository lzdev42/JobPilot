package statusbar

import androidx.compose.runtime.*
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import utils.AppConfig
import utils.AppConstants
import java.lang.management.ManagementFactory
import kotlin.math.roundToInt

class StatusBarViewModel: ViewModel() {
    var heapUsedMb by mutableStateOf(0)
        private set
    var heapTotalMb by mutableStateOf(0)
        private set
    var heapMaxMb by mutableStateOf(0)
        private set
    var nonHeapUsedMb by mutableStateOf(0)
        private set
    private var monitorJob: Job? = null
    private val jvmUtils = JvmMemoryUtils()
    val applicationDisplayVersion: String = AppConstants.APP_VERSION // 使用 UI 显示版本
    val operatingSystemInfo: String


    init {
        // 获取一次最大堆内存，因为它通常不变
        // 在init中获取可以避免每次协程重启都获取
        // 但如果ViewModel可能被完全销毁重建，则在startMonitoring中获取更保险
        // 对于标准的viewModel()行为，它在配置更改后会存活，所以init可以
        val initialHeapInfo = jvmUtils.getHeapMemoryInfo()
        heapMaxMb = initialHeapInfo.maxMb

        val osName = System.getProperty("os.name") ?: "未知系统"
        val osVersion = System.getProperty("os.version") ?: "未知版本"
        val osArch = System.getProperty("os.arch") ?: "未知架构"
        operatingSystemInfo = "$osName $osVersion ($osArch)"

        startMonitoring()
    }

    fun startMonitoring(intervalMillis: Long = 1000) {
        if (monitorJob?.isActive == true) {
            return
        }
        monitorJob = viewModelScope.launch(Dispatchers.IO) {
            while (isActive) {
                val heapInfo = jvmUtils.getHeapMemoryInfo()
                heapUsedMb = heapInfo.usedMb
                heapTotalMb = heapInfo.totalMb
                // heapMaxMb is typically set in init or once at start

                val nonHeapInfo = jvmUtils.getNonHeapMemoryInfo()
                nonHeapUsedMb = nonHeapInfo.usedMb
                //jvmUtils.printCurrentMemoryUsage()
                delay(intervalMillis)
            }
        }
    }

    fun stopMonitoring() {
        monitorJob?.cancel()
        monitorJob = null
    }

    override fun onCleared() {
        super.onCleared()
        stopMonitoring()
    }
}


class JvmMemoryUtils {

    private val MB = 1024 * 1024

    data class HeapMemoryInfo(
        val usedMb: Int,
        val totalMb: Int, // Current committed heap size
        val maxMb: Int    // Max heap size (-Xmx)
    )

    data class NonHeapMemoryInfo(
        val usedMb: Int,
        val committedMb: Int,
        val maxMbString: String // Max can be undefined (-1L)
    )

    /**
     * 获取当前JVM堆内存使用情况。
     * @return HeapMemoryInfo 数据类实例
     */
    fun getHeapMemoryInfo(): HeapMemoryInfo {
        val runtime = Runtime.getRuntime()
        val totalMemory = runtime.totalMemory()
        val freeMemory = runtime.freeMemory()
        val usedMemory = totalMemory - freeMemory
        val maxMemory = runtime.maxMemory()

        return HeapMemoryInfo(
            usedMb = (usedMemory.toDouble() / MB).roundToInt(),
            totalMb = (totalMemory.toDouble() / MB).roundToInt(),
            maxMb = (maxMemory.toDouble() / MB).roundToInt()
        )
    }

    /**
     * 获取当前JVM非堆内存使用情况 (主要是Metaspace和Code Cache等)。
     * @return NonHeapMemoryInfo 数据类实例
     */
    fun getNonHeapMemoryInfo(): NonHeapMemoryInfo {
        val memoryMXBean = ManagementFactory.getMemoryMXBean()
        val nonHeapUsage = memoryMXBean.nonHeapMemoryUsage

        return NonHeapMemoryInfo(
            usedMb = (nonHeapUsage.used.toDouble() / MB).roundToInt(),
            committedMb = (nonHeapUsage.committed.toDouble() / MB).roundToInt(),
            maxMbString = if (nonHeapUsage.max == -1L) "Undefined" else "${(nonHeapUsage.max.toDouble() / MB).roundToInt()} MB"
        )
    }

    /**
     * 简单打印当前内存信息到控制台。
     */
    fun printCurrentMemoryUsage() {
        val heapInfo = getHeapMemoryInfo()
        val nonHeapInfo = getNonHeapMemoryInfo()

        // 内存信息调试输出，只在debug模式显示
        if (AppConfig.isDebugMode) {
            println("--- JVM Heap Memory ---")
            println("Used:      ${heapInfo.usedMb} MB")
            println("Total (Committed): ${heapInfo.totalMb} MB")
            println("Max (-Xmx): ${heapInfo.maxMb} MB")
            println("--- JVM Non-Heap Memory ---")
            println("Used:      ${nonHeapInfo.usedMb} MB")
            println("Committed: ${nonHeapInfo.committedMb} MB")
            println("Max:       ${nonHeapInfo.maxMbString}")
            println("-------------------------")
        }
    }
}