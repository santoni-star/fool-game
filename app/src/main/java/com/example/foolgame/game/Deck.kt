package com.example.foolgame.game

class Deck {
    private val cards = mutableListOf<Card>()
    var trump: Suit = Suit.SPADES
        private set

    init {
        reset()
    }

    fun reset() {
        cards.clear()
        for (suit in Suit.entries) {
            for (rank in Rank.entries) {
                cards.add(Card(suit, rank))
            }
        }
        cards.shuffle()
        trump = cards.last().suit
    }

    val isEmpty: Boolean get() = cards.isEmpty()
    val size: Int get() = cards.size
    val trumpCard: Card? get() = if (cards.isEmpty()) null else cards.last()

    fun draw(): Card? = if (cards.isEmpty()) null else cards.removeLast()

    fun drawMultiple(count: Int): List<Card> {
        val drawn = mutableListOf<Card>()
        for (i in 0 until count) {
            draw()?.let { drawn.add(it) } ?: break
        }
        return drawn
    }

    fun canDraw(count: Int): Int = minOf(count, cards.size)
}
