package com.apk.claw.android.floating

import android.app.Application
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.annotation.DrawableRes
import com.blankj.utilcode.util.ThreadUtils
import com.apk.claw.android.R
import com.apk.claw.android.channel.Channel
import com.apk.claw.android.utils.KVUtils
import com.lzf.easyfloat.EasyFloat
import com.lzf.easyfloat.enums.ShowPattern
import com.lzf.easyfloat.enums.SidePattern
import com.lzf.easyfloat.interfaces.OnFloatCallbacks
import com.lzf.easyfloat.utils.DisplayUtils

/**
 * 圆形悬浮窗管理器
 * 使用 EasyFloat 实现可拖动、记录位置的圆形悬浮窗
 * 支持多种状态：等待任务(IDLE)、任务执行中(RUNNING)、任务成功(SUCCESS)、任务失败(ERROR)
 */
object FloatingCircleManager {

    private const val FLOAT_TAG = "circle_float"
    private const val KEY_FLOAT_X = "floating_circle_x"
    private const val KEY_FLOAT_Y = "floating_circle_y"
    private const val AUTO_RESET_DELAY_MS = 5000L // 5秒后自动重置

    /**
     * 悬浮窗状态
     */
    enum class State {
        IDLE,       // 等待任务（默认）
        RUNNING,    // 任务执行中
        SUCCESS,    // 任务完成
        ERROR       // 任务失败
    }

    private var isShowing = false
    private var currentState: State = State.IDLE
    private var currentRound: Int = 0
    private var currentChannel: Channel? = null

    private val mainHandler = Handler(Looper.getMainLooper())
    private var autoResetRunnable: Runnable? = null

    /**
     * 显示悬浮窗
     * @param application Application 实例
     * @param x 初始位置 X（可选，默认屏幕右边偏中心）
     * @param y 初始位置 Y（可选，默认屏幕中心）
     */
    fun show(
        application: Application,
        x: Int? = null,
        y: Int? = null
    ) {
        if (isShowing) {
            return
        }

        // 计算默认位置：屏幕中心的右边
        val screenWidth = DisplayUtils.getScreenWidth(application)
        val screenHeight = DisplayUtils.getScreenHeight(application)
        val defaultX = 0
        val defaultY = screenHeight / 2

        // 从本地读取保存的位置
        val savedX = getSavedX() ?: x ?: defaultX
        val savedY = getSavedY() ?: y ?: defaultY

        EasyFloat.with(application)
            .setLayout(R.layout.layout_floating_circle)
            .setShowPattern(ShowPattern.ALL_TIME)
            .setSidePattern(SidePattern.DEFAULT)
            .setGravity(android.view.Gravity.START or android.view.Gravity.TOP, savedX, savedY)
            .setDragEnable(true)
            .hasEditText(false)
            .setTag(FLOAT_TAG)
            .registerCallbacks(object : OnFloatCallbacks {

                override fun createdResult(
                    isCreated: Boolean,
                    msg: String?,
                    view: View?
                ) {
                    // 点击事件
                    view?.setOnClickListener {
                        onFloatClick()
                    }
                    // 初始化状态
                    updateStateView(view, currentState)
                }

                override fun dismiss() {
                    isShowing = false
                }

                override fun drag(view: View, event: MotionEvent) {
                }

                override fun dragEnd(view: View) {
                    // 拖动结束，保存位置
                    val location = IntArray(2)
                    view.getLocationOnScreen(location)
                    savePosition(location[0], location[1])
                }

                override fun hide(view: View) {
                    isShowing = false
                }

                override fun show(view: View) {
                    isShowing = true
                }

                override fun touchEvent(view: View, event: MotionEvent) {

                }
            })
            .show()
    }

    /**
     * 隐藏悬浮窗
     */
    fun hide() {
        if (isShowing) {
            EasyFloat.dismiss(FLOAT_TAG)
            isShowing = false
        }
    }

    /**
     * 判断是否显示中
     */
    fun isShowing(): Boolean = isShowing

    /**
     * 切换到等待任务状态（默认）
     */
    fun setIdleState() {
        ThreadUtils.runOnUiThread {
            setState(State.IDLE)
        }
    }

