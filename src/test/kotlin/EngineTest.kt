import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class EngineTest {
    @Test
    fun `blinds posted correctly`() {
        val deckCards = listOf(
            Card(Suit.CLUBS, Rank.ACE), Card(Suit.CLUBS, Rank.KING),
            Card(Suit.CLUBS, Rank.QUEEN), Card(Suit.CLUBS, Rank.JACK),
            Card(Suit.CLUBS, Rank.TEN), Card(Suit.CLUBS, Rank.NINE),
            Card(Suit.DIAMONDS, Rank.TWO), Card(Suit.DIAMONDS, Rank.THREE), Card(Suit.DIAMONDS, Rank.FOUR),
            Card(Suit.DIAMONDS, Rank.FIVE), Card(Suit.DIAMONDS, Rank.SIX)
        )

        val players = listOf(Player("A", 1000u), Player("B", 1000u), Player("C", 1000u))
        val engine = TexasHoldemEngine(ScriptedUI(emptyList()), players, 5u, 10u)
        engine.deckOverride = TestDeck(deckCards)
        engine.newHand()

        val sb = players[1]
        val bb = players[2]
        assertEquals(5u, sb.bet)
        assertEquals(995u, sb.stack)
        assertEquals(10u, bb.bet)
        assertEquals(990u, bb.stack)
    }

    @Test
    fun `fold ends hand with one winner`() {
        val players = listOf(Player("A", 1000u), Player("B", 1000u))
        val actions = listOf(Player.Action.Fold(0))
        val ui = ScriptedUI(actions)
        val engine = TexasHoldemEngine(ui, players, 5u, 10u)

        val deckCards = listOf(
            Card(Suit.HEARTS, Rank.ACE), Card(Suit.HEARTS, Rank.KING),
            Card(Suit.HEARTS, Rank.QUEEN), Card(Suit.HEARTS, Rank.JACK),
            Card(Suit.HEARTS, Rank.TEN), Card(Suit.HEARTS, Rank.NINE),
            Card(Suit.HEARTS, Rank.EIGHT), Card(Suit.HEARTS, Rank.SEVEN),
            Card(Suit.HEARTS, Rank.SIX), Card(Suit.HEARTS, Rank.FIVE)
        )

        engine.deckOverride = TestDeck(deckCards)
        engine.newHand()

        while (!engine.isHandOver) {
            val action = ui.requestAction(engine.actingPlayerIndex)
            engine.handleAction(action)
        }

        assertEquals(10u, players[1].stack - 1000u)
        assertTrue(ui.messages.any { it.contains("A folds") })
        assertTrue(ui.winners.isNotEmpty())
        assertTrue(ui.winners.any { it.first.name == "B" })
    }

    @Test
    fun `all call to showdown and pot distributed`() {
        val players = listOf(Player("A", 1000u), Player("B", 1000u), Player("C", 1000u))

        val actions = listOf(
            Player.Action.Call(0),
            Player.Action.Call(1),
            Player.Action.Check(2),
            Player.Action.Check(1),
            Player.Action.Check(2),
            Player.Action.Check(0),
            Player.Action.Check(1),
            Player.Action.Check(2),
            Player.Action.Check(0),
            Player.Action.Check(1),
            Player.Action.Check(2),
            Player.Action.Check(0)
        )

        val ui = ScriptedUI(actions)
        val engine = TexasHoldemEngine(ui, players, 5u, 10u)

        val deckCards = listOf(
            Card(Suit.CLUBS, Rank.ACE), Card(Suit.CLUBS, Rank.KING),
            Card(Suit.CLUBS, Rank.QUEEN), Card(Suit.CLUBS, Rank.JACK),
            Card(Suit.CLUBS, Rank.TEN), Card(Suit.CLUBS, Rank.NINE),
            Card(Suit.DIAMONDS, Rank.TWO), Card(Suit.DIAMONDS, Rank.THREE), Card(Suit.DIAMONDS, Rank.FOUR),
            Card(Suit.DIAMONDS, Rank.FIVE), Card(Suit.DIAMONDS, Rank.SIX)
        )

        engine.deckOverride = TestDeck(deckCards)
        engine.newHand()

        while (!engine.isHandOver) {
            val action = ui.requestAction(engine.actingPlayerIndex)
            engine.handleAction(action)
        }

        val totalStacks = players.sumOf { it.stack.toLong() }
        assertEquals(3000u, totalStacks.toUInt())
        assertTrue(ui.winners.isNotEmpty())
    }
}
