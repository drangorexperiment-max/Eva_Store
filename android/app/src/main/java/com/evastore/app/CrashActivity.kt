package com.evastore.app

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File

/**
 * Простой экран отчёта об ошибке на классических View (без Compose),
 * чтобы он гарантированно открывался даже при сбое основного UI.
 */
class CrashActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = runCatching { File(filesDir, "last_crash.txt").readText() }
            .getOrDefault("Не удалось прочитать отчёт об ошибке")

        val pad = (16 * resources.displayMetrics.density).toInt()

        val title = TextView(this).apply {
            text = "Eva Store: отчёт об ошибке"
            textSize = 20f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, pad / 2)
        }

        val hint = TextView(this).apply {
            text = "Сделайте скриншот этого экрана или скопируйте текст и отправьте разработчику."
            textSize = 14f
            setTextColor(Color.LTGRAY)
            setPadding(0, 0, 0, pad)
        }

        val traceView = TextView(this).apply {
            text = trace
            textSize = 12f
            setTextColor(Color.parseColor("#FCA5A5"))
            setTextIsSelectable(true)
        }

        val copyButton = Button(this).apply {
            text = "Скопировать текст ошибки"
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("crash", trace))
            }
        }

        val scroll = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f
            )
            addView(traceView)
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.parseColor("#111827"))
            setPadding(pad, pad * 2, pad, pad)
            addView(title)
            addView(hint)
            addView(scroll)
            addView(copyButton)
        }

        setContentView(root)
    }
}
