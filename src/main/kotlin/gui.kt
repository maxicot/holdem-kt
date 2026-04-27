import java.awt.*
import java.awt.event.*
import javax.swing.*
import java.util.concurrent.CompletableFuture

class GUI : UserInterface {
    private lateinit var frame: JFrame
    private lateinit var logArea: JTextArea
    private lateinit var boardLabel: JLabel
    private lateinit var potLabel: JLabel
    private lateinit var betLabel: JLabel
    private lateinit var playersInfoPanel: JPanel
    private lateinit var holeCardsLabel: JLabel
    private lateinit var statusLabel: JLabel
    private lateinit var foldButton: JButton
    private lateinit var callButton: JButton
    private lateinit var checkButton: JButton
    private lateinit var raiseButton: JButton

    private var actionFuture: CompletableFuture<Player.Action>? = null
    private var currentPlayerIndex: Int = -1
    private var players: List<Player> = emptyList()
    private var currentBet: UInt = 0u

    init {
        SwingUtilities.invokeAndWait { buildUI() }
    }

    private fun buildUI() {
        this.frame = JFrame("Texas Hold'em")
        this.frame.defaultCloseOperation = JFrame.EXIT_ON_CLOSE
        this.frame.layout = BorderLayout(5, 5)

        this.logArea = JTextArea(8, 60)
        this.logArea.isEditable = false
        this.logArea.font = Font("Monospaced", Font.PLAIN, 13)
        val logScroll = JScrollPane(logArea)
        this.frame.add(logScroll, BorderLayout.NORTH)

        val tablePanel = JPanel(BorderLayout(10, 10))

        this.boardLabel = JLabel(" ", SwingConstants.CENTER)
        this.boardLabel.font = Font("Monospaced", Font.BOLD, 20)
        this.boardLabel.border = BorderFactory.createTitledBorder("Board")
        tablePanel.add(boardLabel, BorderLayout.CENTER)

        val infoPanel = JPanel(GridLayout(1, 2, 10, 0))

        this.potLabel = JLabel("Pot: 0", SwingConstants.CENTER)
        this.potLabel.font = Font("Monospaced", Font.PLAIN, 14)
        infoPanel.add(potLabel)

        this.betLabel = JLabel("Bet: 0", SwingConstants.CENTER)
        this.betLabel.font = Font("Monospaced", Font.PLAIN, 14)
        infoPanel.add(betLabel)

        tablePanel.add(infoPanel, BorderLayout.SOUTH)

        this.playersInfoPanel = JPanel()
        this.playersInfoPanel.layout = BoxLayout(playersInfoPanel, BoxLayout.Y_AXIS)
        this.playersInfoPanel.border = BorderFactory.createTitledBorder("Players")
        val playersScroll = JScrollPane(playersInfoPanel)
        playersScroll.preferredSize = Dimension(200, 0)
        tablePanel.add(playersScroll, BorderLayout.WEST)

        this.frame.add(tablePanel, BorderLayout.CENTER)

        val lowerPanel = JPanel()
        lowerPanel.layout = BoxLayout(lowerPanel, BoxLayout.Y_AXIS)

        this.holeCardsLabel = JLabel("Your cards: ", SwingConstants.CENTER)
        this.holeCardsLabel.font = Font("Monospaced", Font.BOLD, 18)
        this.holeCardsLabel.alignmentX = Component.CENTER_ALIGNMENT
        lowerPanel.add(holeCardsLabel)

        val buttonPanel = JPanel(FlowLayout(FlowLayout.CENTER, 10, 5))
        this.foldButton = JButton("Fold")
        this.callButton = JButton("Call")
        this.checkButton = JButton("Check")
        this.raiseButton = JButton("Raise")

        this.foldButton.addActionListener {
            this.submitAction(Player.Action.Fold(this.currentPlayerIndex))
        }

        this.callButton.addActionListener {
            val player = players[currentPlayerIndex]
            val toCall = currentBet - player.bet

            if (player.stack < toCall) {
                statusLabel.text = "Not enough chips to call"
                return@addActionListener
            }

            this.submitAction(Player.Action.Call(this.currentPlayerIndex))
        }

        this.checkButton.addActionListener {
            val player = players[this.currentPlayerIndex]

            if (player.bet != currentBet) {
                statusLabel.text = "Cannot check – bet is ${player.bet}, current bet is $currentBet"
                return@addActionListener
            }

            this.submitAction(Player.Action.Check(this.currentPlayerIndex))
        }

        this.raiseButton.addActionListener {
            val player = players[this.currentPlayerIndex]
            val amountStr = JOptionPane.showInputDialog(frame, "Raise to:", "Raise", JOptionPane.QUESTION_MESSAGE)

            if (amountStr == null) {
                return@addActionListener
            }

            val amount = amountStr.toUIntOrNull()

            if (amount == null) {
                statusLabel.text = "Invalid raise amount"
                return@addActionListener
            }

            if (amount <= this.currentBet) {
                statusLabel.text = "Raise must be greater than current bet ($currentBet)"
                return@addActionListener
            }

            val needed = amount - player.bet

            if (player.stack < needed) {
                statusLabel.text = "Not enough chips to raise to $amount"
                return@addActionListener
            }

            this.submitAction(Player.Action.Raise(this.currentPlayerIndex, amount))
        }

        buttonPanel.add(foldButton)
        buttonPanel.add(callButton)
        buttonPanel.add(checkButton)
        buttonPanel.add(raiseButton)
        lowerPanel.add(buttonPanel)

        this.statusLabel = JLabel(" ", SwingConstants.CENTER)
        this.statusLabel.font = Font("SansSerif", Font.PLAIN, 14)
        lowerPanel.add(statusLabel)

        this.frame.add(lowerPanel, BorderLayout.SOUTH)

        this.frame.pack()
        this.frame.setLocationRelativeTo(null)
        this.frame.isVisible = true
    }

