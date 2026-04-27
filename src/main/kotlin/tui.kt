class TUI : UserInterface {
    override fun getPlayers(): List<Player> {
        val players = mutableListOf<Player>()
        println("Enter player data (name and, optionally, stack - 1000 if omitted);\nEmpty line when done.")

        while (true) {
            print("> ");
            val input = readln().trim()

            if (input.isEmpty()) {
                if (players.size >= 2) {
                    break
                } else {
                    println("At least 2 players required.")
                    continue
                }
            }

            val parts = input.split(" ", limit = 2)
            val name = parts[0]

            if (name.isEmpty()) {
                println("Name cannot be blank.")
                continue
            }

            if (players.any { it.name == name }) {
                println("Name already taken. Try again.")
                continue
            }

            val stack: UInt = if (parts.size == 2) {
                val parsed = parts[1].toUIntOrNull()

                if (parsed == null) {
                    println("The amount must be a positive integer.")
                    continue
                } else {
                    parsed
                }
            } else {
                1000u
            }

            players.add(Player(name, stack))
        }

        return players
    }

    override fun requestAction(player: Int): Player.Action {
        while (true) {
            val input = readln().trim().lowercase()

            if (input.isEmpty()) {
                continue
            }

            val parts = input.split("\\s+".toRegex())
            val command = parts[0]

            when (command) {
                "exit" -> {
                    print("Are you sure you want to exit the game? (yes/no): ")

                    when (readln().lowercase().firstOrNull()) {
                        'y' -> System.exit(0)
                        else -> continue
                    }
                }
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
                    onMessage("Unknown command. Try: fold, call, check, raise <amount>, exit")
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

    override fun onPlayerStackUpdate(player: Player) {
        println("New stack of ${player.name}: ${player.stack}")
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
