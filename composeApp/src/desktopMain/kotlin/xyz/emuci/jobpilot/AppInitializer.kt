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
        isInitialized = true

           // AppConfig.createDefaultBossConfig()
           //println(AppConfig.getBossConfigPath() ?: "Boss config file not found.")
    }
}