import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class SystemTest {
    @Test
    fun `full game with 2 players`() {
        val players = listOf(Player("A", 1000u), Player("B", 1000u))

        val actions = listOf(
            Player.Action.Call(0),
            Player.Action.Call(1),
            Player.Action.Check(1),
            Player.Action.Check(0),
            Player.Action.Check(1),
            Player.Action.Check(0),
            Player.Action.Check(1),
            Player.Action.Check(0)
        )

        val ui = ScriptedUI(actions)
        val engine = TexasHoldemEngine(ui, players, 5u, 10u)

        val deckCards = listOf(
            Card(Suit.HEARTS, Rank.ACE), Card(Suit.HEARTS, Rank.KING),
            Card(Suit.HEARTS, Rank.QUEEN), Card(Suit.HEARTS, Rank.JACK),
            Card(Suit.CLUBS, Rank.TWO), Card(Suit.CLUBS, Rank.THREE), Card(Suit.CLUBS, Rank.FOUR),
            Card(Suit.CLUBS, Rank.FIVE), Card(Suit.CLUBS, Rank.SIX)
        )

        engine.deckOverride = TestDeck(deckCards)
        engine.newHand()

        while (!engine.isHandOver) {
            val idx = engine.actingPlayerIndex

            if (players[idx].isAllIn) {
                continue
            }

            val action = ui.requestAction(idx)
            engine.handleAction(action)
        }

        assertTrue(ui.winners.any { it.first.name == "A" })
        val totalStack = players.sumOf { it.stack.toLong() }
        assertEquals(2000u, totalStack.toUInt(), "Total chips should remain 2000")
    }
}
