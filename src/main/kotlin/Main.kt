fun main(args: Array<String>) {
    val useGui = when {
        "--gui" in args -> true
        "--tui" in args -> false
        else -> {
            println("use flag --gui to run in GUI mode and --tui for TUI mode")
            System.console() == null
        }
    }

    val ui = if (useGui) {
        GUI()
    } else {
        TUI()
    }

    val engine = TexasHoldemEngine(
        ui = ui,
        initialPlayers = ui.getPlayers()
    )

    ui.onMessage("Welcome to Texas Hold'em!")
    engine.newHand()

    while (true) {
        if (engine.isHandOver) {
            engine.newHand()
            continue
        }

        val action = ui.requestAction(engine.actingPlayerIndex)

        try {
            engine.handleAction(action)
        } catch (e: Exception) {
            ui.onMessage("Error: ${e.message}")
        }
    }
}
