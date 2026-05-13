import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.sql.*
import java.io.File

class DatabaseTest {
    private lateinit var dbFile: File
    private lateinit var dbUrl: String

    @BeforeEach
    fun setUp() {
        dbFile = File.createTempFile("test", ".db")
        dbUrl = "jdbc:sqlite:${dbFile.absolutePath}"
        PokerDatabase.connect(dbUrl)
    }

    @AfterEach
    fun tearDown() {
        PokerDatabase.usingConnection { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute("DROP TABLE IF EXISTS game_player")
                stmt.execute("DROP TABLE IF EXISTS game")
            }
        }

        dbFile.delete()
    }

    @Test
    fun `recordHand inserts game and player rows`() {
        val recorder = SqliteHandRecorder()

        val players = listOf(
            Player("a", 1000u).apply {
                startingStack = 1000u
                folded = false
                stack = 950u
            },
            Player("b", 1000u).apply {
                startingStack = 1000u
                folded = true
                stack = 1000u
            }
        )

        recorder.recordHand(players, button = 1)

        PokerDatabase.usingConnection { conn ->
            val gameRow = conn.createStatement().executeQuery("SELECT id, button, timestamp FROM game").let { rs ->
                assertTrue(rs.next(), "Expected at least one game row")
                Triple(rs.getInt("id"), rs.getInt("button"), rs.getLong("timestamp"))
            }

            assertTrue(gameRow.third > 0)
            assertEquals(1, gameRow.second)

            val playerRows = mutableListOf<Pair<String, Map<String, Any>>>()

            conn.createStatement().executeQuery(
                "SELECT player_name, starting_stack, ending_stack, profit, folded FROM game_player WHERE game_id = ${gameRow.first}"
            ).use { rs ->
                while (rs.next()) {
                    playerRows.add(
                        Pair(rs.getString("player_name"), mapOf(
                            Pair("starting_stack", rs.getInt("starting_stack")),
                            Pair("ending_stack", rs.getInt("ending_stack")),
                            Pair("profit", rs.getInt("profit")),
                            Pair("folded", rs.getBoolean("folded"))
                        ))
                    )
                }
            }

            assertEquals(2, playerRows.size)

            val a = playerRows.find { it.first == "a" }!!.second
            assertEquals(1000, a["starting_stack"])
            assertEquals(950, a["ending_stack"])
            assertEquals(-50, a["profit"])
            assertFalse(a["folded"] as Boolean)

            val b = playerRows.find { it.first == "b" }!!.second
            assertEquals(1000, b["starting_stack"])
            assertEquals(1000, b["ending_stack"])
            assertEquals(0, b["profit"])
            assertTrue(b["folded"] as Boolean)
        }
    }

    @Test
    fun `recordHand multiple games creates separate rows`() {
        val recorder = SqliteHandRecorder()
        recorder.recordHand(listOf(Player("a", 100u)), 0)
        recorder.recordHand(listOf(Player("b", 200u)), 1)

        PokerDatabase.usingConnection { conn ->
            val rs = conn.createStatement().executeQuery("SELECT COUNT(*) AS cnt FROM game")
            assertTrue(rs.next())
            assertEquals(2, rs.getInt("cnt"))

            val players = conn.createStatement().executeQuery("SELECT COUNT(*) AS cnt FROM game_player")
            assertTrue(players.next())
            assertEquals(2, players.getInt("cnt"))

            val games = conn.createStatement().executeQuery("SELECT button FROM game ORDER BY id")
            assertTrue(games.next())
            assertEquals(0, games.getInt("button"))
            assertTrue(games.next())
            assertEquals(1, games.getInt("button"))
        }
    }

    @Test
    fun `summary with no games returns placeholder`() {
        val stats = PlayerStatistics.summary()
        assertEquals("No games recorded yet", stats)
    }

    @Test
    fun `summary formats single game correctly`() {
        val recorder = SqliteHandRecorder()

        recorder.recordHand(
            listOf(
                Player("a", 1000u).apply {
                    startingStack = 1000u; stack = 950u; folded = false
                },
                Player("b", 1000u).apply {
                    startingStack = 1000u; stack = 1050u; folded = true
                }
            ),
            button = 0
        )

        val summary = PlayerStatistics.summary()
        val lines = summary.lines()

        assertTrue(lines[0].startsWith("Game 1 ("))
        assertTrue(lines[0].endsWith(") Button: seat 0"))
        assertTrue(lines.any { it.contains("a") && it.contains("start 1000") && it.contains("end 950") && it.contains("-50") })
        assertTrue(lines.any { it.contains("b") && it.contains("start 1000") && it.contains("end 1050") && it.contains("+50") && it.contains("FOLDED") })
    }

    @Test
    fun `summary orders games by id`() {
        val recorder = SqliteHandRecorder()

        repeat(3) { i ->
            recorder.recordHand(listOf(Player("P$i", 100u)), button = i)
        }

        val summary = PlayerStatistics.summary()
        assertTrue(summary.contains("Game 1"))
        assertTrue(summary.contains("Game 2"))
        assertTrue(summary.contains("Game 3"))

        val idx1 = summary.indexOf("Game 1")
        val idx2 = summary.indexOf("Game 2")
        val idx3 = summary.indexOf("Game 3")
        assertTrue(idx1 < idx2)
        assertTrue(idx2 < idx3)
    }
}
