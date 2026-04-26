import kotlin.random.Random

enum class Suit {
    HEARTS,
    DIAMONDS,
    CLUBS,
    SPADES;

    fun toSymbol(): String = when (this) {
        HEARTS -> "♥"
        DIAMONDS -> "♦"
        CLUBS -> "♣"
        SPADES -> "♠"
    }
}

enum class Rank {
    TWO,
    THREE,
    FOUR,
    FIVE,
    SIX,
    SEVEN,
    EIGHT,
    NINE,
    TEN,
    JACK,
    QUEEN,
    KING,
    ACE;

    fun toNumber(): Int = when (this) {
        TWO -> 2
        THREE -> 3
        FOUR -> 4
        FIVE -> 5
        SIX -> 6
        SEVEN -> 7
        EIGHT -> 8
        NINE -> 9
        TEN -> 10
        JACK -> 11
        QUEEN -> 12
        KING -> 13
        ACE -> 14
    }

    fun toSymbol(): String = listOf(
        "2",
        "3",
        "4",
        "5",
        "6",
        "7",
        "8",
        "9",
        "T",
        "J",
        "Q",
        "K",
        "A"
    )[this.toNumber() - 2]
}

data class Card(
    val suit: Suit,
    val rank: Rank
) {
    override fun toString(): String = "${this.rank.toSymbol()}${this.suit.toSymbol()}"
}

class Deck(private val random: Random = Random.Default) {
    private val cards = Suit.values().flatMap { suit -> Rank.values().map { rank -> Card(suit, rank) } }.toMutableList()

    fun shuffle() = cards.shuffle(random)

    fun draw(): Card = cards.removeAt(cards.lastIndex)

    fun draw(n: Int): List<Card> = List(n) { this.draw() }
}
