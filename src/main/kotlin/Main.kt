fun main(args: Array<String>) {
    if ("--help" in args || "-h" in args) {
        println("use flag --gui to run in GUI mode and --tui for TUI mode")
        println("use --stats to display game statistics")
        return
    }

    val useGui = when {
        "--gui" in args -> true
        "--tui" in args -> false
        else -> {
            println("use flag --gui to run in GUI mode and --tui for TUI mode")
            System.console() == null
        }
    }

    PokerDatabase.connect()

    if ("--stats" in args) {
        val stats = PlayerStatistics.summary()

        if (useGui) {
            javax.swing.SwingUtilities.invokeLater {
                javax.swing.JOptionPane.showMessageDialog(
                    null,
                    stats,
                    "Game Statistics",
                    javax.swing.JOptionPane.INFORMATION_MESSAGE
                )
            }
        } else {
            println(stats)
        }

        return
    }

    val ui = if (useGui) {
        GUI()
    } else {
        TUI()
    }

    val engine = TexasHoldemEngine(
        ui = ui,
        initialPlayers = ui.getPlayers(),
        handRecorder = SqliteHandRecorder()
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
