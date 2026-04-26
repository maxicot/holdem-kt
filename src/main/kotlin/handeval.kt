sealed class HandRank : Comparable<HandRank> {
    abstract val rankValue: Int
    abstract fun tieBreakerList(): List<Int>

    override fun compareTo(other: HandRank): Int {
        val cmp = rankValue - other.rankValue

        if (cmp != 0) {
            return cmp
        }

        val a = this.tieBreakerList()
        val b = other.tieBreakerList()

        for (i in 0 until minOf(a.size, b.size)) {
            val diff = a[i] - b[i]

            if (diff != 0) {
                return diff
            }
        }

        return a.size - b.size
    }

    data class HighCard(val kickers: List<Int>) : HandRank() {
        override val rankValue = 0
        override fun tieBreakerList() = kickers
    }

    data class OnePair(val pairRank: Int, val kickers: List<Int>) : HandRank() {
        override val rankValue = 1
        override fun tieBreakerList() = listOf(pairRank) + kickers
    }

    data class TwoPair(val highPair: Int, val lowPair: Int, val kicker: Int) : HandRank() {
        override val rankValue = 2
        override fun tieBreakerList() = listOf(highPair, lowPair, kicker)
    }

    data class ThreeOfAKind(val tripRank: Int, val kickers: List<Int>) : HandRank() {
        override val rankValue = 3
        override fun tieBreakerList() = listOf(tripRank) + kickers
    }

    data class Straight(val highCard: Int) : HandRank() {
        override val rankValue = 4
        override fun tieBreakerList() = listOf(highCard)
    }

    data class Flush(val kickers: List<Int>) : HandRank() {
        override val rankValue = 5
        override fun tieBreakerList() = kickers
    }

    data class FullHouse(val tripsRank: Int, val pairRank: Int) : HandRank() {
        override val rankValue = 6
        override fun tieBreakerList() = listOf(tripsRank, pairRank)
    }

    data class FourOfAKind(val quadRank: Int, val kicker: Int) : HandRank() {
        override val rankValue = 7
        override fun tieBreakerList() = listOf(quadRank, kicker)
    }

    data class StraightFlush(val highCard: Int) : HandRank() {
        override val rankValue = 8
        override fun tieBreakerList() = listOf(highCard)
    }
}

fun eval5(cards: List<Card>): HandRank {
    require(cards.size == 5) {
        "Must evaluate exactly 5 cards"
    }

    val sorted = cards.sortedByDescending { it.rank.toNumber() }
    val ranks = sorted.map { it.rank.toNumber() }
    val isFlush = sorted.all { it.suit == sorted.first().suit }
    val isWheel = ranks == listOf(14, 5, 4, 3, 2)
    val isStraight = isWheel || (
        ranks.zipWithNext().all { (a, b) -> a - b == 1 } && ranks.distinct().size == 5
    )

    if (isFlush && isStraight) {
        val high = if (isWheel) {
            5
        } else {
            ranks.first()
        }

        return HandRank.StraightFlush(high)
    }

    val freqMap = ranks.groupingBy { it }.eachCount()
    val groups = freqMap.entries
        .map { Pair(it.value, it.key) }
        .sortedWith(compareByDescending<Pair<Int, Int>> { it.first }.thenByDescending { it.second })
    val counts = groups.map { it.first }
    val values = groups.map { it.second }

    return when (counts) {
        listOf(4, 1) -> HandRank.FourOfAKind(values[0], values[1])
        listOf(3, 2) -> HandRank.FullHouse(values[0], values[1])
        listOf(3, 1, 1) -> HandRank.ThreeOfAKind(values[0], values.subList(1, 3).sortedDescending())
        listOf(2, 2, 1) -> {
            val pairs = values.subList(0, 2).sortedDescending()
            HandRank.TwoPair(pairs[0], pairs[1], values[2])
        }
        listOf(2, 1, 1, 1) -> HandRank.OnePair(values[0], values.subList(1, 4).sortedDescending())
        else -> {
            if (isFlush) {
                HandRank.Flush(ranks)
            } else if (isStraight) {
                val high = if (isWheel) {
                    5
                } else {
                    ranks.first()
                }

                HandRank.Straight(high)
            } else {
                HandRank.HighCard(ranks)
            }
        }
    }
}

fun bestHand(hole: List<Card>, community: List<Card>): HandRank {
    require(hole.size == 2 && community.size == 5) {
        "Must provide 2 hole and 5 board cards"
    }

    val hand = hole + community
    return hand.combinations(5).map { eval5(it) }.max()
}

fun<T> List<T>.combinations(k: Int): List<List<T>> {
    if (k == 0) {
        return listOf(emptyList())
    }

    if (k > size) {
        return emptyList()
    }

    if (k == size) {
        return listOf(this)
    }

    val result = mutableListOf<List<T>>()
    val indices = IntArray(k) { it }
    val n = size

    while (true) {
        result.add(List(k) { this[indices[it]] })
        var i = k - 1

        while (i >= 0 && indices[i] == i + n - k) {
            i--
        }

        if (i < 0) {
            break
        }

        indices[i]++

        for (j in i + 1 until k) {
            indices[j] = indices[j - 1] + 1
        }
    }

    return result
}