    /**
     * 切换到任务执行中状态
     * @param round 当前轮数
     * @param channel 消息来源渠道
     */
    fun setRunningState(round: Int, channel: Channel) {
        ThreadUtils.runOnUiThread {
            currentRound = round
            currentChannel = channel
            setState(State.RUNNING)
        }

    }

    /**
     * 切换到任务完成状态（5秒后自动回到 IDLE）
     */
    fun setSuccessState() {
        ThreadUtils.runOnUiThread {
            setState(State.SUCCESS)
            scheduleAutoReset()
        }
    }

    /**
     * 切换到任务失败状态（5秒后自动回到 IDLE）
     */
    fun setErrorState() {
        ThreadUtils.runOnUiThread {
            setState(State.ERROR)
            scheduleAutoReset()
        }

    }

    /**
     * 设置状态
     */
    private fun setState(state: State) {
        currentState = state
        val view = EasyFloat.getFloatView(FLOAT_TAG)
        view?.let { updateStateView(it, state) }
    }

    /**
     * 更新视图状态
     */
    private fun updateStateView(view: View?, state: State) {
        if (view == null) return

        val cardIdle = view.findViewById<View>(R.id.cardIdle)
        val cardRunning = view.findViewById<View>(R.id.cardRunning)
        val cardSuccess = view.findViewById<View>(R.id.cardSuccess)
        val cardError = view.findViewById<View>(R.id.cardError)

        // 隐藏所有状态
        cardIdle?.visibility = View.GONE
        cardRunning?.visibility = View.GONE
        cardSuccess?.visibility = View.GONE
        cardError?.visibility = View.GONE

        // 取消之前的自动重置
        cancelAutoReset()

        // 显示对应状态
        when (state) {
            State.IDLE -> {
                cardIdle?.visibility = View.VISIBLE
            }
            State.RUNNING -> {
                cardRunning?.visibility = View.VISIBLE
                // 更新轮数显示
                val tvRound = view.findViewById<TextView>(R.id.tvRound)
                tvRound?.text = currentRound.toString()
                // 更新渠道 Logo
                val ivChannelLogo = view.findViewById<ImageView>(R.id.ivChannelLogo)
                ivChannelLogo?.setImageResource(getChannelIcon(currentChannel))
            }
            State.SUCCESS -> {
                cardSuccess?.visibility = View.VISIBLE
            }
            State.ERROR -> {
                cardError?.visibility = View.VISIBLE
            }
        }
    }

    /**
     * 获取渠道对应的图标
     */
    @DrawableRes
    private fun getChannelIcon(channel: Channel?): Int {
        return when (channel) {
            Channel.DINGTALK -> R.drawable.ic_channel_dingtalk
            Channel.FEISHU -> R.drawable.ic_channel_feishu
            Channel.QQ -> R.drawable.ic_channel_qq
            Channel.DISCORD -> R.drawable.ic_channel_discord
            Channel.TELEGRAM -> R.drawable.ic_channel_telegram
            else -> R.drawable.ic_launcher
        }
    }

    /**
     * 5秒后自动重置到 IDLE 状态
     */
    private fun scheduleAutoReset() {
        cancelAutoReset()
        autoResetRunnable = Runnable {
            setIdleState()
        }
        mainHandler.postDelayed(autoResetRunnable!!, AUTO_RESET_DELAY_MS)
    }

    /**
     * 取消自动重置
     */
    private fun cancelAutoReset() {
        autoResetRunnable?.let {
            mainHandler.removeCallbacks(it)
            autoResetRunnable = null
        }
    }

    /**
     * 保存位置
     */
    private fun savePosition(x: Int, y: Int) {
        KVUtils.putInt(KEY_FLOAT_X, x)
        KVUtils.putInt(KEY_FLOAT_Y, y)
    }

    /**
     * 获取保存的 X 坐标
     */
    private fun getSavedX(): Int? {
        val x = KVUtils.getInt(KEY_FLOAT_X, -1)
        return if (x == -1) null else x
    }

    /**
     * 获取保存的 Y 坐标
     */
    private fun getSavedY(): Int? {
        val y = KVUtils.getInt(KEY_FLOAT_Y, -1)
        return if (y == -1) null else y
    }

    /**
     * 点击回调，可以在外部设置
     */
    var onFloatClick: () -> Unit = {}
}
