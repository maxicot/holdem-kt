import java.sql.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Connection manager for an SQLite-based database */
object PokerDatabase {
    private var dbUrl: String? = null

    fun connect(url: String = "jdbc:sqlite:holdem-kt_history.db", driver: String = "org.sqlite.JDBC") {
        dbUrl = url

        try {
            Class.forName(driver)
        } catch (e: ClassNotFoundException) {
            throw RuntimeException("SQLite driver not found", e)
        }

        usingConnection { conn ->
            conn.createStatement().use { stmt ->
                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS game (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        timestamp INTEGER NOT NULL,
                        button INTEGER NOT NULL
                    );
                    """.trimIndent()
                )

                stmt.execute(
                    """
                    CREATE TABLE IF NOT EXISTS game_player (
                        id INTEGER PRIMARY KEY AUTOINCREMENT,
                        game_id INTEGER NOT NULL,
                        player_name VARCHAR(50) NOT NULL,
                        starting_stack INTEGER NOT NULL,
                        ending_stack INTEGER NOT NULL,
                        profit INTEGER NOT NULL,
                        folded INTEGER NOT NULL
                    );
                    """.trimIndent()
                )
            }
        }
    }

    /** Execute a statement using a temporary connection */
    fun <T> usingConnection(block: (Connection) -> T): T {
        val url = dbUrl ?: throw IllegalStateException("Database not connected. Call connect() first.")
        val conn = DriverManager.getConnection(url)

        try {
            return block(conn)
        } finally {
            conn.close()
        }
    }
}

interface HandRecorder {
    fun recordHand(players: List<Player>, button: Int)
}

class SqliteHandRecorder : HandRecorder {
    override fun recordHand(players: List<Player>, button: Int) {
        PokerDatabase.usingConnection { conn ->
            conn.autoCommit = false

            try {
                val gameId = conn.prepareStatement(
                    "INSERT INTO game (timestamp, button) VALUES (?, ?)"
                ).use { ps ->
                    ps.setLong(1, System.currentTimeMillis())
                    ps.setInt(2, button)
                    ps.executeUpdate()

                    conn.createStatement().use { stmt ->
                        val rs = stmt.executeQuery("SELECT last_insert_rowid()")
                        rs.next()
                        rs.getInt(1)
                    }
                }

                conn.prepareStatement(
                    """
                    INSERT INTO game_player (game_id, player_name, starting_stack, ending_stack, profit, folded)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """.trimIndent()
                ).use { ps ->
                    for (player in players) {
                        ps.setInt(1, gameId)
                        ps.setString(2, player.name)
                        ps.setInt(3, player.startingStack.toInt())
                        ps.setInt(4, player.stack.toInt())
                        ps.setInt(5, player.stack.toInt() - player.startingStack.toInt())
                        ps.setInt(6, if (player.folded) 1 else 0)
                        ps.addBatch()
                    }

                    ps.executeBatch()
                }

                conn.commit()
            } catch (e: SQLException) {
                conn.rollback()
                throw e
            } finally {
                conn.autoCommit = true
            }
        }
    }
}

object PlayerStatistics {
    /** Retrieve hand and player statistics */
    fun summary(): String {
        val sb = StringBuilder()

        PokerDatabase.usingConnection { conn ->
            val gameRs = conn.createStatement().executeQuery("SELECT id, timestamp, button FROM game ORDER BY id")
            var gameCount = 0

            while (gameRs.next()) {
                if (gameCount > 0) {
                    sb.append("\n\n")
                }

                val gameId = gameRs.getInt("id")
                val timestamp = gameRs.getLong("timestamp")
                val button = gameRs.getInt("button")
                val dateStr = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
                    .format(Date(timestamp))
                sb.append("Game $gameId ($dateStr) Button: seat $button\n")

                conn.prepareStatement(
                    "SELECT player_name, starting_stack, ending_stack, profit, folded FROM game_player WHERE game_id = ?"
                ).use { ps ->
                    ps.setInt(1, gameId)
                    val playerRs = ps.executeQuery()
                    var first = true

                    while (playerRs.next()) {
                        if (!first) {
                            sb.append("\n")
                        }

                        val name = playerRs.getString("player_name")
                        val start = playerRs.getInt("starting_stack")
                        val end = playerRs.getInt("ending_stack")
                        val profit = playerRs.getInt("profit")
                        val folded = playerRs.getInt("folded") == 1
                        val profitSign = if (profit >= 0) "+" else ""
                        val foldedStr = if (folded) " FOLDED" else ""
                        sb.append("  $name: start $start, end $end ($profitSign$profit)$foldedStr")
                        first = false
                    }
                }

                gameCount++
            }
        }

        return if (sb.isEmpty()) {
            "No games recorded yet"
        } else {
            sb.toString()
        }
    }
}
