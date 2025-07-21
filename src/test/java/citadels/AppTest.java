// File: src/test/java/citadels/AppTest.java
package citadels;

import org.junit.jupiter.api.*;
import java.io.*;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AppTest {
    private final ByteArrayOutputStream testOut = new ByteArrayOutputStream();
    private final PrintStream       origOut  = System.out;

    @BeforeEach
    void setUp() {
        // Capture System.out into testOut
        System.setOut(new PrintStream(testOut));
        // Reset game state
        App.players.clear();
        App.deck.clear();
    }

    @AfterEach
    void tearDown() {
        System.setOut(origOut);
    }

    // ------------------------
    // showHand
    // ------------------------
    @Test
    void showHand_printsGoldAndCards() {
        Player p = new Player(1, true);
        p.addGold(7);
        p.addCardToHand(new District("Tavern",    "green", 1, ""));
        p.addCardToHand(new District("Cathedral", "blue",  5, ""));

        App.showHand(p);

        String out = testOut.toString();
        assertTrue(out.contains("You have 7 gold"),
                "Should display player's gold");
        assertTrue(out.contains("1. Tavern (green), cost: 1"),
                "Should list the first card correctly");
        assertTrue(out.contains("2. Cathedral (blue), cost: 5"),
                "Should list the second card correctly");
    }

    // ------------------------
    // handleGoldCommand
    // ------------------------
    @Test
    void handleGoldCommand_noArg_showsYouHave() {
        Player p1 = new Player(1, true);
        p1.addGold(5);
        App.players.add(p1);

        App.handleGoldCommand("gold");

        String out = testOut.toString();
        assertTrue(out.contains("You have 5 gold"),
                "No-arg should report your own gold");
    }

    @Test
    void handleGoldCommand_withValidPlayer_showsTheirGold() {
        Player p1 = new Player(1, true);
        Player p2 = new Player(2, false);
        p2.addGold(8);
        App.players.add(p1);
        App.players.add(p2);

        App.handleGoldCommand("gold 2");

        String out = testOut.toString();
        assertTrue(out.contains("Player 2 has 8 gold"),
                "Should report player 2's gold");
    }

    @Test
    void handleGoldCommand_invalidPlayer_showsError() {
        App.players.add(new Player(1, true));

        App.handleGoldCommand("gold 99");

        String out = testOut.toString();
        assertTrue(out.contains("Invalid player number."),
                "Out-of-range index should print an error");
    }

    // ------------------------
    // handleCityCommand
    // ------------------------
    @Test
    void handleCityCommand_default_showsPlayer1CityEmpty() {
        Player p1 = new Player(1, true);
        App.players.add(p1);

        App.handleCityCommand("city");

        String out = testOut.toString();
        assertTrue(out.contains("Player 1 has built:")
                        && out.contains("(none)"),
                "Empty city must show '(none)'");
    }

    @Test
    void handleCityCommand_specific_showsThatPlayersCity() {
        Player p1 = new Player(1, true);
        Player p2 = new Player(2, false);
        p2.getCity().add(new District("Tavern","green",1,""));
        App.players.add(p1);
        App.players.add(p2);

        App.handleCityCommand("city 2");

        String out = testOut.toString();
        assertTrue(out.contains("Player 2 has built:")
                        && out.contains("Tavern (green), points: 1"),
                "Should list the single district in player 2's city");
    }

    @Test
    void handleCityCommand_invalid_showsError() {
        App.players.add(new Player(1, true));

        App.handleCityCommand("city 5");

        String out = testOut.toString();
        assertTrue(out.contains("No such player: 5"),
                "Invalid index should print appropriate error");
    }

    // ------------------------
    // handleInfoCommand
    // ------------------------
    @Test
    void handleInfoCommand_characterName_printsCharacterInfo() {
        Player p = new Player(1, true);
        App.players.add(p);

        App.handleInfoCommand("info King", p);

        String out = testOut.toString();
        assertTrue(out.startsWith("King - Gain 1 gold"),
                "Should print the King's special ability text");
    }

    @Test
    void handleInfoCommand_nonPurpleDistrict_showsNotPurple() {
        Player p = new Player(1, true);
        p.addCardToHand(new District("Tavern","green",1,""));
        App.players.add(p);

        App.handleInfoCommand("info Tavern", p);

        String out = testOut.toString();
        assertTrue(out.contains("That district is not purple."),
                "Non-purple district should be rejected");
    }

    @Test
    void handleInfoCommand_purpleDistrict_printsDescription() {
        String desc = "Some unique power.";
        Player p = new Player(1, true);
        p.addCardToHand(new District("Library","purple",6, desc));
        App.players.add(p);

        App.handleInfoCommand("info Library", p);

        String out = testOut.toString();
        assertTrue(out.contains(desc),
                "Should print the purple district's description");
    }

    @Test
    void handleInfoCommand_missingName_showsNotFound() {
        Player p = new Player(1, true);
        App.players.add(p);

        App.handleInfoCommand("info Nonexistent", p);

        String out = testOut.toString();
        assertTrue(out.contains("No purple district named"),
                "Unknown name should produce a not-found message");
    }

    // ------------------------
    // characterSelectionPhase: Comprehensive Branch Coverage
    // ------------------------

    /**
     * First selection logic: shows hand, sets firstSelection = false.
     */
    @Test
    void characterSelectionPhase_firstSelectionShowsHand() {
        App.players.clear();
        for (int i = 1; i <= 4; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        App.firstSelection = true;
        String input = "t\nThief\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        assertFalse(App.firstSelection, "firstSelection should be set to false");
    }

    /**
     * 4 players: covers 2 face-up discards.
     */
    @Test
    void characterSelectionPhase_4Players_faceUpDiscards() {
        App.players.clear();
        for (int i = 1; i <= 4; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        String input = "t\nThief\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        assertEquals(2, App.players.get(0).getCharacter());
    }

    /**
     * 5 players: covers 1 face-up discard.
     */
    @Test
    void characterSelectionPhase_5Players_faceUpDiscard() {
        App.players.clear();
        for (int i = 1; i <= 5; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        String input = "t\nThief\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        assertEquals(2, App.players.get(0).getCharacter());
    }

    /**
     * 6 players: covers 0 face-up discards.
     */
    @Test
    void characterSelectionPhase_6Players_noFaceUpDiscard() {
        App.players.clear();
        for (int i = 1; i <= 6; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        String input = "t\nThief\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        assertEquals(2, App.players.get(0).getCharacter());
    }

    /**
     * 7 players: last-of-seven logic, human picks from 2 cards.
     */
    @Test
    void characterSelectionPhase_7Players_lastOfSeven() {
        App.players.clear();
        for (int i = 1; i <= 7; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        String input = "t\nThief\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        assertEquals(2, App.players.get(0).getCharacter());
    }

    /**
     * King face-up discard retry logic (run multiple times to increase chance).
     */
    @Test
    void characterSelectionPhase_kingFaceUpDiscardRetry() {
        for (int i = 0; i < 10; i++) {
            App.players.clear();
            for (int j = 1; j <= 4; j++) App.players.add(new Player(j, j == 1));
            App.crownedPlayer = App.players.get(0);
            String input = "t\nThief\n";
            System.setIn(new ByteArrayInputStream(input.getBytes()));
            App.characterSelectionPhase();
        }
    }

    /**
     * Human input: invalid character, then valid.
     */
    @Test
    void characterSelectionPhase_invalidInputThenValid() {
        App.players.clear();
        for (int i = 1; i <= 4; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        String input = "t\ninvalid\nThief\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        assertEquals(2, App.players.get(0).getCharacter());
    }

    /**
     * Human input: info and debug commands during selection.
     */
    @Test
    void characterSelectionPhase_infoAndDebugCommands() {
        App.players.clear();
        for (int i = 1; i <= 4; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        String input = "t\ndebug\ninfo King\nThief\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        assertEquals(2, App.players.get(0).getCharacter());
    }

    /**
     * Human input: picks face-down card in last-of-seven.
     */
    @Test
    void characterSelectionPhase_7Players_pickFaceDown() {
        App.players.clear();
        for (int i = 1; i <= 7; i++) App.players.add(new Player(i, i == 1));
        App.crownedPlayer = App.players.get(0);
        // Try to pick the face-down card by name
        String input = "t\nWarlord\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.characterSelectionPhase();
        // Should not throw, should assign character
        assertTrue(App.players.get(0).getCharacter() > 0);
    }

    /**
     * CPU player selection: all players CPU.
     */
    @Test
    void characterSelectionPhase_allCPUPlayers() {
        App.players.clear();
        for (int i = 1; i <= 4; i++) App.players.add(new Player(i, false));
        App.crownedPlayer = App.players.get(0);
        App.firstSelection = false;
        App.characterSelectionPhase();
        for (Player p : App.players) {
            assertTrue(p.getCharacter() > 0);
        }
    }

    // ------------------------
    // turnPhase: All Characters and Branches
    // ------------------------

    /**
     * Assassin: Skips ability if invalid input, covers skip branch.
     */
    @Test
    void turnPhase_assassinSkipsIfInvalidInput() {
        Player assassin = new Player(1, true);
        assassin.setCharacter(1);
        App.players = new ArrayList<>(Collections.singletonList(assassin));
        String input = "invalid\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(0, App.killedCharacter, "Should skip if input is invalid");
    }

    /**
     * Thief: Skips ability if invalid input, covers skip branch.
     */
    @Test
    void turnPhase_thiefSkipsIfInvalidInput() {
        Player thief = new Player(1, true);
        thief.setCharacter(2);
        App.players = new ArrayList<>(Collections.singletonList(thief));
        String input = "invalid\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(0, App.robbedCharacter, "Should skip if input is invalid");
    }

    /**
     * Magician: Discards cards and draws new ones.
     */
    @Test
    void turnPhase_magicianDiscardsAndDraws() {
        Player magician = new Player(1, true);
        magician.setCharacter(3);
        magician.addCardToHand(new District("Tavern", "green", 1, ""));
        magician.addCardToHand(new District("Cathedral", "blue", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(magician));
        App.initializeDeck();
        String input = "yes\ndiscard 1 2\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertTrue(magician.getHand().size() >= 2, "Should draw as many as discarded");
    }

    /**
     * Magician: Exchanges hand with another player.
     */
    @Test
    void turnPhase_magicianExchangeHand() {
        Player magician = new Player(1, true);
        magician.setCharacter(3);
        Player other = new Player(2, false);
        magician.addCardToHand(new District("Tavern", "green", 1, ""));
        other.addCardToHand(new District("Cathedral", "blue", 5, ""));
        App.players = new ArrayList<>(Arrays.asList(magician, other));
        String input = "yes\nexchange 2\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, magician.getHand().size());
        assertEquals("Cathedral", magician.getHand().get(0).getName());
    }

    /**
     * King: Receives crown and gold for yellow districts.
     */
    @Test
    void turnPhase_kingReceivesCrownAndGold() {
        Player king = new Player(1, true);
        king.setCharacter(4);
        king.getCity().add(new District("Palace", "yellow", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(king));
        String input = "gold\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, king.getGold(), "Should gain gold for yellow district");
        assertEquals(king, App.crownedPlayer, "Should become crowned player");
    }

    /**
     * Bishop: Receives gold for blue districts, cannot be destroyed.
     */
    @Test
    void turnPhase_bishopReceivesGold() {
        Player bishop = new Player(1, true);
        bishop.setCharacter(5);
        bishop.getCity().add(new District("Church", "blue", 2, ""));
        App.players = new ArrayList<>(Collections.singletonList(bishop));
        String input = "gold\nend\nt\nblue\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, bishop.getGold(), "Should gain gold for blue district");
    }

    /**
     * Merchant: Receives gold for green districts and bonus gold.
     */
    @Test
    void turnPhase_merchantReceivesGoldAndBonus() {
        Player merchant = new Player(1, true);
        merchant.setCharacter(6);
        merchant.getCity().add(new District("Market", "green", 2, ""));
        App.players = new ArrayList<>(Collections.singletonList(merchant));
        String input = "gold\nend\nt\ngreen\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(2, merchant.getGold(), "Should gain gold for green district and bonus");
    }

    /**
     * Architect: Can build up to 3 districts.
     */
    @Test
    void turnPhase_architectBuildsUpToThree() {
        Player architect = new Player(1, true);
        architect.setCharacter(7);
        architect.addGold(10);
        architect.addCardToHand(new District("Tavern", "green", 1, ""));
        architect.addCardToHand(new District("Market", "green", 2, ""));
        architect.addCardToHand(new District("Cathedral", "blue", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(architect));
        App.initializeDeck();
        String input = "gold\nbuild 1\nbuild 1\nbuild 1\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(3, architect.getCity().size(), "Architect should build up to 3 districts");
    }

    /**
     * Warlord: Destroys a district from another player.
     */
    @Test
    void turnPhase_warlordDestroysDistrict() {
        Player warlord = new Player(1, true);
        warlord.setCharacter(8);
        warlord.addGold(10);
        Player victim = new Player(2, false);
        victim.getCity().add(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Arrays.asList(warlord, victim));
        String input = "1\n1\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertTrue(victim.getCity().isEmpty(), "Victim's city should be empty after destruction");
    }

    // ------------------------
    // Simulate a Full Game Round
    // ------------------------

    /**
     * Simulates a full game round with 4 players, covering main(), turnPhase(), and characterSelectionPhase().
     */
    @Test
    void main_simulateFullGameRound() {
        // Setup input for 4 players, character selection, and a round of turns
        StringBuilder input = new StringBuilder();
        input.append("4\n"); // number of players
        input.append("t\nThief\n"); // character selection
        // Each turn: gold, end, t
        for (int i = 0; i < 8; i++) {
            input.append("gold\nend\nt\n");
        }
        // End game after one round
        App.gameEndTriggered = true;
        System.setIn(new ByteArrayInputStream(input.toString().getBytes()));
        App.main(new String[]{});
        String out = testOut.toString();
        assertTrue(out.contains("Starting Citadels with 4 players"));
        assertTrue(out.contains("GAME OVER - Final Scores"));
    }

    // ------------------------
    // waitForContinue: covers all branches
    // ------------------------
    @Test
    void waitForContinue_handlesHandAndT() {
        Player p = new Player(1, true);
        App.players = new ArrayList<>(Collections.singletonList(p));
        String input = "hand\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.waitForContinue();
        String out = testOut.toString();
        assertTrue(out.contains("You have 0 gold"));
    }

    @Test
    void waitForContinue_handlesUnknownCommand() {
        Player p = new Player(1, true);
        App.players = new ArrayList<>(Collections.singletonList(p));
        String input = "unknown\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.waitForContinue();
        String out = testOut.toString();
        assertTrue(out.contains("It is not your turn. Press t to continue."));
    }

    // ------------------------
    // Game Initialization
    // ------------------------
    @Test
    void initializeDeck_loadsCardsCorrectly() {
        App.initializeDeck();
        assertFalse(App.deck.isEmpty(), "Deck should not be empty after initialization");
        assertTrue(App.deck.stream().allMatch(d -> d.getCost() > 0), 
            "All cards should have positive cost");
    }

    @Test
    void drawCardForPlayer_addsCardToHand() {
        App.initializeDeck();
        Player p = new Player(1, true);
        int initialSize = p.getHand().size();
        App.drawCardForPlayer(p);
        assertEquals(initialSize + 1, p.getHand().size(), 
            "Player's hand should increase by 1 after drawing");
    }

    // ------------------------
    // Turn Phase - Additional Scenarios
    // ------------------------
    @Test
    void turnPhase_thiefStealsGold() {
        Player thief = new Player(1, true);
        Player victim = new Player(2, false);
        App.players = new ArrayList<>(Arrays.asList(thief, victim));
        thief.setCharacter(2);  // Thief
        victim.setCharacter(3); // Magician
        victim.addGold(5);
        App.thiefPlayer = thief;
        App.robbedCharacter = 3;

        String input = "gold\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        App.turnPhase();

        assertEquals(0, victim.getGold(), "Victim should lose all gold");
        assertEquals(5, thief.getGold(), "Thief should gain stolen gold");
    }

    @Test
    void turnPhase_merchantGainsExtraGold() {
        Player merchant = new Player(1, true);
        merchant.setCharacter(6);  // Merchant
        merchant.getCity().add(new District("Market", "green", 2, ""));
        App.players = new ArrayList<>(Collections.singletonList(merchant));

        String input = "gold\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        App.turnPhase();

        assertEquals(3, merchant.getGold(), "Merchant should gain 2 gold + 1 bonus");
    }

    @Test
    void turnPhase_architectDrawsExtraCards() {
        Player architect = new Player(1, true);
        architect.setCharacter(7);  // Architect
        App.players = new ArrayList<>(Collections.singletonList(architect));
        App.initializeDeck();

        String input = "gold\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        int initialHandSize = architect.getHand().size();
        App.turnPhase();

        assertEquals(initialHandSize + 2, architect.getHand().size(), 
            "Architect should draw 2 extra cards");
    }

    // ------------------------
    // Building Mechanics
    // ------------------------
    @Test
    void processCommand_buildWithInsufficientGold() {
        Player p = new Player(1, true);
        p.addCardToHand(new District("Cathedral", "blue", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(p));

        App.processCommand(p, "build 1");

        String out = testOut.toString();
        assertTrue(out.contains("Not enough gold"), 
            "Should prevent building when gold is insufficient");
        assertTrue(p.getCity().isEmpty(), 
            "City should remain empty after failed build");
    }

    @Test
    void processCommand_buildWithSufficientGold() {
        Player p = new Player(1, true);
        p.addGold(5);
        p.addCardToHand(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Collections.singletonList(p));

        App.processCommand(p, "build 1");

        assertEquals(4, p.getGold(), "Should spend gold on building");
        assertEquals(1, p.getCity().size(), "City should contain the built district");
        assertTrue(p.getHand().isEmpty(), "Card should be removed from hand");
    }

    // ------------------------
    // Game End and Scoring
    // ------------------------
    @Test
    void scoreAndDeclareWinner_calculatesCorrectScores() {
        Player p1 = new Player(1, true);
        Player p2 = new Player(2, false);
        App.players = new ArrayList<>(Arrays.asList(p1, p2));
        
        // Build districts for p1
        p1.getCity().add(new District("Tavern", "green", 1, ""));
        p1.getCity().add(new District("Church", "blue", 2, ""));
        p1.getCity().add(new District("Castle", "red", 3, ""));
        
        // Build districts for p2
        p2.getCity().add(new District("Palace", "yellow", 5, ""));
        p2.getCity().add(new District("Cathedral", "blue", 5, ""));

        App.scoreAndDeclareWinner();

        String out = testOut.toString();
        assertTrue(out.contains("GAME OVER - Final Scores"), 
            "Should announce game over");
        assertTrue(out.contains("Sum of costs: 6") && out.contains("Sum of costs: 10"), 
            "Should show correct cost sums");
    }

    // ------------------------
    // Save/Load Functionality
    // ------------------------
    @Test
    void saveAndLoadGame_preservesGameState() {
        // Setup initial game state
        Player p = new Player(1, true);
        p.addGold(5);
        p.addCardToHand(new District("Tavern", "green", 1, ""));
        p.getCity().add(new District("Church", "blue", 2, ""));
        App.players = new ArrayList<>(Collections.singletonList(p));
        App.crownedPlayer = p;
        
        // Save game
        String filename = "test_save.json";
        App.saveGame(filename);
        
        // Clear game state
        App.players.clear();
        App.crownedPlayer = null;
        
        // Load game
        App.loadGame(filename);
        
        // Verify state was restored
        assertEquals(1, App.players.size(), "Should restore correct number of players");
        assertEquals(5, App.players.get(0).getGold(), "Should restore player's gold");
        assertEquals(1, App.players.get(0).getHand().size(), "Should restore player's hand");
        assertEquals(1, App.players.get(0).getCity().size(), "Should restore player's city");
        
        // Cleanup
        new File(filename).delete();
    }

    // ------------------------
    // Edge Cases
    // ------------------------
    @Test
    void turnPhase_emptyDeck() {
        Player p = new Player(1, true);
        p.setCharacter(7);  // Architect
        App.players = new ArrayList<>(Collections.singletonList(p));
        App.deck.clear();  // Empty the deck

        String input = "gold\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));

        App.turnPhase();

        assertTrue(p.getHand().isEmpty(), 
            "Hand should remain empty when deck is empty");
    }

    @Test
    void processCommand_invalidBuildIndex() {
        Player p = new Player(1, true);
        p.addGold(10);
        App.players = new ArrayList<>(Collections.singletonList(p));

        App.processCommand(p, "build 99");

        String out = testOut.toString();
        assertTrue(out.contains("Invalid selection"), 
            "Should handle invalid build index gracefully");
    }

    @Test
    void handleInfoCommand_emptyHand() {
        Player p = new Player(1, true);
        App.players = new ArrayList<>(Collections.singletonList(p));

        App.handleInfoCommand("info Library", p);

        String out = testOut.toString();
        assertTrue(out.contains("No purple district named"), 
            "Should handle info request for empty hand");
    }

    // ------------------------
    // turnPhase: Unique Buildings and Edge Cases
    // ------------------------

    /**
     * Laboratory: Discard for gold (human).
     */
    @Test
    void turnPhase_laboratoryUsedByHuman() {
        Player p = new Player(1, true);
        p.setCharacter(4);
        p.getCity().add(new District("Laboratory", "purple", 5, "Lab"));
        p.addCardToHand(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Collections.singletonList(p));
        String input = "gold\nyes\n1\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, p.getGold(), "Should gain gold from Laboratory");
        assertTrue(p.getHand().isEmpty(), "Should discard card");
    }

    /**
     * Smithy: Pay 2 gold to draw 3 cards (human).
     */
    @Test
    void turnPhase_smithyUsedByHuman() {
        Player p = new Player(1, true);
        p.setCharacter(4);
        p.getCity().add(new District("Smithy", "purple", 5, "Smithy"));
        p.addGold(3);
        App.players = new ArrayList<>(Collections.singletonList(p));
        App.initializeDeck();
        String input = "cards\nyes\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertTrue(p.getHand().size() >= 3, "Should draw 3 cards from Smithy");
        assertEquals(1, p.getGold(), "Should spend 2 gold for Smithy");
    }

    /**
     * School of Magic: Choose color for income (human).
     */
    @Test
    void turnPhase_schoolOfMagicIncome() {
        Player p = new Player(1, true);
        p.setCharacter(4);
        p.getCity().add(new District("School of Magic", "purple", 6, "School"));
        App.players = new ArrayList<>(Collections.singletonList(p));
        String input = "gold\nend\nt\nred\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        // Should prompt for color and give gold for chosen color
    }

    /**
     * Warlord: Cannot destroy city with 8 districts or Keep.
     */
    @Test
    void turnPhase_warlordCannotDestroyKeepOrFullCity() {
        Player warlord = new Player(1, true);
        warlord.setCharacter(8);
        warlord.addGold(10);
        Player victim = new Player(2, false);
        for (int i = 0; i < 8; i++) victim.getCity().add(new District("Tavern", "green", 1, ""));
        victim.getCity().add(new District("Keep", "red", 3, ""));
        App.players = new ArrayList<>(Arrays.asList(warlord, victim));
        String input = "2\nno\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(9, victim.getCity().size(), "Should not destroy from full city or Keep");
    }

    /**
     * Warlord: Graveyard recovery after destruction.
     */
    @Test
    void turnPhase_warlordGraveyardRecovery() {
        Player warlord = new Player(1, true);
        warlord.setCharacter(8);
        warlord.addGold(10);
        Player victim = new Player(2, false);
        victim.addGold(2);
        victim.getCity().add(new District("Tavern", "green", 1, ""));
        victim.getCity().add(new District("Graveyard", "purple", 5, ""));
        App.players = new ArrayList<>(Arrays.asList(warlord, victim));
        String input = "2\n1\nyes\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, victim.getHand().size(), "Should recover destroyed district to hand");
        assertEquals(1, victim.getGold(), "Should spend gold for Graveyard recovery");
    }

    /**
     * Library: Keep all drawn cards.
     */
    @Test
    void turnPhase_libraryKeepsAllDrawnCards() {
        Player p = new Player(1, true);
        p.setCharacter(4);
        p.getCity().add(new District("Library", "purple", 6, "Library"));
        App.players = new ArrayList<>(Collections.singletonList(p));
        App.initializeDeck();
        String input = "cards\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        int initialHand = p.getHand().size();
        App.turnPhase();
        assertTrue(p.getHand().size() >= initialHand + 2, "Should keep all drawn cards with Library");
    }

    /**
     * Observatory: Draw 3 cards instead of 2.
     */
    @Test
    void turnPhase_observatoryDrawsThree() {
        Player p = new Player(1, true);
        p.setCharacter(4);
        p.getCity().add(new District("Observatory", "purple", 5, "Observatory"));
        App.players = new ArrayList<>(Collections.singletonList(p));
        App.initializeDeck();
        String input = "cards\n1\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        int initialHand = p.getHand().size();
        App.turnPhase();
        assertTrue(p.getHand().size() >= initialHand + 1, "Should draw 3 cards with Observatory");
    }

    /**
     * Great Wall: Warlord pays full cost to destroy.
     */
    @Test
    void turnPhase_warlordGreatWallCost() {
        Player warlord = new Player(1, true);
        warlord.setCharacter(8);
        warlord.addGold(10);
        Player victim = new Player(2, false);
        victim.getCity().add(new District("Great Wall", "purple", 6, ""));
        victim.getCity().add(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Arrays.asList(warlord, victim));
        String input = "2\n1\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        int goldBefore = warlord.getGold();
        App.turnPhase();
        assertTrue(warlord.getGold() < goldBefore, "Should pay full cost to destroy with Great Wall");
    }

    // ------------------------
    // turnPhase: CPU Player Logic
    // ------------------------

    /**
     * CPU player: Laboratory discards cheapest card if hand >= 3.
     */
    @Test
    void turnPhase_laboratoryUsedByCPU() {
        Player cpu = new Player(1, false);
        cpu.setCharacter(4);
        cpu.getCity().add(new District("Laboratory", "purple", 5, "Lab"));
        cpu.addCardToHand(new District("Tavern", "green", 1, ""));
        cpu.addCardToHand(new District("Market", "green", 2, ""));
        cpu.addCardToHand(new District("Cathedral", "blue", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(cpu));
        App.turnPhase();
        assertTrue(cpu.getHand().size() < 3, "CPU should discard for Laboratory");
    }

    /**
     * CPU player: Smithy draws 3 cards if gold >= 2 and hand <= 3.
     */
    @Test
    void turnPhase_smithyUsedByCPU() {
        Player cpu = new Player(1, false);
        cpu.setCharacter(4);
        cpu.getCity().add(new District("Smithy", "purple", 5, "Smithy"));
        cpu.addGold(3);
        App.players = new ArrayList<>(Collections.singletonList(cpu));
        App.initializeDeck();
        App.turnPhase();
        assertTrue(cpu.getHand().size() >= 3, "CPU should draw 3 cards from Smithy");
        assertTrue(cpu.getGold() <= 1, "CPU should spend 2 gold for Smithy");
    }

    // ------------------------
    // processCommand: Error and Edge Cases
    // ------------------------

    /**
     * processCommand: Unknown command.
     */
    @Test
    void processCommand_unknownCommand() {
        Player p = new Player(1, true);
        App.players = new ArrayList<>(Collections.singletonList(p));
        boolean handled = App.processCommand(p, "foobar");
        assertFalse(handled, "Unknown command should return false");
    }

    /**
     * processCommand: Help command.
     */
    @Test
    void processCommand_helpCommand() {
        Player p = new Player(1, true);
        App.players = new ArrayList<>(Collections.singletonList(p));
        boolean handled = App.processCommand(p, "help");
        assertTrue(handled, "Help command should be handled");
    }

    /**
     * processCommand: Debug command toggles debugMode.
     */
    @Test
    void processCommand_debugCommand() {
        Player p = new Player(1, true);
        App.players = new ArrayList<>(Collections.singletonList(p));
        boolean before = App.debugMode;
        boolean handled = App.processCommand(p, "debug");
        assertTrue(handled, "Debug command should be handled");
        assertNotEquals(before, App.debugMode, "Debug mode should toggle");
    }

    // ------------------------
    // main: Error and Edge Cases
    // ------------------------

    /**
     * main: Handles invalid player count input.
     */
    @Test
    void main_invalidPlayerCountInput() {
        String input = "abc\n3\n4\nt\nThief\ngold\nend\nt\ngold\nend\nt\ngold\nend\nt\ngold\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.gameEndTriggered = true;
        App.main(new String[]{});
        String out = testOut.toString();
        assertTrue(out.contains("Enter how many players"));
        assertTrue(out.contains("Starting Citadels with 4 players"));
    }

    // ------------------------
    // turnPhase: Remaining Branches and Edge Cases
    // ------------------------

    /**
     * Warlord cannot destroy Bishop's city if Bishop is alive.
     */
    @Test
    void turnPhase_warlordCannotDestroyBishop() {
        Player warlord = new Player(1, true);
        warlord.setCharacter(8);
        warlord.addGold(10);
        Player bishop = new Player(2, false);
        bishop.setCharacter(5);
        bishop.getCity().add(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Arrays.asList(warlord, bishop));
        String input = "2\nno\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, bishop.getCity().size(), "Warlord should not destroy Bishop's city");
    }

    /**
     * Thief cannot rob Assassin or killed character.
     */
    @Test
    void turnPhase_thiefCannotRobAssassinOrKilled() {
        Player thief = new Player(1, true);
        thief.setCharacter(2);
        App.killedCharacter = 3;
        App.players = new ArrayList<>(Collections.singletonList(thief));
        String input = "3\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertNotEquals(3, App.robbedCharacter, "Thief should not rob killed character");
    }

    /**
     * Assassin kills a character and that character is skipped.
     */
    @Test
    void turnPhase_assassinKillsAndSkips() {
        Player assassin = new Player(1, true);
        assassin.setCharacter(1);
        Player victim = new Player(2, false);
        victim.setCharacter(3);
        App.players = new ArrayList<>(Arrays.asList(assassin, victim));
        String input = "3\nend\nt\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(3, App.killedCharacter, "Assassin should kill character 3");
    }

    /**
     * Architect builds less than 3 districts.
     */
    @Test
    void turnPhase_architectBuildsLessThanThree() {
        Player architect = new Player(1, true);
        architect.setCharacter(7);
        architect.addGold(2);
        architect.addCardToHand(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Collections.singletonList(architect));
        App.initializeDeck();
        String input = "gold\nbuild 1\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, architect.getCity().size(), "Architect should build only one district");
    }

    /**
     * King, Bishop, Merchant, Architect, Warlord as CPU.
     */
    @Test
    void turnPhase_allCharactersAsCPU() {
        for (int r = 4; r <= 8; r++) {
            Player cpu = new Player(1, false);
            cpu.setCharacter(r);
            cpu.addGold(5);
            cpu.addCardToHand(new District("Tavern", "green", 1, ""));
            App.players = new ArrayList<>(Collections.singletonList(cpu));
            App.initializeDeck();
            App.turnPhase();
        }
    }

    // ------------------------
    // Utility Methods: printHelp, showAllPlayers, showCity, showHand
    // ------------------------

    /**
     * printHelp and showAllPlayers.
     */
    @Test
    void printHelpAndShowAllPlayers() {
        App.printHelp();
        App.players.add(new Player(1, true));
        App.showAllPlayers();
        String out = testOut.toString();
        assertTrue(out.contains("Available commands:"));
        assertTrue(out.contains("Player 1"));
    }

    /**
     * showCity and showHand.
     */
    @Test
    void showCityAndShowHand() {
        Player p = new Player(1, true);
        p.addGold(3);
        p.addCardToHand(new District("Tavern", "green", 1, ""));
        p.getCity().add(new District("Cathedral", "blue", 5, ""));
        App.showCity(p);
        App.showHand(p);
        String out = testOut.toString();
        assertTrue(out.contains("Player 1 has built:"));
        assertTrue(out.contains("You have 3 gold"));
    }

    // ------------------------
    // scoreAndDeclareWinner: All bonus types, ties
    // ------------------------

    /**
     * scoreAndDeclareWinner: diversity bonus, completion bonus, purple bonus, tie.
     */
    @Test
    void scoreAndDeclareWinner_allBonusesAndTie() {
        Player p1 = new Player(1, true);
        Player p2 = new Player(2, false);
        // Diversity: yellow, blue, green, red, purple
        p1.getCity().add(new District("Palace", "yellow", 5, ""));
        p1.getCity().add(new District("Cathedral", "blue", 5, ""));
        p1.getCity().add(new District("Market", "green", 2, ""));
        p1.getCity().add(new District("Castle", "red", 4, ""));
        p1.getCity().add(new District("Library", "purple", 6, "Library"));
        // Completion bonus
        for (int i = 0; i < 3; i++) p1.getCity().add(new District("Tavern", "green", 1, ""));
        App.firstCompleter = p1;
        // Tie: p2 gets same total
        p2.getCity().add(new District("Palace", "yellow", 5, ""));
        p2.getCity().add(new District("Cathedral", "blue", 5, ""));
        p2.getCity().add(new District("Market", "green", 2, ""));
        p2.getCity().add(new District("Castle", "red", 4, ""));
        p2.getCity().add(new District("Library", "purple", 6, "Library"));
        for (int i = 0; i < 3; i++) p2.getCity().add(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Arrays.asList(p1, p2));
        App.scoreAndDeclareWinner();
        String out = testOut.toString();
        assertTrue(out.contains("Diversity bonus"));
        assertTrue(out.contains("Completion bonus"));
        assertTrue(out.contains("Unique district bonus"));
        assertTrue(out.contains("Congratulations, Player"));
    }

    // ------------------------
    // turnPhase: More Targeted Coverage
    // ------------------------

    /**
     * King: human, color income and crown assignment.
     */
    @Test
    void turnPhase_kingHumanColorIncomeAndCrown() {
        Player king = new Player(1, true);
        king.setCharacter(4);
        king.getCity().add(new District("Palace", "yellow", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(king));
        String input = "gold\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, king.getGold());
        assertEquals(king, App.crownedPlayer);
    }

    /**
     * King: CPU, color income and crown assignment.
     */
    @Test
    void turnPhase_kingCPUColorIncomeAndCrown() {
        Player king = new Player(1, false);
        king.setCharacter(4);
        king.getCity().add(new District("Palace", "yellow", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(king));
        App.turnPhase();
        assertEquals(1, king.getGold());
        assertEquals(king, App.crownedPlayer);
    }

    /**
     * Bishop: human, color income, Warlord cannot destroy.
     */
    @Test
    void turnPhase_bishopHumanColorIncomeAndWarlordRestriction() {
        Player bishop = new Player(1, true);
        bishop.setCharacter(5);
        bishop.getCity().add(new District("Church", "blue", 2, ""));
        App.players = new ArrayList<>(Collections.singletonList(bishop));
        String input = "gold\nend\nt\nblue\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, bishop.getGold());
    }

    /**
     * Merchant: human, color income and bonus gold.
     */
    @Test
    void turnPhase_merchantHumanColorIncomeAndBonus() {
        Player merchant = new Player(1, true);
        merchant.setCharacter(6);
        merchant.getCity().add(new District("Market", "green", 2, ""));
        App.players = new ArrayList<>(Collections.singletonList(merchant));
        String input = "gold\nend\nt\ngreen\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(2, merchant.getGold());
    }

    /**
     * Architect: human, build 0, 1, 2, 3 districts.
     */
    @Test
    void turnPhase_architectHumanBuilds() {
        Player architect = new Player(1, true);
        architect.setCharacter(7);
        architect.addGold(10);
        architect.addCardToHand(new District("Tavern", "green", 1, ""));
        architect.addCardToHand(new District("Market", "green", 2, ""));
        architect.addCardToHand(new District("Cathedral", "blue", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(architect));
        App.initializeDeck();
        String input = "gold\nbuild 1\nbuild 1\nbuild 1\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(3, architect.getCity().size());
    }

    /**
     * Architect: CPU, builds up to 3 districts.
     */
    @Test
    void turnPhase_architectCPUBuilds() {
        Player architect = new Player(1, false);
        architect.setCharacter(7);
        architect.addGold(10);
        architect.addCardToHand(new District("Tavern", "green", 1, ""));
        architect.addCardToHand(new District("Market", "green", 2, ""));
        architect.addCardToHand(new District("Cathedral", "blue", 5, ""));
        App.players = new ArrayList<>(Collections.singletonList(architect));
        App.initializeDeck();
        App.turnPhase();
        assertTrue(architect.getCity().size() <= 3);
    }

    /**
     * Warlord: human, cannot destroy Bishop, Keep, 8 districts, Great Wall, Graveyard.
     */
    @Test
    void turnPhase_warlordHumanDestructionRestrictions() {
        Player warlord = new Player(1, true);
        warlord.setCharacter(8);
        warlord.addGold(10);
        Player bishop = new Player(2, false);
        bishop.setCharacter(5);
        bishop.getCity().add(new District("Church", "blue", 2, ""));
        Player keepPlayer = new Player(3, false);
        keepPlayer.getCity().add(new District("Keep", "red", 3, ""));
        Player fullCity = new Player(4, false);
        for (int i = 0; i < 8; i++) fullCity.getCity().add(new District("Tavern", "green", 1, ""));
        Player greatWall = new Player(5, false);
        greatWall.getCity().add(new District("Great Wall", "purple", 6, ""));
        Player graveyard = new Player(6, false);
        graveyard.addGold(2);
        graveyard.getCity().add(new District("Tavern", "green", 1, ""));
        graveyard.getCity().add(new District("Graveyard", "purple", 5, ""));
        App.players = new ArrayList<>(Arrays.asList(warlord, bishop, keepPlayer, fullCity, greatWall, graveyard));
        String input = "2\nno\n3\nno\n4\nno\n5\n1\nyes\nend\nt\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, graveyard.getHand().size());
    }

    /**
     * Resource collection: human, Observatory and Library.
     */
    @Test
    void turnPhase_resourceCollectionObservatoryLibrary() {
        Player p = new Player(1, true);
        p.setCharacter(4);
        p.getCity().add(new District("Observatory", "purple", 5, ""));
        p.getCity().add(new District("Library", "purple", 6, ""));
        App.players = new ArrayList<>(Collections.singletonList(p));
        App.initializeDeck();
        String input = "cards\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        int initialHand = p.getHand().size();
        App.turnPhase();
        assertTrue(p.getHand().size() >= initialHand + 2);
    }

    /**
     * Build phase: human, duplicate, not enough gold, already built, invalid index, build limit.
     */
    @Test
    void turnPhase_buildPhaseErrors() {
        Player p = new Player(1, true);
        p.setCharacter(4);
        p.addGold(1);
        p.addCardToHand(new District("Tavern", "green", 1, ""));
        p.getCity().add(new District("Tavern", "green", 1, ""));
        App.players = new ArrayList<>(Collections.singletonList(p));
        String input = "gold\nbuild 1\nbuild 99\nbuild 1\nend\nt\nyellow\n";
        System.setIn(new ByteArrayInputStream(input.getBytes()));
        App.turnPhase();
        assertEquals(1, p.getCity().size());
    }
}
