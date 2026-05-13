import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class HandEvalTest {
    @Test
    fun `high card beats lower high card`() {
        val h1 = HandRank.HighCard(listOf(14, 13, 12, 11, 9))
        val h2 = HandRank.HighCard(listOf(14, 13, 12, 11, 8))
        assertTrue(h1 > h2)
    }

    @Test
    fun `pair beats high card`() {
        val h1 = HandRank.OnePair(5, listOf(14, 13, 12))
        val h2 = HandRank.HighCard(listOf(14, 13, 12, 11, 9))
        assertTrue(h1 > h2)
    }

    @Test
    fun `two pair ranking order`() {
        val h1 = HandRank.TwoPair(10, 5, 8)
        val h2 = HandRank.TwoPair(10, 4, 12)
        assertTrue(h1 > h2)
    }

    @Test
    fun `straight flush beats four of a kind`() {
        val sf = HandRank.StraightFlush(8)
        val fk = HandRank.FourOfAKind(14, 2)
        assertTrue(sf > fk)
    }

    @Test
    fun `eval5 detects straight flush`() {
        val cards = listOf(
            Card(Suit.HEARTS, Rank.TEN),
            Card(Suit.HEARTS, Rank.JACK),
            Card(Suit.HEARTS, Rank.QUEEN),
            Card(Suit.HEARTS, Rank.KING),
            Card(Suit.HEARTS, Rank.ACE)
        )

        assertEquals(HandRank.StraightFlush(14), eval5(cards))
    }

    @Test
    fun `eval5 detects wheel straight`() {
        val cards = listOf(
            Card(Suit.CLUBS, Rank.ACE),
            Card(Suit.DIAMONDS, Rank.TWO),
            Card(Suit.HEARTS, Rank.THREE),
            Card(Suit.SPADES, Rank.FOUR),
            Card(Suit.HEARTS, Rank.FIVE)
        )

        assertTrue(eval5(cards) is HandRank.Straight)
        assertEquals(5, (eval5(cards) as HandRank.Straight).highCard)
    }

    @Test
    fun `bestHand with known community cards`() {
        val hole = listOf(
            Card(Suit.SPADES, Rank.ACE),
            Card(Suit.SPADES, Rank.KING)
        )

        val community = listOf(
            Card(Suit.SPADES, Rank.QUEEN),
            Card(Suit.SPADES, Rank.JACK),
            Card(Suit.SPADES, Rank.TEN),
            Card(Suit.HEARTS, Rank.TWO),
            Card(Suit.CLUBS, Rank.THREE)
        )

        val rank = bestHand(hole, community)
        assertTrue(rank is HandRank.StraightFlush)
        assertEquals(14, (rank as HandRank.StraightFlush).highCard)
    }

    @Test
    fun `combinations produces 21 for 7 choose 5`() {
        val list = (1..7).toList()
        assertEquals(21, list.combinations(5).size)
    }
}
