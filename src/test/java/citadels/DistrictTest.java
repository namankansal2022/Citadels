package citadels;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DistrictTest {

    /** Constructor and getters should store and return the correct values. */
    @Test
    void testGetters() {
        District d = new District("Cathedral", "BLUE", 5, "A grand church");
        assertEquals("Cathedral", d.getName(), "getName should return the name");
        // color stored lower-case
        assertEquals("blue", d.getColor(), "getColor should return lower-case color");
        assertEquals(5, d.getCost(), "getCost should return the cost");
        assertEquals("A grand church", d.getDescription(), "getDescription should return the description");
    }

    /** Non-purple districts should return false for isUnique(). */
    @Test
    void testIsUniqueFalse() {
        District market = new District("Market", "green", 2, "");
        assertFalse(market.isUnique(), "A green district is not unique");
    }

    /** Purple districts should return true for isUnique(). */
    @Test
    void testIsUniqueTrue() {
        District keep = new District("Keep", "purple", 3, "");
        assertTrue(keep.isUnique(), "A purple district is unique");
    }

    /** Non-unique districts always have zero end-game bonus. */
    @Test
    void testGetPointBonusNonUnique() {
        District inn = new District("Inn", "red", 1, "");
        assertEquals(0, inn.getPointBonus(), "Non-unique districts give no bonus");
    }

    /** Unique districts other than University or Dragon Gate give zero bonus. */
    @Test
    void testGetPointBonusUniqueNoBonus() {
        District museum = new District("Museum", "purple", 4, "");
        assertTrue(museum.isUnique());
        assertEquals(0, museum.getPointBonus(), "Purple districts without special name give no bonus");
    }

    /** "University" should grant +2 bonus at scoring time. */
    @Test
    void testGetPointBonusUniversity() {
        District uni = new District("University", "purple", 6, "Gain extra card each turn");
        assertTrue(uni.isUnique());
        assertEquals(2, uni.getPointBonus(), "University grants a +2 bonus");
    }

    /** "Dragon Gate" should grant +2 bonus at scoring time. */
    @Test
    void testGetPointBonusDragonGate() {
        District dragonGate = new District("Dragon Gate", "purple", 5, "Score Dragon Gate bonus");
        assertTrue(dragonGate.isUnique());
        assertEquals(2, dragonGate.getPointBonus(), "Dragon Gate grants a +2 bonus");
    }

    /** displayShort should format as "Name [colorcost]". */
    @Test
    void testDisplayShort() {
        District watchtower = new District("Watchtower", "red", 1, "");
        String out = watchtower.displayShort();
        assertTrue(out.contains("Watchtower"), "displayShort must contain the name");
        assertTrue(out.contains("[red1]"), "displayShort must contain [colorcost]");
    }

    /** displayLong(true) should show points for city: "points: cost". */
    @Test
    void testDisplayLongForCity() {
        District tavern = new District("Tavern", "green", 1, "");
        assertEquals("Tavern (green), points: 1", tavern.displayLong(true));
    }

    /** displayLong(false) should show cost: "cost: cost". */
    @Test
    void testDisplayLongNotForCity() {
        District tavern = new District("Tavern", "green", 1, "");
        assertEquals("Tavern (green), cost: 1", tavern.displayLong(false));
    }
}