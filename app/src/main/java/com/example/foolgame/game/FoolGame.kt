package com.example.foolgame.game

/**
 * Who is the attacker in the current exchange.
 * After a successful defense, roles switch.
 */
enum class AttackRole { PLAYER_ATTACKER, AI_ATTACKER }

enum class GamePhase {
    ATTACK_TURN,   // Attacker places a card (first attack or throw)
    DEFENSE_TURN,  // Defender beats the undefended card
    GAME_OVER
}

enum class GameResult { PLAYER_WINS, PLAYER_LOSES }

/**
 * "Дурак подкидной" — full rules.
 *
 * - First attack card: any card.
 * - After a successful defense the attacker can throw additional cards
 *   that match the rank of any card on the table (attacks AND defenses).
 * - Max 6 attack cards per exchange.
 * - Beaten cards are discarded (out of play).
 * - If defender beats everything: roles switch (defender → attacker).
 * - If defender takes cards: same attacker attacks again.
 */
class FoolGame {
    val deck = Deck()
    var playerHand = mutableListOf<Card>()
        private set
    var aiHand = mutableListOf<Card>()
        private set
    var tableCards = mutableListOf<Pair<Card, Card?>>()
        private set

    var attacker = AttackRole.PLAYER_ATTACKER
        private set
    var phase = GamePhase.ATTACK_TURN
        private set
    var result: GameResult? = null
        private set
    var selectedCard: Card? = null
        private set

    // First attack of an exchange — player can play any card.
    // False after the first card is played; subsequent plays are throws.
    var isFirstAttack = true
        private set

    val trump: Suit get() = deck.trump
    val isGameOver: Boolean get() = phase == GamePhase.GAME_OVER

    /** True when the player's current action is "throwing" (not first attack). */
    val isThrowingPhase: Boolean
        get() = phase == GamePhase.ATTACK_TURN && attacker == AttackRole.PLAYER_ATTACKER && !isFirstAttack

    fun startGame() {
        deck.reset()
        playerHand.clear()
        aiHand.clear()
        tableCards.clear()
        deck.drawMultiple(6).let { playerHand.addAll(it) }
        deck.drawMultiple(6).let { aiHand.addAll(it) }
        attacker = AttackRole.PLAYER_ATTACKER
        phase = GamePhase.ATTACK_TURN
        result = null
        selectedCard = null
        isFirstAttack = true
    }

    // --- Card Selection ---

    fun selectCard(card: Card) {
        if (!playerHand.contains(card)) return
        when (phase) {
            GamePhase.ATTACK_TURN -> {
                if (attacker == AttackRole.PLAYER_ATTACKER) selectedCard = card
            }
            GamePhase.DEFENSE_TURN -> {
                if (attacker == AttackRole.AI_ATTACKER) selectedCard = card
            }
            else -> {}
        }
    }

    fun clearSelection() { selectedCard = null }

    // --- Player Actions ---

    /** Player (as attacker) plays the first card or throws. */
    private fun playerAttack(card: Card): Boolean {
        if (phase != GamePhase.ATTACK_TURN || attacker != AttackRole.PLAYER_ATTACKER) return false
        if (!playerHand.contains(card)) return false

        // Throwing requires matching a rank on the table
        if (!isFirstAttack) {
            val ranksOnTable = mutableSetOf<Rank>()
            for ((a, d) in tableCards) { ranksOnTable.add(a.rank); d?.let { ranksOnTable.add(it.rank) } }
            if (card.rank !in ranksOnTable) return false
            if (tableCards.size >= 6) return false
        }

        playerHand.remove(card)
        tableCards.add(card to null)
        isFirstAttack = false
        phase = GamePhase.DEFENSE_TURN
        return true
    }

    /** Convenience: player plays the selected card as attack/throw. */
    fun playerPlaySelected(): Boolean {
        val card = selectedCard ?: return false
        val ok = playerAttack(card)
        if (ok) selectedCard = null
        return ok
    }

