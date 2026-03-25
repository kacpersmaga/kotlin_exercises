package pl.wsei.pam.lab03

import android.widget.ImageButton

data class Tile(val button: ImageButton, var tileResource: Int, val deckResource: Int) {
    init {
        button.setImageResource(deckResource)
    }

    private var _revealed: Boolean = false
    var revealed: Boolean
        get() = _revealed
        set(value) {
            _revealed = value
            if (_revealed) {
                button.setImageResource(tileResource)
            } else {
                button.setImageResource(deckResource)
                button.alpha = 1.0f
                button.scaleX = 1.0f
                button.scaleY = 1.0f
            }
        }

    fun removeOnClickListener() {
        button.setOnClickListener(null)
    }
}