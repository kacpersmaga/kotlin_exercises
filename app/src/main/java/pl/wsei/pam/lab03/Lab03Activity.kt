package pl.wsei.pam.lab03

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.media.MediaPlayer
import android.os.Bundle
import android.view.Gravity
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import android.view.animation.DecelerateInterpolator
import android.widget.GridLayout
import android.widget.ImageButton
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import pl.wsei.pam.lab01.R
import java.util.Random
import kotlin.concurrent.schedule

class Lab03Activity : AppCompatActivity() {
    private lateinit var mBoard: GridLayout
    private lateinit var mBoardModel: MemoryBoardView
    lateinit var completionPlayer: MediaPlayer
    lateinit var negativePLayer: MediaPlayer
    var isSound: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_lab03)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        mBoard = findViewById(R.id.main)

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }

        val size = intent.getIntArrayExtra("size") ?: intArrayOf(3, 3)
        val rows = size[0]
        val cols = size[1]

        mBoard.columnCount = cols
        mBoard.rowCount = rows

        mBoardModel = MemoryBoardView(mBoard, cols, rows)

        if (savedInstanceState != null) {
            val boardState = savedInstanceState.getIntArray("boardState")
                ?: IntArray(cols * rows) { -1 }
            mBoardModel.setState(boardState)
        }

        mBoardModel.setOnGameChangeListener { e ->
            run {
                when (e.state) {
                    GameStates.Matching -> {
                        e.tiles.forEach { it.revealed = true }
                    }
                    GameStates.Match -> {
                        e.tiles.forEach { it.revealed = true }
                        if (isSound) completionPlayer.start()
                        setBoardEnabled(false)
                        var finishedCount = 0
                        val onFinish = Runnable {
                            finishedCount++
                            if (finishedCount == e.tiles.size) {
                                setBoardEnabled(true)
                            }
                        }
                        e.tiles.forEach { tile ->
                            animatePairedButton(tile.button, onFinish)
                        }
                    }
                    GameStates.NoMatch -> {
                        e.tiles.forEach { it.revealed = true }
                        if (isSound) negativePLayer.start()
                        setBoardEnabled(false)
                        var finishedCount = 0
                        val onFinish = Runnable {
                            finishedCount++
                            if (finishedCount == e.tiles.size) {
                                e.tiles.forEach { it.revealed = false }
                                setBoardEnabled(true)
                            }
                        }
                        e.tiles.forEach { tile ->
                            animateNoMatchButton(tile.button, onFinish)
                        }
                    }
                    GameStates.Finished -> {
                        e.tiles.forEach { it.revealed = true }
                        if (isSound) completionPlayer.start()
                        setBoardEnabled(false)
                        var finishedCount = 0
                        val onFinish = Runnable {
                            finishedCount++
                            if (finishedCount == e.tiles.size) {
                                setBoardEnabled(true)
                                Toast.makeText(this, "Game finished", Toast.LENGTH_SHORT).show()
                            }
                        }
                        e.tiles.forEach { tile ->
                            animatePairedButton(tile.button, onFinish)
                        }
                    }
                }
            }
        }
    }

    private fun setBoardEnabled(enabled: Boolean) {
        for (i in 0 until mBoard.childCount) {
            mBoard.getChildAt(i).isEnabled = enabled
        }
    }

    private fun animatePairedButton(button: ImageButton, action: Runnable) {
        val set = AnimatorSet()
        val random = Random()
        button.pivotX = random.nextFloat() * 200f
        button.pivotY = random.nextFloat() * 200f

        val rotation = ObjectAnimator.ofFloat(button, "rotation", 1080f)
        val scallingX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 4f)
        val scallingY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 4f)
        val fade = ObjectAnimator.ofFloat(button, "alpha", 1f, 0f)
        set.startDelay = 500
        set.duration = 2000
        set.interpolator = DecelerateInterpolator()
        set.playTogether(rotation, scallingX, scallingY, fade)
        set.addListener(object : Animator.AnimatorListener {
            override fun onAnimationStart(animator: Animator) {}
            override fun onAnimationEnd(animator: Animator) {
                button.scaleX = 1f
                button.scaleY = 1f
                button.alpha = 0.0f
                action.run()
            }
            override fun onAnimationCancel(animator: Animator) {}
            override fun onAnimationRepeat(animator: Animator) {}
        })
        set.start()
    }

    private fun animateNoMatchButton(button: ImageButton, action: Runnable) {
        val animator = ObjectAnimator.ofFloat(button, "translationX", 0f, 25f, -25f, 25f, -25f, 15f, -15f, 6f, -6f, 0f)
        animator.duration = 500
        animator.startDelay = 500
        animator.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                action.run()
            }
        })
        animator.start()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val inflater: MenuInflater = menuInflater
        inflater.inflate(R.menu.board_activity_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.board_activity_sound -> {
                isSound = !isSound
                if (isSound) {
                    Toast.makeText(this, "Sound turn on", Toast.LENGTH_SHORT).show()
                    item.setIcon(R.drawable.baseline_volume_up_24)
                } else {
                    Toast.makeText(this, "Sound turn off", Toast.LENGTH_SHORT).show()
                    item.setIcon(R.drawable.baseline_volume_off_24)
                }
            }
        }
        return false
    }

    override fun onResume() {
        super.onResume()
        completionPlayer = MediaPlayer.create(applicationContext, R.raw.completion)
        negativePLayer = MediaPlayer.create(applicationContext, R.raw.negative_guitar)
    }

    override fun onPause() {
        super.onPause()
        completionPlayer.release()
        negativePLayer.release()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntArray("boardState", mBoardModel.getState())
    }
}