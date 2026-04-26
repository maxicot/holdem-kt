//package org.holdem
/*
import kotlinx.coroutines.runBlocking
import org.holdem.engine.TexasHoldemEngine
import org.holdem.engine.table.Player
import org.holdem.interface.terminal.TUI
*/
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