    /** Player (as attacker) passes — stops throwing, roles switch. */
    fun playerPassThrow() {
        if (phase != GamePhase.ATTACK_TURN || attacker != AttackRole.PLAYER_ATTACKER) return
        if (isFirstAttack) return  // must play at least one card
        finishExchange()
    }

    /** Player (as defender) beats the undefended card. */
    fun playerDefend(card: Card): Boolean {
        if (phase != GamePhase.DEFENSE_TURN || attacker != AttackRole.AI_ATTACKER) return false
        if (!playerHand.contains(card)) return false

        val last = tableCards.lastOrNull { it.second == null }?.first ?: return false
        if (!canBeat(card, last)) return false

        playerHand.remove(card)
        val idx = tableCards.indexOfLast { it.second == null }
        tableCards[idx] = tableCards[idx].first to card
        selectedCard = null

        onSuccessfulDefense()
        return true
    }

    /** Player (as defender) takes all table cards. */
    fun playerTakeCards() {
        if (phase != GamePhase.DEFENSE_TURN || attacker != AttackRole.AI_ATTACKER) return
        for ((a, d) in tableCards) {
            playerHand.add(a); d?.let { playerHand.add(it) }
        }
        tableCards.clear()
        selectedCard = null
        drawCards()
        checkGameOver()
        if (phase != GamePhase.GAME_OVER) {
            isFirstAttack = true
            phase = GamePhase.ATTACK_TURN   // same attacker
        }
    }

    // --- AI Actions ---

    fun executeAiTurn(): AiAction {
        if (phase == GamePhase.GAME_OVER) return AiAction.NOTHING

        return when {
            phase == GamePhase.ATTACK_TURN && attacker == AttackRole.AI_ATTACKER -> {
                if (isFirstAttack) aiFirstAttack() else aiThrow()
            }
            phase == GamePhase.DEFENSE_TURN && attacker == AttackRole.PLAYER_ATTACKER -> {
                aiDefend()
            }
            else -> AiAction.NOTHING
        }
    }

    private fun aiFirstAttack(): AiAction {
        if (aiHand.isEmpty()) { checkGameOver(); return AiAction.NOTHING }
        tableCards.clear()
        val card = aiHand.minBy { it.rank.value }
        aiHand.remove(card)
        tableCards.add(card to null)
        isFirstAttack = false
        phase = GamePhase.DEFENSE_TURN
        return AiAction.ATTACKED(card)
    }

    private fun aiThrow(): AiAction {
        if (aiHand.isEmpty()) { checkGameOver(); return AiAction.NOTHING }

        val ranksOnTable = mutableSetOf<Rank>()
        for ((a, d) in tableCards) { ranksOnTable.add(a.rank); d?.let { ranksOnTable.add(it.rank) } }

        // AI throws the lowest matching card; stops if none or table full
        val throwable = aiHand.filter { it.rank in ranksOnTable }.sortedBy { it.rank.value }
        if (throwable.isEmpty() || tableCards.size >= 6) {
            finishExchange()
            return AiAction.THROW_PASS
        }

        val card = throwable.first()
        aiHand.remove(card)
        tableCards.add(card to null)
        phase = GamePhase.DEFENSE_TURN
        return AiAction.THROWN(card)
    }

    private fun aiDefend(): AiAction {
        val last = tableCards.lastOrNull { it.second == null }?.first ?: return AiAction.NOTHING
        val beaters = aiHand.filter { canBeat(it, last) }.sortedBy { it.rank.value }

        if (beaters.isNotEmpty()) {
            val card = beaters.first()
            aiHand.remove(card)
            val idx = tableCards.indexOfLast { it.second == null }
            tableCards[idx] = tableCards[idx].first to card
            onSuccessfulDefense()
            return AiAction.DEFENDED(card)
        }

        // AI takes cards
        for ((a, d) in tableCards) { aiHand.add(a); d?.let { aiHand.add(it) } }
        tableCards.clear()
        drawCards()
        checkGameOver()
        if (phase != GamePhase.GAME_OVER) {
            isFirstAttack = true
            phase = GamePhase.ATTACK_TURN   // same attacker (player)
        }
        return AiAction.TOOK_CARDS
    }

