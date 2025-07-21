package citadels;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class PlayerTest {

    private Player human;
    private Player cpu;
    private District redDistr;
    private District blueDistr;
    private District greenDistr;
    private District yellowDistr;
    private District purpleDistr;
    private District university;  // for extra bonus

    @BeforeEach
    void setUp() {
        human = new Player(1, true);
        cpu   = new Player(2, false);

        redDistr    = new District("Barracks", "red",    3, "");
        blueDistr   = new District("Temple",   "blue",   1, "");
        greenDistr  = new District("Market",   "green",  2, "");
        yellowDistr = new District("Manor",    "yellow", 3, "");
        purpleDistr = new District("Keep",     "purple", 3, "");
        university  = new District("University", "purple", 6, "Extra card each turn");
    }

    /** Constructor and getters should initialize state correctly. */
    @Test
    void testInitialState() {
        assertEquals(1, human.getId());
        assertTrue(human.isHuman());
        assertEquals(0, human.getGold());
        assertTrue(human.getHand().isEmpty());
        assertTrue(human.getCity().isEmpty());
        assertEquals(0, human.getCharacter());

        assertEquals(2, cpu.getId());
        assertFalse(cpu.isHuman());
    }

    /** setCharacter and getCharacter should round-trip. */
    @Test
    void testSetGetCharacter() {
        human.setCharacter(7);
        assertEquals(7, human.getCharacter());
        cpu.setCharacter(3);
        assertEquals(3, cpu.getCharacter());
    }

    /** addGold should increase gold, spendGold should work only up to current gold. */
    @Test
    void testGoldOperations() {
        human.addGold(5);
        assertEquals(5, human.getGold());

        // spend within limit
        assertTrue(human.spendGold(3));
        assertEquals(2, human.getGold());

        // spend exact remainder
        assertTrue(human.spendGold(2));
        assertEquals(0, human.getGold());

        // cannot overspend
        assertFalse(human.spendGold(1));
        assertEquals(0, human.getGold());
    }

    /** addCardToHand should add to hand, and toString's cards= count should reflect it. */
    @Test
    void testAddCardToHandAndToStringCards() {
        human.addCardToHand(redDistr);
        human.addCardToHand(blueDistr);
        assertEquals(2, human.getHand().size());
        String repr = human.toString();
        assertTrue(repr.contains("cards=2"), "toString must show cards=2");
    }

    /** hasBuilt should detect a name in city (case-insensitive) and false when not present. */
    @Test
    void testHasBuilt() {
        assertFalse(human.hasBuilt("Keep"), "empty city → hasBuilt is false");
        human.getCity().add(purpleDistr);
        assertTrue(human.hasBuilt("keep"), "hasBuilt should be case-insensitive");
    }

    /** calculateScore: no districts → zero. */
    @Test
    void testCalculateScoreEmpty() {
        assertEquals(0, human.calculateScore(false, false));
    }

    /** calculateScore: sum of costs, no diversity, no completion, no purple bonus. */
    @Test
    void testCalculateScoreBasic() {
        human.getCity().add(redDistr);    // cost 3
        human.getCity().add(blueDistr);   // cost 1
        assertEquals(4, human.calculateScore(false, false));
    }

    /** calculateScore: diversity bonus only (5 colors present). */
    @Test
    void testCalculateScoreDiversity() {
        human.getCity().addAll(Arrays.asList(
                redDistr, blueDistr, greenDistr, yellowDistr, purpleDistr
        ));
        // costs = 3+1+2+3+3 = 12, +3 diversity = 15
        assertEquals(15, human.calculateScore(false, false));
    }

    /** calculateScore: completion bonus when firstCompleter true/false. */
    @Test
    void testCalculateScoreCompletionBonus() {
        // 8 * cost(3) = 24
        for (int i = 0; i < 8; i++) {
            human.getCity().add(redDistr);
        }
        assertEquals(24 + 4, human.calculateScore(true, true));
        assertEquals(24 + 2, human.calculateScore(false, true));
    }

    /** calculateScore: purple end-game point bonus applies. */
    @Test
    void testCalculateScorePurpleBonus() {
        human.getCity().add(university);  // cost 6, bonus 2
        assertEquals(8, human.calculateScore(false, false));
    }

    /** toString on empty city for CPU (no “(you)”). */
    @Test
    void testToStringEmptyCityCpu() {
        String repr = cpu.toString();
        assertTrue(repr.startsWith("Player 2"), "starts with Player id");
        assertFalse(repr.contains("(you)"), "non-human must not show (you)");
        assertTrue(repr.contains("cards=0"), "cards=0");
        assertTrue(repr.contains("gold=0"), "gold=0");
        // trailing space only after “city=”
        assertTrue(repr.endsWith("city="), "ends with empty city");
    }

    /** toString on human with non-empty city should include “(you)” and the built district names. */
    @Test
    void testToStringNonEmptyCityHuman() {
        human.addCardToHand(redDistr);
        human.addCardToHand(blueDistr);
        human.getCity().addAll(Arrays.asList(redDistr, purpleDistr));
        String repr = human.toString();
        assertTrue(repr.contains("(you)"), "human must show (you)");
        // cityStr is displayShort of each city district
        assertTrue(repr.contains("Barracks [red3]"), "should show Barracks in city");
        assertTrue(repr.contains("Keep [purple3]"), "should show Keep in city");
    }
}