package xyz.emuci.jobpilot

import ai.AiManager
import utils.AppConfig
import kotlinx.coroutines.runBlocking

object AppInitializer {
    private var isInitialized = false
    private var lock = Any()
    fun initialize() {
        if (isInitialized){
            return
        }
        init()
    }

    private fun init(){
        AiManager.initGemini()
        
        // 每次启动预加载Boss配置
        runBlocking {
            AppConfig.getBossConfig()
        }
        
        isInitialized = true
    }
}