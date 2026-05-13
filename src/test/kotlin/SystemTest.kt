import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.io.File
import java.sql.*

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
            Card(Suit.HEARTS, Rank.ACE),
            Card(Suit.HEARTS, Rank.KING),
            Card(Suit.HEARTS, Rank.QUEEN),
            Card(Suit.HEARTS, Rank.JACK),
            Card(Suit.CLUBS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
            Card(Suit.CLUBS, Rank.FIVE),
            Card(Suit.CLUBS, Rank.SIX)
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

    @Test
    fun `full game recorded in database`() {
        val dbFile = File.createTempFile("test", ".db")
        val dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"
        PokerDatabase.connect(dbUrl)

        val recorder = SqliteHandRecorder()
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
        val engine = TexasHoldemEngine(ui, players, 5u, 10u, handRecorder = recorder)

        val deckCards = listOf(
            Card(Suit.SPADES, Rank.ACE),
            Card(Suit.SPADES, Rank.KING),
            Card(Suit.HEARTS, Rank.QUEEN),
            Card(Suit.HEARTS, Rank.JACK),
            Card(Suit.CLUBS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE),
            Card(Suit.CLUBS, Rank.FOUR),
            Card(Suit.CLUBS, Rank.FIVE),
            Card(Suit.DIAMONDS, Rank.SEVEN)
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

        val winnerName = ui.winners.firstOrNull()?.first?.name
        assertNotNull(winnerName)

        PokerDatabase.usingConnection { conn ->
            val gameCount = conn.createStatement().executeQuery("SELECT COUNT(*) AS cnt FROM game").use { rs ->
                rs.next()
                rs.getInt(1)
            }

            assertEquals(1, gameCount)

            val playersFromDb = mutableListOf<Pair<String, Int>>()

            conn.createStatement().executeQuery("SELECT player_name, ending_stack FROM game_player").use { rs ->
                while (rs.next()) {
                    playersFromDb.add(Pair(rs.getString("player_name"), rs.getInt("ending_stack")))
                }
            }

            assertEquals(2, playersFromDb.size)
            assertEquals(2000, playersFromDb.sumOf { it.second })

            val winnerRow = playersFromDb.find { it.first == winnerName }!!
            assertTrue(winnerRow.second > 1000)
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
