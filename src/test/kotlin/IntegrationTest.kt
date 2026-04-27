import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class IntegrationTest {
    @Test
    fun `side pot with short all-in`() {
        val players = listOf(
            Player("A", 10u),
            Player("B", 1000u),
            Player("C", 100u)
        )

        val actions = mutableListOf(
            Player.Action.Call(0),
            Player.Action.Call(1),
            Player.Action.Call(2),

            Player.Action.Check(1),
            Player.Action.Check(2),

            Player.Action.Check(1),
            Player.Action.Check(2),

            Player.Action.Check(1),
            Player.Action.Check(2)
        )

        val ui = ScriptedUI(actions)
        val engine = TexasHoldemEngine(ui, players, 5u, 10u)

        val deckCards = listOf(
            Card(Suit.CLUBS, Rank.ACE),
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.HEARTS, Rank.TWO),
            Card(Suit.HEARTS, Rank.SEVEN),
            Card(Suit.DIAMONDS, Rank.THREE),
            Card(Suit.DIAMONDS, Rank.EIGHT),
            Card(Suit.CLUBS, Rank.FOUR),
            Card(Suit.SPADES, Rank.NINE),
            Card(Suit.SPADES, Rank.TEN),
            Card(Suit.DIAMONDS, Rank.JACK),
            Card(Suit.CLUBS, Rank.QUEEN)
        )

        engine.deckOverride = TestDeck(deckCards)
        engine.newHand()

        while (!engine.isHandOver) {
            val acting = engine.actingPlayerIndex
            val action = ui.requestAction(acting)
            engine.handleAction(action)
        }

        val a = players[0]
        val b = players[1]
        val c = players[2]
        assertEquals(30u, a.stack)
        assertEquals(1110u, a.stack + b.stack + c.stack)
    }
}
