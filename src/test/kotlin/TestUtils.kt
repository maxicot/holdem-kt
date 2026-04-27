import kotlin.random.Random

class TestDeck(cards: List<Card>) : Deck(Random.Default) {
    private val iterator = cards.iterator()

    override fun draw(): Card = iterator.next()
    override fun draw(n: Int): List<Card> = List(n) { draw() }
    override fun shuffle() {}
}

class ScriptedUI(actions: List<Player.Action>) : UserInterface {
    private val actions: MutableList<Player.Action> = actions.toMutableList()
    val messages = mutableListOf<String>()
    val winners = mutableListOf<Pair<Player, UInt>>()

    override fun requestAction(player: Int): Player.Action {
        require(actions.isNotEmpty()) {
            "No more scripted actions"
        }

        return actions.removeAt(0)
    }

    override fun getPlayers(): List<Player> = emptyList()

    override fun onPotUpdate(newPot: UInt) {
        messages.add("Pot: $newPot")
    }

    override fun onBoardUpdate(cards: List<Card>) {
        if (cards.isNotEmpty()) {
            messages.add("Board: ${cards.joinToString(" ")}")
        }
    }

    override fun onPhaseUpdate(newPhase: GamePhase) {
        messages.add("--- $newPhase ---")
    }

    override fun onBetUpdate(amount: UInt) {
        messages.add("To call: $amount")
    }

    override fun onActingPlayerUpdate(player: Player) {
        messages.add("Action on ${player.name} (${player.stack})")
    }

    override fun onPlayerAction(player: Player, action: Player.Action) {
        val s = when (action) {
            is Player.Action.Fold -> "folds"
            is Player.Action.Call -> "calls"
            is Player.Action.Raise -> "raises to ${action.amount}"
            is Player.Action.Check -> "checks"
        }

        messages.add("${player.name} $s")
    }
    override fun onPlayerStackUpdate(player: Player) {
        messages.add("${player.name} stack: ${player.stack}")
    }
    override fun onHandStart(button: Int, smallBlind: UInt, bigBlind: UInt) {
        messages.add("=== NEW HAND ===")
        messages.add("Button $button Blinds $smallBlind/$bigBlind")
    }
    override fun onHandEnd() {
        messages.add("--- HAND OVER ---")
    }

    override fun notifyWinner(player: Player, amount: UInt) {
        winners.add(player to amount)
        messages.add("${player.name} wins $amount!")
    }

    override fun onMessage(message: String) {
        messages.add(message)
    }
}
