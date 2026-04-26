import kotlin.properties.Delegates

data class Player(
    val name: String,
    var stack: UInt,
    var bet: UInt = 0u,
    var hole: List<Card> = emptyList(),
    var folded: Boolean = false,
) {
    var contribution: UInt = 0u
    val isAllIn: Boolean get() = this.stack == 0u

    sealed class Action {
        abstract val index: Int

        data class Fold(override val index: Int) : Action()
        data class Call(override val index: Int) : Action()
        data class Raise(override val index: Int, val amount: UInt) : Action()
        data class Check(override val index: Int) : Action()
    }
}

data class Table(
    val players: List<Player>,
    var button: Int,
    val smallBlind: UInt,
    val bigBlind: UInt,
    val ui: UserInterface,
    var deck: Deck
) {
    var pot: UInt by Delegates.observable(0u) { _, old, new ->
        if (old != new) {
            ui.onPotUpdate(new)
        }
    }

    var board: List<Card> by Delegates.observable(emptyList()) { _, _, new ->
        ui.onBoardUpdate(new)
    }

    var phase: GamePhase by Delegates.observable(GamePhase.PREFLOP) { _, old, new ->
        if (old != new) {
            ui.onPhaseUpdate(new)
        }
    }

    var currentBet: UInt by Delegates.observable(this.bigBlind) { _, _, new ->
        ui.onBetUpdate(new)
    }

    var actingPlayer: Int by Delegates.observable(0) { _, _, new ->
        ui.onActingPlayerUpdate(players[new])
    }
}
