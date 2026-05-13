import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.*

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

    @Test
    fun `hand result recorded in database`() {
        val dbFile = File.createTempFile("test", ".db")
        val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"
        PokerDatabase.connect(dbUrl)

        val recorder = SqliteHandRecorder()
        val players = listOf(Player("A", 1000u), Player("B", 1000u))
        val actions = listOf(Player.Action.Fold(0))
        val ui = ScriptedUI(actions)
        val engine = TexasHoldemEngine(ui, players, 5u, 10u, handRecorder = recorder)

        val deckCards = listOf(
            Card(Suit.HEARTS, Rank.ACE),
            Card(Suit.HEARTS, Rank.KING),
            Card(Suit.HEARTS, Rank.QUEEN),
            Card(Suit.HEARTS, Rank.JACK),
            Card(Suit.HEARTS, Rank.TEN),
            Card(Suit.HEARTS, Rank.NINE),
            Card(Suit.HEARTS, Rank.EIGHT),
            Card(Suit.HEARTS, Rank.SEVEN),
            Card(Suit.HEARTS, Rank.SIX),
            Card(Suit.HEARTS, Rank.FIVE)
        )

        engine.deckOverride = TestDeck(deckCards)
        engine.newHand()

        while (!engine.isHandOver) {
            val action = ui.requestAction(engine.actingPlayerIndex)
            engine.handleAction(action)
        }

        PokerDatabase.usingConnection { conn ->
            val games = conn.createStatement().executeQuery("SELECT COUNT(*) AS cnt FROM game")
            assertTrue(games.next())
            assertEquals(1, games.getInt("cnt"))

            val playersFromDb = mutableListOf<Map<String, Any>>()

            conn.createStatement().executeQuery("SELECT player_name, ending_stack, folded FROM game_player").use { rs ->
                while (rs.next()) {
                    playersFromDb.add(
                        mapOf(
                            Pair("name", rs.getString("player_name")),
                            Pair("ending_stack", rs.getInt("ending_stack")),
                            Pair("folded", rs.getBoolean("folded"))
                        )
                    )
                }
            }
            assertEquals(2, playersFromDb.size)

            val winnerRow = playersFromDb.find { it["name"] == "B" }!!
            val loserRow = playersFromDb.find { it["name"] == "A" }!!

            assertEquals(1010, winnerRow["ending_stack"])
            assertEquals(990, loserRow["ending_stack"])
            assertTrue(loserRow["folded"] as Boolean)
            assertFalse(winnerRow["folded"] as Boolean)
        }

        PokerDatabase.usingConnection { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DROP TABLE IF EXISTS game_player")
                stmt.execute("DROP TABLE IF EXISTS game")
            }
        }

        dbFile.delete()
    }
}
