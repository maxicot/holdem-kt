interface UserInterface {
    fun requestAction(player: Int): Player.Action
    fun onPotUpdate(newPot: UInt)
    fun onBoardUpdate(cards: List<Card>)
    fun onPhaseUpdate(newPhase: GamePhase)
    fun onBetUpdate(amount: UInt)
    fun onActingPlayerUpdate(player: Player)
    fun onPlayerAction(player: Player, action: Player.Action)
    fun onPlayerStackUpdate(player: Player, newStack: UInt)
    fun onHandStart(button: Int, smallBlind: UInt, bigBlind: UInt)
    fun onHandEnd()
    fun notifyWinner(player: Player, amount: UInt)
    fun onMessage(message: String)
}
