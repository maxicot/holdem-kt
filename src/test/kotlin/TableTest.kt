import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class TableTest {
    @Test
    fun `isAllIn true when stack zero`() {
        val p = Player("Test", 100u)
        p.stack = 0u
        assertTrue(p.isAllIn)
    }

    @Test
    fun `isAllIn false when stack positive`() {
        val p = Player("Test", 1u)
        assertFalse(p.isAllIn)
    }

    @Test
    fun `action fold has correct index`() {
        val f = Player.Action.Fold(3)
        assertEquals(3, f.index)
    }

    @Test
    fun `action raise holds amount`() {
        val r = Player.Action.Raise(2, 500u)
        assertEquals(500u, r.amount)
    }

    @Test
    fun `contribution starts at zero`() {
        val p = Player("X", 100u)
        assertEquals(0u, p.contribution)
    }
}
