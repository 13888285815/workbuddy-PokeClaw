package com.apk.claw.android.tool

import com.apk.claw.android.tool.impl.*
import com.apk.claw.android.tool.impl.mobile.*
import com.apk.claw.android.tool.impl.tv.*

object ToolRegistry {

    enum class DeviceType { TV, MOBILE }

    private val tools = LinkedHashMap<String, BaseTool>()
    var deviceType: DeviceType = DeviceType.TV
        private set

    @JvmStatic
    fun getInstance(): ToolRegistry = this

    fun registerAllTools(type: DeviceType = DeviceType.TV) {
        deviceType = type
        tools.clear()
        registerCommonTools()
        when (type) {
            DeviceType.TV -> registerTvTools()
            DeviceType.MOBILE -> registerMobileTools()
        }
    }

    private fun registerCommonTools() {
        register(GetScreenInfoTool())
        register(FindNodeInfoTool())
        register(InputTextTool())
        register(PressBackTool())
        register(PressHomeTool())
        register(OpenRecentAppsTool())
        register(ExpandNotificationsTool())
        register(CollapseNotificationsTool())
        register(OpenAppTool())
        register(GetInstalledAppsTool())
        register(LockScreenTool())
        register(TakeScreenshotTool())
        register(WaitTool())
        register(RepeatActionsTool())
        register(SendFileTool())
        register(FinishTool())
    }

    private fun registerTvTools() {
        register(DpadUpTool())
        register(DpadDownTool())
        register(DpadLeftTool())
        register(DpadRightTool())
        register(DpadCenterTool())
        register(VolumeUpTool())
        register(VolumeDownTool())
        register(PressMenuTool())
        register(PressPowerTool())
    }

    private fun registerMobileTools() {
        register(TapTool())
        register(LongPressTool())
        register(SwipeTool())
        register(ClickByTextTool())
        register(ClickByIdTool())
    }

    fun register(tool: BaseTool) {
        tools[tool.getName()] = tool
    }

    fun getTool(name: String): BaseTool? = tools[name]

    fun getDisplayName(name: String): String = tools[name]?.getDisplayName() ?: name

    fun getAllTools(): List<BaseTool> = tools.values.toList()

    fun executeTool(name: String, params: Map<String, Any>): ToolResult {
        val tool = tools[name] ?: return ToolResult.error("Unknown tool: $name")
        return try {
            tool.execute(params)
        } catch (e: Exception) {
            ToolResult.error("Tool execution failed: ${e.message}")
        }
    }
}
