package pl.wsei.pam.lab03

import android.view.Gravity
import android.view.View
import android.widget.GridLayout
import android.widget.ImageButton
import pl.wsei.pam.lab01.R // Upewnij się, że to poprawny import R dla Twojego projektu
import java.util.Stack

class MemoryBoardView(
    private val gridLayout: GridLayout,
    private val cols: Int,
    private val rows: Int
) {
    // Mapa przechowująca obiekty Tile, kluczem jest tag przycisku (np. "0x1")
    private val tiles: MutableMap<String, Tile> = mutableMapOf()

    // Lista identyfikatorów ikon dla przedniej strony kart.
    // DODAJ TU WIĘCEJ IKON, jeśli planujesz większe plansze!
    private val icons: List<Int> = listOf(
        R.drawable.baseline_360_24,
        R.drawable.baseline_audiotrack_24,
        R.drawable.baseline_anchor_24,
        R.drawable.baseline_cloud_24,
        R.drawable.baseline_add_home_24,
        R.drawable.baseline_alarm_on_24,
        R.drawable.baseline_album_24,
        R.drawable.baseline_account_balance_wallet_24,
        R.drawable.baseline_star_24,
        R.drawable.baseline_favorite_24,
        R.drawable.baseline_bolt_24,
        R.drawable.baseline_pets_24,
        R.drawable.baseline_cake_24,
        R.drawable.baseline_directions_car_24,
        R.drawable.baseline_eco_24,
        R.drawable.baseline_flight_24,
        R.drawable.baseline_sports_soccer_24,
        R.drawable.baseline_diamond_24
    )

    // Identyfikator zasobu dla tyłu karty (rewersu). MUSISZ MIEĆ TEN ZASÓB!
    private val deckResource: Int = R.drawable.deck

    // Listener zmian stanu gry, domyślnie pusta funkcja
    private var onGameChangeStateListener: (MemoryGameEvent) -> Unit = {}

    // Stos przechowujący aktualnie odkrywaną parę kart
    private val matchedPair: Stack<Tile> = Stack()

    // Logika gry, maxMatches to połowa liczby wszystkich kart
    private val logic: MemoryGameLogic = MemoryGameLogic(cols * rows / 2)

    init {
        // 1. Przygotowanie ikon: pobieramy potrzebną liczbę unikalnych ikon
        val numPairs = (cols * rows) / 2

        // Sprawdzenie, czy mamy wystarczająco dużo ikon w liście 'icons'
        if (numPairs > icons.size) {
            throw IllegalArgumentException("Za mało ikon w liście 'icons' dla planszy ${rows}x${cols}. Potrzeba $numPairs unikalnych ikon.")
        }

        val selectedIcons = icons.subList(0, numPairs)

        // 2. Tworzymy pary ikon i je mieszamy
        val shuffledIcons: MutableList<Int> = (selectedIcons + selectedIcons).toMutableList()
        shuffledIcons.shuffle()

        // 3. Pętla generująca przyciski (przeniesiona i zmodyfikowana z Lab03Activity)
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                // Tworzymy ImageButton używając kontekstu z GridLayout
                val btn = ImageButton(gridLayout.context).also {
                    // Ustawiamy tag formatu "0x1", "1x2" itd.
                    it.tag = "${row}x${col}"

                    val layoutParams = GridLayout.LayoutParams()
                    layoutParams.width = 0
                    layoutParams.height = 0
                    layoutParams.setGravity(Gravity.CENTER)

                    // Definicja wag (weight=1f), aby przyciski wypełniały ekran
                    layoutParams.columnSpec = GridLayout.spec(col, 1, 1f)
                    layoutParams.rowSpec = GridLayout.spec(row, 1, 1f)
                    it.layoutParams = layoutParams

                    // Pobieramy pierwszą ikonę z przemieszanej listy i usuwamy ją
                    val icon = shuffledIcons.removeAt(0)

                    // Dodajemy przycisk do logiki Tile i do widoku
                    addTile(it, icon)
                    gridLayout.addView(it)
                }
            }
        }
    }

    // Funkcja obsługująca kliknięcie w kartę
    private fun onClickTile(v: View) {
        // Pobieramy obiekt Tile na podstawie tagu klikniętego przycisku
        val tile = tiles[v.tag.toString()]

        // Ignorujemy kliknięcie, jeśli karta nie istnieje lub jest już odkryta
        if (tile == null || tile.revealed) return

        // Dodajemy kartę do stosu aktualnej pary
        matchedPair.push(tile)

        // Przesyłamy identyfikator ikony do logiki gry, aby sprawdzić stan
        val matchResult = logic.process { tile.tileResource }

        // Wywołujemy listener, informując o nowym wydarzeniu w grze
        onGameChangeStateListener(MemoryGameEvent(matchedPair.toList(), matchResult))

        // Jeśli zakończyliśmy turę (Match, NoMatch, Finished), czyścimy stos pary
        if (matchResult != GameStates.Matching) {
            matchedPair.clear()
        }
    }

    fun getState(): IntArray {
        val state = IntArray(rows * cols) { -1 }
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val tile = tiles["${row}x${col}"]
                if (tile != null && tile.revealed) {
                    state[row * cols + col] = tile.tileResource
                }
            }
        }
        return state
    }

    fun setState(state: IntArray) {
        val matchedResources = state.filter { it != -1 }.toSet()
        val matchCount = state.count { it != -1 } / 2

        val unmatchedIcons = icons.filter { it !in matchedResources }
        val shuffledUnmatched: MutableList<Int> = (unmatchedIcons + unmatchedIcons).toMutableList()
        shuffledUnmatched.shuffle()

        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val tile = tiles["${row}x${col}"] ?: continue
                val savedResource = state[row * cols + col]
                if (savedResource != -1) {
                    tile.tileResource = savedResource
                    tile.revealed = true
                } else {
                    tile.tileResource = shuffledUnmatched.removeAt(0)
                    tile.revealed = false
                }
            }
        }
        logic.setMatches(matchCount)
    }

    // Publiczna funkcja do ustawiania listenera z poziomu Aktywności
    fun setOnGameChangeListener(listener: (event: MemoryGameEvent) -> Unit) {
        onGameChangeStateListener = listener
    }

    // Funkcja pomocnicza: tworzy obiekt Tile, ustawia ClickListener i dodaje do mapy
    private fun addTile(button: ImageButton, resourceImage: Int) {
        button.setOnClickListener(::onClickTile)
        val tile = Tile(button, resourceImage, deckResource)
        // Kluczem w mapie jest tag przycisku (String)
        tiles[button.tag.toString()] = tile
    }
}