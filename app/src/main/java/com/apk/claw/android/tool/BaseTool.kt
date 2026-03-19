package com.apk.claw.android.tool

abstract class BaseTool {

    companion object {
        /** 工具描述语言，设为 true 使用中文描述，false 使用英文 */
        @JvmField
        var useChineseDescription: Boolean = true
    }

    abstract fun getName(): String
    abstract fun getParameters(): List<ToolParameter>
    abstract fun execute(params: @JvmSuppressWildcards Map<String, Any>): ToolResult

    /** 英文描述，子类必须实现 */
    abstract fun getDescriptionEN(): String

    /** 中文描述，子类必须实现 */
    abstract fun getDescriptionCN(): String

    /** 根据语言开关返回描述 */
    fun getDescription(): String =
        if (useChineseDescription) getDescriptionCN() else getDescriptionEN()

    /** 用于展示给用户看的中文名称，子类可覆写 */
    open fun getDisplayName(): String = getName()

    // === Parameter helpers ===

    protected fun requireString(params: @JvmSuppressWildcards Map<String, Any>, key: String): String {
        return params[key]?.toString()
            ?: throw IllegalArgumentException("Missing required parameter: $key")
    }

    protected fun requireInt(params: @JvmSuppressWildcards Map<String, Any>, key: String): Int {
        val value = params[key] ?: throw IllegalArgumentException("Missing required parameter: $key")
        return when (value) {
            is Number -> value.toInt()
            else -> value.toString().toInt()
        }
    }

    protected fun requireLong(params: @JvmSuppressWildcards Map<String, Any>, key: String): Long {
        val value = params[key] ?: throw IllegalArgumentException("Missing required parameter: $key")
        return when (value) {
            is Number -> value.toLong()
            else -> value.toString().toLong()
        }
    }

    protected fun optionalInt(params: @JvmSuppressWildcards Map<String, Any>, key: String, defaultValue: Int): Int {
        val value = params[key] ?: return defaultValue
        return when (value) {
            is Number -> value.toInt()
            else -> value.toString().toInt()
        }
    }

    protected fun optionalLong(params: @JvmSuppressWildcards Map<String, Any>, key: String, defaultValue: Long): Long {
        val value = params[key] ?: return defaultValue
        return when (value) {
            is Number -> value.toLong()
            else -> value.toString().toLong()
        }
    }

    protected fun optionalString(params: @JvmSuppressWildcards Map<String, Any>, key: String, defaultValue: String): String {
        return params[key]?.toString() ?: defaultValue
    }
}
