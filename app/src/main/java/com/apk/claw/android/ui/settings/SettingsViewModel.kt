package com.apk.claw.android.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.apk.claw.android.ClawApplication
import com.apk.claw.android.R
import com.apk.claw.android.channel.ChannelManager
import com.apk.claw.android.server.ConfigServerManager
import com.apk.claw.android.utils.KVUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * SettingsActivity 的 ViewModel
 */
class SettingsViewModel : ViewModel() {

    // 设置项数据 Flow（用于动态更新）
    private val _settingItems = MutableStateFlow<Map<String, SettingValue>>(emptyMap())
    val settingItems: StateFlow<Map<String, SettingValue>> = _settingItems

    // 菜单点击事件
    private val _menuClickEvent = MutableStateFlow<MenuAction?>(null)
    val menuClickEvent: StateFlow<MenuAction?> = _menuClickEvent

    init {
        refresh()
    }

    fun refresh() {
        val dingtalkAppKey = KVUtils.getDingtalkAppKey().isNotEmpty()
        val dingtalkAppSecret = KVUtils.getDingtalkAppSecret().isNotEmpty()
        val feishuAppId = KVUtils.getFeishuAppId().isNotEmpty()
        val feishuAppSecret = KVUtils.getFeishuAppSecret().isNotEmpty()
        val qqAppId = KVUtils.getQqAppId().isNotEmpty()
        val qqAppSecret = KVUtils.getQqAppSecret().isNotEmpty()
        val discordBotToken = KVUtils.getDiscordBotToken().isNotEmpty()
        val telegramBotToken = KVUtils.getTelegramBotToken().isNotEmpty()
        val map = mapOf(
            MenuAction.LLM_CONFIG.name to SettingValue.Text(if (KVUtils.hasLlmConfig()) KVUtils.getLlmModelName() else ClawApplication.instance.getString(R.string.common_unconfigured)),
            MenuAction.DINGDING.name to SettingValue.Text(ClawApplication.instance.getString(if (dingtalkAppKey && dingtalkAppSecret) R.string.common_bound else R.string.common_unbound)),
            MenuAction.FEISHU.name to SettingValue.Text(ClawApplication.instance.getString(if (feishuAppId && feishuAppSecret) R.string.common_bound else R.string.common_unbound)),
            MenuAction.QQ.name to SettingValue.Text(ClawApplication.instance.getString(if (qqAppId && qqAppSecret) R.string.common_bound else R.string.common_unbound)),
            MenuAction.DISCORD.name to SettingValue.Text(ClawApplication.instance.getString(if (discordBotToken) R.string.common_bound else R.string.common_unbound)),
            MenuAction.TELEGRAM.name to SettingValue.Text(ClawApplication.instance.getString(if (telegramBotToken) R.string.common_bound else R.string.common_unbound)),
            MenuAction.LAN_CONFIG.name to SettingValue.Text(getLanConfigTrailingText())
        )
        _settingItems.value = map
    }

    /**
     * 更新设置项值
     */
    fun updateSettingValue(key: String, value: SettingValue) {
        _settingItems.value = _settingItems.value.toMutableMap().apply {
            put(key, value)
        }
    }

    /**
     * 更新尾部文字
     */
    fun updateTrailingText(key: String, text: String) {
        updateSettingValue(key, SettingValue.Text(text))
    }

    /**
     * 处理菜单项点击
     */
    fun onMenuItemClick(action: MenuAction) {
        _menuClickEvent.value = action
    }

    /**
     * 清空菜单点击事件
     */
    fun clearMenuClickEvent() {
        _menuClickEvent.value = null
    }


    /**
     * 切换局域网配置服务开关
     */
    fun toggleConfigServer(context: Context): String {
        return if (ConfigServerManager.isRunning()) {
            ConfigServerManager.stop()
            KVUtils.setConfigServerEnabled(false)
            val text = getLanConfigTrailingText()
            updateTrailingText(MenuAction.LAN_CONFIG.name, text)
            text
        } else {
            val started = ConfigServerManager.start(context)
            if (started) {
                KVUtils.setConfigServerEnabled(true)
                val text = getLanConfigTrailingText()
                updateTrailingText(MenuAction.LAN_CONFIG.name, text)
                text
            } else {
                ClawApplication.instance.getString(R.string.lan_config_no_wifi)
            }
        }
    }

    private fun getLanConfigTrailingText(): String {
        return if (ConfigServerManager.isRunning()) {
            ConfigServerManager.getAddress() ?: ClawApplication.instance.getString(R.string.lan_config_stopped)
        } else {
            ClawApplication.instance.getString(R.string.lan_config_stopped)
        }
    }

    fun isDingtalkBound(): Boolean {
        return KVUtils.getDingtalkAppKey().isNotEmpty() && KVUtils.getDingtalkAppSecret().isNotEmpty()
    }

    fun isFeishuBound(): Boolean {
        return KVUtils.getFeishuAppId().isNotEmpty() && KVUtils.getFeishuAppSecret().isNotEmpty()
    }

    fun isQqBound(): Boolean {
        return KVUtils.getQqAppId().isNotEmpty() && KVUtils.getQqAppSecret().isNotEmpty()
    }

    fun isDiscordBound(): Boolean {
        return KVUtils.getDiscordBotToken().isNotEmpty()
    }

    fun isTelegramBound(): Boolean {
        return KVUtils.getTelegramBotToken().isNotEmpty()
    }

    fun unbindDingtalk() {
        KVUtils.setDingtalkAppKey("")
        KVUtils.setDingtalkAppSecret("")
        ChannelManager.reinitDingTalkFromStorage()
        refresh()
    }

    fun unbindFeishu() {
        KVUtils.setFeishuAppId("")
        KVUtils.setFeishuAppSecret("")
        ChannelManager.reinitFeiShuFromStorage()
        refresh()
    }

    fun unbindQq() {
        KVUtils.setQqAppId("")
        KVUtils.setQqAppSecret("")
        ChannelManager.reinitQQFromStorage()
        refresh()
    }

    fun unbindDiscord() {
        KVUtils.setDiscordBotToken("")
        ChannelManager.reinitDiscordFromStorage()
        refresh()
    }

    fun unbindTelegram() {
        KVUtils.setTelegramBotToken("")
        ChannelManager.reinitTelegramFromStorage()
        refresh()
    }

    /**
     * 设置值密封类
     */
    sealed class SettingValue {
        data class Text(val text: String) : SettingValue()
        data class Switch(val isOn: Boolean) : SettingValue()
    }

    /**
     * 菜单动作枚举
     */
    enum class MenuAction {
        DINGDING, FEISHU, QQ, DISCORD, TELEGRAM, WECHAT_OFFICIAL_ACCOUNT,
        LAN_CONFIG,
        LLM_CONFIG
    }
}
