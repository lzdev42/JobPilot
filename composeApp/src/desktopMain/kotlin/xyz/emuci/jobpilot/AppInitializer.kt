package xyz.emuci.jobpilot

import ai.AiManager
import utils.AppConfig

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
        
        // 创建默认Boss配置文件
        AppConfig.createDefaultBossConfig()
        
        isInitialized = true
    }
}