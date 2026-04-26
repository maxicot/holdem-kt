class TUI : UserInterface {
    override fun requestAction(player: Int): Player.Action {
        while (true) {
            val input = readln().trim().lowercase()

            if (input.isEmpty()) {
                continue
            }

            val parts = input.split("\\s+".toRegex())
            val command = parts[0]

            when (command) {
                "fold" -> return Player.Action.Fold(player)
                "call" -> return Player.Action.Call(player)
                "check" -> return Player.Action.Check(player)
                "raise" -> {
                    if (parts.size < 2) {
                        onMessage("Usage: raise <amount>")
                        continue
                    }

                    val amount = parts[1].toUIntOrNull()

                    if (amount == null) {
                        onMessage("Raise amount must be a positive integer")
                        continue
                    }

                    return Player.Action.Raise(player, amount)
                }
                else -> {
                    onMessage("Unknown command. Try: fold, call, check, raise <amount>")
                    continue
                }
            }
        }
    }

    override fun onPotUpdate(newPot: UInt) {
        println("Pot: $newPot")
    }

    override fun onBoardUpdate(cards: List<Card>) {
        if (cards.isNotEmpty()) {
            println("Board: ${cards.joinToString(" ")}")
        }
    }

    override fun onPhaseUpdate(newPhase: GamePhase) {
        println("\n--- $newPhase ---")
    }

    override fun onBetUpdate(amount: UInt) {
        println("To call: $amount")
    }

    override fun onActingPlayerUpdate(player: Player) {
        println("\nAction on ${player.name} (stack: ${player.stack})")
    }

    override fun onPlayerAction(player: Player, action: Player.Action) {
        val actionStr = when (action) {
            is Player.Action.Fold -> "folds"
            is Player.Action.Call -> "calls"
            is Player.Action.Raise -> "raises to ${action.amount}"
            is Player.Action.Check -> "checks"
        }

        println("${player.name} $actionStr")
    }

    override fun onPlayerStackUpdate(player: Player, newStack: UInt) {
        println("${player.name} stack: $newStack")
    }

    override fun onHandStart(button: Int, smallBlind: UInt, bigBlind: UInt) {
        println("\n=== NEW HAND ===")
        println("Button at seat $button\nBlinds: $smallBlind and $bigBlind")
    }

    override fun onHandEnd() {
        println("\n--- HAND OVER ---")
    }

    override fun notifyWinner(player: Player, amount: UInt) {
        println("${player.name} won $amount!")
    }

    override fun onMessage(message: String) {
        println(message)
    }
}
