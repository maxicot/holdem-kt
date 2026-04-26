//package org.holdem.engine
/*
import org.holdem.engine.cards.*
import org.holdem.engine.table.*
import org.holdem.engine.handeval.*
import org.holdem.interface.UserInterface
*/
import kotlin.random.Random

enum class GamePhase {
    PREFLOP,
    FLOP,
    TURN,
    RIVER,
    SHOWDOWN
}

class TexasHoldemEngine(
    protected val ui: UserInterface,
    protected val playerNames: List<String>,
    protected val smallBlind: UInt = 5u,
    protected val bigBlind: UInt = smallBlind * 2u,
    protected val startingStack: UInt = bigBlind * 100u,
    protected val random: Random = Random.Default
) {
    protected val table: Table = Table(
        players = playerNames.map { Player(it, startingStack) }.toMutableList(),
        button = 0,
        smallBlind = smallBlind,
        bigBlind = bigBlind,
        ui = ui,
        deck = Deck(random)
    )

    protected val playersThatActed = mutableSetOf<Int>()

    val actingPlayerIndex: Int get() = this.table.actingPlayer
    val isHandOver: Boolean get() = this.table.phase == GamePhase.SHOWDOWN

    protected val isRoundComplete: Boolean get() {
        val activePlayers = this.table.players.filter { !it.folded && !it.isAllIn }

        if (activePlayers.isEmpty()) {
            return true
        }

        return activePlayers.all {
            (it.bet == this.table.currentBet) && this.playersThatActed.contains(this.table.players.indexOf(it))
        }
    }

    fun newHand() {
        this.table.pot = 0u
        this.table.board = emptyList()
        this.table.phase = GamePhase.PREFLOP
        this.table.deck = Deck(random)
        this.table.deck.shuffle()
        this.table.actingPlayer = (this.table.button + 3) % this.table.players.size

        this.table.players.forEach {
            it.hole = this.table.deck.draw(2)
            it.bet = 0u
            it.contribution = 0u
            it.folded = false
        }

        this.playersThatActed.clear()
        this.postBlinds()
        ui.onHandStart(this.table.button, this.table.smallBlind, this.table.bigBlind)
    }

    protected fun postBlinds() {
        val sbIndex = (this.table.button + 1) % this.table.players.size
        val sb = this.table.players[sbIndex]
        val sbAmount = minOf(this.table.smallBlind, sb.stack)
        sb.stack -= sbAmount
        sb.contribution += sbAmount
        sb.bet = sbAmount
        ui.onPlayerAction(sb, Player.Action.Raise(sbIndex, this.table.smallBlind))

        val bbIndex = (this.table.button + 2) % this.table.players.size
        val bb = this.table.players[bbIndex]
        val bbAmount = minOf(this.table.bigBlind, bb.stack)
        bb.stack -= bbAmount
        bb.contribution += bbAmount
        bb.bet = bbAmount
        ui.onPlayerAction(bb, Player.Action.Raise(bbIndex, this.table.bigBlind))

        this.table.currentBet = this.table.bigBlind
        this.table.pot = sb.bet + bb.bet
    }

    fun handleAction(action: Player.Action) {
        require(this.table.actingPlayer == action.index) {
            "Not your turn"
        }

        val player = this.table.players[action.index]

        when (action) {
            is Player.Action.Fold -> {
                player.folded = true
                ui.onPlayerAction(player, action)
                this.advanceTurn()
            }
            is Player.Action.Call -> {
                val amount = this.table.currentBet - player.bet

                require(player.stack >= amount) {
                    "Insufficient chips"
                }

                player.stack -= amount
                player.contribution += amount
                player.bet += amount
                this.table.pot += amount
                ui.onPlayerAction(player, action)
                this.advanceTurn()
            }
            is Player.Action.Raise -> {
                require(action.amount > this.table.currentBet) {
                    "Raise amount must be greater than the current bet"
                }

                val amount = action.amount - player.bet

                require(player.stack >= amount) {
                    "Insufficient chips"
                }

                player.stack -= amount
                player.contribution += amount
                player.bet = action.amount
                this.table.pot += amount
                this.table.currentBet = action.amount
                this.playersThatActed.clear()
                this.playersThatActed.add(action.index)
                ui.onPlayerAction(player, action)
                this.advanceTurn()
            }
            is Player.Action.Check -> {
                require(player.bet == this.table.currentBet) {
                    "Can't check with a bet too low"
                }

                ui.onPlayerAction(player, action)
                this.advanceTurn()
            }
        }
    }

    protected fun advanceTurn() {
        this.playersThatActed.add(this.table.actingPlayer)
        var next = (this.table.actingPlayer + 1) % this.table.players.size

        while (this.table.players[next].folded || this.table.players[next].isAllIn) {
            next = (next + 1) % this.table.players.size

            if (next == this.table.actingPlayer) {
                break
            }
        }

        if (this.isRoundComplete) {
            this.advancePhase()
        } else {
            this.table.actingPlayer = next
        }
    }

    protected fun advancePhase() {
        this.table.players.forEach { it.bet = 0u }
        this.table.currentBet = 0u
        this.playersThatActed.clear()

        when (this.table.phase) {
            GamePhase.PREFLOP -> {
                this.table.board += this.table.deck.draw(3)
                this.table.phase = GamePhase.FLOP
                this.table.actingPlayer = (this.table.button + 1) % this.table.players.size
            }
            GamePhase.FLOP -> {
                this.table.board += this.table.deck.draw(1)
                this.table.phase = GamePhase.TURN
                this.table.actingPlayer = (this.table.button + 1) % this.table.players.size
            }
            GamePhase.TURN -> {
                this.table.board += this.table.deck.draw(1)
                this.table.phase = GamePhase.RIVER
                this.table.actingPlayer = (this.table.button + 1) % this.table.players.size
            }
            GamePhase.RIVER -> {
                this.table.button = (this.table.button + 1) % this.table.players.size
                this.table.phase = GamePhase.SHOWDOWN
                val activePlayers = this.table.players.filter { !it.folded }

                if (activePlayers.isEmpty()) {
                    return
                }

                val hands = activePlayers.associateWith {
                    bestHand(it.hole, this.table.board)
                }

                val pots = this.dividePot(activePlayers)

                for ((eligiblePlayers, amount) in pots) {
                    val bestHand = eligiblePlayers.maxOf { hands[it]!! }
                    val winners = eligiblePlayers.filter { hands[it] == bestHand }
                    val share = amount / winners.size.toUInt()

                    for (winner in winners) {
                        winner.stack += share
                        ui.notifyWinner(winner, share)
                    }
                }

                ui.onHandEnd()
            }
            GamePhase.SHOWDOWN -> error("advancePhase called during SHOWDOWN")
        }
    }

    protected fun dividePot(activePlayers: List<Player>): List<Pair<List<Player>, UInt>> {
        val contributors = activePlayers.sortedBy { it.contribution }
        val sidePots = mutableListOf<Pair<List<Player>, UInt>>()
        var previousAmount = 0u

        for (player in contributors) {
            val additional = player.contribution - previousAmount

            if (additional > 0u) {
                val eligible = activePlayers.filter { it.contribution >= player.contribution }
                val potSize = additional * eligible.size.toUInt()

                if (eligible.size > 1) {
                    sidePots.add(Pair(eligible, potSize))
                } else {
                    eligible.first().stack += potSize
                    ui.notifyWinner(player, potSize)
                }
            }

            previousAmount = player.contribution
        }

        this.table.pot = 0u
        return sidePots
    }
}
