package com.example.foolgame.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import com.example.foolgame.game.Card
import com.example.foolgame.game.Suit

class CardView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    var card: Card? = null
    var isFaceUp: Boolean = true
    var cardSelected: Boolean = false
    var isPlayable: Boolean = true
    var canBeat: Boolean = false

    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.WHITE }
    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.LTGRAY; style = Paint.Style.STROKE; strokeWidth = 1.5f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 36f; typeface = Typeface.DEFAULT_BOLD
    }
    private val smallTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f; typeface = Typeface.DEFAULT_BOLD
    }
    private val bigSuitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 64f; typeface = Typeface.DEFAULT_BOLD; textAlign = Paint.Align.CENTER
    }
    private val selectedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xCC4CAF50.toInt(); style = Paint.Style.STROKE; strokeWidth = 4f
    }
    private val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val backPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val backPatternPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF3333AA.toInt() }
    private val backCenterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF8888DD.toInt(); textSize = 36f; textAlign = Paint.Align.CENTER; typeface = Typeface.DEFAULT_BOLD
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = (w * 160f / 110f).toInt()
        setMeasuredDimension(w, h)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        val r = 10f

        if (canBeat) {
            glowPaint.color = 0x4400CC00.toInt()
            canvas.drawRoundRect(2f, 2f, w - 2f, h - 2f, r, r, glowPaint)
        } else if (isPlayable) {
            glowPaint.color = 0x3300AA00.toInt()
            canvas.drawRoundRect(2f, 2f, w - 2f, h - 2f, r, r, glowPaint)
        }

        if (isFaceUp && card != null) {
            drawCardFace(canvas, card!!, w, h, r)
        } else {
            drawCardBack(canvas, w, h, r)
        }

        if (cardSelected) {
            canvas.drawRoundRect(2f, 2f, w - 2f, h - 2f, r, r, selectedPaint)
        }
    }

    private fun drawCardFace(canvas: Canvas, c: Card, w: Float, h: Float, r: Float) {
        canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, r, r, bgPaint)
        canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, r, r, borderPaint)

        val color = if (c.suit == Suit.HEARTS || c.suit == Suit.DIAMONDS) Color.RED else Color.BLACK
        textPaint.color = color
        smallTextPaint.color = color
        bigSuitPaint.color = color

        val rankStr = c.rank.display
        val suitSym = c.suit.symbol

        // Top-left rank
        smallTextPaint.textAlign = Paint.Align.LEFT
        canvas.drawText(rankStr, 8f, 22f, smallTextPaint)
        canvas.drawText(suitSym, 8f, 42f, smallTextPaint)

        // Bottom-right rank
        canvas.save()
        canvas.rotate(180f, w / 2f, h / 2f)
        canvas.drawText(rankStr, 8f, 22f, smallTextPaint)
        canvas.drawText(suitSym, 8f, 42f, smallTextPaint)
        canvas.restore()

        // Center suit
        bigSuitPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(suitSym, w / 2f, h / 2f + 22f, bigSuitPaint)
    }

    private fun drawCardBack(canvas: Canvas, w: Float, h: Float, r: Float) {
        backPaint.color = 0xFF191970.toInt()
        canvas.drawRoundRect(1f, 1f, w - 1f, h - 1f, r, r, backPaint)

        backPaint.color = 0xFF4444CC.toInt()
        backPaint.style = Paint.Style.STROKE
        backPaint.strokeWidth = 2f
        canvas.drawRoundRect(3f, 3f, w - 3f, h - 3f, r, r, backPaint)

        // Diagonal pattern
        backPatternPaint.color = 0x223333AA.toInt()
        backPatternPaint.strokeWidth = 2f
        for (i in -h.toInt() until w.toInt() step 14) {
            canvas.drawLine(i.toFloat(), 0f, (i - h).toFloat(), h, backPatternPaint)
            canvas.drawLine(i.toFloat(), 0f, (i + h).toFloat(), h, backPatternPaint)
        }

        // Center ellipse
        backPaint.color = 0x445555DD.toInt()
        backPaint.style = Paint.Style.STROKE
        backPaint.strokeWidth = 2f
        canvas.drawOval(w * 0.2f, h * 0.25f, w * 0.8f, h * 0.75f, backPaint)

        backCenterPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("\u2660\u2665", w / 2f, h / 2f + 12f, backCenterPaint)
    }
}
