package com.example.foolgame.game

enum class Suit(val symbol: String, val color: String) {
    SPADES("\u2660", "black"),
    HEARTS("\u2665", "red"),
    CLUBS("\u2663", "black"),
    DIAMONDS("\u2666", "red");

    companion object {
        fun fromName(name: String): Suit = when (name) {
            "spades" -> SPADES
            "hearts" -> HEARTS
            "clubs" -> CLUBS
            "diamonds" -> DIAMONDS
            else -> throw IllegalArgumentException("Unknown suit: $name")
        }
    }
}

enum class Rank(val value: Int, val display: String) {
    SIX(6, "6"),
    SEVEN(7, "7"),
    EIGHT(8, "8"),
    NINE(9, "9"),
    TEN(10, "10"),
    JACK(11, "J"),
    QUEEN(12, "Q"),
    KING(13, "K"),
    ACE(14, "A");

    companion object {
        fun fromValue(v: Int): Rank = entries.first { it.value == v }
    }
}

data class Card(val suit: Suit, val rank: Rank) : Comparable<Card> {
    val imageName: String get() = "card_${suit.name.lowercase()}_${rank.display.lowercase()}.png"

    override fun compareTo(other: Card): Int = rank.value - other.rank.value

    override fun toString(): String = "${rank.display}${suit.symbol}"
}
