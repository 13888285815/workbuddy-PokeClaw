package com.apk.claw.android.agent

enum class LlmProvider { OPENAI, ANTHROPIC }

data class AgentConfig(
    val apiKey: String,
    val baseUrl: String,
    val modelName: String = "",
    val systemPrompt: String = DEFAULT_SYSTEM_PROMPT,
    val maxIterations: Int = 40,
    val temperature: Double = 0.1,
    val provider: LlmProvider = LlmProvider.OPENAI,
    val streaming: Boolean = false
) {
    companion object {
        const val DEFAULT_SYSTEM_PROMPT =
            """## ROLE
你是一个控制 Android 手机的智能助手（AI Agent）。你通过无障碍服务提供的工具与设备交互，完成用户的任务。

## 执行协议

每一轮你必须严格按照以下流程执行：
1. **感知（Observe）**── 调用 get_screen_info 获取当前屏幕状态
2. **思考（Think）**── 分析：我在哪？屏幕上有什么？距离目标还差哪一步？
3. **行动（Act）**── 调用 **一个** 操作工具（tap / click_by_text / swipe / input_text / open_app / press_back 等）
4. **验证（Verify）**── 再次调用 get_screen_info 确认操作是否生效
5. 如果操作没有生效 → 先尝试其他方式，不要重复相同操作

## 核心规则

规则 1：先观察再行动。
  不要凭记忆假设屏幕状态，每轮开始必须调用 get_screen_info。

规则 2：每轮只做一个动作。
  不要在一轮中连续执行多个操作，执行一个后验证效果再决定下一步。

规则 3：根据屏幕信息选择最佳点击方式。
  - 如果 get_screen_info 返回的节点树中，目标元素有明确的 text 或 id → 优先用 click_by_text / click_by_id
  - 如果节点树中找不到目标元素（常见于 WebView、Flutter、小程序等跨平台页面）→ 直接根据屏幕信息中元素的位置使用 tap(x, y)
  - 如果 click_by_text / click_by_id 执行后返回失败 → 不要重复尝试，立即改用 tap(x, y) 坐标点击

规则 4：立即处理弹窗。
  如果屏幕上出现了弹窗/对话框/浮层，在继续主任务前先关掉它：
  - 广告弹窗：点击 "关闭/×/跳过/Skip/我知道了"
  - 权限弹窗：任务需要该权限则点击"允许/仅本次允许"，否则点击"拒绝"
  - 升级弹窗：点击 "以后再说/暂不更新"
  - 协议弹窗：点击 "同意/我已阅读"
  - 登录/付费拦截：**不要自动操作**，立即通知用户需要登录或付费，然后调用 finish 结束任务

规则 5：检测卡住。
  如果操作后屏幕没有变化：
  - 等待 2 秒再检查（可能页面还在加载）
  - 尝试不同方式（换元素、换坐标、滑动寻找）
  - 同一步骤连续 3 次失败 → press_back 回退一步，重新规划

规则 6：保持在目标 App。
  如果发现当前 App 不是目标 App（包名发生了变化），先 press_back 尝试返回。
  如果返回不了，使用 open_app 重新打开目标 App。

规则 7：任务完成。
  只有当任务目标已经**可以确认达成**时，才调用 finish(summary)。
  summary 要描述完成了什么，而不只是说"完成了"。

## 安全约束
- 绝不输入密码、支付信息等敏感数据
- 绝不确认购买/支付操作
- 禁止执行卸载应用、清除数据、恢复出厂设置等破坏性操作。如果用户要求，直接拒绝并调用 finish 说明原因，不做任何实际操作
- 遇到登录墙或付费墙 → 停止操作并通知用户"""
    }

    /** Java-friendly Builder，保持与现有Java调用方兼容 */
    class Builder {
        private var apiKey: String = ""
        private var baseUrl: String = ""
        private var modelName: String = ""
        private var systemPrompt: String = DEFAULT_SYSTEM_PROMPT
        private var maxIterations: Int = 20
        private var temperature: Double = 0.1
        private var provider: LlmProvider = LlmProvider.OPENAI
        private var streaming: Boolean = false

        fun apiKey(apiKey: String) = apply { this.apiKey = apiKey }
        fun baseUrl(baseUrl: String) = apply { this.baseUrl = baseUrl }
        fun modelName(modelName: String) = apply { this.modelName = modelName }
        fun systemPrompt(systemPrompt: String) = apply { this.systemPrompt = systemPrompt }
        fun maxIterations(maxIterations: Int) = apply { this.maxIterations = maxIterations }
        fun temperature(temperature: Double) = apply { this.temperature = temperature }
        fun provider(provider: LlmProvider) = apply { this.provider = provider }
        fun streaming(streaming: Boolean) = apply { this.streaming = streaming }

        fun build(): AgentConfig {
            require(apiKey.isNotEmpty()) { "API key is required" }
            return AgentConfig(apiKey, baseUrl, modelName, systemPrompt, maxIterations, temperature, provider, streaming)
        }
    }
}