    override fun requestAction(player: Int): Player.Action {
        this.currentPlayerIndex = player

        SwingUtilities.invokeLater {
            this.statusLabel.text = "Your turn, ${this.players[player].name}"
            this.holeCardsLabel.text = "Your cards: ${this.players[player].hole.joinToString(" ")}"
            this.setButtonsEnabled(true)
        }

        val future = CompletableFuture<Player.Action>()
        this.actionFuture = future

        return future.get()
    }

    private fun submitAction(action: Player.Action) {
        this.actionFuture?.complete(action)

        SwingUtilities.invokeLater {
            this.setButtonsEnabled(false)
            this.holeCardsLabel.text = " "
            this.statusLabel.text = "Waiting..."
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        this.foldButton.isEnabled = enabled
        this.callButton.isEnabled = enabled
        this.checkButton.isEnabled = enabled
        this.raiseButton.isEnabled = enabled
    }

    override fun getPlayers(): List<Player> {
        while (true) {
            val panel = JPanel()
            panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
            val rows = mutableListOf<PlayerRow>()

            fun addRow() {
                val row = PlayerRow()
                rows.add(row)
                panel.add(row.panel)
                panel.revalidate()
                panel.repaint()
            }

            val addButton = JButton("Add Player")
            addButton.addActionListener { addRow() }

            addRow()
            addRow()

            val controlPanel = JPanel(FlowLayout(FlowLayout.LEFT))
            controlPanel.add(addButton)
            panel.add(controlPanel)

            val scrollPane = JScrollPane(panel)
            scrollPane.preferredSize = Dimension(350, 250)

            val result = JOptionPane.showOptionDialog(
                frame,
                scrollPane,
                "Enter Players",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
                null
            )

            if (result != JOptionPane.OK_OPTION) {
                continue
            }

            val newPlayers = rows.mapNotNull { row ->
                val name = row.nameField.text.trim()

                if (name.isEmpty()) {
                    return@mapNotNull null
                }

                val stackText = row.stackField.text.trim()

                val stack = if (stackText.isEmpty()) {
                    1000u
                } else {
                    stackText.toUIntOrNull()
                } ?: 1000u

                Player(name, stack)
            }

            if (newPlayers.size < 2) {
                JOptionPane.showMessageDialog(frame, "At least 2 players are required.", "Error", JOptionPane.ERROR_MESSAGE)
                continue
            }

            this.players = newPlayers
            this.refreshPlayerList()

            return newPlayers
        }
    }

    private class PlayerRow {
        val panel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 2))
        val nameField = JTextField(10)
        val stackField = JTextField("1000", 6)

        init {
            panel.add(JLabel("Name:"))
            panel.add(nameField)
            panel.add(JLabel("Stack:"))
            panel.add(stackField)
        }
    }

    private fun refreshPlayerList() {
        this.playersInfoPanel.removeAll()

        for (p in this.players) {
            val label = JLabel("${p.name} (${p.stack})")
            label.font = Font("SansSerif", Font.PLAIN, 14)
            this.playersInfoPanel.add(label)
        }

        this.playersInfoPanel.revalidate()
        this.playersInfoPanel.repaint()
    }

    private fun appendLog(text: String) {
        SwingUtilities.invokeLater {
            this.logArea.append(text + "\n")
            this.logArea.caretPosition = this.logArea.document.length
        }
    }

    override fun onPotUpdate(newPot: UInt) {
        SwingUtilities.invokeLater { this.potLabel.text = "Pot: $newPot" }
        this.appendLog("Pot: $newPot")
    }

    override fun onBoardUpdate(cards: List<Card>) {
        SwingUtilities.invokeLater {
            this.boardLabel.text = if (cards.isEmpty()) {
                " "
            } else {
                cards.joinToString("  ")
            }
        }

        if (cards.isNotEmpty()) {
            this.appendLog("Board: ${cards.joinToString(" ")}")
        }
    }

    override fun onPhaseUpdate(newPhase: GamePhase) {
        this.appendLog("\n--- $newPhase ---")
    }

    override fun onBetUpdate(amount: UInt) {
        this.currentBet = amount
        SwingUtilities.invokeLater { this.betLabel.text = "Bet: $amount" }
        this.appendLog("To call: $amount")
    }

    override fun onActingPlayerUpdate(player: Player) {
        this.appendLog("\nAction on ${player.name} (${player.stack})")
        this.refreshPlayerList()
    }

    override fun onPlayerAction(player: Player, action: Player.Action) {
        val actionStr = when (action) {
            is Player.Action.Fold -> "folds"
            is Player.Action.Call -> "calls"
            is Player.Action.Raise -> "raises to ${action.amount}"
            is Player.Action.Check -> "checks"
        }

        this.appendLog("${player.name} $actionStr")
        this.refreshPlayerList()
    }

    override fun onPlayerStackUpdate(player: Player) {
        this.appendLog("${player.name} stack: ${player.stack}")
        this.refreshPlayerList()
    }

    override fun onHandStart(button: Int, smallBlind: UInt, bigBlind: UInt) {
        this.appendLog("\n=== NEW HAND ===")
        this.appendLog("Button at seat $button   Blinds: $smallBlind / $bigBlind")
        this.refreshPlayerList()
    }

    override fun onHandEnd() {
        this.appendLog("\n--- HAND OVER ---")
    }

    override fun notifyWinner(player: Player, amount: UInt) {
        this.appendLog("${player.name} wins $amount!")
        this.refreshPlayerList()
    }

    override fun onMessage(message: String) {
        this.appendLog(message)
    }
}