    // --- Exchange lifecycle ---

    /** Called after every successful defense (all cards beaten). */
    private fun onSuccessfulDefense() {
        val ranksOnTable = mutableSetOf<Rank>()
        for ((a, d) in tableCards) { ranksOnTable.add(a.rank); d?.let { ranksOnTable.add(it.rank) } }

        val hasThrow = if (attacker == AttackRole.PLAYER_ATTACKER)
            playerHand.any { it.rank in ranksOnTable } && tableCards.size < 6
        else
            aiHand.any { it.rank in ranksOnTable } && tableCards.size < 6

        if (hasThrow) {
            phase = GamePhase.ATTACK_TURN   // same attacker can throw
        } else {
            finishExchange()
        }
    }

    /** Exchange done: discard table, draw, switch roles. */
    private fun finishExchange() {
        tableCards.clear()
        drawCards()
        checkGameOver()
        if (phase != GamePhase.GAME_OVER) {
            attacker = if (attacker == AttackRole.PLAYER_ATTACKER) AttackRole.AI_ATTACKER else AttackRole.PLAYER_ATTACKER
            isFirstAttack = true
            phase = GamePhase.ATTACK_TURN
        }
    }

    // --- Helpers ---

    fun canBeat(card: Card, target: Card): Boolean {
        if (card.suit == target.suit) return card.rank.value > target.rank.value
        return card.suit == trump && target.suit != trump
    }

    fun isPlayable(card: Card): Boolean = when (phase) {
        GamePhase.ATTACK_TURN -> {
            if (attacker != AttackRole.PLAYER_ATTACKER) false
            else if (isFirstAttack) true
            else {
                val ranks = mutableSetOf<Rank>()
                for ((a, d) in tableCards) { ranks.add(a.rank); d?.let { ranks.add(it.rank) } }
                card.rank in ranks && tableCards.size < 6
            }
        }
        GamePhase.DEFENSE_TURN -> {
            if (attacker != AttackRole.AI_ATTACKER) false
            else tableCards.lastOrNull { it.second == null }?.first?.let { canBeat(card, it) } ?: false
        }
        else -> false
    }

    /** Whether the player (attacker) can throw any card right now. */
    fun playerCanThrow(): Boolean {
        if (phase != GamePhase.ATTACK_TURN || attacker != AttackRole.PLAYER_ATTACKER) return false
        if (isFirstAttack) return true  // must play first
        if (tableCards.size >= 6) return false
        val ranks = mutableSetOf<Rank>()
        for ((a, d) in tableCards) { ranks.add(a.rank); d?.let { ranks.add(it.rank) } }
        return playerHand.any { it.rank in ranks }
    }

    private fun drawCards() {
        playerHand.addAll(deck.drawMultiple(maxOf(0, 6 - playerHand.size)))
        aiHand.addAll(deck.drawMultiple(maxOf(0, 6 - aiHand.size)))
    }

    /** Deck empty → whoever emptied their hand first wins. */
    private fun checkGameOver() {
        if (!deck.isEmpty) return
        when {
            playerHand.isEmpty() && aiHand.isEmpty() -> { result = GameResult.PLAYER_WINS; phase = GamePhase.GAME_OVER }
            playerHand.isEmpty()                      -> { result = GameResult.PLAYER_WINS; phase = GamePhase.GAME_OVER }
            aiHand.isEmpty()                          -> { result = GameResult.PLAYER_LOSES; phase = GamePhase.GAME_OVER }
        }
    }
}

sealed class AiAction {
    data class ATTACKED(val card: Card) : AiAction()
    data class DEFENDED(val card: Card) : AiAction()
    data class THROWN(val card: Card) : AiAction()
    object THROW_PASS : AiAction()
    object TOOK_CARDS : AiAction()
    object NOTHING : AiAction()
}
