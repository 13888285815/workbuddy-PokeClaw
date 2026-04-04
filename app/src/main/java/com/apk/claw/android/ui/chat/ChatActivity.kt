package com.apk.claw.android.ui.chat

import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import com.apk.claw.android.R
import com.apk.claw.android.base.BaseActivity
import com.apk.claw.android.utils.KVUtils
import com.apk.claw.android.utils.XLog
import com.apk.claw.android.widget.CommonToolbar
import com.apk.claw.android.widget.KButton
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.util.concurrent.Executors

/**
 * Simple chat UI to verify on-device LLM works.
 * Direct conversation with the model — no agent/tool pipeline.
 */
class ChatActivity : BaseActivity() {

    companion object {
        private const val TAG = "ChatActivity"
    }

    private lateinit var tvStatus: TextView
    private lateinit var layoutMessages: LinearLayout
    private lateinit var scrollView: ScrollView
    private lateinit var etInput: EditText
    private lateinit var btnSend: KButton

    private val executor = Executors.newSingleThreadExecutor()
    private var engine: Engine? = null
    private var conversation: Conversation? = null
    private var isModelReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        findViewById<CommonToolbar>(R.id.toolbar).apply {
            setTitle("Chat (Local LLM)")
            showBackButton(true) { finish() }
        }

        tvStatus = findViewById(R.id.tvStatus)
        layoutMessages = findViewById(R.id.layoutMessages)
        scrollView = findViewById(R.id.scrollView)
        etInput = findViewById(R.id.etInput)
        btnSend = findViewById(R.id.btnSend)

        btnSend.isEnabled = false

        btnSend.setOnClickListener {
            val text = etInput.text.toString().trim()
            if (text.isEmpty()) return@setOnClickListener
            if (!isModelReady) {
                Toast.makeText(this, "Model still loading...", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            addMessage(text, isUser = true)
            etInput.text.clear()
            btnSend.isEnabled = false

            val responseView = addMessage("...", isUser = false)

            executor.submit {
                try {
                    val response = conversation!!.sendMessage(text)
                    val responseText = response?.toString() ?: "(empty response)"
                    runOnUiThread {
                        responseView.text = responseText
                        scrollToBottom()
                        btnSend.isEnabled = true
                    }
                } catch (e: Exception) {
                    XLog.e(TAG, "Inference error", e)
                    runOnUiThread {
                        responseView.text = "Error: ${e.message}"
                        btnSend.isEnabled = true
                    }
                }
            }
        }

        // Load model
        val modelPath = KVUtils.getLocalModelPath()
        if (modelPath.isEmpty()) {
            tvStatus.text = "No model downloaded. Go to Settings → LLM Config → On-Device LLM → Download"
            return
        }

        tvStatus.text = "Loading model: ${modelPath.substringAfterLast('/')}"
        executor.submit { loadModel(modelPath) }
    }

    private fun loadModel(modelPath: String) {
        try {
            // Try GPU first, fall back to CPU if GPU not supported
            val backend = try {
                runOnUiThread { tvStatus.text = "Trying GPU backend..." }
                Backend.GPU()
            } catch (e: Exception) {
                XLog.w(TAG, "GPU backend not available, falling back to CPU", e)
                runOnUiThread { tvStatus.text = "GPU not available, using CPU..." }
                Backend.CPU()
            }

            runOnUiThread { tvStatus.text = "Initializing engine (${if (backend is Backend.GPU) "GPU" else "CPU"})..." }

            val engineConfig = EngineConfig(
                modelPath = modelPath,
                backend = backend,
                maxNumTokens = 2048,
                cacheDir = cacheDir.path
            )
            engine = Engine(engineConfig)

            runOnUiThread { tvStatus.text = "Loading model (this may take a minute)..." }
            engine!!.initialize()

            runOnUiThread { tvStatus.text = "Creating conversation..." }
            conversation = engine!!.createConversation(
                ConversationConfig(
                    systemInstruction = Contents.of("You are a helpful assistant on an Android phone."),
                    samplerConfig = SamplerConfig(topK = 64, topP = 0.95, temperature = 0.7)
                )
            )

            isModelReady = true
            runOnUiThread {
                tvStatus.text = "Model ready: ${modelPath.substringAfterLast('/')}"
                btnSend.isEnabled = true
                addMessage("Model loaded! You can start chatting.", isUser = false)
            }
        } catch (e: Exception) {
            XLog.e(TAG, "Failed to load model", e)
            runOnUiThread {
                tvStatus.text = "Failed to load: ${e.message}"
            }
        }
    }

    private fun addMessage(text: String, isUser: Boolean): TextView {
        val tv = TextView(this).apply {
            this.text = text
            textSize = 15f
            setPadding(dp(14), dp(10), dp(14), dp(10))

            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(6)
                bottomMargin = dp(6)
                if (isUser) {
                    gravity = Gravity.END
                    marginStart = dp(60)
                } else {
                    gravity = Gravity.START
                    marginEnd = dp(60)
                }
            }
            layoutParams = params

            if (isUser) {
                setBackgroundColor(0xFF2962FF.toInt())
                setTextColor(0xFFFFFFFF.toInt())
            } else {
                setBackgroundColor(0xFF333333.toInt())
                setTextColor(0xFFE0E0E0.toInt())
            }
        }

        layoutMessages.addView(tv)
        scrollToBottom()
        return tv
    }

    private fun scrollToBottom() {
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    override fun onDestroy() {
        super.onDestroy()
        executor.submit {
            conversation?.close()
            engine?.close()
        }
        executor.shutdown()
    }
}
