fun main() {
    val ui = TUI()

    val engine = TexasHoldemEngine(
        ui = ui,
        playerNames = listOf("Alice", "Bob", "John"),
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
